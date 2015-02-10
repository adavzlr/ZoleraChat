package zolera.chat.server;

import java.rmi.*;

import zolera.chat.infrastructure.*;

public class ChatRoom
implements IChatRoom {
	private int capacity;
	private int occupancy;
	
	private String[]      clientNames;
	private IChatClient[] clientRefs;
	
	public ChatRoom(int size) {
		capacity  = size;
		occupancy = 0;
		
		clientNames = new String[size];
		clientRefs  = new IChatClient[size];
		
		for (int c = 0; c < size; c++) {
			clientNames[c] = null;
			clientRefs[c]  = null;
		}
	}
	
	public boolean isFull() {
		return occupancy >= capacity;
	}
	
	public boolean addClient(String clientName, IChatClient clientRef) {
		if (isFull())
			return false;
		
		clientNames[occupancy] = clientName;
		clientRefs[occupancy]  = clientRef;
		
		occupancy++;
		return true;
	}
	
	@Override
	public void submit(IChatMessage msg)
	throws RemoteException {
		for (int c = 0; c < occupancy; c++) {
			clientRefs[c].receive(msg);
		}
	}
}