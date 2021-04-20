package org.brewchain.mcore.bean;

import java.util.ArrayList;
import java.util.List;

import org.brewchain.mcore.model.Block.BlockInfo;
import org.brewchain.mcore.tools.bytes.BytesComparisons;

import lombok.Data;

@Data
public class BlockMessageMark {
	private BlockMessageMarkEnum mark;
	private BlockInfo block;
	private List<BlockMessage> childBlock = new ArrayList<>();

	public enum BlockMessageMarkEnum {
		DROP, CONNECTED, EXISTS_PREV, CACHE, APPLY, APPLY_CHILD, DONE, ERROR, NEED_TRANSACTION
	}

	public void addChildBlock(BlockMessage child) {
		for (BlockMessage bm : childBlock) {
			if (BytesComparisons.equal(bm.getHash(), child.getHash())) {
				return;
			}
		}
		childBlock.add(child);
	}
}
