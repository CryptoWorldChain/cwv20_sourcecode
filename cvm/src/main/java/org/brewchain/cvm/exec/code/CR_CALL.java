package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.exec.PrecompiledContracts;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

import javax.xml.crypto.Data;

public class CR_CALL extends AbstractCodeRunner {

	public CR_CALL(OpCode op) {
		super(op);
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		int gas_cost = 1;
		program.stackPop(); // use adjustedCallGas instead of requested
		DataWord codeAddress = program.stackPop();
		DataWord value = op.callHasValue() ? program.stackPop() : DataWord.ZERO;

		DataWord inDataOffs = program.stackPop();
		DataWord inDataSize = program.stackPop();

		DataWord outDataOffs = program.stackPop();
		DataWord outDataSize = program.stackPop();
//		if (hint != null) {
			hint.append("call addr: " + Hex.toHexString(codeAddress.getLast20Bytes()) + " gas: " + 0 + " inOff: "
					+ inDataOffs.shortHex() + " inSize: " + inDataSize.shortHex()
				+" value: "+value.shortHex());
			// log.info(logString, String.format("%5s", "[" + program.getPC() + "]"),
			// String.format("%-12s", op.name()), 0, program.getCallDeep(), hint);
//		}
		program.memoryExpand(outDataOffs, outDataSize);

		PrecompiledContracts.PrecompiledContract contract = PrecompiledContracts
				.getContractForAddress(program.getMcore(), codeAddress);

		// 调用内部合约
		if (contract != null) {
			program.callToPrecompiledAddress(inDataOffs, inDataSize, outDataOffs, outDataSize, contract);
		} else {//调用外部合约
			gas_cost = program.callToAddress(op, codeAddress, inDataOffs, inDataSize, outDataOffs, outDataSize,value);
		}

		program.step();
		return gas_cost;
	}

}
