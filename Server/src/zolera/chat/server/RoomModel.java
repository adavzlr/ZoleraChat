package zolera.chat.server;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
	private Queue<ChatMessage> broadcast;
	private ChatLog            chatlog;
	private Thread             consumerThread;
	
	private RemoteRoomModel roomRef;
	private int[] serverBootStatus;
	private RemoteRoomModel [] remoteRooms;
	
	private int serverId;
	private int masterId;
	
	public static final int SERVER_UNKNOWN_STATUS     = 0;
	public static final int SERVER_ONLINE             = 617;
	public static final int SERVER_OFFLINE     = 619;
	
	public RoomModel(String name, int serverId)
	throws RemoteException {
		config   = ServerConfiguration.getGlobal();
		roomname = name;
		
		maxCapacity = config.getMaxRoomCapacity();
		clients     = new HashMap<>(maxCapacity);
		
		pending        = new ArrayDeque<>(config.getInitialMessageListCapacity());
		broadcast      = new ArrayDeque<>(config.getInitialMessageListCapacity());
		chatlog        = new ChatLog(config.getInitialChatLogCapacity());
		consumerThread = new Thread(this);
		this.serverId = serverId;
		masterId = -1;
		roomRef = (RemoteRoomModel) UnicastRemoteObject.exportObject(this, 0);
		remoteRooms = new RemoteRoomModel[config.getRegistryAddressesListLength()];
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
					if(masterId != -1)
					{
						broadcastMessageBatchToClients();
						processPendingMessageBatch();
					}
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
	
	
	
	private synchronized void processPendingMessageBatch() {
		ChatMessage[] batch = getPendingBatch();
		if (batch == null)
			return;
		
		addBroadcastBatch(batch);
	}
	
	private synchronized void broadcastMessageBatchToClients() {
		ChatMessage[] batch = getBroadcastBatch();
		if (batch == null)
			return;
		
		chatlog.addMessageBatch(batch);
		
		Iterator<ClientHandle> iterator = clients.values().iterator();
		while(iterator.hasNext()) {
			ClientHandle handle    = iterator.next();
			
			try {
				sendMessageBatchToClient(handle, batch);
			}
			catch (DeadClientException dce) {
				System.out.println("\n" + dce.getMessage());
				
				// Remove client when we determine it is unresponsive
				addPendingMessage(new ChatMessage(config.getSystemMessagesUsername(),"User '" + handle.getUsername() + "' left the room"));
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
	
	
	
	private synchronized void addPendingMessage(ChatMessage msg) {
		if (msg == null)
			return;
		
		pending.add(msg);
	}
	
	private synchronized ChatMessage[] getPendingBatch() {
		if (pending.isEmpty())
			return null;
		
		ChatMessage[] batch = new ChatMessage[pending.size()];
		for (int m = 0; m < batch.length; m++)
			batch[m] = pending.remove();
		
		return batch;
	}
	
	private synchronized void addBroadcastBatch(ChatMessage[] batch) {
		if (batch == null || batch.length == 0)
			return;
		
		for (int m = 0; m < batch.length; m++)
			broadcast.add(batch[m]);
	}
	
	private synchronized ChatMessage[] getBroadcastBatch() {
		if (broadcast.isEmpty())
			return null;
		
		ChatMessage[] batch = new ChatMessage[broadcast.size()];
		for (int m = 0; m < batch.length; m++)
			batch[m] = broadcast.remove();
		
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
		addPendingMessage(new ChatMessage(config.getSystemMessagesUsername(), "User '" + username + "' joined the room"));
		
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
			addPendingMessage(msg);
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

	@Override
	public void broadcast(ChatMessage[] batch)
	throws RemoteException {
		
	}

	@Override
	public synchronized void share(ChatMessage[] batch)
	throws RemoteException {
		
	}
	
	public synchronized boolean registerRoom(int serverId, RemoteRoomModel remoteRoom)
	throws RemoteException
	{
		remoteRooms[serverId] = remoteRoom;
		
		if(masterId != -1){
			if(amIMaster()){
				remoteRoom.setMaster(serverId);
			}
			return false;
		}
		else if(serverBootStatus[serverId] == SERVER_UNKNOWN_STATUS){
			serverBootStatus[serverId] = SERVER_ONLINE;
			return true;
		}
		else return serverBootStatus[serverId] == SERVER_ONLINE;
	}
	
	private boolean amIMaster(){
		return serverId == masterId;
	}
	
	public void setMaster(int masterId)
	{
		this.masterId = masterId;
		serverBootStatus = new int [ServerConfiguration.getDefaultConfiguration().getRegistryAddressesListLength()];
	}
	
	public void registerWithOthers() 
	throws TerminateServerException{
		serverBootStatus = new int[remoteRooms.length];
		boolean amIMasterable = true;
		for(int id = 0; id < remoteRooms.length; id++)
		{
			synchronized(this)
			{
				String host;
				int port;
				try {
					// Parse server address
					String   address    = config.getRegistryAddress(id);
					String[] components = address.split(":");
					
					serverId = id;
					host = components[0];
					port = Integer.parseInt(components[1]);
				}
				catch (NumberFormatException | IndexOutOfBoundsException ex) {
					throw new TerminateServerException("Error parsing server address", ex);
				}
				
				Registry registry;
				try {
					// Get the registry of the server
					registry = LocateRegistry.getRegistry(host, port);
					RemoteServerModel serverRef = (RemoteServerModel) registry.lookup(config.getServerRegisteredName());
					RemoteRoomModel remoteRoom = serverRef.reference(config.getDefaultRoomname());
					if(!remoteRoom.registerRoom(id, roomRef))
						amIMasterable = false;
					remoteRooms[id] = remoteRoom;
					serverBootStatus[id] = SERVER_ONLINE;
				}
				catch (NotBoundException | RemoteException e) {
					System.err.println("Error: (" + roomname + ") ");
					e.printStackTrace();
					serverBootStatus[id] = SERVER_OFFLINE;
					continue;
				}
			}
		}
		
		if(amIMasterable){
			synchronized(this){
				for(int i = 0; i < serverBootStatus.length; i++){
					if(serverBootStatus[i] == SERVER_ONLINE){
						if(i == serverId){
							becomeMaster(); // I AM THE SWORD MASTAAAAAA!!!!!!!!!!!!!
						}
						break;
					}
				}
			}
		}
		
		serverBootStatus = null;
	}
	
	private synchronized void becomeMaster(){
		masterId = serverId;
		
		for(int i = 0; i < remoteRooms.length; i++ ) {
			if(remoteRooms[i] != null){
				try {
					remoteRooms[i].setMaster(serverId);
				} catch (RemoteException e) {
					System.err.println("Error: (" + roomname + ") ");
					e.printStackTrace();
					removeRoom(i);
				}
			}
		}
	}
	
	private synchronized void removeRoom(int roomId)
	{
		remoteRooms[roomId] = null;
	}
}