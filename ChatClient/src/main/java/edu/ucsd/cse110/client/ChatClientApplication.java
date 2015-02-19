package edu.ucsd.cse110.client;

import java.net.URISyntaxException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;

public class ChatClientApplication {

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
	
	public static void main(String[] args) 
	{
        Scanner in = new Scanner(System.in);
        String input = null;
        String recipient = null;
        
        ChatClient client = null;
		
        // Specify server
        
        while (true)
        {
        	System.out.println("Servers:");
        	System.out.println("\t(1) Test server 1");
        	System.out.println("\t(2) Test server 2");
        	System.out.print("Please specify server: ");
        	
        	int selection;
        	
        	try
        	{
        		selection = in.nextInt();
        	}
        	catch (InputMismatchException e)
        	{
        		System.out.println("Please enter choice 1 or 2");
        		continue;
        	}
        	catch (NoSuchElementException e)
        	{
        		System.out.println("Please enter choice 1 or 2");
        		continue;
        	}
        	
        	if (selection == 2)
        	{
        		System.out.println("Test server 2 not avalible.");
        	}
        	else if (selection != 1)
        	{
        		System.out.println("Please enter choice 1 or 2");
        	}
        	else
        	{
	    		try 
	    		{
	    			
	    			/* 
	    			 * We have some other function wire the ChatClient 
	    			 * to the communication platform
	    			 */
	    			client = wireClient();
	    	        System.out.println("ChatClient connected.");
	    			/* 
	    			 * Now we can happily send messages around
	    			 */
		    		break;
		    		
	    		} catch (JMSException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		} catch (URISyntaxException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		
        	}
        }
        
        in.nextLine();
        input = null;
        
        // User login info
        String username = null;
        String password = null;
        
        while (true)
        {
	        while (true)
	        {
	        	System.out.print("Please enter user name: ");
	        	
	        	input = in.nextLine();
	        	if (input == null)
	        	{
	        		continue;
	        	}
	        	else if (input.contains(" ,"))
	        	{
	        		System.out.println("User name cannot contain any spaces or commas.");
	        		continue;
	        	}
	        	else
	        	{
	        		username = input;
	        		break;
	        	}
	        }
	        
	        input = null;
	        
	        while (true)
	        {
	        	System.out.print("Please enter password: ");
	        	
	        	input = in.nextLine();
	        	if (input == null)
	        	{
	        		continue;
	        	}
	        	else
	        	{
	        		password = input;
	        		break;
	        	}
	        }
	        
	        System.out.println("Attempting login...");
	        
	        if (client.attemptLogin(username, password)) break;
        }
        
        input = null;
        
        System.out.println("Start chatting!");
        
        // Start Chatting
        while (true)
        {
        	input = in.nextLine();
        	
        	// isn't a next line
        	if (input == null)
        	{
        		
        	}
        	// special command
        	else if (input.startsWith("\\"))
        	{
        		input = input.substring(1);
        		
        		String cmd = null;
        		
        		if (input.indexOf(' ') == -1)
        		{
        			cmd = input;
        		}
        		else
        		{
        			cmd = input.substring(0, input.indexOf(' '));
        		}
        		
        		if (cmd.equals("quit"))
        		{
        			break;
        		}
        		else if (cmd.equals("help"))
        		{
        			System.out.println("Valid commands are \\help \\list \\chatroom \\to \\quit");
        		}
        		else if (cmd.equals("list"))
        		{
        			List<String> users = client.getUserList();
        			
        			if (users.size() < 1)
        			{
        				System.out.println("Error, no user list returned from server.");
        			}
        			else
        			{
        				System.out.println("Online users:");
        				while (users.size() > 0)
        				{
        					System.out.println(users.remove(0));
        				}
        				System.out.println();
        			}
        		}
        		else if (cmd.equals("listchatrooms"))
        		{
        			List<String> users = client.getChatrooms();
        			
        			if (users.size() < 1)
        			{
        				System.out.println("Error, no chatroom list returned from server.");
        			}
        			else
        			{
        				System.out.println("Chatrooms:");
        				while (users.size() > 0)
        				{
        					System.out.println(users.remove(0));
        				}
        				System.out.println();
        			}
        		}
        		else if (cmd.equals("logout"))
        		{
        			client.logout();
        			System.exit(0);
        		}
        		else if (cmd.equals("chatroom"))
        		{
        			input = input.substring(input.indexOf(' ') + 1);
        			client.enterChatroom(input);
        			
        			System.out.println("Now in " + client.getChatroom() + ".");
        		}
        		else if (cmd.equals("to"))
        		{
        			input = input.substring(input.indexOf(' ') + 1);
        			
        			if (input.indexOf(' ') == -1)
        				recipient = input;
        			else
        				recipient = input.substring(0, input.indexOf(' '));
        			
        			if (recipient.equals("all")) recipient = null;
        		}
        		else
        		{
        			System.out.println("Invalid command. Type \"\\help\" for a list of commands." );
        		}
        	}
        	// empty line
        	else if (input.equals(""))
        	{
        		
        	}
        	// actual message
        	else
        	{
        		client.sendUserMessage(recipient, input);
        	}
        	
        	// Get message since enter press
        	client.pollMessages();
        	
        	while (true)
        	{
        		//System.out.println("Checking messages: ");
        		String msg = client.getNextUserMessage();
        		if (msg == null) break;
        		else System.out.println(msg);
        	}
        }
		
        // Auto signoff stuff should be here
        
        System.exit(0);

	}

}
