package org.brewchain.cvm.exec.invoke;

//import org.apache.maven.model.Repository;
import org.brewchain.cvm.base.DataWord;
import org.brewchain.mcore.bean.ApplyBlockContext;

public class BlockContextDWORD {

	/**
	 * BLOCK env ** 因为通常也不一定会用到这些参数，需要的时候再去转换好了。
	 */
	private DataWord prevHash, coinbase, timestamp, number;
	
	ApplyBlockContext blockContext;

	public BlockContextDWORD(ApplyBlockContext blockContext) {
		this.blockContext = blockContext;

	}

	public DataWord getPrevHash() {
		if (prevHash == null) {
			prevHash = new DataWord(blockContext.getBlockInfo().getHeader().getParentHash().toByteArray());
		}
		return prevHash;
	}

	public DataWord getCoinbase() {
		return DataWord.ZERO;
	}

	/* TIMESTAMP op */
	public DataWord getTimestamp() {
		if (timestamp == null) {
			timestamp = new DataWord(blockContext.getBlockInfo().getHeader().getTimestamp());
		}

		return timestamp;
	}

	/* NUMBER op */
	public DataWord getNumber() {
		if (number == null) {
			number = new DataWord(blockContext.getBlockInfo().getHeader().getHeight());
		}

		return number;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		BlockContextDWORD that = (BlockContextDWORD) o;

		if (coinbase != null ? !coinbase.equals(that.coinbase) : that.coinbase != null)
			return false;
		if (number != null ? !number.equals(that.number) : that.number != null)
			return false;
		if (prevHash != null ? !prevHash.equals(that.prevHash) : that.prevHash != null)
			return false;
		if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null)
			return false;

		return true;
	}

	@Override
	public String toString() {
		return "BlockContextDWORD{" + " prevHash=" + prevHash + ", coinbase=" + coinbase + ", timestamp=" + timestamp
				+ ", number=" + number + '}';
	}
}
