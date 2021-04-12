package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_NUMBER extends AbstractCodeRunner {

	public CR_NUMBER(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord number = program.getNumber();

//		if (hint!=null)
		{
			hint.append("number: " + number.value());
		}

		program.stackPush(number);
		program.step();
		
		return 0;
	}

}
