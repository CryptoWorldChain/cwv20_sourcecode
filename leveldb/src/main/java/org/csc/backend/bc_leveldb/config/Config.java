package org.csc.backend.bc_leveldb.config;

public interface Config {
	/** levelDB 打开文件最大数量 */
	String MAX_OPEN_FILE = "org.brewchain.level.max.open.file";

	/** blockSize */
	String BLOCK_SIZE = "org.brewchain.level.block.size";

	/** WRITE_BUFFER_SIZE */
	String WRITE_BUFFER_SIZE = "org.brewchain.level.write.buffer.size";

	/** MAX_FILE_SIZE */
	String MAX_FILE_SIZE = "org.brewchain.level.max.file.size";
	
	
	/** MAX_FILE_SIZE */
	String FILTER_POLICY = "org.brewchain.level.filter.policy";
	
	
}
