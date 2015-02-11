package zolera.chat.server;

import java.util.*;

import zolera.chat.infrastructure.*;

public class ChatLog {
	private List<ChatMessage> messages;
	
	public ChatLog() {
		messages = new ArrayList<ChatMessage>(ServerConfiguration.getGlobal().getInitialChatLogCapacity());
	}
	
	public synchronized void addMessage(ChatMessage msg) {
		messages.add(msg);
	}
	
	public synchronized ChatMessage[] getAllMessages() {
		ChatMessage[] all = new ChatMessage[messages.size()];
		messages.toArray(all);
		return all;
	}
}