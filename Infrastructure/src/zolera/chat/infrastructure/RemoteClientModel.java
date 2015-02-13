package zolera.chat.infrastructure;

import java.rmi.*;

public interface RemoteClientModel
extends Remote {
	public void receive(ChatMessage[] batch)
	throws RemoteException;
	
	public void chatlog(ChatMessage[] batch)
	throws RemoteException;
}