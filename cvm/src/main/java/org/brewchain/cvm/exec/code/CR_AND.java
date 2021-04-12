package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_AND extends AbstractCodeRunner {

	public CR_AND(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		DataWord word2 = program.stackPop();
		if (hint != null) {
			hint.append(word1.value().toString(16)).append(" && ").append(word2.value().toString(16));
		}
		word1.and(word2);
		program.stackPush(word1);
		program.step();
		return 1;
	}

}
