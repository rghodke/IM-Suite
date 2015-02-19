package edu.ucsd.cse110.server;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import edu.ucsd.cse110.server.Constants;

public class Server 
{
	JmsTemplate template = null;
	int messageNumber = 0;
	Map<String, Integer> messageNumbers = new TreeMap<String, Integer>();
	List<String> onlineUsers = new LinkedList<String>();
	Map<String, String> chatroomsByUser = new TreeMap<String, String>();
	
	private String processMessage (String msg)
	{
		// We only want messages NOT from the server
		if (!msg.startsWith("Server "))
		{	
			String from = msg.substring(0, msg.indexOf(' '));
			//System.out.println("Message from: " + from);
			
			// Remove name
			msg = msg.substring( msg.indexOf(' ') + 1 );
			
			// Get message number
			//int msgNumber = Integer.parseInt( msg.substring(0, msg.indexOf(' ')) );
			msg = msg.substring( msg.indexOf(' ') + 1 );
			
			// Have we heard from him before?
			if (!messageNumbers.containsKey(from))
			{
				messageNumbers.put(from, -1);
			}
			
			//System.out.println( from + "'s old number:" + messageNumbers.get(from));
			//System.out.println("Msg number: " + messageNumber);
			
			if (true) //Ignoring dupes for now... messageNumbers.get(from) < messageNumber)
			{
				messageNumbers.put(from, messageNumber);
				
				// Make sure it's for us
				if (msg.startsWith("Server "))
				{
					//System.out.println("Minus address: " + msg.substring(clientName.length() + 1));
					return msg.substring(7);
				}
			}
		}
		
		// We drop out if it is not formatted correctly
		return null;
	}
	
	private List<String> getUsersInChatroom (String chatroom)
	{
		//System.out.println("In getUsersInChatroom(" + chatroom + ")");
		List<String> users = new LinkedList<String>();
		
		//System.out.println("Getting users in chatroom: " + chatroom);
		
		for (String u : onlineUsers)
		{
			//System.out.println("\tChecking: " + u);
			
			//System.out.println("\t\tis in " + chatroomsByUser.get(u));
			if (chatroomsByUser.get(u).equals(chatroom))
			{
				//System.out.println("\t\t\tadding");
				users.add(u);
			}
		}
		
		//System.out.println("\treturning " + users);
		
		return users;
	}
	
	// This class handles all messages from clients
	public void receive(String msg)
	{
		System.out.println("Got: " + msg);
		
		String from = msg.substring(0, msg.indexOf(' '));

		msg = processMessage(msg);
		
		//System.out.println("Processed Message: " + msg);
		
		if (!onlineUsers.contains(from) && !msg.startsWith(Constants.LOGIN_REQUEST))
		{
			return;
		}

		if (msg.startsWith(Constants.LOGIN_REQUEST))
		{
			//System.out.println("Login request");
			msg = msg.substring(Constants.LOGIN_REQUEST.length());
			String username = from;
			msg = msg.substring(username.length()+1);
			String password = msg;
			
			attemptLogin(username, password);
		}
		else if (msg.startsWith(Constants.USER_LIST_REQUEST))
		{
			//System.out.println("User list request");
			sendUserList(from, chatroomsByUser.get(from));
		}
		else if (msg.startsWith(Constants.CHATROOM_LIST_REQUEST))
		{
			//System.out.println("Chatroom list request");
			sendChatroomList(from);
		}
		else if (msg.startsWith(Constants.USER_MESSAGE))
		{
			//System.out.println("User message");
			msg = msg.substring(msg.indexOf(' ') + 1);
			String to = getLongName(msg.substring(0, msg.indexOf(' ')));
			msg = msg.substring(msg.indexOf(' ') + 1);
			from = getShortName(from);
			send(Constants.USER_MESSAGE + " " + from + " " + msg, to);
		}
		else if (msg.startsWith(Constants.BROADCAST_MESSAGE))
		{
			List<String> recipients = null;
			
			//System.out.println("Broadcast message");
			
			msg = msg.substring(msg.indexOf(' ') + 1);
			
			String usersChatroom = null;
			
			try
			{
				usersChatroom = chatroomsByUser.get(from);
			}
			catch (Exception e)
			{
				System.out.println("Problem getting chatroom");
			}
			
			//System.out.println("Calling getUsersInChatroom(" + usersChatroom + ")");
			
			recipients = getUsersInChatroom(usersChatroom);
			
			//System.out.println(recipients);
			
			from = getShortName(from);
			
			for (String r : recipients)
			{
				send(Constants.USER_MESSAGE + " " + from + " " + msg, r);
			}
		}
		else if (msg.startsWith(Constants.ENTER_CHATROOM_REQUEST))
		{
			//System.out.println("Enter chatroom request");
			msg = msg.substring(msg.indexOf(' ') + 1);
			chatroomsByUser.put(from, msg);
			send(Constants.ENTER_CHATROOM_RESPONSE + " " + msg, from);
		}
		else if (msg.startsWith(Constants.LOGOUT))
		{
			chatroomsByUser.remove(from);
			onlineUsers.remove(from);
		}
		else
		{
			System.out.println("Got bad message!: " + msg);
		}
	}
	
