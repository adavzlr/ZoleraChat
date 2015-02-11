package zolera.chat.server;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import zolera.chat.infrastructure.*;

public class ChatRoom
implements IChatRoom, Runnable {
	private String roomname;
	private Map<IChatClient, ChatClientHandle> clients;
	private int maxCapacity;	
	private Queue<ChatMessage> pending;
	private Thread consumerThread;
	
	private IChatRoom roomRef;
	
	public ChatRoom(String name, int capacity)
	throws RemoteException {
		roomname    = name;
		maxCapacity = capacity;
		clients     = new HashMap<IChatClient, ChatClientHandle>(maxCapacity);
		
		pending = new ArrayDeque<ChatMessage>(maxCapacity * ServerConfiguration.getGlobal().getInitialMessagesPerClient());
		consumerThread = new Thread(this);
		
		roomRef = (IChatRoom) UnicastRemoteObject.exportObject(this, 0);
	}
	
	public boolean isClient(IChatClient clientRef) {
		if (clientRef == null)
			return false;
		else
			return clients.containsKey(clientRef);
	}
	
	public boolean isFull() {
		return clients.size() >= maxCapacity;
	}
	
	public IChatRoom getRoomRef() {
		return roomRef;
	}
	
	public boolean addClient(String clientName, IChatClient clientRef) {
		if (isFull() || isClient(clientRef))
			return false;
		
		ChatClientHandle clientHandle = new ChatClientHandle(clientName);
		clients.put(clientRef, clientHandle);
		return true;
	}
	
	private void addMessage(ChatMessage msg) {
		if (msg == null)
			return;
		
		synchronized(pending) {
			pending.add(msg);
		}
	}
	
	private ChatMessage getMessage() {
		synchronized(pending) {
			if (pending.isEmpty())
				return null;
			else
				return pending.remove();
		}
	}
	
	private ChatClientHandle getClientHandle(IChatClient ref) {
		return clients.get(ref);
	}
	
	public void startConsumerThread() {
		consumerThread.start();
	}
	
	public void stopConsumerThread() {
		while (consumerThread.isAlive()) {
			try {
				consumerThread.join();
			}
			catch (InterruptedException ie) {
				System.out.println("Error: (" + roomname + ") " + ie.getMessage());
			}
		}
	}
	
	private void broadcastMessageToClients(ChatMessage msg) {
		for(ChatClientHandle handle : clients.values()) {
			if (handle.isReady()) {
				IChatClient clientRef = handle.getClientRef();
				clientRef.receive(msg);
			}
		}
	}
	
	@Override
	public int submit(IChatClient clientRef, ChatMessage msg)
	throws RemoteException {
		if (!submit_verifyValidity(clientRef, msg))
			return IChatRoom.VALIDITY_CHECK_FAILED;
		else {
			addMessage(msg);
			return IChatRoom.MESSAGE_SUBMITTED;
		}
	}
	
	private boolean submit_verifyValidity(IChatClient clientRef, ChatMessage msg) {
		if (clientRef == null || msg == null)
			return false;
		if (!isClient(clientRef))
			return false;
		if (msg.getSenderName() == null || msg.getMessageText() == null)
			return false;
		
		ChatClientHandle handle = getClientHandle(clientRef);
		if (!handle.isReady())
			return false;
		if (!handle.getUsername().equals(msg.getSenderName()))
			return false;
		
		return true;
	}

	@Override
	public int ready(IChatClient clientRef)
	throws RemoteException {
		if (!ready_verifyValidity(clientRef))
			return IChatRoom.VALIDITY_CHECK_FAILED;
		
		ChatClientHandle handle = getClientHandle(clientRef);
		handle.setReady(true);
		return IChatRoom.READY_ACKNOWLEDGED;
	}
	
	private boolean ready_verifyValidity(IChatClient clientRef) {
		if (clientRef == null || !isClient(clientRef))
			return false;
		
		ChatClientHandle handle = getClientHandle(clientRef);
		if (handle.isReady())
			return false;
		
		return true;
	}

	@Override
	public void run() {
		// This is the consumerThread entry point
		
	}
}