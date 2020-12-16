package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_GASLIMIT extends AbstractCodeRunner {

	public CR_GASLIMIT(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord gaslimit = DataWord.ZERO;

		if (hint != null) {
			hint.append("gaslimit: " + gaslimit);
		}

		program.stackPush(gaslimit);
		program.step();
		return 0;
	}

}
