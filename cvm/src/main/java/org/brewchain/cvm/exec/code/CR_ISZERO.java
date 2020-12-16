package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_ISZERO extends AbstractCodeRunner {

	public CR_ISZERO(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		if (word1.isZero()) {
			word1.getData()[31] = 1;
		} else {
			word1.and(DataWord.ZERO);
		}

		if (hint != null) {
			hint.append(word1.value());
		}

		program.stackPush(word1);
		program.step();
		return 1;
	}

}
