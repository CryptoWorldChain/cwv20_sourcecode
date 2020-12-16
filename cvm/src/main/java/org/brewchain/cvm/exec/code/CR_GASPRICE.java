package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_GASPRICE extends AbstractCodeRunner {

	public CR_GASPRICE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord gasPrice = DataWord.ZERO;

		if (hint != null) {
			hint.append("price: " + gasPrice.toString());
		}

		program.stackPush(gasPrice);
		program.step();
		return 0;
	}

}