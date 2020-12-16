package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_JUMPDEST extends AbstractCodeRunner {

	public CR_JUMPDEST(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		program.step();
		return 1;
	}

}
