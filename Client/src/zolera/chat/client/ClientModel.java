package zolera.chat.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import zolera.chat.infrastructure.*;

public class ClientModel
implements RemoteClientModel {
	private ServerConfiguration config;
	private RemoteClientModel clientRef;
	private ChatLog chatlog;
	private ProcessMessagesDelegate dlgMsgProc;
	private ProcessMessagesDelegate dlgLogMsgProc;
	
	private String      username;
	private String      roomname;
	private RemoteServerModel serverRef;
	private RemoteRoomModel   roomRef;
	
	
	public ClientModel() {
		config  = ServerConfiguration.getGlobal();
				
		clientRef     = null;
		dlgMsgProc    = null;
		dlgLogMsgProc = null;
		username      = null;
		roomname      = null;
		chatlog       = null;
		serverRef     = null;
		roomRef       = null;
	}
	
	
	
	public void prepare(ProcessMessagesDelegate procMsg, ProcessMessagesDelegate procLogMsg)
	throws TerminateClientException {
		// Single preparation for the entire life of the instance
		if (clientRef != null)
			throw new IllegalStateException("Cannot prepare the client more than once");
		
		// reset everything
		username  = null;
		roomname  = null;
		chatlog   = null;
		serverRef = null;
		roomRef   = null;
		
		try {
			clientRef     = (RemoteClientModel) UnicastRemoteObject.exportObject(this, 0);
			dlgMsgProc    = procMsg;
			dlgLogMsgProc = procLogMsg;
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	public void connect(int serverId)
	throws TerminateClientException {
		// Need to be prepared
		if (clientRef == null)
			throw new IllegalStateException("Cannot connect to a server until prepared");
		
		// reset everything.
		username  = null;
		roomname  = null;
		chatlog   = null;
		serverRef = null;
		roomRef   = null;
		
		try {
			// Parse server address
			String   serverAddress = config.getRegistryAddress(serverId);
			String[] serverInfo    = serverAddress.split(":");
			String   serverHost    = serverInfo[0];
			int      serverPort    = Integer.parseInt(serverInfo[1]);
			
			// Look for the server reference in the registry at the given address
			serverRef = (RemoteServerModel) LocateRegistry.getRegistry(serverHost, serverPort).lookup(config.getServerRegisteredName());
		}
		catch (NumberFormatException | IndexOutOfBoundsException ex) {
			throw new TerminateClientException("Error parsing server address", ex);
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
		catch (NotBoundException nbe) {
			throw new TerminateClientException("Server not bound to registry");
		}
	}
	
	public void join(String room, String user)
	throws TerminateClientException {
		// Need to be prepared and connected
		if (clientRef == null || serverRef == null)
			throw new IllegalStateException("Cannot join a room until prepared and connected to a server");
		
		// reset everything, but the server information
		username = user;
		roomname = room;
		chatlog  = new ChatLog(config.getInitialChatLogCapacity());
		roomRef  = null;
		
		try {
			// ask the server for a reference to the chat room
			roomRef = serverRef.reference(roomname);
			if (roomRef == null)
				throw getValidityCheckException("room reference request");
			
			// join the chatroom
			int retcode = roomRef.join(user, clientRef);
			if (retcode == RemoteRoomModel.ROOM_IS_FULL)
				throw new TerminateClientException("Room is full");
			else if (retcode != RemoteRoomModel.SUCCESSFUL_JOIN)
				throw getServerResponseException(retcode);
		}
		catch(RemoteException re) {
			throw getRMIException(re);
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
			if (retcode == RemoteRoomModel.VALIDITY_CHECK_FAILED)
				throw getValidityCheckException("message submission");
			else if (retcode != RemoteRoomModel.MESSAGE_SUBMITTED)
				throw getServerResponseException(retcode);
		}
		catch(RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	private TerminateClientException getRMIException(RemoteException re) {
		return new TerminateClientException("Failure on RMI layer", re);
	}
	
	private TerminateClientException getValidityCheckException(String op) {
		return new TerminateClientException("Validity check of '" + op + "' failed");
	}
	
	private TerminateClientException getServerResponseException(int retcode) {
		return new TerminateClientException("Unexpected response from server (" + retcode + ")");
	}
	
	
	
	@Override
	public synchronized void receive(ChatMessage[] batch)
	throws RemoteException {
		chatlog.addMessageBatch(batch);
		if (dlgMsgProc != null)
			dlgMsgProc.process(batch);
	}
	
	@Override
	public synchronized void chatlog(ChatMessage[] batch)
	throws RemoteException {
		chatlog.addMessageBatch(batch);
		if (dlgLogMsgProc != null)
			dlgLogMsgProc.process(batch);
	}
}
