package edu.ucsd.cse110.client;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.swing.*;

import org.apache.activemq.ActiveMQConnection;

import java.awt.*;
import java.awt.event.*;
import java.net.URISyntaxException;

public class LoginWindow extends JFrame {
	Class<? extends LoginWindow> loginWindow = getClass();
	ChatClient chatClient; 
	JPanel panel = new JPanel();	
	JLabel serverLabel = new JLabel("Server:");
	JLabel userLabel = new JLabel("Username:");
	JLabel passLabel = new JLabel("Password:");
	JTextField serverField = new JTextField( "localhost:8887", 15 );
	JTextField userField = new JTextField(15);
	JPasswordField passField = new JPasswordField(15);
	JButton loginButton = new JButton("Login");
	
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
	
	LoginWindow() {
		super( "ChatClient Login" );
		try {chatClient = wireClient();} 
		catch (Exception e) { e.printStackTrace(); } 
		setSize( 300,200 );
		setLocation( 500,280 );
		panel.setLayout( null ); // center window 
		serverLabel.setBounds( 45, 30, 100, 20 );
		serverField.setBounds( 100,30,150,20 );
		serverField.setEditable(false);
		userLabel.setBounds( 20, 60, 100, 20 );
		userField.setBounds(100,60,150,20);
		passLabel.setBounds( 21, 90, 100, 20 );
		passField.setBounds(100,90,150,20);
		loginButton.setBounds(170,120,80,20);		
		panel.add( serverLabel );
		panel.add( serverField );
		panel.add( userLabel );
		panel.add( userField );
		panel.add( passLabel );
		panel.add( passField );
		panel.add( loginButton );
		
		getContentPane().add( panel );
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		setVisible( true );
		actionlogin();
	}
	
	public void actionlogin() {
		loginButton.addActionListener( new ActionListener() {	
			public void actionPerformed( ActionEvent ae ) {
				String server = serverField.getText();
				String user = userField.getText();
				String pass = passField.getText();
				
				if( chatClient.attemptLogin( user,pass ) ) {
					ApplicationWindow gui = new ApplicationWindow( chatClient );
					gui.setVisible(true);
					dispose(); // kills login window
				} else {
					JOptionPane.showMessageDialog(null,"Wrong Password / Username");
					userField.setText("");
					passField.setText("");
					userField.requestFocus();
				}
			}
		});
	}
	
	public static void main(String[] args) {
		LoginWindow loginWindow = new LoginWindow();
	}
}