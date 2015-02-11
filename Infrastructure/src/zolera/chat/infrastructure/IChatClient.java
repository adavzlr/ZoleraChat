package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatClient
extends Remote {
	public void receive(ChatMessage msg)
	throws RemoteException;
	
	public void receiveBatch(ChatMessage[] messages)
	throws RemoteException;
}