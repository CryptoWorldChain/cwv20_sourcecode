package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_ADDMOD extends AbstractCodeRunner {

	public CR_ADDMOD(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		DataWord word2 = program.stackPop();
		DataWord word3 = program.stackPop();
		word1.addmod(word2, word3);
		program.stackPush(word1);
		program.step();
		return 1;
	}

}
