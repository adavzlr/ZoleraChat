package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatServer
extends Remote {
	public IChatRoom connect(String username, IChatClient clientRef) throws RemoteException;
}