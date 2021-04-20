package org.brewchain.mcore.bean;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class BlockSyncMessage {
	long currentHeight;
	long wantHeight;
	private List<byte[]> syncTxHash = new ArrayList<>();

	public enum BlockSyncCodeEnum {
		SS, ER, LB, LT
	}

	BlockSyncCodeEnum syncCode;
}
