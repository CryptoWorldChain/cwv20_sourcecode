package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_CREATE extends AbstractCodeRunner {

	public CR_CREATE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {

		DataWord value = program.stackPop();
		DataWord inOffset = program.stackPop();
		DataWord inSize = program.stackPop();

		if (hint != null) {
			hint.append(String.format("%5s", "[" + program.getPC() + "]") + String.format("%-12s", op) + ","
					+ program.getCallDeep());
		}
//		program.createContract(value, inOffset, inSize);
//
		program.step();
		
		return 0;
	}

}
