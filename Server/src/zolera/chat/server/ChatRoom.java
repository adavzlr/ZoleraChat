package zolera.chat.server;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import zolera.chat.infrastructure.*;

public class ChatRoom
implements IChatRoom, Runnable {
	private ServerConfiguration config;
	private String roomname;
	
	private Map<IChatClient, ChatClientHandle> clients;
	private int maxCapacity;	
	
	private Queue<ChatMessage> pending;
	private ChatLog            chatlog;
	private Thread             consumerThread;
	
	private IChatRoom roomRef;
	
	public ChatRoom(String name, int capacity)
	throws RemoteException {
		config   = ServerConfiguration.getGlobal();
		roomname = name;
		
		maxCapacity = capacity;
		clients     = new HashMap<IChatClient, ChatClientHandle>(maxCapacity);
		
		pending = new ArrayDeque<ChatMessage>(maxCapacity * config.getInitialMessagesPerClient());
		chatlog = new ChatLog(config.getInitialChatLogCapacity());
		consumerThread = new Thread(this);
		
		roomRef = (IChatRoom) UnicastRemoteObject.exportObject(this, 0);
	}
	
	public synchronized IChatRoom getRoomRef() {
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
		
		Iterator<ChatClientHandle> iterator = clients.values().iterator();
		while(iterator.hasNext()) {
			ChatClientHandle handle    = iterator.next();
			
			try {
				if (handle.isReady()) {
					sendMessageBatchToClient(handle, msg);
				}
				else {
					long timeSinceCreation = System.currentTimeMillis() - handle.getCreationTime();
					
					if (timeSinceCreation > config.getWaitReadyTimeoutMillis())
						throw getRemovingUserException(handle, "ready timeout", null);
				}
			}
			catch (DeadClientException dce) {
				// Remove client when we determine it is unresponsive (timeout or RMI exceptions)
				
				if (DebuggingTools.DEBUG_MODE)
					dce.printStackTrace();
				else
					System.out.println("\nError: " + dce.getMessage());
				
				addMessage(new ChatMessage(config.getSystemMessagesUsername(),"User " + handle.getUsername() + " left the room"));
				iterator.remove();
			}
		}
	}
	
	private synchronized void sendMessageBatchToClient(ChatClientHandle handle, ChatMessage[] msg)
	throws DeadClientException {
		try {
			handle.getClientRef().receiveBatch(msg);
		}
		catch (RemoteException re) {
			throw getRemovingUserException(handle, "RMI layer", re);
		}
	}
	
	private synchronized DeadClientException getRemovingUserException(ChatClientHandle handle, String reason, Throwable cause) {
		String message = "User '" + handle.getUsername() + "' needs to be removed "
	                     + "(because of " + reason + ") on room '" + roomname + "'";
		
		if (cause == null)
			return new DeadClientException(message);
		else
			return new DeadClientException(message, cause);
	}
	
	
	
	public synchronized boolean isClient(IChatClient clientRef) {
		if (clientRef == null)
			return false;
		
		return clients.containsKey(clientRef);
	}
	
	public synchronized boolean isFull() {
		return clients.size() >= maxCapacity;
	}
	
	public synchronized boolean addClient(String clientName, IChatClient clientRef) {
		if (isFull() || isClient(clientRef))
			return false;
		
		ChatClientHandle clientHandle = new ChatClientHandle(clientName, clientRef);
		clients.put(clientRef, clientHandle);
		
		return true;
	}
	
	public synchronized ChatClientHandle getClientHandle(IChatClient ref) {
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
	public synchronized int submit(IChatClient clientRef, ChatMessage msg)
	throws RemoteException {
		if (!submit_verifyValidity(clientRef, msg))
			return IChatRoom.VALIDITY_CHECK_FAILED;
		else {
			addMessage(msg);
			return IChatRoom.MESSAGE_SUBMITTED;
		}
	}
	
	private synchronized boolean submit_verifyValidity(IChatClient clientRef, ChatMessage msg) {
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
	public synchronized int ready(IChatClient clientRef)
	throws RemoteException {
		if (!ready_verifyValidity(clientRef))
			return IChatRoom.VALIDITY_CHECK_FAILED;
		
		ChatClientHandle handle = getClientHandle(clientRef);
		handle.setReady(true);
		
		// send all the messages on chatlog to the new client
		ChatMessage[] messages = chatlog.getAllMessages();
		clientRef.receiveLog(messages);
		
		// inform users of joining user
		addMessage(new ChatMessage(config.getSystemMessagesUsername(), "User '" + handle.getUsername() + "' joined the room"));
		
		return IChatRoom.READY_ACKNOWLEDGED;
	}
	
	private synchronized boolean ready_verifyValidity(IChatClient clientRef) {
		if (clientRef == null || !isClient(clientRef))
			return false;
		
		ChatClientHandle handle = getClientHandle(clientRef);
		if (handle.isReady())
			return false;
		
		return true;
	}
}