package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_BLOCKHASH extends AbstractCodeRunner {

	public CR_BLOCKHASH(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		int blockIndex = program.stackPop().intValueSafe();

		DataWord blockHash = program.getBlockHash(blockIndex);

		if (hint != null) {
			hint.append("blockHash: " + blockHash);
		}

		program.stackPush(blockHash);
		program.step();
		return 1;
	}

}
