package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_CALLDATALOAD extends AbstractCodeRunner {

	public CR_CALLDATALOAD(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord dataOffs = program.stackPop();
		DataWord value = program.getDataValue(dataOffs);

		if (hint!=null)
		{
			hint.append("data: " + value);
		}

		program.stackPush(value);
		program.step();
		return 1;
	}

}
