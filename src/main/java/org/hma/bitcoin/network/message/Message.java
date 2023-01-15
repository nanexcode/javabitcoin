package org.hma.bitcoin.network.message;

import org.hma.bitcoin.crypto.Sha256;
import org.hma.bitcoin.util.ByteUtils;
import org.hma.bitcoin.util.VarInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public abstract class Message {
    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    public static final int MAX_SIZE = 0x02000000; // 32MB
    private static final int COMMAND_LEN = 12;
    public static final int UNKNOWN_LENGTH = Integer.MIN_VALUE;
    protected int offset;
    protected int cursor;
    protected int length = UNKNOWN_LENGTH;
    protected byte[] payload;
    protected boolean recached = false;

    public abstract byte[] serialize();

    public abstract byte[] deserialize();


    public void networkSerialize(OutputStream buf) throws IOException {
        byte[] header = new byte[4 + COMMAND_LEN + 4 + 4 /* checksum */];
        ByteUtils.uint32ToByteArrayBE(0x0b110907, header, 0);


        String name = "version";

        byte[] message = serialize();

        for (int i = 0; i < name.length() && i < COMMAND_LEN; i++) {
            header[4 + i] = (byte) (name.codePointAt(i) & 0xFF);
        }

        ByteUtils.uint32ToByteArrayLE(message.length, header, 4 + COMMAND_LEN);

        byte[] hash = Sha256.hashTwice(message);
        System.arraycopy(hash, 0, header, 4 + COMMAND_LEN + 4, 4);
        buf.write(header);
        buf.write(message);

    }

    protected long readUint32() throws Exception {
        try {
            long u = ByteUtils.readUint32(payload, cursor);
            cursor += 4;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception(e);
        }
    }

    protected long readInt64() throws Exception {
        try {
            long u = ByteUtils.readInt64(payload, cursor);
            cursor += 8;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception(e);
        }
    }

    protected BigInteger readUint64() throws Exception {
        // Java does not have an unsigned 64 bit type. So scrape it off the wire then flip.
        return new BigInteger(ByteUtils.reverseBytes(readBytes(8)));
    }

    protected VarInt readVarInt() throws Exception {
        return readVarInt(0);
    }

    protected VarInt readVarInt(int offset) throws Exception {
        try {
            VarInt varint = new VarInt(payload, cursor + offset);
            cursor += offset + varint.getOriginalSizeInBytes();
            return varint;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception(e);
        }
    }

    private void checkReadLength(int length) throws Exception {
        if ((length > MAX_SIZE) || (cursor + length > payload.length)) {
            throw new Exception("Claimed value length too large: " + length);
        }
    }

    protected byte[] readBytes(int length) throws Exception {
        checkReadLength(length);
        try {
            byte[] b = new byte[length];
            System.arraycopy(payload, cursor, b, 0, length);
            cursor += length;
            return b;
        } catch (IndexOutOfBoundsException e) {
            throw new Exception(e);
        }
    }

    protected byte readByte() throws Exception {
        checkReadLength(1);
        return payload[cursor++];
    }

    protected byte[] readByteArray() throws Exception {
        final int length = readVarInt().intValue();
        return readBytes(length);
    }

    protected String readStr() throws Exception {
        int length = readVarInt().intValue();
        return length == 0 ? "" : new String(readBytes(length), StandardCharsets.UTF_8); // optimization for empty strings
    }

    //protected Sha256Hash readHash() throws Exception {
        // We have to flip it around, as it's been read off the wire in little endian.
        // Not the most efficient way to do this but the clearest.
    //    return Sha256Hash.wrapReversed(readBytes(32));
    //}

    protected boolean hasMoreBytes() {
        return cursor < payload.length;
    }
}
