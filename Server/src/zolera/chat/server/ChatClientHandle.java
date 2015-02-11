package zolera.chat.server;

import zolera.chat.infrastructure.*;

public class ChatClientHandle {
	private boolean     ready;
	private long        creationTime;
	
	private String      username;
	
	private IChatClient clientRef;
	
	public ChatClientHandle(String name, IChatClient ref) {
		ready        = false;
		creationTime = System.currentTimeMillis();
		
		username  = name;
		
		clientRef = ref;
	}
	
	public void setReady(boolean val) {
		ready = val;
	}
	
	public boolean isReady() {
		return ready;
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
	public String getUsername() {
		return username;
	}
	
	public IChatClient getClientRef() {
		return clientRef;
	}
}
