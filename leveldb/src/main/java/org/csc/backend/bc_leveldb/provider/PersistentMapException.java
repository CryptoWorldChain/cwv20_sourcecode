package org.csc.backend.bc_leveldb.provider;

/**
 *
 * @author brew
 */
public class PersistentMapException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6179414063366187161L;

	public PersistentMapException(Exception e) {
        super(e); 
    }

	public PersistentMapException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PersistentMapException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public PersistentMapException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public PersistentMapException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public PersistentMapException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}
    
    
}