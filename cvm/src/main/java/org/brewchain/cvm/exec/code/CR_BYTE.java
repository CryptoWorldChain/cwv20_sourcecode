package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_BYTE extends AbstractCodeRunner {

	public CR_BYTE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		DataWord word2 = program.stackPop();
		final DataWord result;
		if (word1.value().compareTo(_32_) == -1) {
			byte tmp = word2.getData()[word1.intValue()];
			word2.and(DataWord.ZERO);
			word2.getData()[31] = tmp;
			result = word2;
		} else {
			result = new DataWord();
		}

		if (hint != null) {
			hint.append(result.value());
		}

		program.stackPush(result);
		program.step();
		return 1;
	}

}
