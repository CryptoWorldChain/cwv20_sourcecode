package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_DUPS_1_16 extends AbstractCodeRunner {

	public CR_DUPS_1_16(OpCode op) {
		super(op);
	}

	int n = op.val() - OpCode.DUP1.val() + 1;

	public int exec(Program program, Stack stack, StringBuffer hint) {

		DataWord word_1 = stack.get(stack.size() - n);
		program.stackPush(word_1.clone());
		hint.append("DUPS: " + n + ",stack size="+stack.size() + ",top="+stack.peek().toString());

		program.step();
		
		return 1;
	}

}
