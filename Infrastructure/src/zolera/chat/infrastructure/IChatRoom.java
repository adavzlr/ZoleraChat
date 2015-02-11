package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatRoom
extends Remote {
	public static final int MESSAGE_SUBMITTED     = 100;
	public static final int READY_ACKNOWLEDGED    = 200;
	public static final int VALIDITY_CHECK_FAILED = 1_000_001;
	
	public int submit(IChatClient clientRef, ChatMessage msg)
	throws RemoteException;
	
	public int ready(IChatClient clientRef)
	throws RemoteException;
}
