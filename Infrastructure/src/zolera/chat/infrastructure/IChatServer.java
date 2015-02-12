package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatServer
extends Remote {
	public IChatRoom reference(String roomname)
	throws RemoteException;
}