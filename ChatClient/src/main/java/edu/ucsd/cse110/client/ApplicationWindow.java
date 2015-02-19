package edu.ucsd.cse110.client;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ApplicationWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	final ChatClient chatClient;
	Vector<JPanel> chatRooms = new Vector<JPanel>();
	Vector<DefaultListModel<String>> messageLists = new Vector<DefaultListModel<String>>();
	Vector<DefaultListModel<String>> userLists = new Vector<DefaultListModel<String>>();
	Vector<JList<String>> onlineUsers = new Vector<JList<String>>();
	private WorkerThread task;
	int previousTabIndex = 0;
	int currentTabIndex;

	public ApplicationWindow(final ChatClient chatClient ) {
		this.chatClient = chatClient; 
		setSize(800, 500);
		setVisible(true);
		setResizable(false);
		setTitle("Chat Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new FlowLayout());
		JTabbedPane tabs = new JTabbedPane();
		setUpChatRooms(tabs, chatClient.getChatrooms());
		add(tabs);
		
		setUpLobby();
    		
		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent changeEvent) {
			    JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
			    int currentTabIndex = sourceTabbedPane.getSelectedIndex();
			    String chatRoomName = sourceTabbedPane.getTitleAt(currentTabIndex);	    
			    
			    if(previousTabIndex != currentTabIndex) { 
			    	task.cancel(true); 
			    	previousTabIndex = currentTabIndex; 
			    }
			    
			    refreshMessageLists(chatRoomName,currentTabIndex);	    
 				chatClient.enterChatroom(chatRoomName);
 				refreshOnlineUsers(currentTabIndex);
	          	(task = new WorkerThread(currentTabIndex)).execute();
            }
		};
		
		tabs.addChangeListener(changeListener);	
		actionWindowClosingLogout();
	}
	
	private void setUpChatRooms(JTabbedPane tabs, final Vector<String> chatRoomNames) {	
		for(int i=0; i<chatRoomNames.size(); i++) {
			JPanel panel = new JPanel( new FlowLayout() );
			panel.setPreferredSize( new Dimension(700,410) );
			
			/* Message Panel */
			final JPanel messagePanel = new JPanel( new FlowLayout() );
			messagePanel.setPreferredSize(new Dimension( 450,355 ) );
			JLabel messagePanelLabel = new JLabel("Chat History");
			DefaultListModel<String> messageListModel = new DefaultListModel<String>();
			final JList<String> messageList = new JList<String>(messageListModel);
			JScrollPane messageListScroller = new JScrollPane(messageList);
			messageListScroller.setPreferredSize(new Dimension(450, 300));
			final JTextField messageField = new JTextField(33);
			JButton sendButton = new JButton("Send");
			messagePanel.add(messagePanelLabel);
			messagePanel.add(messageListScroller);
			messagePanel.add(messageField);
			messagePanel.add(sendButton);
			/**/
			
			/* Online Panel */
			final JPanel onlinePanel = new JPanel( new FlowLayout() );
			onlinePanel.setPreferredSize(new Dimension( 200,350));
			JLabel onlineUsersLabel = new JLabel("Online Users");
			DefaultListModel<String> userListModel = new DefaultListModel<String>();
			final JList<String> onlineUser = new JList<String>(userListModel);
			onlineUser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane onlineUsersScroller = new JScrollPane( onlineUser );
			onlineUsersScroller.setPreferredSize( new Dimension(200, 300) );
			onlinePanel.add(onlineUsersLabel);
			onlinePanel.add(onlineUsersScroller);
			/**/
			
			// Send buttons
			sendButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(onlineUser.getSelectedValue() == "#All") {
						chatClient.broadcast( messageField.getText() );
						messageField.setText(null);
					} else {
						if(!onlineUser.getSelectedValue().equals(chatClient.getClientName()) ) {
							messageLists.get(currentTabIndex).addElement(chatClient.getClientName() + ": "  +  messageField.getText());
							chatClient.sendUserMessage(onlineUser.getSelectedValue(), messageField.getText());
							messageField.setText(null);
						}
					}
					}
				}
			); 
			
			/* User Options */
			final JPanel userPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
			userPanel.setPreferredSize(new Dimension( 700,30));
			JButton logoutButton = new JButton("Log Out");
			userPanel.add(logoutButton);
			
			logoutButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	chatClient.logout();
	            	dispose();
	            }
	        }); 
			/**/
			
			panel.add(userPanel);		
			panel.add(messagePanel);
			panel.add(onlinePanel);
			chatRooms.add(panel);
			messageLists.add( messageListModel );
			userLists.add( userListModel  );
			onlineUsers.add( onlineUser );
			tabs.addTab(chatRoomNames.get(i), chatRooms.get(i));
		}	
	}
	
	/*
	 * 
	 */
	private static class WorkerTask {
		private boolean newMessage = false;
		private boolean newUserList = false;
        private String aMessage;
        private Vector<String> userList;
        WorkerTask(String input) {
        	newMessage = true;
            aMessage = input;
        }
        
        WorkerTask(Vector<String> userList) {
        	newUserList = true;
        	this.userList = userList;
        }
    }

	/*
	 * 
	 */
    private class WorkerThread extends SwingWorker<Void, WorkerTask> {
    	int index;
        public WorkerThread(int index) {
			this.index = index;
		}

		@Override
        protected Void doInBackground() {
            while (!isCancelled()) {
            	if(chatClient.hasMessage()) {
            		publish(new WorkerTask( chatClient.getNextUserMessage() ));
            	}
            	
            	publish(new WorkerTask( chatClient.getUserList() ));
            	
                try { Thread.sleep(1000); } 
                catch (InterruptedException e) {}
            }
            return null;
        }

        @Override
        protected void process(List<WorkerTask> jobs) {
        	WorkerTask job = jobs.get(jobs.size() - 1);
        	
        	if(job.newMessage) {
        		messageLists.get(index).addElement(job.aMessage);
        	}
            
        	if(job.newUserList) {
        		String previousSelection = onlineUsers.get(index).getSelectedValue();
        		userLists.get(index).clear();
        		userLists.get(index).addElement("#All");
        		
        		for(String item : job.userList ) {
        			userLists.get(index).addElement(item);
        		}
        		
        		// Re-select the previous selection.
        		if(job.userList.contains(previousSelection)) {
        			onlineUsers.get(index).setSelectedValue(previousSelection, true);
        		} else {
        			onlineUsers.get(index).setSelectedIndex(0);
        		}
        	} 
        }
    }
    
    /*
     * Refresh message lists. 
     */
    private void refreshMessageLists( String chatRoomName, int currentTabIndex ) {
    	messageLists.get(currentTabIndex).clear();
      	messageLists.get(currentTabIndex).addElement("#");
      	messageLists.get(currentTabIndex).addElement("# You have joined " + chatRoomName + ".");
      	messageLists.get(currentTabIndex).addElement("#");
    }
    
    /*
     * Refresh Online user window. 
     */
    private void refreshOnlineUsers(int currentTabIndex) {
    	userLists.get(currentTabIndex).clear();
		userLists.get(currentTabIndex).addElement("#All");
		onlineUsers.get(currentTabIndex).setSelectedIndex(0);

		String users = "";
		for(String item : chatClient.getUserList() ) {
			userLists.get(currentTabIndex).addElement(item);
		}
    }
    
    /*
     * Manually set-up the lobby.
     */
    private void setUpLobby() {
    	messageLists.get(0).addElement("#");
      	messageLists.get(0).addElement("# You have connected to the server. Welcome to the Lobby.");
      	messageLists.get(0).addElement("#");
      	userLists.get(0).addElement("#All");
      	onlineUsers.get(currentTabIndex).setSelectedIndex(0);
      	
      	for(String item : chatClient.getUserList() ) {
      		userLists.get(0).addElement(item);
		}
		(task = new WorkerThread(0)).execute(); // Lobby
    }
    
	/*
	 * Log out from the server when the user clicks the X icon.
	 */
    private void actionWindowClosingLogout() {
    	// Logout on X click 
    	addWindowListener(new WindowAdapter() {
    		public void windowClosing(WindowEvent winEvt) {
    			chatClient.logout();
    			}
    		}
    	);
    }
    
    //public static void main(String[] args) {
		//ApplicationWindow gui = new ApplicationWindow();
	//}
}
