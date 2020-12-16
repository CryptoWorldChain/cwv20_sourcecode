package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_BALANCE extends AbstractCodeRunner {

	public CR_BALANCE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord address = program.stackPop();
		DataWord balance = program.getBalance(address);

		if (hint != null) {
			hint.append("address: " + Hex.toHexString(address.getLast20Bytes()) + " balance: " + balance.toString());
		}

		program.stackPush(balance);
		program.step();
		return 0;
	}

}
