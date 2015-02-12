package zolera.chat.client;

import java.io.*;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

import java.util.*;

import zolera.chat.infrastructure.*;

public class ChatClient
implements IChatClient {
	private ServerConfiguration config;
	private Scanner input;
	private int serverId;
	
	private String username;
	private String roomname;
	
	private String  lastMsgUser;
	private ChatLog chatlog;
	
	private IChatClient clientRef;
	private IChatServer serverRef;
	private IChatRoom   roomRef;
	
	public ChatClient() {
		input    = null;
		serverId = -1;
		config   = ServerConfiguration.getDefaultConfiguration();
		
		username = null;
		roomname = config.getDefaultRoomname(); // we currently don't support multiple chat rooms
		
		lastMsgUser = null;
		chatlog     = new ChatLog(config.getInitialChatLogCapacity());
		
		clientRef = null;
		serverRef = null;
		roomRef   = null;
	}
	
	public void run(InputStream is) {
		input = new Scanner(is);
		
		try {
			System.out.println("ZoleraChat Client started\n");
			selectServer();
			selectName();
			
			connectToServer();
			joinChatRoom();
			serviceLoop();
		}
		catch (TerminateClientException tce) {
			// gracefully terminate client when it hits an unrecoverable exception
			if (DebuggingTools.DEBUG_MODE)
				throw new RuntimeException("Client termination requested", tce);
			else {
				System.out.println();
				System.out.println("Error: " + tce.getMessage());
				System.out.println("Terminating client");
			}
		}
		finally {
			input.close();
		}
	}
	
	private void selectServer()
	throws TerminateClientException {
		System.out.print("Choose a server id: ");
		String idLine = nextInputLine();
		
		if (!idLine.matches("^[0-9]{1,3}$"))
			throw new TerminateClientException("Server is is not an integer (" + idLine + ")");
		
		serverId = Integer.parseInt(idLine);
		if (serverId < 0 || serverId > config.getRegistryAddressesListLength())
			throw new TerminateClientException("Invalid server id (" + serverId + ")");
	}
	
	private void selectName()
	throws TerminateClientException {
		System.out.print("Choose a username: ");
		username = nextInputLine();
		
		if (!username.matches(config.getUsernamePattern()))
			throw new TerminateClientException();
		
		System.out.println("Your username is '" + username + "'\n");
	}
	
	private void connectToServer()
	throws TerminateClientException {
		System.out.println("Connecting to server");
		
		try {
			clientRef = (IChatClient) UnicastRemoteObject.exportObject(this, 0);
			serverRef = (IChatServer) LocateRegistry.getRegistry("localhost").lookup("ZoleraChatServer");
			roomRef   = serverRef.reference(roomname);
		}
		catch(RemoteException re) {
			throw getRMIException(re);
		}
		catch(NotBoundException nbe) {
			throw new TerminateClientException("Server unreachable");
		}
		
		if (roomRef == null)
			throw getValidityCheckException("room reference request");
	}
	
	private void joinChatRoom()
	throws TerminateClientException {
		int retcode;
		System.out.println("Joining chat room '" + roomname + "'");
		
		try {
			retcode = roomRef.join(username, clientRef);
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
		
		if (retcode == IChatRoom.ROOM_IS_FULL)
			throw new TerminateClientException("Room is full");
		else if (retcode != IChatRoom.SUCCESSFUL_JOIN)
			throw getServerResponseException(retcode);
	}
	
	private TerminateClientException getRMIException(RemoteException re) {
		return new TerminateClientException("Failure on RMI layer", re);
	}
	
	private TerminateClientException getValidityCheckException(String op) {
		return new TerminateClientException("Validity check of " + op + " failed");
	}
	
	private TerminateClientException getServerResponseException(int retcode) {
		return new TerminateClientException("Unexpected response from server (" + retcode + ")");
	}
	
	private void serviceLoop()
	throws TerminateClientException {
		// We break out when a special termination string is submitted as a Message
		while (true) {
			String line = nextInputLine();
			
			if (line.equals(""))
				continue;   // ignore empty lines
			else if (config.getClientTerminationString().toLowerCase().equals(line.toLowerCase()))
				break;   // exit service loop
			else {
				ChatMessage msg = new ChatMessage(username, line);
				submitMessage(msg);
			}
		}
	}
	
	private String nextInputLine()
	throws TerminateClientException {
		if (!input.hasNextLine())
			throw new TerminateClientException("Input stream reached EOF");
		
		return input.nextLine();
	}
	
	private void submitMessage(ChatMessage msg)
	throws TerminateClientException {
		try {
			int retcode = roomRef.submit(clientRef, msg);
			
			if (retcode == IChatRoom.VALIDITY_CHECK_FAILED)
				throw getValidityCheckException("message submission");
			else if (retcode != IChatRoom.MESSAGE_SUBMITTED)
				throw getServerResponseException(retcode);
		}
		catch(RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	@Override
	public synchronized void receive(ChatMessage[] batch)
	throws RemoteException {
		for (int m = 0; m < batch.length; m++)
			printMessage(batch[m]);
	}
	
	public synchronized void printMessage(ChatMessage msg) {
		String  sender = msg.getSenderName();
		String  text   = msg.getMessageText();
		boolean sysmsg = sender.equals(config.getSystemMessagesUsername());
		
		// add to log
		chatlog.addMessage(msg);
		
		// print sender header
		if (sysmsg)
			lastMsgUser = null;
		else if (!sender.equals(lastMsgUser)) {
			lastMsgUser = sender;
			System.out.println(sender + ":");
		}
		
		if (sysmsg)
			System.out.print(">>>");
		
		// print message
		System.out.println("\t" + text);
	}
	
	@Override
	public synchronized void chatlog(ChatMessage[] batch)
	throws RemoteException {
		System.out.println("----- Chat Room '" + roomname + "' (message log) -----");
		receive(batch);
		System.out.println("----- Chat Room '" + roomname + "' (end of log) -----");
	}
	
	public static void main(String args[]) {
		ChatClient client = new ChatClient();
		client.run(System.in);
	}
}