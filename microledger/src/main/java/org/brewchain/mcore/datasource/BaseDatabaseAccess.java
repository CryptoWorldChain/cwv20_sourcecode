package org.brewchain.mcore.datasource;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.mcore.api.ODBSupport;
import org.brewchain.mcore.config.StatRunner;
import org.brewchain.mcore.exception.ODBException;

import com.google.protobuf.Message;

import lombok.Getter;
import onight.oapi.scala.commons.SessionModules;

public class BaseDatabaseAccess extends SessionModules<Message> {
	protected static final String daoProviderId = "bc_db";

	protected byte[] get(ODBSupport dbs, byte[] key) throws ODBException, InterruptedException, ExecutionException {
		return dbs.get(key).get();
	}
	
	protected byte[][] gets(ODBSupport dbs, List<byte[]> keys) throws ODBException, InterruptedException, ExecutionException {
		return dbs.list(keys).get();
	}

	protected byte[] put(ODBSupport dbs, byte[] key, byte[] value)
			throws ODBException, InterruptedException, ExecutionException {
		return dbs.put(key, value).get();
	}

	protected byte[][] batchPuts(ODBSupport dbs, List<byte[]> keys, List<byte[]> values)
			throws ODBException, InterruptedException, ExecutionException {
		return dbs.batchPuts(keys, values).get();
	}
	
	protected void delete(ODBSupport dbs, byte[] key) {
		dbs.delete(key);
	}

}
