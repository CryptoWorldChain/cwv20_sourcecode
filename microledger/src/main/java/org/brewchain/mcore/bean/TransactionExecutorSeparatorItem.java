package org.brewchain.mcore.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import onight.tfw.outils.pool.ReusefulLoopPool;

@AllArgsConstructor
@Data
public class TransactionExecutorSeparatorItem {
	int index;
	TransactionInfoWrapper tx;
	ReusefulLoopPool<TransactionExecutorSeparatorItem> pool;

	public void reset(int index, TransactionInfoWrapper tx) {
		this.index = index;
		this.tx = tx;
	}
	
	public void release() {
		this.pool.retobj(this);
	}
}
