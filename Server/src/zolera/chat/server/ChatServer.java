package zolera.chat.server;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

import zolera.chat.infrastructure.*;

public class ChatServer
implements IChatServer {
	private ChatRoom  room;
	private IChatRoom roomRef;
	
	public ChatServer() {
		try {
			room    = new ChatRoom(10);
			roomRef = (IChatRoom) UnicastRemoteObject.exportObject(room, 0);
		}
		catch (Exception e) {
			throw new IllegalStateException("ZoleraChat Server: error exporting chat room", e);
		}
	}
	
	@Override
	public IChatRoom connect(String username, IChatClient clientRef)
	throws RemoteException {
		if (room.addClient(username, clientRef))
			return roomRef;
		else
			return null;
	}
	
	public static void main(String args[]) {
		try {
			ChatServer  server    = new ChatServer();
			IChatServer serverRef = (IChatServer) UnicastRemoteObject.exportObject(server, 0);
			LocateRegistry.getRegistry().rebind("ZoleraChatServer", serverRef);
			System.out.println("ZoleraChat Server: Up and running!");
		}
		catch (Exception e) {
			throw new IllegalStateException("ZoleraChat Server: error starting server", e);
		}
	}
}