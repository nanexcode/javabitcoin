package org.hma.bitcoin.network;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.net.SocketFactory;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class ClientPool {
    private final SocketFactory socketFactory = SocketFactory.getDefault();

    private final Set<Client> clients = Collections.synchronizedSet(new HashSet<>());

    private int connectTimeOut = 1000;

    public Client connect(final SocketAddress serverAddress) {
        try {
            Client client = new Client(serverAddress, socketFactory, connectTimeOut);
            clients.add(client);
            client.openConnection();
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
