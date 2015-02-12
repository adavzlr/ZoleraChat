package zolera.chat.server;

import zolera.chat.infrastructure.*;

public class ChatClientHandle {
	private String      username;
	private IChatClient clientRef;
	
	public ChatClientHandle(String name, IChatClient ref) {
		username  = name;
		clientRef = ref;
	}
	
	public String getUsername() {
		return username;
	}
	
	public IChatClient getClientRef() {
		return clientRef;
	}
}
