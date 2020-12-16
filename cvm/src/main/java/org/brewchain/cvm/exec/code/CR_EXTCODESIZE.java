package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_EXTCODESIZE extends AbstractCodeRunner {

	public CR_EXTCODESIZE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		int length;
		DataWord address = program.stackPop();
		length = program.getCodeAt(address).length;
		DataWord codeLength = new DataWord(length);

		if (hint != null) {
			hint.append("size: " + length);
		}

		program.stackPush(codeLength);
		program.step();
		return 0;
	}

}
