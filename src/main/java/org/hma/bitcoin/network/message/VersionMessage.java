package org.hma.bitcoin.network.message;

import com.google.common.net.InetAddresses;
import org.hma.bitcoin.model.Address;
import org.hma.bitcoin.util.ByteUtils;
import org.hma.bitcoin.util.VarInt;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class VersionMessage extends Message {

    public static final String LIBRARY_VERSION = "0.0.1";
    public static final String LIBRARY_SUBVER = "/bitcoinj:" + LIBRARY_VERSION + "/";

    /**
     * This node can be asked for full blocks instead of just headers.
     */
    public static final int NODE_NETWORK = 1 << 0;

    /**
     * See BIP 0064
     */
    public static final int NODE_GETUTXO = 1 << 1;

    /**
     * See BIP 0111
     */
    public static final int NODE_BLOOM = 1 << 2;

    /**
     * See BIP 0144
     */
    public static final int NODE_WITNESS = 1 << 3;

    /**
     * Never formally proposed (as a BIP), and discontinued.
     * Was historically sporadically seen on the network
     */
    public static final int NODE_XTHIN = 1 << 4;

    /**
     * See BIP 0157
     */
    public static final int NODE_NETWORK_LIMITED = 1 << 10;

    /**
     * See BIP 0159
     */
    public static final int NODE_BITCOIN_CASH = 1 << 5;
    public int clientVersion;
    public long localServices;
    public long time;
    public Address receivingAddr;
    public Address fromAddr;
    public String subVer;
    public long bestHeight;
    public boolean relayTxesBeforeFilter;

    public VersionMessage() {
        clientVersion = 70014;
        localServices = 0;
        time = System.currentTimeMillis() / 1000;
        // Note that the Bitcoin Core doesn't do anything with these, and finding out your own external IP address
        // is kind of tricky anyway, so we just put nonsense here for now.
        InetAddress localhost = InetAddresses.forString("127.0.0.1");
        //MessageSerializer serializer = this.serializer.withProtocolVersion(0);
        receivingAddr = new Address(localhost, 18333, BigInteger.ZERO);
        //receivingAddr.setParent(this);
        fromAddr = new Address(localhost, 18333, BigInteger.ZERO);
        //fromAddr.setParent(this);
        subVer = LIBRARY_SUBVER;
        bestHeight = 0;
        relayTxesBeforeFilter = true;
    }


    @Override
    public void serialize(OutputStream buf) throws IOException {
        ByteUtils.uint32ToByteStreamLE(clientVersion, buf);
        ByteUtils.uint32ToByteStreamLE(localServices, buf);
        ByteUtils.uint32ToByteStreamLE(localServices >> 32, buf);
        ByteUtils.uint32ToByteStreamLE(time, buf);
        ByteUtils.uint32ToByteStreamLE(time >> 32, buf);
        receivingAddr.serialize(buf);
        fromAddr.serialize(buf);
        ByteUtils.uint32ToByteStreamLE(0, buf);
        ByteUtils.uint32ToByteStreamLE(0, buf);
        // Now comes subVer.
        byte[] subVerBytes = subVer.getBytes(StandardCharsets.UTF_8);
        buf.write(new VarInt(subVerBytes.length).encode());
        buf.write(subVerBytes);
        // Size of known block chain.
        ByteUtils.uint32ToByteStreamLE(bestHeight, buf);
        //if (clientVersion >= params.getProtocolVersionNum(NetworkParameters.ProtocolVersion.BLOOM_FILTER)) {
            buf.write(relayTxesBeforeFilter ? 1 : 0);
        //}
    }

    @Override
    public byte[] deserialize() {
        return new byte[0];
    }
}
