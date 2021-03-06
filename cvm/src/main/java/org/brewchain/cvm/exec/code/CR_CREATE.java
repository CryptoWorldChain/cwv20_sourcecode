package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.brewchain.cvm.utils.ByteUtil;
import org.spongycastle.util.encoders.Hex;

public class CR_CREATE extends AbstractCodeRunner {

	public CR_CREATE(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {

		DataWord value = program.stackPop();
		DataWord inOffset = program.stackPop();
		DataWord inSize = program.stackPop();

		byte[] codeData = program.memoryChunk(inOffset.intValueSafe(), inSize.intValueSafe());
		byte[] encoded = program.getMcore().getCrypto().sha3(codeData);
		byte[] buffer = ByteUtil.merge(new byte[]{(byte)0xff},program.getCallerAddress().getLast20Bytes(),
				new DataWord(program.getNonce()).getData(),encoded);

		byte[] encodedHash = program.getMcore().getCrypto().sha3(buffer);

		byte []address=new DataWord(encodedHash).getLast20Bytes();

		if (hint != null) {
			hint.append(",create Contract address="+ Hex.toHexString(address));
		}

		program.createContract(codeData,address,value);
		program.stackPush(new DataWord(address));

		program.step();


		return 0;
	}

}
