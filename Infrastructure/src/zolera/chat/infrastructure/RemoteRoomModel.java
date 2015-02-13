package zolera.chat.infrastructure;

import java.rmi.*;

public interface RemoteRoomModel
extends Remote {
	public static final int MESSAGE_SUBMITTED     = 100;
	public static final int SUCCESSFUL_JOIN       = 200;
	public static final int ROOM_IS_FULL          = 210;
	public static final int VALIDITY_CHECK_FAILED = 1_000_001;
	
	public int join(String username, RemoteClientModel clientRef)
	throws RemoteException;
	
	public int submit(RemoteClientModel clientRef, ChatMessage msg)
	throws RemoteException;
}
