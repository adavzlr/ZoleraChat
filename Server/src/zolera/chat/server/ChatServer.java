package zolera.chat.server;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

import zolera.chat.infrastructure.*;

public class ChatServer
implements IChatServer {
	private boolean running;
	private ServerConfiguration config;
	
	private ChatRoom defaultRoom;
	
	private IChatServer serverRef;
	
	public ChatServer() {
		running = false;
		config  = ServerConfiguration.getDefaultConfiguration();
		
		defaultRoom = null;
		
		serverRef      = null;
	}
	
	public void run() {
		running  = true;
		
		try {
			System.out.println("ZoleraChat Server started\n");
			
			register();
			createDefaultRoom();
		}
		catch (TerminateServerException tse) {
			// gracefully terminate server when it hits an unrecoverable exception
			if (DebuggingTools.DEBUG_MODE)
				throw new RuntimeException("Server termination requested", tse);
			else {
				System.out.println();
				System.out.println("Error: " + tse.getMessage());
				System.out.println("Terminating server");
			}
		}
		
		terminate();
		return;
	}
	
	private void terminate() {
		running = false;
	}
	
	private void register()
	throws TerminateServerException {
		try {
			serverRef = (IChatServer) UnicastRemoteObject.exportObject(this, 0);
			LocateRegistry.getRegistry().rebind(config.getServerRegisteredName(), serverRef);
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	private TerminateServerException getRMIException(RemoteException re) {
		return new TerminateServerException("Failure on RMI layer", re);
	}
	
	private ChatRoom createRoom(String name)
	throws RemoteException {
		ChatRoom room = new ChatRoom(name, config.getMaxRoomCapacity());
		room.startConsumerThread();
		return room;
	}
	
	private void createDefaultRoom()
	throws TerminateServerException {
		try {
			defaultRoom = createRoom(config.getDefaultRoomname());
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	private ChatRoom getChatRoom(String name) {
		return defaultRoom;
	}
	
	@Override
	public int connect(String roomname, String username, IChatClient clientRef)
	throws RemoteException {
		if (!connect_verifyValidity(roomname, username, clientRef))
			return IChatServer.VALIDITY_CHECK_FAILED;
			
		ChatRoom room = getChatRoom(roomname);
		
		if (room == null)
			return IChatServer.ROOM_NOT_FOUND;
		else {
			if (room.addClient(username, clientRef))
				return IChatServer.CONNECTION_SUCCESSFUL;
			else
				return IChatServer.ROOM_IS_FULL;
		}
	}
	
	private boolean connect_verifyValidity(String roomname, String username, IChatClient clientRef) {
		if (roomname == null || username == null || clientRef == null)
			return false;
		if (!roomname.matches(config.getRoomnamePattern()))
			return false;
		if (!username.matches(config.getUsernamePattern()))
			return false;
		
		return true;
	}
	
	@Override
	public IChatRoom getRoomRef(String roomname, IChatClient clientRef)
	throws RemoteException {
		if (!getRoomRef_verifyValidity(roomname, clientRef))
			return null;
		else
			return getChatRoom(roomname).getRoomRef();
	}
	
	private boolean getRoomRef_verifyValidity(String roomname, IChatClient clientRef) {
		if (roomname == null || clientRef == null)
			return false;
		if (!roomname.matches(config.getRoomnamePattern()))
			return false;
		
		ChatRoom room = getChatRoom(roomname);
		if (room == null || !room.isClient(clientRef))
			return false;
		
		return true;
	}
	
	public static void main(String args[]) {
		ChatServer server = new ChatServer();
		server.run();
	}
}