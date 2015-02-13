package zolera.chat.infrastructure;

import java.rmi.*;

public interface RemoteServerModel
extends Remote {
	public RemoteRoomModel reference(String roomname)
	throws RemoteException;
}