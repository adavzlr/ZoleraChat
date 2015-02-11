package zolera.chat.client;

import java.io.*;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

import java.util.*;

import zolera.chat.infrastructure.*;

public class ChatClient
implements IChatClient {
	private Scanner input;
	private boolean running;
	private ServerConfiguration config;
	
	private String username;
	private String roomname;
	
	private String             lastMsgUser;
	private ChatLog            chatlog;
	
	private IChatClient clientRef;
	private IChatServer serverRef;
	private IChatRoom   roomRef;
	
	public ChatClient(InputStream is) {
		input   = new Scanner(is);
		running = false;
		config  = ServerConfiguration.getDefaultConfiguration();
		
		username = null;
		roomname = config.getDefaultRoomname(); // we currently don't support multiple chat rooms
		
		lastMsgUser = null;
		chatlog     = new ChatLog(config.getInitialChatLogCapacity());
		
		clientRef = null;
		serverRef = null;
		roomRef   = null;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void run() {
		running = true;
		
		try {
			System.out.println("ZoleraChat Client started\n");
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
		
		terminate();
		return;
	}
	
	private void terminate() {
		running = false;
		input.close();
	}
	
	private void selectName()
	throws TerminateClientException {
		// we break out when an appropriate name is chosen
		while (true) {
			String name, ok;
			
			System.out.print("Choose a username: ");
			name = nextInputLine();
			
			if (!name.matches(config.getUsernamePattern())) {
				System.out.println();
				System.out.println("Invalid username");
				System.out.println("Use alphanumeric characters and underscores only");
				System.out.println("Length must be between 1 and " + config.getMaxUsernameLength() + " characters");
				System.out.println("Try again");
				System.out.println();
				continue;
			}
			
			System.out.print("Is '" + name + "' OK? [Y/N]: ");
			ok = nextInputLine();
			
			if (!ok.matches("^[YN]$") || ok.equals("N")) {
				System.out.println("Try again");
				System.out.println();
				continue;
			}
			
			// if you get here it means the name is appropriate and we are done
			username = name;
			System.out.println("Your username is '" + username + "'");
			System.out.println();
			break;
		}
	}
	
	private void connectToServer()
	throws TerminateClientException {
		System.out.println("Connecting to server");
		
		try {
			clientRef = (IChatClient) UnicastRemoteObject.exportObject(this, 0);
			serverRef = (IChatServer) LocateRegistry.getRegistry("localhost").lookup("ZoleraChatServer");
		}
		catch(RemoteException re) {
			throw getRMIException(re);
		}
		catch(NotBoundException nbe) {
			throw new TerminateClientException("Server unavailable");
		}
	}
	
	private void joinChatRoom()
	throws TerminateClientException {
		System.out.println("Joining chat room '" + roomname + "'");
		
		try {
			int retcode = serverRef.connect(roomname, username, clientRef);
			
			if (retcode == IChatServer.CONNECTION_SUCCESSFUL)
				roomRef = serverRef.getRoomRef(roomname, clientRef);
			else if (retcode == IChatServer.ROOM_IS_FULL)
				throw new TerminateClientException("Chat room '" + roomname + "' is full");
			else
				throw getServerResponseException(retcode);
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
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
		submitReady();
		
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
	
	private void submitReady()
	throws TerminateClientException {
		try {
			int retcode = roomRef.ready(clientRef);
			if (retcode == IChatRoom.VALIDITY_CHECK_FAILED)
				throw getValidityCheckException("ready submission");
			else if (retcode != IChatRoom.READY_ACKNOWLEDGED)
				throw getServerResponseException(retcode);
		}
		catch (RemoteException re) {
			throw getRMIException(re);
		}
	}
	
	@Override
	public synchronized void receive(ChatMessage msg)
	throws RemoteException {
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
	public synchronized void receiveBatch(ChatMessage[] messages)
	throws RemoteException {
		System.out.println("----- Chat Room '" + roomname + "' (message log) -----");
		
		for (int m = 0; m < messages.length; m++)
			receive(messages[m]);
		
		System.out.println("----- Chat Room '" + roomname + "' (end of log) -----");
	}
	
	public static void main(String args[]) {
		ChatClient client = new ChatClient(System.in);
		client.run();
	}
}