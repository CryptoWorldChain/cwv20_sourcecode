package org.brewchain.cvm.exec.code;

import static org.brewchain.cvm.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_STOP extends AbstractCodeRunner {

	public CR_STOP(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		program.setHReturn(EMPTY_BYTE_ARRAY);
		program.stop();
		return 0;
	}

}
