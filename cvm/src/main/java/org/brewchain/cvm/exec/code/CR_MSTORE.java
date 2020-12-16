package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_MSTORE extends AbstractCodeRunner {

	public CR_MSTORE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord addr = program.stackPop();
		DataWord value = program.stackPop();

		if (hint!=null)
		{
			hint.append("addr: " + addr + " value: " + value);
		}

		program.memorySave(addr, value);
		program.step();
		return 1;
	}

}
