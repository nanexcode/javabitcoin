package org.hma.bitcoin.network;

import org.hma.bitcoin.network.message.Headers;
import org.hma.bitcoin.network.message.Message;
import org.hma.bitcoin.network.message.VersionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;




public class Client {

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    private SocketAddress address;
    private SocketFactory socketFactory;
    private Socket socket;
    private int connectTimeout;

    public Client(
            final SocketAddress serverAddress,
            final SocketFactory socketFactory,
            final int connectTimeout
    ) {
        this.socketFactory = socketFactory;
        this.address = serverAddress;
        this.connectTimeout = connectTimeout;
    }

    public Client openConnection() {
        try {
            socket = socketFactory.createSocket();
            Thread t = new Thread(() -> {
                try {
                    socket.connect(address, connectTimeout);
                    read(socket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.setName("Testing ...");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            LOG.error("Unable to open socket channel connection");
            throw new RuntimeException(t);
        }

        return this;
    }

    private void read(Socket socket) throws IOException {
        final InputStream stream = socket.getInputStream();
        final OutputStream ostream = socket.getOutputStream();
        while (true) {
            try {
                final Message m = new VersionMessage();
                m.networkSerialize(ostream);
                ostream.flush();
                byte[] bytes = stream.readAllBytes();
                new Headers(bytes);
            } catch (Exception e) {
                LOG.error("Unable to read data from the stream", e);
            }
        }
    }
}
