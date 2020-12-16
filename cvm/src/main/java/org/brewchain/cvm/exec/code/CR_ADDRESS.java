package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_ADDRESS extends AbstractCodeRunner {

	public CR_ADDRESS(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord address = program.getOwnerAddress();

		if (hint != null) {
			hint.append("address: " + Hex.toHexString(address.getLast20Bytes()));
		}

		program.stackPush(address);
		program.step();
		return 0;
	}

}
