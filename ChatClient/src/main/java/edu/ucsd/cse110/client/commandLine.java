package edu.ucsd.cse110.client;

import java.io.Console;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;

public class commandLine {
	
	static private class CheckMessages extends Thread {
		ChatClient client;
		private CheckMessages(ChatClient client) {
			super("Check Msg Thread");
			this.client = client;
			this.start();
		}
		
		public void run() {
			while (true)
			{
				if (client.hasMessage())
					System.out.println(client.getNextUserMessage());
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
	
	/*
	 * This inner class is used to make sure we clean up when the client closes
	 */
	
	static private class CloseHook extends Thread {
		ActiveMQConnection connection;
		private CloseHook(ActiveMQConnection connection) {
			this.connection = connection;
		}
		
		public static Thread registerCloseHook(ActiveMQConnection connection) {
			Thread ret = new CloseHook(connection);
			Runtime.getRuntime().addShutdownHook(ret);
			return ret;
		}
		
		public void run() {
			try {
				System.out.println("Closing ActiveMQ connection");
				connection.close();
			} catch (JMSException e) {
				/* 
				 * This means that the connection was already closed or got 
				 * some error while closing. Given that we are closing the
				 * client we can safely ignore this.
				*/
			}
		}
	}
	
	/*
	 * This method wires the client class to the messaging platform
	 * Notice that ChatClient does not depend on ActiveMQ (the concrete 
	 * communication platform we use) but just in the standard JMS interface.
	 */
	private static ChatClient wireClient() throws JMSException, URISyntaxException {
		ActiveMQConnection connection = 
				ActiveMQConnection.makeConnection(
				/*Constants.USERNAME, Constants.PASSWORD,*/ Constants.ACTIVEMQ_URL);
        connection.start();
        CloseHook.registerCloseHook(connection);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue toClientQueue = session.createQueue(Constants.TO_CLIENT_QUEUE_NAME);
        Queue toServerQueue = session.createQueue(Constants.TO_SERVER_QUEUE_NAME);
        MessageProducer producer = session.createProducer(toServerQueue);
        MessageConsumer consumer = session.createConsumer(toClientQueue);
        return new ChatClient(producer, consumer, session);
	}
	
	

	public static void main(String args[]) throws IOException {

		String messagesTo = null;

		while(true) {

		/* Initially creating a scanner object to read from the command line
		 * and a ChatClient object in order to connect to the server and start
		 * communicating. */
		Scanner in = new Scanner(System.in);
		String input = null;
		
		while (true)
		{
			System.out.println("Please select a server:");
			System.out.println("1: " + Constants.ACTIVEMQ_URL);
			System.out.println("2: Other");
			
			input = in.nextLine();
			
			if (input.equals("1"))
			{
				break;
			}
			else
			{
				while (true)
				{
					System.out.println("Please enter an address: ");
					input = in.nextLine();
					System.out.println("Server not available.");
					break;
				}
			}
		}

		ChatClient client = null;
		
		try 
		{
			client = wireClient();
		} 
		catch (JMSException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (URISyntaxException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// create string to store username and password
		String username = null;

		// exit variable used to exit when called
		boolean exit = false;

		// infinite loop where the log-in happens. 
		while(true) {

			// get username from command line input
			while(true) {
				System.out.println("Please enter username: ");
				input = in.nextLine();

				// do some basic checks before accepting username input
				if(input == null) {
					System.out.println("Username field cannot be empty.");
					continue;
				}
				else if(input.contains(" ,")) {
					System.out.println("Username cannot contain spaces or commas.");
					continue;
				}
				else {
					username = input;
					break;
				}

			}

			// Get password using the console for input masking.
			Console console = System.console();
			
			String password = "";
			
			System.out.println("You enter: " + username);

			if(client.attemptLogin(username, password)) {
				break;
			}
			else {
				System.out.println("Wrong credentials.");
			}

		}

		// initialize current chat room as lobby.
		String currChatroom = "Lobby";

		// In the chat now, can start chatting, see who's online, etc.
		System.out.println("Start chatting or type \"\\help\" to see available commands");
		Thread msgCheck = new CheckMessages(client);
		
		while(true) 
		{

			// always check if there is a message waiting
			if(client.hasMessage()) {
				System.out.println(client.getNextUserMessage());
			}

			input = in.nextLine();

			// special input to print out what commands can be done.
			if(input.equals("\\help")) {
				printHelpOptions();
			}

			// quitting chat app.
			else if(input.equals("\\exit")) {

				client.logout();
				exit = true;
				break;
			}

			else if(input.equals("\\logout")) {

				client.logout();
				msgCheck.interrupt();
				break;
			}

			// Listing all online users.
			else if(input.equals("\\listusers")) {
				Vector<String> listUsers = client.getUserList();
				
				System.out.println("Users in the rooms:");
				
				// loop through vector acquired by chatclient and print out contents
				for(int i = 0; i < listUsers.size(); i++) {
					System.out.println(listUsers.get(i));
				}
			}

			// listing all available chat rooms.
			else if(input.equals("\\listchatrooms")) {
				Vector<String> rooms = client.getChatrooms();

				System.out.println("Chatrooms:");
				// loop through array and print out all chatrooms
				for(int i = 0; i < rooms.size(); i++) {
					System.out.println(rooms.get(i));
				}
			}

			// joining specified chatroom.
			else if(input.startsWith("\\joinchatroom")) {

				// get room list and name of the specified room.
				String roomName = input.substring(14);
				Vector<String> roomList = client.getChatrooms();

				// flag used to check if room exists
				boolean roomExists = false;

				// loop through list of rooms to check if specified room exists
				for(int i = 0; i < roomList.size(); i++) {
					
					if(roomList.contains(roomName)) {
						roomExists = true;
						break;
					}
				}

				// if the room exists, try to join the chatroom.
				if(true) {

					// update current chatroom
					currChatroom = roomName;

					// join chatroom.
					client.enterChatroom(roomName);			
				}
//
//				else {
//					// otherwise, tell user that room does not exist.
//					System.out.println("Specified room \"" + 
//										roomName + 
//										"\" does not exist. Try again using a room from this list:");
//
//					// loop through array and print out all chatrooms
//					for(int j = 0; j < roomList.size(); j++) {
//						System.out.println(roomList.get(j));
//					}
//
//				}

			}
			
			// send message to specified user
			else if(input.startsWith("\\pm")) {

				String specifiedUser = null;
				
				// checking if specified user is available to receive message.
				if (input.indexOf(' ') < 0)
				{
					System.out.println("No user specified. You are now in broadcast mode.");
				}
				else
				{
					specifiedUser = input.substring(input.indexOf(' ') + 1);
					System.out.println("Messages will now be sent to " + specifiedUser);
				}
				
				messagesTo = specifiedUser;
			}

			// checking if user wants to broadcast
			else if(input.equals("\\broadcast")) {

				// prompt user for a message to send
				System.out.println("You are now in broadcast mode.");
				
				messagesTo = null;
			}

			// exiting chatroom if user is in one.
			else if(input.equals("\\exitchatroom")) {

				// check if user is in a chat room.
				if(currChatroom == "Lobby") {

					// tell user that they are not in a chatroom.
					System.out.println("You are now in the lobby.");
				}

				// otherwise, leave the chatroom and update current chatroom.
				else{ 
					client.enterChatroom("Lobby");
					currChatroom = "Lobby";
				}

			}
			else if (input.startsWith("\\"))
			{
				System.out.println("Invalid command: " + input);
				printHelpOptions();
			}
			// if input is none of the above, send error message.
		 	else 
		 	{
		 		if (messagesTo == null)
		 			client.broadcast(input);
		 		else
		 			client.sendUserMessage(messagesTo, input);
		 	}



		}

		if(exit == true) {
			msgCheck.interrupt();
			System.exit(0);
			break; 
		}

	}


}



	// private helper method to print out available commands
	private static void printHelpOptions() {

		// Commands that can be used in general circumstances
		System.out.println(" General Commands:");
		System.out.println("\\exit: Will exit out of the chat program.");
		System.out.println("\\logout: Will exit out of the chat program. return to login");		
		System.out.println("\\listusers: Will show a list of all online users in current chat room or lobby.");
		System.out.println("\\listchatrooms: Will show a list of all chat rooms ");
		System.out.println("\\joinchatroom roomName: will join specified chat room");
		System.out.println("\\pm username: all messages entered after this command will go to the specified user.");
		System.out.println("\\broadcast: all messages entered after this command will be broadcast to all users in the chatroom. ");
		System.out.println("\\exitchatroom: will exit current chat room");

	}

}
