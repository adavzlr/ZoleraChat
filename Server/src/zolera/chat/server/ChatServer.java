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
	private int serverId;
	private ChatRoom defaultRoom;
	private int registryPort;
	private IChatServer serverRef;
	
	public ChatServer(int id) {
		config       = ServerConfiguration.getGlobal();
		serverId     = id;
		defaultRoom  = null;
		registryPort = -1;
		serverRef    = null;
	}
	
	
	
	public void run() {
		System.out.println("ZoleraChat Server started\n");
		
		try {
			createDefaultRoom();
			register();
		}
		catch (TerminateServerException tse) {
			// gracefully terminate server when it hits an unrecoverable exception
			terminate();
			
			if (DebuggingTools.DEBUG_MODE)
				throw new RuntimeException("Server termination requested", tse);
			else {
				System.out.println();
				System.out.println("Error: " + tse.getMessage());
				System.out.println("Terminating server");
			}
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
		loadRegistryInfo();
		
		try {
			serverRef = (IChatServer) UnicastRemoteObject.exportObject(this, 0);
			Registry currentRegistry = LocateRegistry.createRegistry(registryPort);
			currentRegistry.rebind(config.getServerRegisteredName(), serverRef);
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	private void loadRegistryInfo()
	throws TerminateServerException {
		if (serverId < 0 || serverId > config.getRegistryAddressesListLength())
			throw new TerminateServerException("Invalid server id " + serverId);
		
		String registryAddress = config.getRegistryAddress(serverId);
		String [] splitAddress = registryAddress.split(":");
		if(splitAddress.length == 2){
			registryPort = Integer.parseInt(splitAddress[1]);
		}
		else
		{
			throw new TerminateServerException("Invalid format in registry address");
		}
	}
	
	private TerminateServerException getRMIException(RemoteException re) {
		return new TerminateServerException("Failure on RMI layer", re);
	}
	
	
	
	@Override
	public IChatRoom getRoomRef(String roomname, IChatClient clientRef)
	throws RemoteException {
		if (!getRoomRef_verifyValidity(roomname, clientRef))
			return null;
		
		return defaultRoom.getRoomRef();
	}
	
	private boolean getRoomRef_verifyValidity(String roomname, IChatClient clientRef) {
		if (roomname == null || clientRef == null)
			return false;
		if (!roomname.matches(config.getRoomnamePattern()))
			return false;
		if (!roomname.equals(config.getDefaultRoomname()))
			return false; // not currently supporting other rooms
		
		return true;
	}
	
	
	
	public static void main(String args[]) {
		int serverId = readServerId(System.in);
		ChatServer server = new ChatServer(serverId);
		server.run();
	}
	
	private static int readServerId(InputStream is) {
		Scanner input = new Scanner(is);
		int id = -1;
		
		System.out.print("Server ID: ");
		if(input.hasNextInt()){
			id = input.nextInt();
		}

		input.close();
		return id;
	}
}