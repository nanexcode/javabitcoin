package org.hma.bitcoin.network;

public class Network {
    private int port;
    private String name;
    private String startString;
    private String maxNbits;

    public Network() {}

    public Network(int port, String name, String startString, String maxNbits) {
        this.port = port;
        this.name = name;
        this.startString = startString;
        this.maxNbits = maxNbits;
    }

    public static Network mainnet() {
        return new Network(
                8333,
                "Mainnet",
                "0xf9beb4d9",
                "0x1d00ffff"
        );
    }

    public static Network testnet() {
        return new Network(
                18333,
                "Testnet",
                "0x0b110907",
                "0x1d00ffff"
        );
    }

    public static Network regtest() {
        return new Network(
                18444,
                "Regtest",
                "0xfabfb5da",
                "0x207fffff"
        );
    }
}



