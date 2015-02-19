package edu.ucsd.cse110.client;

public interface Constants {
	public static String ACTIVEMQ_URL = "tcp://localhost:61616";
	public static String USERNAME = "max";	
	public static String PASSWORD = "pwd";	
	
	public static String TO_CLIENT_QUEUE_NAME = "TO_CLIENT_QUEUE";
	public static String TO_SERVER_QUEUE_NAME = "TO_SERVER_QUEUE";
	
	public static int MAX_USERS = 1;
	
	public static String LOGIN_REQUEST = "LOGIN_REQUEST ";
	public static String LOGIN_RESPONSE = "LOGIN_RESPONSE ";
	public static String USER_LIST_REQUEST = "USER_LIST_REQUEST";
	public static String USER_LIST_RESPONSE = "USER_LIST_RESPONSE";
	public static String USER_MESSAGE = "USER_MESSAGE";
	public static String LOGOUT = "LOGOUT";
	public static String BROADCAST_MESSAGE = "BROADCAST_MESSAGE";
	public static String GLOBAL_NAME = "GLOBAL_NAME";
	public static String ENTER_CHATROOM_REQUEST = "ENTER_CHATROOM_REQUEST";
	public static String ENTER_CHATROOM_RESPONSE = "ENTER_CHATROOM_RESPONSE";
	public static String CHATROOM_LIST_REQUEST = "CHATROOM_LIST_REQUEST";
	public static String CHATROOM_LIST_RESPONSE = "CHATROOM_LIST_RESPONSE";
	public static int UIDLength = 20;
}