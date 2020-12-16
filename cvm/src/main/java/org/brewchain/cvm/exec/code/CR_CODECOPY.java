package org.brewchain.cvm.exec.code;

import static org.brewchain.cvm.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_CODECOPY extends AbstractCodeRunner {

	public CR_CODECOPY(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		byte[] fullCode = EMPTY_BYTE_ARRAY;
		// if (op == OpCode.CODECOPY)
		fullCode = program.getContractAccount().getCompiledProgram().getOps();

		// if (op == OpCode.EXTCODECOPY) {
		// DataWord address = program.stackPop();
		// fullCode = program.getCodeAt(address);
		// }

		int memOffset = program.stackPop().intValueSafe();
		int codeOffset = program.stackPop().intValueSafe();
		int lengthData = program.stackPop().intValueSafe();

		int sizeToBeCopied = (long) codeOffset + lengthData > fullCode.length
				? (fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset)
				: lengthData;

		byte[] codeCopy = new byte[lengthData];

		if (codeOffset < fullCode.length)
			System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);

		if (hint != null) {
			hint.append("code: " + Hex.toHexString(codeCopy));
		}

		program.memorySave(memOffset, codeCopy);
		program.step();
		return Math.max(10,fullCode.length/100) + 1;
	}

}
