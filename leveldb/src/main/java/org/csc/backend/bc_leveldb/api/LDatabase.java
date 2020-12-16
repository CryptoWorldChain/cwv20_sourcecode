package org.csc.backend.bc_leveldb.api;

import java.io.File;

import org.apache.commons.codec.binary.Hex;
import org.csc.backend.bc_leveldb.api.TransactionConfig.CompressionType;
import org.csc.backend.bc_leveldb.api.TransactionConfig.Option;
import org.csc.backend.bc_leveldb.config.Config;
import org.csc.backend.bc_leveldb.jni.LDBNative;
import org.csc.backend.bc_leveldb.provider.LDBHelper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.conf.PropHelper;

@Data
@Slf4j
public class LDatabase {
	long dbinst;

	LDBNative nativeInst;

	String filepath;

	int filter_policy;

	public boolean isOpen() {
		return dbinst > 0;
	}

	public Option option = new Option();

	public LDatabase(LDBNative nativeInst, String filepath, int filter_policy) {
		super();
		this.nativeInst = nativeInst;
		this.filepath = filepath;
		this.filter_policy = filter_policy;
		PropHelper params = new PropHelper(null);
		option.setCreate_if_missing(true);
		// option.setCompression(CompressionType.kSnappyCompression.ordinal());
		option.setCompression(CompressionType.kNoCompression.ordinal());
		option.setParanoid_checks(true);
		// 10M
		option.setWrite_buffer_size(params.get(Config.WRITE_BUFFER_SIZE, 10 * 1024 * 1024));
		// 1w
		option.setMax_open_files(params.get(Config.MAX_OPEN_FILE, 10000));
		// 10M
		option.setBlock_size(params.get(Config.BLOCK_SIZE, 10 * 1024 * 1024));
		// 500M
		option.setMax_file_size(params.get(Config.MAX_FILE_SIZE, 500 * 1024 * 1024));

		if (filter_policy > 0) {
			option.setFilter_policy(filter_policy);
		} else {
			option.setFilter_policy(params.get(Config.FILTER_POLICY, 0));
		}

		ensureOpen();
	}

	public void ensureOpen() {
		if (dbinst == 0) {
			synchronized (filepath.intern()) {
				if (dbinst == 0) {
					dbinst = nativeInst.openDB(option, filepath);
					log.debug("reopen db:" + filepath + ",instance=" + dbinst);
				}
			}
		}
	}

	public boolean reclusiveDelete(File path) {
		if (path.isDirectory()) {
			for (File file : path.listFiles()) {
				reclusiveDelete(file);
			}
		}
		return path.delete();
	}

	public void deleteAll() {
		close();
		reclusiveDelete(new File(filepath));
		ensureOpen();
	}

	public void close() {
		if (dbinst > 0) {
			synchronized (filepath.intern()) {
				if (dbinst > 0) {
					nativeInst.closeDB(dbinst);
					log.debug("close db:" + filepath + ",instance=" + dbinst);
					dbinst = 0;
				}
			}
		}
	}

	public void sync() {

	}

	public int syncPut(byte[] key, byte[] value) {
		return nativeInst.syncPut(dbinst, key, value);
	}

	// Remove the database entry (if any) for "key". Returns OK on
	// success, and a non-OK status on error. It is not an error if "key"
	// did not exist in the database.
	// Note: consider setting options.sync = true.
	public int syncDelete(byte[] key) {
		return nativeInst.syncDelete(dbinst, key);
	}

	// flatten
	// Apply the specified updates to the database.
	// Returns OK on success, non-OK on failure.
	// Note: consider setting options.sync = true.
	public int syncBatchPut(byte[][] keys, byte[][] values) {
		return nativeInst.syncBatchPut(dbinst, keys, values);

	}

	// If the database contains an entry for "key" store the
	// corresponding value in *value and return OK.
	//
	// If there is no entry for "key" leave *value unchanged and return
	// a status for which StatusCode::IsNotFound() returns true.

	// May return some other StatusCode on an error.
	// verify_checksums=false,fill_cache=false,snapshot=NULL
	public byte[] fastGet(byte[] key) {
		return nativeInst.fastGet(dbinst, key);

	}

	// verify_checksums=false,fill_cache=true,snapshot=NULL
	public byte[] fillGet(byte[] key) {
		return nativeInst.fillGet(dbinst, key);
	}

}
