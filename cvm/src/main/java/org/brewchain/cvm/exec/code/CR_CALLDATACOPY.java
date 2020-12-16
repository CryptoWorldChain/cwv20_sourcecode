package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_CALLDATACOPY extends AbstractCodeRunner {

	public CR_CALLDATACOPY(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord memOffsetData = program.stackPop();
		DataWord dataOffsetData = program.stackPop();
		DataWord lengthData = program.stackPop();

		byte[] msgData = program.getDataCopy(dataOffsetData, lengthData);

		if (hint!=null)
		{
			hint.append("data: " + Hex.toHexString(msgData));
		}

		program.memorySave(memOffsetData.intValueSafe(), msgData);
		program.step();
		return 1;
	}

}
