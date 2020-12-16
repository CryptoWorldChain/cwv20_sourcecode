package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_SSTORE extends AbstractCodeRunner {

	public CR_SSTORE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord addr = program.stackPop();
		DataWord value = program.stackPop();

		if (hint != null) {
			hint.append("[" + program.getOwnerAddress().toPrefixString() + "] key: " + addr + " value: " + value);
		}

		program.storageSave(addr, value);
		program.step();
		return 1;
	}

}
