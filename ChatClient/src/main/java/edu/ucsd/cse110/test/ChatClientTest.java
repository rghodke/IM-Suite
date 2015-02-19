package edu.ucsd.cse110.test;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ucsd.cse110.client.*;
import edu.ucsd.cse110.server.*;
import edu.ucsd.cse110.client.Constants;

public class ChatClientTest 
{
	
	static ChatClient client1 = null;
	static ChatClient client2 = null;
	static ChatClient client3 = null;
	static ChatServerApplication server = null;
	
	static private class CloseHook extends Thread 
	{
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
	private static ChatClient wireClient() throws JMSException, URISyntaxException 
	{
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
	
	@BeforeClass
	public static void initialSetup () throws JMSException, URISyntaxException
	{
		client1 = wireClient();
		client2 = wireClient();
		client3 = wireClient();
	}
	
	@After
	public void cleanup () throws Exception
	{
		client1.logout();
		client2.logout();
		client3.logout();
	}
	
	@Test
	public void testLogin () throws Exception
	{
		assertTrue(client1.attemptLogin("A", ""));
		assertTrue(client2.attemptLogin("B", ""));
		assertFalse(client3.attemptLogin("A", ""));
		assertTrue(client3.attemptLogin("C", ""));
		client1.logout();
		client3.logout();
		assertTrue(client3.attemptLogin("A", ""));
		client1.logout();
		client2.logout();
		client3.logout();
		
		assertTrue(client1.attemptLogin("D", ""));
		assertTrue(client2.attemptLogin("E", ""));
		
		assertFalse(client3.attemptLogin("D", ""));
		
		assertTrue(client3.attemptLogin("F", ""));
		client1.logout();
		client3.logout();
		assertTrue(client3.attemptLogin("D", ""));
		client1.logout();
		client2.logout();
		client3.logout();
		
		assertTrue(client1.attemptLogin("D", ""));
		assertTrue(client2.attemptLogin("E", ""));
		
		assertFalse(client3.attemptLogin("D", ""));
		
		assertTrue(client3.attemptLogin("F", ""));
		client1.logout();
		client3.logout();
		assertTrue(client3.attemptLogin("D", ""));
		client1.logout();
		client2.logout();
		client3.logout();
		
		assertTrue(client1.attemptLogin("A", ""));
		assertTrue(client2.attemptLogin("B", ""));
		assertFalse(client3.attemptLogin("A", ""));
		assertTrue(client3.attemptLogin("C", ""));
		client1.logout();
		client3.logout();
		assertTrue(client3.attemptLogin("A", ""));
		client1.logout();
		client2.logout();
		client3.logout();
	}
	
	@Test
	public void testChatroomsUserList () throws Exception
	{	
		List<String> users = new LinkedList<String>();
		
		assertTrue(client1.attemptLogin("client1", ""));
		assertTrue(client2.attemptLogin("client2", ""));
		assertTrue(client3.attemptLogin("client3", ""));
		
		users.add("client1");
		users.add("client2");
		users.add("client3");
		
		assertEquals(users, client1.getUserList());
		assertEquals(users, client2.getUserList());
		assertEquals(users, client3.getUserList());
		
		client1.enterChatroom("Basketball");
		client2.enterChatroom("Basketball");
		client3.enterChatroom("Basketball");
		
		assertEquals(users, client1.getUserList());
		assertEquals(users, client2.getUserList());
		assertEquals(users, client3.getUserList());
		
		client1.enterChatroom("Baseball");
		
		users.clear();
		users.add("client1");
		
		assertEquals(users, client1.getUserList());
		
		users.clear();
		users.add("client2");
		users.add("client3");
		
		assertEquals(users, client2.getUserList());
		assertEquals(users, client3.getUserList());
		
		client2.enterChatroom("Soccer");
		
		users.clear();
		users.add("client2");
		
		assertEquals(users, client2.getUserList());
		
		users.clear();
		users.add("client1");
		
		assertEquals(users, client1.getUserList());
		
		users.clear();
		users.add("client3");
		
		assertEquals(users, client3.getUserList());
		
		users.clear();
		
		client1.logout();
		client2.logout();
		client3.logout();
		
		assertTrue(client1.attemptLogin("client1", ""));
		assertTrue(client2.attemptLogin("client2", ""));
		assertTrue(client3.attemptLogin("client3", ""));
		
		users.add("client1");
		users.add("client2");
		users.add("client3");
		
		assertEquals(users, client1.getUserList());
		assertEquals(users, client2.getUserList());
		assertEquals(users, client3.getUserList());
		
		client1.enterChatroom("Basketball");
		client2.enterChatroom("Basketball");
		client3.enterChatroom("Basketball");
		
		assertEquals(users, client1.getUserList());
		assertEquals(users, client2.getUserList());
		assertEquals(users, client3.getUserList());
		
		client1.enterChatroom("Baseball");
		
		users.clear();
		users.add("client1");
		
		assertEquals(users, client1.getUserList());
		
		users.clear();
		users.add("client2");
		users.add("client3");
		
		assertEquals(users, client2.getUserList());
		assertEquals(users, client3.getUserList());
		
		client2.enterChatroom("Soccer");
		
		users.clear();
		users.add("client2");
		
		assertEquals(users, client2.getUserList());
		
		users.clear();
		users.add("client1");
		
		assertEquals(users, client1.getUserList());
		
		users.clear();
		users.add("client3");
		
		assertEquals(users, client3.getUserList());
		
		
		client1.logout();
		client2.logout();
		client3.logout();
	}
	
	@Test
	public void testGetChatrooms ()
	{
		Set<String> chatrooms = new HashSet<String>(); 
		chatrooms.add("Lobby");
		chatrooms.add("Sales");
		chatrooms.add("Support");
		chatrooms.add("Service");
		
		assertTrue(client1.attemptLogin("client1", ""));
		assertTrue(client2.attemptLogin("client2", ""));
		assertTrue(client3.attemptLogin("client3", ""));
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		
		client1.enterChatroom("Sales");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Sales", client1.getChatroom());
		assertEquals("Lobby", client2.getChatroom());
		assertEquals("Lobby", client3.getChatroom());
		
		client2.enterChatroom("Lobby");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Sales", client1.getChatroom());
		assertEquals("Lobby", client2.getChatroom());
		assertEquals("Lobby", client3.getChatroom());
		
		client1.enterChatroom("Lobby");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Lobby", client1.getChatroom());
		assertEquals("Lobby", client2.getChatroom());
		assertEquals("Lobby", client3.getChatroom());
		
		client2.enterChatroom("Sales");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Lobby", client1.getChatroom());
		assertEquals("Sales", client2.getChatroom());
		assertEquals("Lobby", client3.getChatroom());
		
		client1.enterChatroom("Sales");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Sales", client1.getChatroom());
		assertEquals("Sales", client2.getChatroom());
		assertEquals("Lobby", client3.getChatroom());
		
		client1.enterChatroom("Support");
		client3.enterChatroom("Support");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Support", client1.getChatroom());
		assertEquals("Sales", client2.getChatroom());
		assertEquals("Support", client3.getChatroom());
		
		client1.enterChatroom("Hobbies");
		client3.enterChatroom("Sports");
		
		chatrooms.add("Hobbies");
		chatrooms.add("Sports");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Hobbies", client1.getChatroom());
		assertEquals("Sales", client2.getChatroom());
		assertEquals("Sports", client3.getChatroom());
		
		client1.enterChatroom("Lobby");
		client3.enterChatroom("Lobby");
		
		chatrooms.remove("Hobbies");
		chatrooms.remove("Sports");
		
		assertEquals(chatrooms, new HashSet<String>(client1.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client2.getChatrooms()));
		assertEquals(chatrooms, new HashSet<String>(client3.getChatrooms()));
		
		assertEquals("Lobby", client1.getChatroom());
		assertEquals("Sales", client2.getChatroom());
		assertEquals("Lobby", client3.getChatroom());
	}
	
	@Test
	public void testSendMessage ()
	{
		assertTrue(client1.attemptLogin("client1", ""));
		assertTrue(client2.attemptLogin("client2", ""));
		assertTrue(client3.attemptLogin("client3", ""));
		
		client1.broadcast("Broadcast from client1");
		
		assertTrue(client1.hasMessage());
		assertTrue(client2.hasMessage());
		assertTrue(client3.hasMessage());
		
		assertEquals("client1: Broadcast from client1", client1.getNextUserMessage());
		assertEquals("client1: Broadcast from client1", client2.getNextUserMessage());
		assertEquals("client1: Broadcast from client1", client3.getNextUserMessage());
		
		client1.sendUserMessage("client2", "PM from client1 to client2");
		
		assertFalse(client1.hasMessage());
		assertTrue(client2.hasMessage());
		assertFalse(client3.hasMessage());
		
		assertNull(client1.getNextUserMessage());
		assertEquals("client1: PM from client1 to client2", client2.getNextUserMessage());
		assertNull(client3.getNextUserMessage());
		
		client2.sendUserMessage("client1", "PM from client2 to client1");
		
		assertTrue(client1.hasMessage());
		assertFalse(client2.hasMessage());
		assertFalse(client3.hasMessage());
		
		assertEquals("client2: PM from client2 to client1", client1.getNextUserMessage());
		assertNull(client2.getNextUserMessage());
		assertNull(client3.getNextUserMessage());
		
		client1.sendUserMessage("client2", "PM from client1 to client2");
		
		assertFalse(client1.hasMessage());
		assertTrue(client2.hasMessage());
		assertFalse(client3.hasMessage());
		
		assertNull(client1.getNextUserMessage());
		assertEquals("client1: PM from client1 to client2", client2.getNextUserMessage());
		assertNull(client3.getNextUserMessage());
		
		client2.sendUserMessage("client1", "PM from client2 to client1");
		
		assertTrue(client1.hasMessage());
		assertFalse(client2.hasMessage());
		assertFalse(client3.hasMessage());
		
		assertEquals("client2: PM from client2 to client1", client1.getNextUserMessage());
		assertNull(client2.getNextUserMessage());
		assertNull(client3.getNextUserMessage());
		
		client1.logout();
		client2.logout();
		client3.logout();
	}
}