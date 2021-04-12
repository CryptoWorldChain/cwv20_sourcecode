package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_POP extends AbstractCodeRunner {

	public CR_POP(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		program.stackPop();

		hint.append("pop,stack size="+stack.size());
		if(stack.size()>0){
			hint.append(":").append(stack.peek().toString());
		}


		program.step();
		return 0;
	}

}
