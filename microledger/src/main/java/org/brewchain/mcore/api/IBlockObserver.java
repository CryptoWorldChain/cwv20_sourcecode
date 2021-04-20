package org.brewchain.mcore.api;

import org.brewchain.mcore.model.Block.BlockInfo;

public interface IBlockObserver {
	void onNotify(BlockInfo bi);
}
