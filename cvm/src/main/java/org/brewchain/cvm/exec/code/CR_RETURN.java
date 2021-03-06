package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_RETURN extends AbstractCodeRunner {

	public CR_RETURN(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord offset = program.stackPop();
		DataWord size = program.stackPop();

		byte[] hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe());
		program.setHReturn(hReturn);

		if (hint != null) {
			hint.append("data: " + Hex.toHexString(hReturn) + " offset: " + offset.value() + " size: " + size.value());
		}

		program.step();
		program.stop();
		return 0;
	}

}
