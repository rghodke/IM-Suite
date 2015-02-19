package edu.ucsd.cse110.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import edu.ucsd.cse110.server.Constants;

public class ChatClient {
	private MessageProducer producer;
	private Session session;
	private MessageConsumer consumer;
	
	private int messageNumber;
	private String clientName;
	private int serverMessageNumber;
	private List<String> recievedUserMessages;
	private String listMessage;
	private String chatroomListMessage;
	private String chatroom;
	private boolean loggedIn;
	
	public ChatClient(MessageProducer producer, MessageConsumer consumer, Session session) 
	{
		super();
		this.producer = producer;
		this.session = session;
		this.consumer = consumer;
		
		this.recievedUserMessages = new LinkedList<String>();
		messageNumber = 0;
		chatroom = "Lobby";
		loggedIn = false;
		chatroomListMessage = null;
		listMessage = null;
		clientName = null;
		
		serverMessageNumber = -1;
	}
	
	private String getConvertedName (String name)
	{
		int l = name.length();
		return name.substring(0, l-Constants.UIDLength);
	}
	
	private String getShortName (String longName)
	{
		int l = longName.length();
		String shortName = longName.substring(0, l-Constants.UIDLength);
		//System.out.println("getShortName(" + longName + ")" + ": " + shortName);
		return shortName;
	}
	
	public String getClientName ()
	{
		return getShortName(clientName);
	}
	
	public void setClientName (String name)
	{
		Random r = new Random(System.currentTimeMillis() + System.nanoTime());
		r = new Random(r.nextLong() + System.currentTimeMillis() + System.nanoTime());
		
		String rString = Long.toHexString(r.nextLong());
		
		while (rString.length() < Constants.UIDLength)
			rString = rString + "x";
		
		this.clientName = name + rString;
	}
	
	// This sends a user message to each user in the list of users
	public void sendUserMessage (List<String> to, String message)
	{
		if (to == null)
		{
			broadcast(message);
		}
		else
		{
			for (String addr : to)
				send(Constants.USER_MESSAGE + " " + addr + " " + message);
		}
	}
	
	// This sends s user message to the username "to"
	public void sendUserMessage (String to, String message)
	{
		if (to == null)
		{
			broadcast(message);
		}
		else
		{
			send(Constants.USER_MESSAGE + " " + to + " " + message);
		}
	}
	
	// This attempts to enter a particular chatroom
	public void enterChatroom (String chatroomName)
	{
		send(Constants.ENTER_CHATROOM_REQUEST + " " + chatroomName);
		
		pollMessages();
		
		getUserList();
	}
	
	public Vector<String> getChatrooms ()
	{
		Vector<String> rooms = new Vector<String>();
		
		send(Constants.CHATROOM_LIST_REQUEST);
		
		pollMessages();
		
		String response = chatroomListMessage;
		
		if (response == null)
		{
			System.out.println("No response from server.");
			return rooms;
		}
		
		if (response.startsWith(Constants.CHATROOM_LIST_RESPONSE))
		{
			response = response.substring(Constants.CHATROOM_LIST_RESPONSE.length() + 1);
			
			while (response.length() > 0)
			{
				int nextChatroomEndIndex = response.indexOf(' ');
				
				// last one badly formed
				if (nextChatroomEndIndex == 0)
				{
					System.err.println("Badly formed response to list request.");
					return rooms;
				}
				// last one
				else if (nextChatroomEndIndex == -1)
				{
					rooms.add(response);
					return rooms;
				}
				//others
				else
				{
					rooms.add(response.substring(0, nextChatroomEndIndex));
					response = response.substring(nextChatroomEndIndex + 1);
				}
			}
		}
		
		System.err.println("Bad response to list request: " + response);
		
		return rooms;
	}
	
	// This returns the chatroom you are currently in
	public String getChatroom ()
	{
		pollMessages();
		return chatroom;
	}
	
	public boolean hasMessage ()
	{
		pollMessages();
		return recievedUserMessages.size() > 0;
	}
	
	// This sends a user message to all users in the same chatroom
	public void broadcast (String msg)
	{
		send(Constants.BROADCAST_MESSAGE + " " + msg);
	}
	
	
	// This sends a logout message to the server
	public void logout()
	{
		if (loggedIn)
		{
			pollMessages();
			send(Constants.LOGOUT);
		}
		loggedIn = false;
	}
	
	// This is the same as enterChatroom("Lobby");
	public void leaveChatroom ()
	{
		enterChatroom("Lobby");
	}
	
	// Tries to log in
	public boolean attemptLogin (String username, String password)
	{
		if (loggedIn) logout();
		
		setClientName(username);
		
		username = clientName;
		
		long time = System.currentTimeMillis();
		int messageCount = 0;
        
		send(Constants.LOGIN_REQUEST + username + " " + password);
		
		while (true)
		{
			if (messageCount > 3) return false;
			//if (time - System.currentTimeMillis() > 1000) return false;
			
			pollMessages();
			
			if (loggedIn)
			{
				return true;
			}
			else
			{
				messageCount += 1;
			}
		}
	}
	
