package org.brewchain.cvm.program;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.brewchain.cvm.exec.OpCode;

import lombok.Data;

/**
 * Created by Anton Nashatyrev on 06.02.2017.
 */
@Data
@Slf4j
public class ProgramPrecompile {

	private Set<Integer> jumpdest = new HashSet<>();
	private byte[] ops;

	public static ProgramPrecompile compile(byte[] ops) {
		ProgramPrecompile ret = new ProgramPrecompile(ops);
		for (int i = 0; i < ops.length; ++i) {

			OpCode op = OpCode.code(ops[i]);
			if (op == null)
			{
				log.error("unknow code:0x"+ BigInteger.valueOf(ops[i]&0xff).toString(16));
				continue;
			}

			if (op.equals(OpCode.JUMPDEST))
			{
//				log.error("jumpdest:"+i);
				ret.jumpdest.add(i);
			}
			log.info("code:0x"+ BigInteger.valueOf(ops[i]&0xff).toString(16));

			if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
				i += op.asInt() - OpCode.PUSH1.asInt() + 1;
			}
		}
		return ret;
	}

	public boolean hasJumpDest(int pc) {
		return jumpdest.contains(pc);
	}

	private ProgramPrecompile(byte[] ops) {
		super();
		this.ops = ops;
	}
}
