package org.errai.samples.helloworld.server;

import org.jboss.errai.bus.client.CommandMessage;
import org.jboss.errai.bus.client.MessageCallback;
import org.jboss.errai.bus.server.annotations.Service;

@Service("HelloWorld")
public class HelloWorld implements MessageCallback {
    public void callback(CommandMessage message) {
        System.out.println("Hello, World!");
    }
}
