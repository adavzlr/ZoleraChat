package zolera.chat.client;

public class TerminateClientException
extends Exception {
	private static final long serialVersionUID = 1L;
	
	public TerminateClientException() {
		super();
	}
	
	public TerminateClientException(String message) {
		super(message);
	}
	
	public TerminateClientException(Throwable cause) {
		super(cause);
	}
	
	public TerminateClientException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TerminateClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
