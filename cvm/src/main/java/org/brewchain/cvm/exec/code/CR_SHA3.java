package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_SHA3 extends AbstractCodeRunner {

	public CR_SHA3(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		DataWord memOffsetData = program.stackPop();
		DataWord lengthData = program.stackPop();
		byte[] buffer = program.memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());

		byte[] encoded = program.getMcore().getCrypto().sha3(buffer);

		DataWord word = new DataWord(encoded);

		if (hint!=null)
		{
			hint.append(word.toString());
		}

		program.stackPush(word);
		program.step();
		return 2;
	}

}
