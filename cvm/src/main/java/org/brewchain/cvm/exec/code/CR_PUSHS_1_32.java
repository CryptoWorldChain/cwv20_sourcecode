package org.brewchain.cvm.exec.code;

import static org.brewchain.cvm.exec.OpCode.PUSH1;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_PUSHS_1_32 extends AbstractCodeRunner {

	public CR_PUSHS_1_32(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}
	int nPush = op.val() - PUSH1.val() + 1;
	public int exec(Program program, Stack stack, StringBuffer hint) {

		byte[] data = program.sweep(nPush);
		program.stackPush(data);

//		if (hint!=null)
		{
			hint.append(Hex.toHexString(data) +",n="+ nPush +",stack size="+stack.size());
			if(stack.size()>0){
				hint.append(":").append(stack.peek().toString());
			}
		}

		return 0;
	}

}
