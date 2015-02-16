package zolera.chat.server;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import zolera.chat.infrastructure.*;

public class RoomModel
implements RemoteRoomModel, Runnable {
	private ServerConfiguration config;
	private String roomname;
	
	private Map<RemoteClientModel, ClientHandle> clients;
	private int maxCapacity;	
	
	private Queue<ChatMessage> pending;
	private ChatLog            chatlog;
	private Thread             consumerThread;
	
	private RemoteRoomModel roomRef;
	
	public RoomModel(String name, int capacity)
	throws RemoteException {
		config   = ServerConfiguration.getGlobal();
		roomname = name;
		
		maxCapacity = capacity;
		clients     = new HashMap<RemoteClientModel, ClientHandle>(maxCapacity);
		
		pending = new ArrayDeque<ChatMessage>(maxCapacity * config.getInitialMessagesPerClient());
		chatlog = new ChatLog(config.getInitialChatLogCapacity());
		consumerThread = new Thread(this);
		
		roomRef = (RemoteRoomModel) UnicastRemoteObject.exportObject(this, 0);
	}
	
	public synchronized RemoteRoomModel getReference() {
		return roomRef;
	}
	
	
	
	public void startConsumerThread() {
		consumerThread.start();
	}
	
	public void stopConsumerThread() {
		if (!consumerThread.isAlive())
			return;
		
		// interrupt thread and wait for it to end
		consumerThread.interrupt();
		while(consumerThread.isAlive()) {
			try {
				consumerThread.join();
			}
			catch (InterruptedException ie) {
				System.err.println("Error: (" + roomname + ") ");
				ie.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		// This is the consumerThread entry point
		consumerThreadLoop();
	}
	
	private void consumerThreadLoop() {
		while (true) {
			try {
				if (consumerThread.isInterrupted())
					throw new InterruptedException("Interruption detected in service loop");
				
				synchronized(this) {
					ChatMessage[] msg = getMessageBatch();
					
					if (msg != null)
						broadcastMessageBatchToClients(msg);
				}
			
				consumerThreadSleep();
			}
			catch (InterruptedException ie) {
				break;
			}
		}
	}
	
	private void consumerThreadSleep()
	throws InterruptedException {
		long ellapsedTime = 0;
		long startTime    = System.currentTimeMillis();
		long sleepTime    = config.getSleepTimeRoomConsumerThreadMillis();
		
		do {
			Thread.sleep(sleepTime - ellapsedTime);
			ellapsedTime = System.currentTimeMillis() - startTime;
		} while (ellapsedTime < sleepTime);
	}
	
	private synchronized void broadcastMessageBatchToClients(ChatMessage[] msg) {
		chatlog.addMessageBatch(msg);
		
		Iterator<ClientHandle> iterator = clients.values().iterator();
		while(iterator.hasNext()) {
			ClientHandle handle    = iterator.next();
			
			try {
				sendMessageBatchToClient(handle, msg);
			}
			catch (DeadClientException dce) {
				System.out.println("\n" + dce.getMessage());
				
				// Remove client when we determine it is unresponsive
				addMessage(new ChatMessage(config.getSystemMessagesUsername(),"User '" + handle.getUsername() + "' left the room"));
				iterator.remove();
			}
		}
	}
	
	private synchronized void sendMessageBatchToClient(ClientHandle handle, ChatMessage[] msg)
	throws DeadClientException {
		try {
			handle.getClientRef().receive(msg);
		}
		catch (RemoteException re) {
			throw getRemovingUserException(handle, "RMI layer", re);
		}
		catch (Exception e) {
			throw getRemovingUserException(handle, "thrown exception on client code", e);
		}
	}
	
	private synchronized DeadClientException getRemovingUserException(ClientHandle handle, String reason, Throwable cause) {
		String message = "User '" + handle.getUsername() + "' needs to be removed "
	                     + "(because of " + reason + ") on room '" + roomname + "'";
		
		if (cause == null)
			return new DeadClientException(message);
		else
			return new DeadClientException(message, cause);
	}
	
	
	
	public synchronized boolean isClient(RemoteClientModel clientRef) {
		if (clientRef == null)
			return false;
		
		return clients.containsKey(clientRef);
	}
	
	public synchronized boolean isFull() {
		return clients.size() >= maxCapacity;
	}
	
	public synchronized ClientHandle addClient(String clientName, RemoteClientModel clientRef) {
		ClientHandle handle = new ClientHandle(clientName, clientRef);
		clients.put(clientRef, handle);
		
		return handle;
	}
	
	public synchronized ClientHandle getClientHandle(RemoteClientModel ref) {
		return clients.get(ref);
	}
	
	private synchronized void addMessage(ChatMessage msg) {
		if (msg == null)
			return;
		
		pending.add(msg);
	}
	
	private synchronized ChatMessage[] getMessageBatch() {
		if (pending.isEmpty())
			return null;
		
		ChatMessage[] batch = new ChatMessage[pending.size()];
		for (int m = 0; m < batch.length; m++)
			batch[m] = pending.remove();
		
		return batch;
	}
	
	
	
	@Override
	public synchronized int join(String username, RemoteClientModel clientRef)
	throws RemoteException {
		if (!join_verifyValidity(username, clientRef))
			return RemoteRoomModel.VALIDITY_CHECK_FAILED;
		if (isFull())
			return RemoteRoomModel.ROOM_IS_FULL;
		
		addClient(username, clientRef);
		
		try {
			// send all the messages on chat log to the new client
			ChatMessage[] messages = chatlog.getAllMessages();
			clientRef.chatlog(messages);
		}
		catch (Exception e) {
			return RemoteRoomModel.ERROR_ON_CLIENT;
		}
		
		// inform users of joining user
		addMessage(new ChatMessage(config.getSystemMessagesUsername(), "User '" + username + "' joined the room"));
		
		return RemoteRoomModel.SUCCESSFUL_JOIN;
	}
	
	private boolean join_verifyValidity(String username, RemoteClientModel clientRef) {
		if (username == null || clientRef == null)
			return false;
		if (!username.matches(config.getUsernamePattern()))
			return false;
		if (isClient(clientRef))
			return false;
		
		return true;
	}
	
	@Override
	public synchronized int submit(RemoteClientModel clientRef, ChatMessage msg)
	throws RemoteException {
		if (!submit_verifyValidity(clientRef, msg))
			return RemoteRoomModel.VALIDITY_CHECK_FAILED;
		else {
			addMessage(msg);
			return RemoteRoomModel.MESSAGE_SUBMITTED;
		}
	}
	
	private synchronized boolean submit_verifyValidity(RemoteClientModel clientRef, ChatMessage msg) {
		if (clientRef == null || msg == null)
			return false;
		if (msg.getSenderName() == null || msg.getMessageText() == null)
			return false;
		if (!isClient(clientRef))
			return false;
		
		ClientHandle handle = getClientHandle(clientRef);
		if (!handle.getUsername().equals(msg.getSenderName()))
			return false;
		
		return true;
	}
}