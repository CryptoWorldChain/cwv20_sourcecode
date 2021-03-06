package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_MLOAD extends AbstractCodeRunner {

	public CR_MLOAD(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord addr = program.stackPop();
		DataWord data = program.memoryLoad(addr);

//		if (hint!=null)
		{
			hint.append("data: " + data);
		}

		program.stackPush(data);
		program.step();
		
		return 1;
	}

}
