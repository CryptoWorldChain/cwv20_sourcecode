package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_TIMESTAMP extends AbstractCodeRunner {

	public CR_TIMESTAMP(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord timestamp = program.getTimestamp();

		if (hint != null) {
			hint.append("timestamp: " + timestamp.value());
		}

		program.stackPush(timestamp);
		program.step();
		return 0;
	}

}
