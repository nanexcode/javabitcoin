package org.hma.bitcoin.service;

import org.hma.bitcoin.network.NetworkServiceStatus;

public interface INetworkService {

    void connect();

    void disconnect();

    NetworkServiceStatus status();

}
