package org.brewchain.cvm.exec.code;

import java.math.BigInteger;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_SIGNEXTEND extends AbstractCodeRunner {

	public CR_SIGNEXTEND(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}
	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord word1 = program.stackPop();
		BigInteger k = word1.value();

		if (k.compareTo(_32_) < 0) {
			DataWord word2 = program.stackPop();
			if (hint != null) {
				hint.append(word1.value()).append("  ").append(word2.value());
			}
			word2.signExtend(k.byteValue());
			program.stackPush(word2);
		}
		program.step();
		
		return 2;
	}

}
