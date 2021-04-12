package org.brewchain.cvm.exec.code;

import java.util.ArrayList;
import java.util.List;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.base.LogInfo;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

public class CR_LOGS_0_4 extends AbstractCodeRunner {

	public CR_LOGS_0_4(OpCode op) {
		super(op); 
	}

	int nTopics = op.val() - OpCode.LOG0.val();

	public int exec(Program program, Stack stack, StringBuffer hint) {
		//do nothing
		DataWord address = program.getOwnerAddress();

		DataWord memStart = stack.pop();
		DataWord memOffset = stack.pop();
//
		List<DataWord> topics = new ArrayList<>();
		for (int i = 0; i < nTopics; ++i) {
			DataWord topic = stack.pop();
			topics.add(topic);
		}

		byte[] data = program.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe());

		LogInfo logInfo = new LogInfo(address.getLast20Bytes(), topics, data);

//		if (hint != null) {
			hint.append(logInfo.toString());
//		}

		program.getResult().addLogInfo(logInfo);
		program.step();
		return 0;
	}

}
