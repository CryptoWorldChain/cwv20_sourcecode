package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_CALLVALUE extends AbstractCodeRunner {

	public CR_CALLVALUE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord callValue = program.getCallValue();

		if (hint != null) {
			hint.append("value: " + callValue);
		}

		program.stackPush(callValue);
		program.step();
		return 0;
	}

}
