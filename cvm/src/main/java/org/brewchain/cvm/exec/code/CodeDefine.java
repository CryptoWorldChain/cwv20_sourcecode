package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.exec.OpCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodeDefine {

	public static AbstractCodeRunner[] runerMap = new AbstractCodeRunner[256];

	public static void pushCodeRunner(OpCode op, Class<?> runnerClass) {
		try {
			if (runerMap[op.val()&0xFF] != null) {
				log.error("error in init code runnner:" + op + ":duplicate runner:"+runerMap[op.val()&0xFF] );
			}
			runerMap[op.val()&0xFF] = (AbstractCodeRunner) runnerClass.getConstructor(OpCode.class).newInstance(op);
		} catch (Exception e) {
			log.error("error in init code runnner:" + op + ":" + e, e);
			e.printStackTrace();
		}
	}

	/*
	 * init
	 * 
	 * 
	 * 
	 */
	static {
		pushCodeRunner(OpCode.STOP, CR_STOP.class);
		pushCodeRunner(OpCode.ADD, CR_ADD.class);
		pushCodeRunner(OpCode.MUL, CR_MUL.class);
		pushCodeRunner(OpCode.SUB, CR_SUB.class);
		pushCodeRunner(OpCode.DIV, CR_DIV.class);
		pushCodeRunner(OpCode.SDIV, CR_SDIV.class);
		pushCodeRunner(OpCode.MOD, CR_MOD.class);
		pushCodeRunner(OpCode.SMOD, CR_SMOD.class);
		pushCodeRunner(OpCode.ADDMOD, CR_ADDMOD.class);
		pushCodeRunner(OpCode.MULMOD, CR_MULMOD.class);
		pushCodeRunner(OpCode.EXP, CR_EXP.class);
		pushCodeRunner(OpCode.SIGNEXTEND, CR_SIGNEXTEND.class);
		pushCodeRunner(OpCode.LT, CR_LT.class);
		pushCodeRunner(OpCode.GT, CR_GT.class);
		pushCodeRunner(OpCode.SLT, CR_SLT.class);
		pushCodeRunner(OpCode.SGT, CR_SGT.class);
		pushCodeRunner(OpCode.EQ, CR_EQ.class);
		pushCodeRunner(OpCode.ISZERO, CR_ISZERO.class);
		pushCodeRunner(OpCode.AND, CR_AND.class);
		pushCodeRunner(OpCode.OR, CR_OR.class);
		pushCodeRunner(OpCode.XOR, CR_XOR.class);
		pushCodeRunner(OpCode.NOT, CR_NOT.class);
		pushCodeRunner(OpCode.BYTE, CR_BYTE.class);
		pushCodeRunner(OpCode.SHA3, CR_SHA3.class);
		pushCodeRunner(OpCode.ADDRESS, CR_ADDRESS.class);
		pushCodeRunner(OpCode.BALANCE, CR_BALANCE.class);
		pushCodeRunner(OpCode.ORIGIN, CR_ORIGIN.class);
		pushCodeRunner(OpCode.CALLER, CR_CALLER.class);
		pushCodeRunner(OpCode.CALLVALUE, CR_CALLVALUE.class);
		pushCodeRunner(OpCode.CALLDATALOAD, CR_CALLDATALOAD.class);
		pushCodeRunner(OpCode.CALLDATASIZE, CR_CALLDATASIZE.class);
		pushCodeRunner(OpCode.CALLDATACOPY, CR_CALLDATACOPY.class);
		pushCodeRunner(OpCode.CODESIZE, CR_CODESIZE.class);
		pushCodeRunner(OpCode.CODECOPY, CR_CODECOPY.class);
		pushCodeRunner(OpCode.RETURNDATASIZE, CR_RETURNDATASIZE.class);
		pushCodeRunner(OpCode.RETURNDATACOPY, CR_RETURNDATACOPY.class);
		pushCodeRunner(OpCode.GASPRICE, CR_GASPRICE.class);
		pushCodeRunner(OpCode.EXTCODESIZE, CR_EXTCODESIZE.class);
		pushCodeRunner(OpCode.EXTCODECOPY, CR_EXTCODECOPY.class);
		pushCodeRunner(OpCode.BLOCKHASH, CR_BLOCKHASH.class);
		pushCodeRunner(OpCode.COINBASE, CR_COINBASE.class);
		pushCodeRunner(OpCode.TIMESTAMP, CR_TIMESTAMP.class);
		pushCodeRunner(OpCode.NUMBER, CR_NUMBER.class);
		pushCodeRunner(OpCode.DIFFICULTY, CR_DIFFICULTY.class);
		pushCodeRunner(OpCode.GASLIMIT, CR_GASLIMIT.class);
		pushCodeRunner(OpCode.POP, CR_POP.class);
		pushCodeRunner(OpCode.MLOAD, CR_MLOAD.class);
		pushCodeRunner(OpCode.MSTORE, CR_MSTORE.class);
		pushCodeRunner(OpCode.MSTORE8, CR_MSTORE8.class);
		pushCodeRunner(OpCode.SLOAD, CR_SLOAD.class);
		pushCodeRunner(OpCode.SSTORE, CR_SSTORE.class);
		pushCodeRunner(OpCode.JUMP, CR_JUMP.class);
		pushCodeRunner(OpCode.JUMPI, CR_JUMPI.class);
		pushCodeRunner(OpCode.PC, CR_PC.class);
		pushCodeRunner(OpCode.MSIZE, CR_MSIZE.class);
		pushCodeRunner(OpCode.GAS, CR_GAS.class);
		pushCodeRunner(OpCode.JUMPDEST, CR_JUMPDEST.class);
		pushCodeRunner(OpCode.PUSH1, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH2, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH3, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH4, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH5, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH6, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH7, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH8, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH9, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH10, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH11, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH12, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH13, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH14, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH15, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH16, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH17, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH18, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH19, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH20, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH21, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH22, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH23, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH24, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH25, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH26, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH27, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH28, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH29, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH30, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH31, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.PUSH32, CR_PUSHS_1_32.class);
		pushCodeRunner(OpCode.DUP1, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP2, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP3, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP4, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP5, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP6, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP7, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP8, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP9, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP10, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP11, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP12, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP13, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP14, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP15, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.DUP16, CR_DUPS_1_16.class);
		pushCodeRunner(OpCode.SWAP1, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP2, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP3, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP4, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP5, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP6, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP7, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP8, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP9, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP10, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP11, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP12, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP13, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP14, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP15, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.SWAP16, CR_SWAPS_1_16.class);
		pushCodeRunner(OpCode.LOG0, CR_LOGS_0_4.class);
		pushCodeRunner(OpCode.LOG1, CR_LOGS_0_4.class);
		pushCodeRunner(OpCode.LOG2, CR_LOGS_0_4.class);
		pushCodeRunner(OpCode.LOG3, CR_LOGS_0_4.class);
		pushCodeRunner(OpCode.LOG4, CR_LOGS_0_4.class);
		pushCodeRunner(OpCode.CREATE, CR_CREATE.class);
		pushCodeRunner(OpCode.CALL, CR_CALL.class);
		pushCodeRunner(OpCode.CALLCODE, CR_CALLCODE.class);
		pushCodeRunner(OpCode.RETURN, CR_RETURN.class);
		pushCodeRunner(OpCode.DELEGATECALL, CR_DELEGATECALL.class);
		pushCodeRunner(OpCode.STATICCALL, CR_STATICCALL.class);
		pushCodeRunner(OpCode.REVERT, CR_REVERT.class);
		pushCodeRunner(OpCode.SUICIDE, CR_SUICIDE.class);
	}
}
