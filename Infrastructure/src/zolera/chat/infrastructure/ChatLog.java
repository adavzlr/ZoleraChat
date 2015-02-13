package zolera.chat.infrastructure;

import java.util.*;

public class ChatLog {
	private List<ChatMessage> messages;
	
	public ChatLog(int initialCapacity) {
		messages = new ArrayList<ChatMessage>(initialCapacity);
	}
	
	public void addMessageBatch(ChatMessage[] msg) {
		for (int m = 0; m < msg.length; m++)
			messages.add(msg[m]);
	}
	
	public ChatMessage[] getAllMessages() {
		ChatMessage[] all = new ChatMessage[messages.size()];
		messages.toArray(all);
		return all;
	}
	
	public int getSize() {
		return messages.size();
	}
}