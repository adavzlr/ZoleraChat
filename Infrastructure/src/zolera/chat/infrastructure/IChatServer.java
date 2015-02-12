package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatServer
extends Remote {
	public IChatRoom getRoomRef(String roomname, IChatClient clientRef)
	throws RemoteException;
}