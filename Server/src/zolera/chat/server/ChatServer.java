package zolera.chat.server;

import java.io.InputStream;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.Scanner;

import zolera.chat.infrastructure.*;

public class ChatServer
implements IChatServer {
	private ServerConfiguration config;
	private ChatRoom defaultRoom;
	private int registryId;
	private String registryHost;
	private int registryPort;
	
	private IChatServer serverRef;
	private Scanner input;
	
	public ChatServer(InputStream is) {
		input = new Scanner(is);
		config      = ServerConfiguration.getGlobal();
		defaultRoom = null;
		serverRef   = null;
	}
	
	
	
	public void run() {
		System.out.println("ZoleraChat Server started\n");
		
		try {
			createDefaultRoom();
			register();
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
			
			terminate();
		}
		
		return;
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
	
	private ChatRoom createRoom(String name)
	throws RemoteException {
		ChatRoom room = new ChatRoom(name, config.getMaxRoomCapacity());
		room.startConsumerThread();
		return room;
	}
	
	private void terminate() {
		if (defaultRoom != null)
			terminateRoom(defaultRoom);
	}
	
	private void terminateRoom(ChatRoom room) {
		room.stopConsumerThread();
	}
	
	private void register()
	throws TerminateServerException {
		readServerId();
		loadRegistryInfo();
		try {
			serverRef = (IChatServer) UnicastRemoteObject.exportObject(this, 0);
			LocateRegistry.getRegistry(registryHost, registryPort).rebind(config.getServerRegisteredName(), serverRef);
			//LocateRegistry.getRegistry().rebind(config.getServerRegisteredName(), serverRef);
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	private void loadRegistryInfo()
	throws TerminateServerException {
		String registryAddress = config.getRegistryAddress(registryId);
		String [] splitAddress = registryAddress.split(":");
		if(splitAddress.length == 2){
			registryHost = splitAddress[0];
			registryPort = Integer.parseInt(splitAddress[1]);
		}
		else
		{
			throw new TerminateServerException("Invalid format in registry address");
		}
	}
	
	private void readServerId()
	throws TerminateServerException {
		System.out.println("Server ID: ");
		if(input.hasNextInt()){
			registryId = input.nextInt();
			if(registryId < 1 || registryId > ServerConfiguration.getDefaultConfiguration().getRegistryAddressesListLength()){
				throw new TerminateServerException("Invalid Id");
			}
		}
		else {
			throw new TerminateServerException("Not an integer");
		}
	}
	
	private TerminateServerException getRMIException(RemoteException re) {
		return new TerminateServerException("Failure on RMI layer", re);
	}
	
	
	
	@Override
	public int connect(String roomname, String username, IChatClient clientRef)
	throws RemoteException {
		if (!connect_checkParameters(roomname, username, clientRef))
			return IChatServer.VALIDITY_CHECK_FAILED;
		
		ChatRoom room = defaultRoom;
		synchronized(room) {
			if (!connect_verifyValidity(room, clientRef))
				return IChatServer.VALIDITY_CHECK_FAILED;
			
			if (room.addClient(username, clientRef))
				return IChatServer.CONNECTION_SUCCESSFUL;
			else
				return IChatServer.ROOM_IS_FULL;
		}
	}
	
	private boolean connect_checkParameters(String roomname, String username, IChatClient clientRef) {
		if (roomname == null || username == null || clientRef == null)
			return false;
		if (!roomname.matches(config.getRoomnamePattern()))
			return false;
		if (!username.matches(config.getUsernamePattern()))
			return false;
		if (!roomname.equals(config.getDefaultRoomname()))
			return false; // not currently supporting other rooms
		
		return true;
	}
	
	private boolean connect_verifyValidity(ChatRoom room, IChatClient clientRef) {
		if (room.isClient(clientRef))
			return false;
		
		return true;
	}
	
	@Override
	public IChatRoom getRoomRef(String roomname, IChatClient clientRef)
	throws RemoteException {
		if (!getRoomRef_checkParameters(roomname, clientRef))
			return null;
		
		ChatRoom room = defaultRoom;		
		synchronized(room) {
			if (!getRoomRef_verifyValidity(room, clientRef))
				return null;
			
			return room.getRoomRef();
		}
	}
	
	private boolean getRoomRef_checkParameters(String roomname, IChatClient clientRef) {
		if (roomname == null || clientRef == null)
			return false;
		if (!roomname.matches(config.getRoomnamePattern()))
			return false;
		if (!roomname.equals(config.getDefaultRoomname()))
			return false; // not currently supporting other rooms
		
		return true;
	}
	
	private boolean getRoomRef_verifyValidity(ChatRoom room, IChatClient clientRef) {
		if(!room.isClient(clientRef))
			return false;
		if (room.getClientHandle(clientRef).isReady())
			return false;

		return true;
	}
	
	
	public static void main(String args[]) {
		ChatServer server = new ChatServer(System.in);
		server.run();
	}
}