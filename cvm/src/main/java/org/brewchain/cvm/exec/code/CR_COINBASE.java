package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_COINBASE extends AbstractCodeRunner {

	public CR_COINBASE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord coinbase = program.getCoinbase();

		if (hint != null) {
			hint.append("coinbase: " + Hex.toHexString(coinbase.getLast20Bytes()));
		}

		program.stackPush(coinbase);
		program.step();
		return 0;
	}

}
