package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatClient
extends Remote {
	public void receive(IChatMessage msg) throws RemoteException;
}