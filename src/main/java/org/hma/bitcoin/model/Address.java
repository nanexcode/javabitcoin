package org.hma.bitcoin.model;

import com.google.common.io.BaseEncoding;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.hma.bitcoin.network.message.Message;
import org.hma.bitcoin.util.ByteUtils;
import org.hma.bitcoin.util.VarInt;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class Address extends Message  {
    private InetAddress addr;   // Used for IPV4, IPV6, null otherwise or if not-yet-parsed
    private String hostname;    // Used for (.onion addresses) TORV2, TORV3, null otherwise or if not-yet-parsed
    private int port;
    private BigInteger services;
    private long time;

    private static final BaseEncoding BASE32 = BaseEncoding.base32().omitPadding().lowerCase();
    private static final byte[] ONIONCAT_PREFIX = ByteUtils.HEX.decode("fd87d87eeb43");

    public Address(InetAddress addr, int port, BigInteger services) {
        this.addr = addr;
        this.port = port;
        this.services = services;
        this.time = System.currentTimeMillis() / 1000;
    }

    @Override
    public byte[] deserialize() {
        return new byte[0];
    }

    // BIP-155 reserved network IDs, see: https://github.com/bitcoin/bips/blob/master/bip-0155.mediawiki
    private enum NetworkId {
        IPV4(1),
        IPV6(2),
        TORV2(3),
        TORV3(4),
        I2P(5),
        CJDNS(6);

        final int value;

        NetworkId(int value) {
            this.value = value;
        }

        static Optional<NetworkId> of(int value) {
            return Stream.of(values())
                    .filter(id -> id.value == value)
                    .findFirst();
        }
    }


    @Override
    public void serialize(OutputStream stream) throws IOException {
        int protocolVersion = 70014;
        if (protocolVersion < 0 || protocolVersion > 2)
            throw new IllegalStateException("invalid protocolVersion: " + protocolVersion);

        if (protocolVersion >= 1) {
            ByteUtils.uint32ToByteStreamLE(time, stream);
        }
        if (protocolVersion == 2) {
            stream.write(new VarInt(services.longValue()).encode());
            if (addr != null) {
                if (addr instanceof Inet4Address) {
                    stream.write(0x01);
                    stream.write(new VarInt(4).encode());
                    stream.write(addr.getAddress());
                } else if (addr instanceof Inet6Address) {
                    stream.write(0x02);
                    stream.write(new VarInt(16).encode());
                    stream.write(addr.getAddress());
                } else {
                    throw new IllegalStateException();
                }
            } else if (addr == null && hostname != null && hostname.toLowerCase(Locale.ROOT).endsWith(".onion")) {
                byte[] onionAddress = BASE32.decode(hostname.substring(0, hostname.length() - 6));
                if (onionAddress.length == 10) {
                    // TORv2
                    stream.write(0x03);
                    stream.write(new VarInt(10).encode());
                    stream.write(onionAddress);
                } else if (onionAddress.length == 32 + 2 + 1) {
                    // TORv3
                    stream.write(0x04);
                    stream.write(new VarInt(32).encode());
                    byte[] pubkey = Arrays.copyOfRange(onionAddress, 0, 32);
                    byte[] checksum = Arrays.copyOfRange(onionAddress, 32, 34);
                    byte torVersion = onionAddress[34];
                    if (torVersion != 0x03)
                        throw new IllegalStateException("version");
                    if (!Arrays.equals(checksum, onionChecksum(pubkey, torVersion)))
                        throw new IllegalStateException("checksum");
                    stream.write(pubkey);
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
        } else {
            ByteUtils.uint64ToByteStreamLE(services, stream);  // nServices.
            if (addr != null) {
                // Java does not provide any utility to map an IPv4 address into IPv6 space, so we have to do it by
                // hand.
                byte[] ipBytes = addr.getAddress();
                if (ipBytes.length == 4) {
                    byte[] v6addr = new byte[16];
                    System.arraycopy(ipBytes, 0, v6addr, 12, 4);
                    v6addr[10] = (byte) 0xFF;
                    v6addr[11] = (byte) 0xFF;
                    ipBytes = v6addr;
                }
                stream.write(ipBytes);
            } else if (hostname != null && hostname.toLowerCase(Locale.ROOT).endsWith(".onion")) {
                byte[] onionAddress = BASE32.decode(hostname.substring(0, hostname.length() - 6));
                if (onionAddress.length == 10) {
                    // TORv2
                    stream.write(ONIONCAT_PREFIX);
                    stream.write(onionAddress);
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
        }
        // And write out the port. Unlike the rest of the protocol, address and port is in big endian byte order.
        ByteUtils.uint16ToByteStreamBE(port, stream);
    }

    protected void parse() throws Exception {
        int protocolVersion = 70014;
        if (protocolVersion < 0 || protocolVersion > 2)
            throw new IllegalStateException("invalid protocolVersion: " + protocolVersion);

        length = 0;
        if (protocolVersion >= 1) {
            time = readUint32();
            length += 4;
        } else {
            time = -1;
        }
        if (protocolVersion == 2) {
            VarInt servicesVarInt = readVarInt();
            length += servicesVarInt.getSizeInBytes();
            services = BigInteger.valueOf(servicesVarInt.longValue());
            int networkId = readByte();
            length += 1;
            byte[] addrBytes = readByteArray();
            int addrLen = addrBytes.length;
            length += VarInt.sizeOf(addrLen) + addrLen;
            Optional<NetworkId> id = NetworkId.of(networkId);
            if (id.isPresent()) {
                switch(id.get()) {
                    case IPV4:
                        if (addrLen != 4)
                            throw new ProtocolException("invalid length of IPv4 address: " + addrLen);
                        addr = getByAddress(addrBytes);
                        hostname = null;
                        break;
                    case IPV6:
                        if (addrLen != 16)
                            throw new ProtocolException("invalid length of IPv6 address: " + addrLen);
                        addr = getByAddress(addrBytes);
                        hostname = null;
                        break;
                    case TORV2:
                        if (addrLen != 10)
                            throw new ProtocolException("invalid length of TORv2 address: " + addrLen);
                        hostname = BASE32.encode(addrBytes) + ".onion";
                        addr = null;
                        break;
                    case TORV3:
                        if (addrLen != 32)
                            throw new ProtocolException("invalid length of TORv3 address: " + addrLen);
                        byte torVersion = 0x03;
                        byte[] onionAddress = new byte[35];
                        System.arraycopy(addrBytes, 0, onionAddress, 0, 32);
                        System.arraycopy(onionChecksum(addrBytes, torVersion), 0, onionAddress, 32, 2);
                        onionAddress[34] = torVersion;
                        hostname = BASE32.encode(onionAddress) + ".onion";
                        addr = null;
                        break;
                    case I2P:
                    case CJDNS:
                        // ignore unimplemented network IDs for now
                        addr = null;
                        hostname = null;
                        break;
                }
            } else {
                // ignore unknown network IDs
                addr = null;
                hostname = null;
            }
        } else {
            services = readUint64();
            length += 8;
            byte[] addrBytes = readBytes(16);
            length += 16;
            if (Arrays.equals(ONIONCAT_PREFIX, Arrays.copyOf(addrBytes, 6))) {
                byte[] onionAddress = Arrays.copyOfRange(addrBytes, 6, 16);
                hostname = BASE32.encode(onionAddress) + ".onion";
            } else {
                addr = getByAddress(addrBytes);
                hostname = null;
            }
        }
        port = ByteUtils.readUint16BE(payload, cursor);
        cursor += 2;
        length += 2;
    }

    private static InetAddress getByAddress(byte[] addrBytes) {
        try {
            return InetAddress.getByAddress(addrBytes);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    private static byte[] onionChecksum(byte[] pubkey, byte version) {
        if (pubkey.length != 32)
            throw new IllegalArgumentException();
        SHA3.Digest256 digest256 = new SHA3.Digest256();
        digest256.update(".onion checksum".getBytes(StandardCharsets.US_ASCII));
        digest256.update(pubkey);
        digest256.update(version);
        return Arrays.copyOf(digest256.digest(), 2);
    }

    public String getHostname() {
        return hostname;
    }

    public InetAddress getAddr() {
        return addr;
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddr(), getPort());
    }

    public int getPort() {
        return port;
    }

    public BigInteger getServices() {
        return services;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        if (hostname != null) {
            return "[" + hostname + "]:" + port;
        } else if (addr != null) {
            return "[" + addr.getHostAddress() + "]:" + port;
        } else {
            return "[ PeerAddress of unsupported type ]:" + port;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address other = (Address) o;
        // time is deliberately not included in equals
        return  Objects.equals(addr, other.addr) &&
                Objects.equals(hostname, other.hostname) &&
                port == other.port &&
                Objects.equals(services, other.services);
    }

    @Override
    public int hashCode() {
        // time is deliberately not included in hashcode
        return Objects.hash(addr, hostname, port, services);
    }

    public InetSocketAddress toSocketAddress() {
        // Reconstruct the InetSocketAddress properly
        if (hostname != null) {
            return InetSocketAddress.createUnresolved(hostname, port);
        } else {
            // A null addr will create a wildcard InetSocketAddress
            return new InetSocketAddress(addr, port);
        }
    }
}
