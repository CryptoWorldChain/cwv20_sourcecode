package org.brewchain.cvm.program;

import java.util.HashSet;
import java.util.Set;

import org.brewchain.cvm.exec.OpCode;

import lombok.Data;

/**
 * Created by Anton Nashatyrev on 06.02.2017.
 */
@Data
public class ProgramPrecompile {

	private Set<Integer> jumpdest = new HashSet<>();
	private byte[] ops;

	public static ProgramPrecompile compile(byte[] ops) {
		ProgramPrecompile ret = new ProgramPrecompile(ops);
		for (int i = 0; i < ops.length; ++i) {

			OpCode op = OpCode.code(ops[i]);
			if (op == null)
				continue;

			if (op.equals(OpCode.JUMPDEST))
				ret.jumpdest.add(i);

			if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
				i += op.asInt() - OpCode.PUSH1.asInt() + 1;
			}
		}
		return ret;
	}

	public boolean hasJumpDest(int pc) {
		return jumpdest.contains(pc);
	}

	public ProgramPrecompile(byte[] ops) {
		super();
		this.ops = ops;
	}
}
