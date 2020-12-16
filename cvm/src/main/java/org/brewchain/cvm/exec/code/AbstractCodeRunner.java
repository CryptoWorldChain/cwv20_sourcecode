package org.brewchain.cvm.exec.code;

import java.math.BigInteger;

import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;

import lombok.Data;

@Data
public abstract class AbstractCodeRunner {

	protected OpCode op;
	public AbstractCodeRunner(OpCode op) {
		super();
		this.op = op;
	}
	
	public static BigInteger _32_ = BigInteger.valueOf(32);

	public abstract int exec(Program program, Stack stack, StringBuffer hint);

}
