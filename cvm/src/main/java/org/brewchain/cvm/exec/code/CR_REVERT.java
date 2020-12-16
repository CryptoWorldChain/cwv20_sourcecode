package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_REVERT extends CR_RETURN {

	public CR_REVERT(OpCode op) {
		super(op);
	}

	@Override
	public int exec(Program program, Stack stack, StringBuffer hint) {
		super.exec(program, stack, hint);
		program.getResult().setRevert();
		return 1;
	}


}
