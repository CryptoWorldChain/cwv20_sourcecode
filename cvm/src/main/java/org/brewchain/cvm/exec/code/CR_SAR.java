package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

import java.math.BigInteger;

public class CR_SAR extends AbstractCodeRunner {

	public CR_SAR(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

//The SAR instruction (arithmetic shift right) pops 2 values from the stack, first arg1 and then arg2, and pushes on the stack arg2 shifted to the right by arg1 number of bits with sign extension. The result is equal to
//
//floor(arg2 / 2^arg1)
//Notes:
//
//The value (arg2) is interpreted as a signed number.
//The shift amount (arg1) is interpreted as an unsigned number.
//If the shift amount (arg1) is greater or equal 256 the result is 0 if arg2 is non-negative or -1 if arg2 is negative.
//This is not equivalent to PUSH1 2 EXP SDIV, since it rounds differently. See SDIV(-1, 2) == 0, while SAR(-1, 1) == -1.

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		DataWord word2 = program.stackPop();
		if (hint != null) {
			hint.append("floor(").append(word1.value()).append("/ 2^").append(word2.value());
		}

		if(word1.intValueSafe()>=256){
			if(word2.isNegative()){
				program.stackPush(new DataWord(-1));
			}else{
				program.stackPush(DataWord.ZERO);
			}
			program.step();
		}else{
			BigInteger bi = word2.value().shiftRight(word1.intValueSafe());
			program.stackPush(new DataWord(bi.toByteArray()));
			program.step();
		}
		return 1;
	}

}
