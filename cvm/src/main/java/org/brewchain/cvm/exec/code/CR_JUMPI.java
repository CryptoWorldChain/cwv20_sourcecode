package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_JUMPI extends AbstractCodeRunner {

	public CR_JUMPI(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord pos = program.stackPop();
		DataWord cond = program.stackPop();

		hint.append(  cond.isZero()+"~> " );
		if (!cond.isZero()) {
			int nextPC = program.verifyJumpDest(pos);

//			if (hint!=null)
			hint.append( "~> " + nextPC);

			program.setPC(nextPC);
		} else {
			program.step();
		}
		
		return 1;
	}

}
