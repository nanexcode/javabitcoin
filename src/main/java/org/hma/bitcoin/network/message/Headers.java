package org.hma.bitcoin.network.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Headers {

    private static final Logger LOG = LoggerFactory.getLogger(Headers.class);

    /**
     * Magic bytes indicating the originating network; used to seek to next message when stream state is unknown.
     */
    private byte[] startString = new byte[4];
    /**
     * ASCII string which identifies what message type is contained in the payload.
     * Followed by nulls (0x00) to pad out byte count; for example: version\0\0\0\0\0.
     */
    private byte[] commandName = new byte[12];
    /**
     * Number of bytes in payload. The current maximum number of bytes (“MAX_SIZE”) allowed in the payload by Bitcoin Core
     * is 32 MiB—messages with a payload size larger than this will be dropped or rejected.
     */
    private byte[] payloadSize = new byte[4];
    /**
     * Added inprotocol version 209. First 4 bytes of SHA256(SHA256(payload)) in internal byte order.
     * If payload is empty, as in verack and “getaddr” messages,
     * the checksum is always 0x5df6e0e2 (SHA256(SHA256(<empty string>))).
     */
    private byte[] checksum = new byte[4];


    public Headers(byte[] data) {
        if (data == null) throw new RuntimeException("Headers data cannot be null");
        if (data.length == 0) throw new RuntimeException("Headers size cannot be 0");
        if (data.length < 24) throw new RuntimeException("Data size should be at least 24 bytes");

        startString = Arrays.copyOfRange(data, 0, 3);
        commandName = Arrays.copyOfRange(data, 4, 15);
        payloadSize = Arrays.copyOfRange(data, 16, 19);
        checksum = Arrays.copyOfRange(data, 20, 23);

        LOG.info("{} ... start string: {}", startString, "TODO");
        LOG.info("{} ... command name: {}", commandName, "TODO");
        LOG.info("{} ... byte count: {}", payloadSize, "TODO");
        LOG.info("{} ... checksum: {}", checksum, "TODO");
    }

    public byte[] getStartString() {
        return startString;
    }

    public void setStartString(byte[] startString) {
        this.startString = startString;
    }

    public byte[] getCommandName() {
        return commandName;
    }

    public void setCommandName(byte[] commandName) {
        this.commandName = commandName;
    }

    public byte[] getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(byte[] payloadSize) {
        this.payloadSize = payloadSize;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }
}
