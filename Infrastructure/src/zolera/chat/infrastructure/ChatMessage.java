package zolera.chat.infrastructure;

import java.io.*;

public class ChatMessage
implements IChatMessage, Serializable {
	private static final long serialVersionUID = 1L;
	
	private String sender;
	private String message;
	
	public ChatMessage() {
		sender  = null;
		message = null;
	}
	
	public ChatMessage(String user, String msg) {
		sender  = user;
		message = msg;
	}
	
	public String getSenderName() {
		return sender;
	}
	
	public void setSenderName(String user) {
		sender = user;
	}
	
	public String getMessageText() {
		return message;
	}
	
	public void setMessageText(String msg) {
		message = msg;
	}
}
