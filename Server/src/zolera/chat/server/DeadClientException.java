package zolera.chat.server;

public class DeadClientException
extends Exception {
	private static final long serialVersionUID = 1L;
	
	public DeadClientException() {
		super();
	}
	
	public DeadClientException(String message) {
		super(message);
	}
	
	public DeadClientException(Throwable cause) {
		super(cause);
	}
	
	public DeadClientException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DeadClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
