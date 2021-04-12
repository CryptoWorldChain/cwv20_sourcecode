package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_NOT extends AbstractCodeRunner {

	public CR_NOT(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		word1.bnot();

//		if (hint!=null)
		{
			hint.append(word1.value());
		}

		program.stackPush(word1);
		program.step();
		return 1;
	}

}
