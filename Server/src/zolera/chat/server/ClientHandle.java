package zolera.chat.server;

import zolera.chat.infrastructure.*;

public class ClientHandle {
	private String      username;
	private RemoteClientModel clientRef;
	
	public ClientHandle(String name, RemoteClientModel ref) {
		username  = name;
		clientRef = ref;
	}
	
	public String getUsername() {
		return username;
	}
	
	public RemoteClientModel getClientRef() {
		return clientRef;
	}
}
