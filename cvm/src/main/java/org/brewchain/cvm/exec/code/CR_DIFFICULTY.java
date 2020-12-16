package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_DIFFICULTY extends AbstractCodeRunner {

	public CR_DIFFICULTY(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord difficulty = DataWord.ZERO.clone();

		if (hint != null) {
			hint.append("difficulty: " + difficulty);
		}

		program.stackPush(difficulty);
		program.step();
		return 0;
	}

}
