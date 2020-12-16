package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_POP extends AbstractCodeRunner {

	public CR_POP(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		program.stackPop();
		program.step();
		return 0;
	}

}
