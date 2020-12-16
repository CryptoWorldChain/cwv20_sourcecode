package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_MSIZE extends AbstractCodeRunner {


	public CR_MSIZE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		int memSize = program.getMemSize();
		DataWord wordMemSize = new DataWord(memSize);

		if (hint != null) {
			hint.append(memSize);
		}

		program.stackPush(wordMemSize);
		program.step();
		return 1;
	}

}
