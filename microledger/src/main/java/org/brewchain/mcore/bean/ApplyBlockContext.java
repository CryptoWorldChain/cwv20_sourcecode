package org.brewchain.mcore.bean;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.concurrent.AtomicBigInteger;
import org.brewchain.mcore.model.Block.BlockInfo;

import com.google.protobuf.ByteString;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplyBlockContext {
	BlockInfo.Builder currentBlock;
	BlockInfo blockInfo;
	ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts;
	byte[][] results;
	CountDownLatch cdl;
	byte[][] txkeys;
	byte[][] txvalues;
	AtomicBigInteger  gasAccumulate;
}