	// This function checks messages from the server and handle them, sorting them and processing them depending on type.
	public void pollMessages ()
	{
		if (clientName == null)
		{
			while (true)
			{
				String msg = popMessage(10);
				
				//System.err.println("popMessage: " + clientName + " got: " + msg);
				
				if (msg == null) return;
			}
		}
		
		while (true)
		{
			//System.out.println("Polling messages: ");
			String msg = getNextMessageInternal(20);
			
			//System.err.println("getNextMsgInt: " + clientName + " got: " + msg);
			
			if (msg == null)
			{
				break;
			}
			else if (msg.startsWith(Constants.USER_MESSAGE))
			{
				msg = msg.substring( msg.indexOf(' ') + 1 );
				String from = msg.substring(0, msg.indexOf(' '));
				msg = msg.substring( msg.indexOf(' ') + 1 );
				
				recievedUserMessages.add(from + ": " + msg);
			}
			else if (msg.startsWith(Constants.USER_LIST_RESPONSE))
			{
				listMessage = msg;
			}
			else if (msg.startsWith(Constants.CHATROOM_LIST_RESPONSE))
			{
				chatroomListMessage = msg;
			}
			else if (msg.startsWith(Constants.ENTER_CHATROOM_RESPONSE))
			{
				msg = msg.substring( msg.indexOf(' ') + 1);
				chatroom = msg;
			}
			else if (msg.startsWith(Constants.LOGIN_RESPONSE))
			{
				if (loggedIn) continue;
				
				if (msg.equals(Constants.LOGIN_RESPONSE + "Confirmed"))
				{
					//System.out.println("Login successful.");
					loggedIn = true;
				}
				else if (msg.equals(Constants.LOGIN_RESPONSE + "Bad"))
				{
					//System.out.println("Bad login information.");
				}
			}
		}
	}
	
	// This function get the next message sent from a user
	public String getNextUserMessage ()
	{
		if (recievedUserMessages.size() > 0)
			return recievedUserMessages.remove(0);
		else
			return null;
	}
	
	// This function get the next message to us from the server. It returns the bare message without addressing token in front
	private String getNextMessageInternal (long timeout)
	{
		while (true)
		{
			String msg = popMessage(timeout);
			
			if (msg == null) return null;
			
			//System.err.println("Got: " + msg);
			
			// We only want messages from the server
			if (msg.startsWith("Server"))
			{
				//System.out.println("Minus Name: " + msg.substring(7));
				
				msg = msg.substring(7);
				int msgNumber = Integer.parseInt( msg.substring(0, msg.indexOf(' ')) );
				msg = msg.substring( msg.indexOf(' ') + 1 );
				
				//System.out.println("Number: " + Integer.toString(msgNumber));
				//System.out.println("Minus Number: " + msg);
				
				// Make sure it's not a repeat
				if (serverMessageNumber < msgNumber)
				{
					serverMessageNumber = msgNumber;
					
					// Make sure it's for us
					if (msg.startsWith(clientName))
					{
						//System.out.println("Minus address: " + msg.substring(msg.indexOf(' ') + 1));
						return msg.substring(msg.indexOf(' ') + 1);
					}
				}
			}
		}
	}
	
	// This gets a raw message from the queue
	private String popMessage (long timeout)
	{
		Message msg = null;
		
		if (timeout == 0)
		{
			try 
			{
				msg = consumer.receiveNoWait();
			} 
			catch (JMSException e1) 
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else
		{
			try 
			{
				msg = consumer.receive(timeout);
			} 
			catch (JMSException e1) 
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
			
		if (msg == null)
		{
			//System.out.println("No message");
		}
		else if (msg instanceof TextMessage)
		{
			try {
				//System.err.println("recieve: " + clientName + " got: " + ((TextMessage) msg).getText());
				return ((TextMessage) msg).getText();
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Unexpected message type.");
		}
		
		return null;
	}
	
	// This sends a raw message to the server
	private void send(String msg)
	{
		System.err.println("Sending: " + msg);
		for (int r = 0; r < Constants.MAX_USERS; r += 1)
		{
			try 
			{
				producer.send(session.createTextMessage(clientName + " " + 
														Integer.toString(messageNumber) + " " + 
														"Server " + msg));
			} 
			catch (JMSException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		messageNumber += 1;
	}

	// This requests a user list update and then returns a list of users from the latest user list update
	public Vector<String> getUserList() 
	{
		send(Constants.USER_LIST_REQUEST);
		
		pollMessages();
		
		String response = listMessage;
		
		Vector<String> users = new Vector<String>();
		
		if (response == null)
		{
			System.out.println("No response from server.");
			return users;
		}
		
		if (response.startsWith(Constants.USER_LIST_RESPONSE))
		{
			response = response.substring(Constants.USER_LIST_RESPONSE.length() + 1);
			
			while (response.length() > 0)
			{
				int nextUsernameEndIndex = response.indexOf(' ');
				
				// last one badly formed
				if (nextUsernameEndIndex == 0)
				{
					System.err.println("Badly formed response to list request.");
					return users;
				}
				// last one
				else if (nextUsernameEndIndex == -1)
				{
					users.add(getConvertedName(response));
					return users;
				}
				//others
				else
				{
					users.add(getConvertedName(response.substring(0, nextUsernameEndIndex)));
					response = response.substring(nextUsernameEndIndex + 1);
				}
			}
		}
		
		System.err.println("Bad response to list request: " + response);
		return users;
	}
}
