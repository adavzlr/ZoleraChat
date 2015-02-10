package zolera.chat.client;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;

import zolera.chat.infrastructure.*;

public class ChatClient
implements IChatClient {
	private String    username;
	
	private IChatClient clientRef;
	private IChatServer serverRef;
	private IChatRoom   roomRef;
	
	public ChatClient(String name) {
		username = name;
		connectToServer();		
	}
	
	public void connectToServer() {
		try {
			clientRef = (IChatClient) UnicastRemoteObject.exportObject(this, 0);
			serverRef = (IChatServer) LocateRegistry.getRegistry("localhost").lookup("ZoleraChatServer");
			roomRef   = serverRef.connect(username, clientRef);
			
			if (roomRef == null)
				throw new IllegalStateException("ZoleraChat Client: connection refused by server");
		}
		catch (Exception e) {
			throw new IllegalStateException("ZoleraChat Client: error connecting to server", e);
		}
	}
	
	public void startChatLoop() {
		System.out.println("ZoleraChat Client: Started. Logged in as " + username);
		
		Scanner input   = new Scanner(System.in);
		ChatMessage msg = new ChatMessage(username, null);
		
		while (input.hasNextLine()) {
			String line = input.nextLine();
			
			if (!line.equals("")) {
				msg.setMessageText(line);
				submitMessage(msg);
			}
		}
		
		input.close();
	}
	
	public void submitMessage(ChatMessage msg) {
		try {
			roomRef.submit(msg);
		}
		catch(Exception e) {
			IllegalStateException ise = new IllegalStateException("ZoleraChat Client: error submitting message", e);
			ise.printStackTrace();
		}
	}
	
	@Override
	public void receive(IChatMessage msg)
	throws RemoteException {
		System.out.println(msg.getSenderName() + ": " + msg.getMessageText());
	}
	
	public static void main(String args[]) {
		if (args.length < 1)
			throw new IllegalArgumentException("ZoleraChat Client: must provide username");
		
		ChatClient client = new ChatClient(args[0]);
		client.startChatLoop();
	}
}