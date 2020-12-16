package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_RETURNDATASIZE extends AbstractCodeRunner {

	public CR_RETURNDATASIZE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord dataSize = program.getReturnDataBufferSize();

		if (hint != null) {
			hint.append("size: " + dataSize.value());
		}

		program.stackPush(dataSize);
		program.step();
		return 0;
	}
	

}
