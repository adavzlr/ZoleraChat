package zolera.chat.infrastructure;

import java.util.*;

public class ChatLog {
	private List<ChatMessage> messages;
	
	public ChatLog(int initialCapacity) {
		messages = new ArrayList<ChatMessage>(initialCapacity);
	}
	
	public void addMessage(ChatMessage msg) {
		messages.add(msg);
	}
	
	public ChatMessage[] getAllMessages() {
		ChatMessage[] all = new ChatMessage[messages.size()];
		messages.toArray(all);
		return all;
	}
}