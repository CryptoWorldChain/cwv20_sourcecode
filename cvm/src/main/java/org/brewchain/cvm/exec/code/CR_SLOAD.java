package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_SLOAD extends AbstractCodeRunner {

	public CR_SLOAD(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord key = program.stackPop();
		DataWord val = program.storageLoad(key);

//		if (hint!=null)
		{
			hint.append("key: " + key + " value: " + val);
		}

		if (val == null)
			val = key.and(DataWord.ZERO);

		program.stackPush(val);
		program.step();
		return 1;
	}

}
