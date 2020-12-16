package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_GT extends AbstractCodeRunner {

	public CR_GT(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		DataWord word2 = program.stackPop();

		if (hint != null) {
			hint.append(word1.value() + " > " + word2.value());
		}

		if (word1.value().compareTo(word2.value()) == 1) {
			word1.and(DataWord.ZERO);
			word1.getData()[31] = 1;
		} else {
			word1.and(DataWord.ZERO);
		}
		program.stackPush(word1);
		program.step();
		return 1;
	}

}
