package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatClient
extends Remote {
	public void receiveBatch(ChatMessage[] batch)
	throws RemoteException;
	
	public void receiveLog(ChatMessage[] batch)
	throws RemoteException;
}