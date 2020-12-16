package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_GAS extends AbstractCodeRunner {

	public CR_GAS(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord gas = DataWord.ZERO;

		if (hint != null) {
			hint.append(gas);
		}

		program.stackPush(gas);
		program.step();
		return 0;
	}

}
