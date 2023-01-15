package org.hma.bitcoin;


import jakarta.annotation.PostConstruct;
import org.hma.bitcoin.network.ClientPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Autowired
    private ClientPool pool;

    @PostConstruct
    public void initTest() {
        pool.connect(new InetSocketAddress("testnet-seed.bitcoin.jonasschnelli.ch", 18333));
    }
}