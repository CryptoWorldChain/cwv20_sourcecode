package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_CALLER extends AbstractCodeRunner {

	public CR_CALLER(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		//!.modify by brew
		// DataWord callerAddress = new DataWord(program.getSenderAddress());
		
		DataWord callerAddress = program.getCallerAddress();

//		if (hint!=null)
		{
			hint.append("address: " + Hex.toHexString(callerAddress.getLast20Bytes()));
		}

		program.stackPush(callerAddress);
		program.step();
		return 0;
	}

}
