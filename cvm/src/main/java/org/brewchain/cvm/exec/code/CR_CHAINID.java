package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_CHAINID extends AbstractCodeRunner {

	public CR_CHAINID(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {

		DataWord chainID = program.getChainID();
		if (hint != null) {
			hint.append("chainID: " + chainID);
		}

		program.stackPush(chainID);
		program.step();
		return 1;
	}

}
