package zolera.chat.infrastructure;

import java.io.*;

// Immutable
public final class ChatMessage
implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String sender;
	private final String message;
	
	public ChatMessage(String username, String text) {
		sender  = username;
		message = text;
	}
	
	public String getSenderName() {
		return sender;
	}
	
	public String getMessageText() {
		return message;
	}
}