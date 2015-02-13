package zolera.chat.client;

import java.io.*;
import java.util.*;

import zolera.chat.infrastructure.*;

public class CLIView {
	private ServerConfiguration config;
	private Scanner input;
	private int serverId;
	
	private String username;
	private String roomname;
	
	private String     lastMsgUser;
	private ClientModel client;
	
	private ProcessMessagesDelegate procMsg;
	private ProcessMessagesDelegate procLogMsg;
	
	public CLIView() {
		input    = null;
		serverId = -1;
		config   = ServerConfiguration.getDefaultConfiguration();
		
		username = null;
		roomname = config.getDefaultRoomname(); // we currently don't support multiple chat rooms
		
		lastMsgUser = null;
		client      = new ClientModel();
		
		procMsg = new ProcessMessagesDelegate() {
			public void process(ChatMessage[] batch) {
				printMessages(batch);
			}
		};
		
		procLogMsg = new ProcessMessagesDelegate() {
			public void process(ChatMessage[] batch) {
				printLogMessages(batch);
			}
		};
	}
	
	public void run(InputStream is) {
		input = new Scanner(is);
		
		try {
			System.out.println("ZoleraChat Client started\n");
			selectServer();
			selectName();
			
			client.prepare();
			client.connect(serverId);
			client.join(roomname, username, procMsg, procLogMsg);
			serviceLoop();
		}
		catch (TerminateClientException tce) {
			// gracefully terminate client when it hits an unrecoverable exception
			if (DebuggingTools.DEBUG_MODE)
				throw new RuntimeException("Client termination requested", tce);
			else {
				System.out.println("\nError: " + tce.getMessage());
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
	
	private void serviceLoop()
	throws TerminateClientException {
		// We break out when a special termination string is submitted as a Message
		while (true) {
			String line = nextInputLine();
			
			if (line.equals(""))
				continue;   // ignore empty lines
			else if (config.getClientTerminationString().toLowerCase().equals(line.toLowerCase()))
				break;   // exit service loop
			else
				client.send(line);
		}
	}
	
	private String nextInputLine()
	throws TerminateClientException {
		if (!input.hasNextLine())
			throw new TerminateClientException("Input stream reached EOF");
		
		return input.nextLine();
	}
	
	
	
	
	
	private void printMessages(ChatMessage[] batch) {
		for (int m = 0; m < batch.length; m++) {
			ChatMessage msg = batch[m];
			String  sender  = msg.getSenderName();
			String  text    = msg.getMessageText();
			boolean sysmsg  = sender.equals(config.getSystemMessagesUsername());
			
			// print sender header
			if (sysmsg)
				lastMsgUser = null;
			else if (!sender.equals(lastMsgUser)) {
				lastMsgUser = sender;
				System.out.println(sender + ":");
			}
			
			// print system header
			if (sysmsg)
				System.out.print(">>>");
			
			// print message
			System.out.println("\t" + text);
		}
	}
	
	private void printLogMessages(ChatMessage[] batch) {
		System.out.println("----- Chat Room '" + roomname + "' (message log) -----");
		printMessages(batch);
		System.out.println("----- Chat Room '" + roomname + "' (end of log) -----");
	}
	
	public static void main(String args[]) {
		CLIView client = new CLIView();
		client.run(System.in);
	}
}