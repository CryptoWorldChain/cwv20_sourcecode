package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.brewchain.cvm.utils.ByteUtil;

import java.math.BigInteger;

public class CR_SHL extends AbstractCodeRunner {

	public CR_SHL(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

//	The SHL instruction (shift left) pops 2 values from the stack, first arg1 and then arg2, and pushes on the stack arg2 shifted to the left by arg1 number of bits. The result is equal to
//
//(arg2 * 2^arg1) mod 2^256
//Notes:
//
//The value (arg2) is interpreted as an unsigned number.
//The shift amount (arg1) is interpreted as an unsigned number.
//If the shift amount (arg1) is greater or equal 256 the result is 0.
//This is equivalent to PUSH1 2 EXP MUL.

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		DataWord word2 = program.stackPop();
		if (hint != null) {
			hint.append(word2.value()).append(" *  2^").append(word1.value()).append(" mod 2^256 ");
		}

		if(word1.intValueSafe()>=256){
			program.stackPush(DataWord.ZERO);
			program.step();
		}else{
			BigInteger bi = word2.value().shiftLeft(word1.intValueSafe());
//			BigInteger bi = word2.value().multiply(BigInteger.valueOf(2).pow(word1.intValueSafe())).mod(BigInteger.valueOf(2).pow(256));
			program.stackPush(new DataWord(ByteUtil.copyToArray(bi)));
			program.step();
		}
		return 1;
	}

}