	private void sendChatroomList(String from) 
	{
		String rooms = Constants.CHATROOM_LIST_RESPONSE;
		
		List<String> list = getChatrooms();
		
		for (int u = 0; u < list.size(); u += 1)
		{
			rooms = rooms + " " + list.get(u);
		}
		
		send(rooms, from);
	}
	
	private List<String> getChatrooms()
	{
		List<String> rooms = new LinkedList<String>();
		
		Iterator<Entry<String, String>> it = chatroomsByUser.entrySet().iterator();
		
	    for (String u : onlineUsers)
	    {
	    	if (rooms.contains(chatroomsByUser.get(u)))
	    	{
	    		
	    	}
	    	else
	    	{
	    		rooms.add(chatroomsByUser.get(u));
	    	}
	    }
	    
	    if (!rooms.contains("Lobby"))
	    {
	    	rooms.add("Lobby");
	    }
	    
	    if (!rooms.contains("Support"))
	    {
	    	rooms.add("Support");
	    }
	    
	    if (!rooms.contains("Sales"))
	    {
	    	rooms.add("Sales");
	    }
	    
	    if (!rooms.contains("Service"))
	    {
	    	rooms.add("Service");
	    }
	    
		
		return rooms;
	}

	private void sendUserList(String to, String chatroom) 
	{
		String users = Constants.USER_LIST_RESPONSE;
		
		System.out.println(chatroomsByUser);
		
		List<String> list = getUsersInChatroom(chatroom);
		
		for (int u = 0; u < list.size(); u += 1)
		{
			users = users + " " + list.get(u);
		}
		
		send(users, to);
	}

	private void attemptLogin (String username, String password)
	{
		//System.out.println("In attempt login username: " + username + " password: " + password);
		String shortName = getShortName(username);
		
		if (getLongName(shortName) != null)
		{
			send(Constants.LOGIN_RESPONSE + "Bad", username);
		}
		else
		{
			send(Constants.LOGIN_RESPONSE + "Confirmed", username);
			chatroomsByUser.put(username, "Lobby");
			onlineUsers.add(username);
		}
		
		//System.out.print("Online users: ");
		System.out.println(chatroomsByUser);
		
		return;
	}
	
	private String getLongName (String shortName)
	{
		//System.out.println("getLongName(" + shortName + ")");
		for (String u : onlineUsers)
		{
			if (u.startsWith(shortName))
			{
				//System.out.println("\tChecking " + u + ": " + getShortName(u) + " | " + shortName);
				if (getShortName(u).equals(shortName))
				{
					return u;
				}
			}
		}
		return null;
	}
	
	private String getShortName (String longName)
	{
		int l = longName.length();
		String shortName = longName.substring(0, l-Constants.UIDLength);
		//System.out.println("getShortName(" + longName + ")" + ": " + shortName);
		return shortName;
	}
	
	public void send (final String msg, final String to)
	{
		
		final String toSend = 
				"Server " + Integer.toString(messageNumber) + " " + to + " " + msg;
		
		System.out.println("Sending: " + toSend);
		
		messageNumber += 1;
		
		sendMessageToQueue(toSend);
	}
	
	private void sendMessageToQueue (final String msg)
	{		
		MessageCreator messageCreator = new MessageCreator() 
		{
			public Message createMessage(Session session) throws JMSException 
			{
				return session.createTextMessage(msg);
			}
        };
        
        int count = onlineUsers.size() + 5;
        
        System.out.println("x" + count);
        
        for (int r = 0; r < count; r += 1)
        	template.send(Constants.TO_CLIENT_QUEUE_NAME, messageCreator);
	}

	public void setTemplate(JmsTemplate jmsTemplate) 
	{
		template = jmsTemplate;
		
	}
}
