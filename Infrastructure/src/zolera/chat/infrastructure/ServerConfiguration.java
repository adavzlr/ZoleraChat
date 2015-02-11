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
	
	private ServerConfiguration() {
		this(0);
	}
	
	private ServerConfiguration(int configId) {
		switch (configId) {
		case 0:
		default:
			serverRegisteredName     = "ZoleraChatServer";
			maxUsernameLength        = 10;
			usernamePattern          = "^\\w{1," + maxUsernameLength + "}$";
			maxRoomnameLength        = 15;
			roomnamePattern          = "^[\\w ]{1," + maxRoomnameLength + "}$";
			clientTerminationString  = "exit zolerachat";
			defaultRoomname          = "Default";
			maxRoomCapacity          = 10;
			initialChatLogCapacity   = 100;
			initialMessagesPerClient = 5;
			
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
	
	// Factory Methods
	public static ServerConfiguration getDefaultConfiguration() {
		return new ServerConfiguration();
	}
}