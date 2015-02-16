package zolera.chat.client;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import zolera.chat.infrastructure.*;

public class ClientModel
implements RemoteClientModel {
	private ServerConfiguration config;
	private RemoteClientModel clientRef;
	private ChatLog chatlog;
	private ProcessMessagesDelegate dlgMsgProc;
	private ProcessMessagesDelegate dlgLogMsgProc;
	
	private int         serverId;
	private String      username;
	private String      roomname;
	private RemoteServerModel serverRef;
	private RemoteRoomModel   roomRef;
	
	public ClientModel() {
		config        = ServerConfiguration.getGlobal();
		clientRef     = null;
		
		serverId  = -1;
		serverRef = null;
		
		username      = null;
		roomname      = null;
		chatlog       = null;
		dlgMsgProc    = null;
		dlgLogMsgProc = null;
		roomRef       = null;
	}
	
	public int getServerId() {
		return serverId;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getRoomname() {
		return roomname;
	}
	
	public int getMessageCount() {
		return chatlog.getSize();
	}
	
	
	
	public void prepare()
	throws TerminateClientException {
		// Single preparation for the entire life of the instance
		if (clientRef != null)
			throw new IllegalStateException("Cannot prepare the client more than once");
		
		// reset everything
		serverId      = -1;
		serverRef     = null;
		username      = null;
		roomname      = null;
		chatlog       = null;
		dlgMsgProc    = null;
		dlgLogMsgProc = null;
		roomRef       = null;
		
		try {
			clientRef = (RemoteClientModel) UnicastRemoteObject.exportObject(this, 0);
		}
		catch (RemoteException re) {
			terminate();
			throw new TerminateClientException("Failed to open the client for incoming connections", re);
		}
	}
	
	public void terminate() {
		if (clientRef == null)
			return;
		
		try {
			UnicastRemoteObject.unexportObject(this, true);
		}
		catch (NoSuchObjectException nsoe) {
			throw new IllegalStateException("Failedto unexport the client", nsoe);
		}
		finally {
			clientRef     = null;
			serverId      = -1;
			serverRef     = null;
			username      = null;
			roomname      = null;
			chatlog       = null;
			dlgMsgProc    = null;
			dlgLogMsgProc = null;
			roomRef       = null;
		}
	}
	
	public void connect(int id)
	throws TerminateClientException {
		// Need to be prepared
		if (clientRef == null)
			throw new IllegalStateException("Cannot connect to a server until prepared");
		
		// reset everything
		username      = null;
		roomname      = null;
		chatlog       = null;
		dlgMsgProc    = null;
		dlgLogMsgProc = null;
		roomRef       = null;
		
		String host;
		int    port;
		try {
			// Parse server address
			String   address    = config.getRegistryAddress(id);
			String[] components = address.split(":");
			
			serverId = id;
			host = components[0];
			port = Integer.parseInt(components[1]);
		}
		catch (NumberFormatException | IndexOutOfBoundsException ex) {
			terminate();
			throw new IllegalStateException("Error parsing server address", ex);
		}
		
		Registry registry;
		try {
			// Get the registry of the server
			registry = LocateRegistry.getRegistry(host, port);
		}
		catch (RemoteException re) {
			terminate();
			throw new TerminateClientException("Cannot access the registry", re);
		}
		
		try {
			serverRef = (RemoteServerModel) registry.lookup(config.getServerRegisteredName());
		}
		catch (ServerException se) {
			terminate();
			throw new TerminateClientException("Access to registry denied", se);
		}
		catch (RemoteException re) {
			terminate();
			throw new TerminateClientException("Communication with the registry failed");
		}
		catch (NotBoundException nbe) {
			terminate();
			throw new TerminateClientException("Server not found on the registry");
		}
	}
	
	public void join(String room, String user, ProcessMessagesDelegate procMsg, ProcessMessagesDelegate procLogMsg)
	throws TerminateClientException {
		// Need to be prepared and connected
		if (clientRef == null || serverRef == null)
			throw new IllegalStateException("Cannot join a room until prepared and connected to a server");
		
		try {
			username      = user;
			roomname      = room;
			chatlog       = new ChatLog(config.getInitialChatLogCapacity());
			dlgMsgProc    = procMsg;
			dlgLogMsgProc = procLogMsg;
			roomRef       = serverRef.reference(roomname);
			
			if (roomRef == null)
				throw getValidityCheckException("room reference request");
		}
		catch(RemoteException re) {
			terminate();
			throw new TerminateClientException("Failed to retrieve a reference to room '" + roomname + "'", re);
		}
		
		try {
			// join the chat room
			int retcode = roomRef.join(user, clientRef);
			if (retcode == RemoteRoomModel.VALIDITY_CHECK_FAILED) {
				// terminate() only required for TerminateClientExceptions
				throw getValidityCheckException("room join request");
			}
			else if (retcode == RemoteRoomModel.ROOM_IS_FULL) {
				terminate();
				throw new TerminateClientException("Room '" + roomname + "' is full");
			}
			else if (retcode != RemoteRoomModel.SUCCESSFUL_JOIN) {
				terminate();
				throw getServerResponseException(retcode);
			}
		}
		catch(RemoteException re) {
			terminate();
			throw new TerminateClientException("Failed to join the room", re);
		}
	}
	
	public void send(String text)
	throws TerminateClientException {
		// Need to be prepared, connected and inside a room
		if (clientRef == null || serverRef == null || roomRef == null)
			throw new IllegalStateException("Cannot send messages until prepared, connected to a server and inside a room");
		
		try {
			// create message
			ChatMessage message = new ChatMessage(username, text);
			
			// send message to chat room
			int retcode = roomRef.submit(clientRef, message);
			if (retcode == RemoteRoomModel.VALIDITY_CHECK_FAILED) {
				// terminate() only required for TerminateClientExceptions
				throw getValidityCheckException("message submission");
			}
			else if (retcode != RemoteRoomModel.MESSAGE_SUBMITTED) {
				terminate();
				throw getServerResponseException(retcode);
			}
		}
		catch(RemoteException re) {
			terminate();
			throw new TerminateClientException("Failed to send message", re);
		}
	}
	
	private IllegalArgumentException getValidityCheckException(String op) {
		return new IllegalArgumentException("Validity check of '" + op + "' failed");
	}
	
	private TerminateClientException getServerResponseException(int retcode) {
		return new TerminateClientException("Unexpected response from server (" + retcode + ")");
	}
	
	
	
	@Override
	public synchronized void receive(ChatMessage[] batch)
	throws RemoteException {
		// Need to be prepared, connected and inside a room
		if (clientRef == null || serverRef == null || roomRef == null)
			throw new IllegalStateException("Cannot receive messages until prepared, connected to a server and inside a room");
		
		chatlog.addMessageBatch(batch);
		if (dlgMsgProc != null)
			dlgMsgProc.process(batch);
	}
	
	@Override
	public synchronized void chatlog(ChatMessage[] batch)
	throws RemoteException {
		// Need to be prepared, connected and inside a room
		if (clientRef == null || serverRef == null || roomRef == null)
			throw new IllegalStateException("Cannot receive messages until prepared, connected to a server and inside a room");
		
		chatlog.addMessageBatch(batch);
		if (dlgLogMsgProc != null)
			dlgLogMsgProc.process(batch);
	}
}
