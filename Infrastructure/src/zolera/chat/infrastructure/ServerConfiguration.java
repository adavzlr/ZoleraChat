package zolera.chat.infrastructure;

// Immutable
public final class ServerConfiguration {
	private static ServerConfiguration instance = null;
	
	public static synchronized ServerConfiguration getGlobal(ServerConfiguration inst) {
		if (inst != null)
			instance = inst;
		else if (instance == null)
			instance = getDefaultConfiguration();
		return instance;
	}
	
	public static synchronized ServerConfiguration getGlobal() {
		return getGlobal(null);
	}
	
	private final String serverRegisteredName;
	private final int    maxUsernameLength;
	private final String usernamePattern;
	private final int    maxRoomnameLength;
	private final String roomnamePattern;
	private final String clientTerminationString;
	private final String defaultRoomname;
	private final int    maxRoomCapacity;
	private final int    initialChatLogCapacity;
	private final int    initialMessagesPerClient;
	private final long   sleepTimeRoomConsumerThreadMillis;
	private final long   waitReadyTimeoutMillis;
	private final String systemMessagesUsername;
	private final String [] registryAddressesList;
	
	private ServerConfiguration() {
		this(0);
	}
	
	private ServerConfiguration(int configId) {
		switch (configId) {
		case 0:
		default:
			serverRegisteredName              = "ZoleraChatServer";
			maxUsernameLength                 = 15;
			usernamePattern                   = "^\\w{1," + maxUsernameLength + "}$";
			maxRoomnameLength                 = 30;
			roomnamePattern                   = "^[\\w ]{1," + maxRoomnameLength + "}$";
			clientTerminationString           = "exit zolerachat";
			defaultRoomname                   = "Default";
			maxRoomCapacity                   = 10;
			initialChatLogCapacity            = 100;
			initialMessagesPerClient          = 5;
			sleepTimeRoomConsumerThreadMillis = 100;
			waitReadyTimeoutMillis            = 5_000;
			systemMessagesUsername            = "ZoleraChatSys";
			registryAddressesList                        = new String[]{
					              					"localhost:1099",
					              					"localhost:1001",
					              					"localhost:1002",
					              					"localhost:1003"
					              				};
			break;
		}
	}
	
	// Getter Methods
	public String getServerRegisteredName() {
		return serverRegisteredName;
	}
	
	public int getMaxUsernameLength() {
		return maxUsernameLength;
	}
	
	public String getUsernamePattern() {
		return usernamePattern;
	}
	
	public int getMaxRoomnameLength() {
		return maxRoomnameLength;
	}
	
	public String getRoomnamePattern() {
		return roomnamePattern;
	}
	
	public String getClientTerminationString() {
		return clientTerminationString;
	}
	
	public String getDefaultRoomname() {
		return defaultRoomname;
	}
	
	public int getMaxRoomCapacity() {
		return maxRoomCapacity;
	}
	
	public int getInitialChatLogCapacity() {
		return initialChatLogCapacity;
	}
	
	public int getInitialMessagesPerClient() {
		return initialMessagesPerClient;
	}
	
	public long getSleepTimeRoomConsumerThreadMillis() {
		return sleepTimeRoomConsumerThreadMillis;
	}
	
	public long getWaitReadyTimeoutMillis() {
		return waitReadyTimeoutMillis;
	}
	
	public String getSystemMessagesUsername() {
		return systemMessagesUsername;
	}
	
	public String getRegistryAddress(int index){
		int length = registryAddressesList.length;
		
		if (index < 0 || index > length)
			throw new IndexOutOfBoundsException("Index (" + index + ") out of bounds [0," + length + "]");
		
		return registryAddressesList[index];
	}
	
	public int getRegistryAddressesListLength(){
		return registryAddressesList.length;
	}
	
	// Factory Methods
	public static ServerConfiguration getDefaultConfiguration() {
		return new ServerConfiguration();
	}
}