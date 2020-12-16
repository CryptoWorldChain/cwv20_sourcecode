package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_MSTORE8 extends AbstractCodeRunner {

	public CR_MSTORE8(OpCode op) {
		super(op);
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord addr = program.stackPop();
		DataWord value = program.stackPop();
		byte[] byteVal = { value.getData()[31] };
		program.memorySave(addr.intValueSafe(), byteVal);
		program.step();
		
		return 1;
	}

}
