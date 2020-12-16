package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CR_UNKNOWN extends AbstractCodeRunner {

	public CR_UNKNOWN(OpCode op) {
		super(op);
		// TODO Auto-generated constructor stub
	}

	public int exec(Program program, Stack stack, StringBuffer hint) {
		if (hint != null) {
			hint.append("unknow opcode:" + op);
		}
		log.info("unknow opcode:" + op.name()+",code="+op.val()+","+op);
		return 10;
	}

}
