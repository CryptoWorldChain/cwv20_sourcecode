package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_ORIGIN extends AbstractCodeRunner {

	public CR_ORIGIN(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord originAddress = program.getOriginAddress();

		if (hint != null) {
			hint.append("address: " + Hex.toHexString(originAddress.getLast20Bytes()));
		}

		program.stackPush(originAddress);
		program.step();
		return 0;
	}

}
