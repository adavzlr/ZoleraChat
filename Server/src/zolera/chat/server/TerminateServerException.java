package zolera.chat.server;

public class TerminateServerException
extends Exception {
	private static final long serialVersionUID = 1L;
	
	public TerminateServerException() {
		super();
	}
	
	public TerminateServerException(String message) {
		super(message);
	}
	
	public TerminateServerException(Throwable cause) {
		super(cause);
	}
	
	public TerminateServerException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TerminateServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
