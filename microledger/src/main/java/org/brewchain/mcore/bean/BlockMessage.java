package org.brewchain.mcore.bean;

import org.brewchain.mcore.model.Block.BlockInfo;

import lombok.Data;

@Data
public class BlockMessage {
	byte[] hash;
	byte[] parentHash;
	long height;
	BlockMessageStatusEnum status = BlockMessageStatusEnum.UNKNOWN;
	BlockInfo block;

	public enum BlockMessageStatusEnum {
		CONNECT, STABLE, UNKNOWN
	}

	public BlockMessage(byte[] hash, byte[] parentHash, long height, BlockInfo block) {
		this.hash = hash;
		this.parentHash = parentHash;
		this.height = height;
		this.block = block;
	}
}
