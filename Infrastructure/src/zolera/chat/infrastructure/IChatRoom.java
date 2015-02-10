package zolera.chat.infrastructure;

import java.rmi.*;

public interface IChatRoom
extends Remote {
	public void submit(IChatMessage msg) throws RemoteException;
}
