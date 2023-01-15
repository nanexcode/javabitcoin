package org.hma.bitcoin.crypto;

import com.google.common.primitives.Ints;
import org.hma.bitcoin.util.ByteUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

public class Sha256 implements Comparable<Sha256>  {
    public static final int LENGTH = 32; // bytes
    public static final Sha256 ZERO_HASH = wrap(new byte[LENGTH]);

    private final byte[] bytes;

    private Sha256(byte[] rawHashBytes) {
        checkArgument(rawHashBytes.length == LENGTH);
        this.bytes = rawHashBytes;
    }

    public static Sha256 wrap(byte[] rawHashBytes) {
        return new Sha256(rawHashBytes);
    }

    public static Sha256 twiceOf(byte[] contents) {
        return wrap(hashTwice(contents));
    }

    public static byte[] hashTwice(byte[] input) {
        return hashTwice(input, 0, input.length);
    }

    public static byte[] hashTwice(byte[] input1, byte[] input2) {
        MessageDigest digest = newDigest();
        digest.update(input1);
        digest.update(input2);
        return digest.digest(digest.digest());
    }

    public static byte[] hashTwice(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest(digest.digest());
    }

    public static byte[] hashTwice(byte[] input1, int offset1, int length1,
                                   byte[] input2, int offset2, int length2) {
        MessageDigest digest = newDigest();
        digest.update(input1, offset1, length1);
        digest.update(input2, offset2, length2);
        return digest.digest(digest.digest());
    }

    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(bytes, ((Sha256Hash)o).bytes);
    }

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable hash code even for
     * blocks, where the goal is to try and get the first bytes to be zeros (i.e. the value as a big integer lower
     * than the target value).
     */
    @Override
    public int hashCode() {
        // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
        return Ints.fromBytes(bytes[LENGTH - 4], bytes[LENGTH - 3], bytes[LENGTH - 2], bytes[LENGTH - 1]);
    }

    @Override
    public String toString() {
        return ByteUtils.HEX.encode(bytes);
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    public BigInteger toBigInteger() {
        return ByteUtils.bytesToBigInteger(bytes);
    }

    /**
     * Returns the internal byte array, without defensively copying. Therefore do NOT modify the returned array.
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Returns a reversed copy of the internal byte array.
     */
    public byte[] getReversedBytes() {
        return ByteUtils.reverseBytes(bytes);
    }

    @Override
    public int compareTo(final Sha256 other) {
        for (int i = LENGTH - 1; i >= 0; i--) {
            final int thisByte = this.bytes[i] & 0xff;
            final int otherByte = other.bytes[i] & 0xff;
            if (thisByte > otherByte)
                return 1;
            if (thisByte < otherByte)
                return -1;
        }
        return 0;
    }
}
