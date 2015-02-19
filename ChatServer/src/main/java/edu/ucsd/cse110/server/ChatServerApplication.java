package edu.ucsd.cse110.server;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

@Configuration
@ComponentScan
public class ChatServerApplication {
	
	static Server server = new Server();

    @Bean
    ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(
                new ActiveMQConnectionFactory(Constants.ACTIVEMQ_URL));
    }
    
    @Bean
    MessageListenerAdapter receiver() {
        return new MessageListenerAdapter(server) {{
            setDefaultListenerMethod("receive");
        }};
    }
    
    @Bean
    SimpleMessageListenerContainer container(final MessageListenerAdapter messageListener,
            final ConnectionFactory connectionFactory) {
        return new SimpleMessageListenerContainer() {{
            setMessageListener(messageListener);
            setConnectionFactory(connectionFactory);
            setDestinationName(Constants.TO_SERVER_QUEUE_NAME);
        }};
    }

    @Bean
    JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

	public static void main(String[] args) throws Throwable {
		BrokerService broker = new BrokerService();
		broker.addConnector(Constants.ACTIVEMQ_URL);
		broker.setPersistent(false);
		broker.start();
		
		AnnotationConfigApplicationContext context = 
		          new AnnotationConfigApplicationContext(ChatServerApplication.class);
        
        JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
        
        server.setTemplate(jmsTemplate);
        
        //server.send("Test message from server.", "Server");
        System.out.println("Server started.");
        //context.close();
	}

}
