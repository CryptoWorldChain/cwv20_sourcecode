package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_SUICIDE extends AbstractCodeRunner {

	public CR_SUICIDE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {

//		DataWord address = program.stackPop();
//		program.suicide(address);
//!!		program.getResult().addTouchAccount(address.getLast20Bytes());

		if (hint != null) {
			hint.append("kill address: " + Hex.toHexString(program.getOwnerAddress().getLast20Bytes()));
		}

		program.stop();
		
		return 10;
	}

}
