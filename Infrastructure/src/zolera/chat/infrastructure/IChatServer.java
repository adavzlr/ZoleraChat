package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatServer
extends Remote {
	public static final int CONNECTION_SUCCESSFUL = 100;
	public static final int ROOM_IS_FULL          = 201;
	public static final int ROOM_NOT_FOUND        = 202;
	public static final int VALIDITY_CHECK_FAILED = 1_000_001;
	
	public int connect(String roomname, String username, IChatClient clientRef)
	throws RemoteException;
	
	public IChatRoom getRoomRef(String roomname, IChatClient clientRef)
	throws RemoteException;
}