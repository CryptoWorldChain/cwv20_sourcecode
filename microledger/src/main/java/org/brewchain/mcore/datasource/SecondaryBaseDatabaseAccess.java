package org.brewchain.mcore.datasource;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.brewchain.mcore.api.ODBSupport;
import org.brewchain.mcore.exception.ODBException;
import org.brewchain.mcore.tools.bytes.BytesHashMap;

public class SecondaryBaseDatabaseAccess extends BaseDatabaseAccess {

	protected byte[] put(ODBSupport dbs, byte[] key, byte[] secondaryKey, byte[] value)
			throws ODBException, InterruptedException, ExecutionException {
		return dbs.put(key, secondaryKey, value).get();
	}

	protected BytesHashMap<byte[]> getBySecondaryKey(ODBSupport dbs, byte[] secondaryKey)
			throws ODBException, InterruptedException, ExecutionException {
				
		return dbs.listBySecondKey(secondaryKey).get();
	}
	
	protected void deleteBySecondKey(ODBSupport dbs, byte[] secondaryKey, List<byte[]> keys) {
		dbs.deleteBySecondKey(secondaryKey, keys);
	}
}
