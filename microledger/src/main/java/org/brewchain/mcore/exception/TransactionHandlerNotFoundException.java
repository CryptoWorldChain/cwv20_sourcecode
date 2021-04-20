package org.brewchain.mcore.exception;

public class TransactionHandlerNotFoundException extends RuntimeException {
	public TransactionHandlerNotFoundException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
