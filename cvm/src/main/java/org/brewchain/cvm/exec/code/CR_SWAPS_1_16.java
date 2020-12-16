package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_SWAPS_1_16 extends AbstractCodeRunner {

	public CR_SWAPS_1_16(OpCode op) {
		super(op);
	}

	int n = op.val() - OpCode.SWAP1.val() + 2;

	public int exec(Program program, Stack stack, StringBuffer hint) {

		stack.swap(stack.size() - 1, stack.size() - n);
		program.step();
		
		return 1;
	}

}
