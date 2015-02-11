package zolera.chat.server;

import zolera.chat.infrastructure.*;

public class ChatClientHandle {
	private boolean     ready;
	private String      username;
	private IChatClient clientRef;
	
	public ChatClientHandle(String name, IChatClient ref) {
		ready     = false;
		username  = name;
		clientRef = ref;
	}
	
	public String getUsername() {
		return username;
	}
	
	public IChatClient getClientRef() {
		return clientRef;
	}
	
	public boolean isReady() {
		return ready;
	}
	
	public void setReady(boolean val) {
		ready = val;
	}
}
