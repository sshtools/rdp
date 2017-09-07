package com.sshtools.javardp.layers.nla;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.Packet;

public class NTLMAVPairs implements PacketPayload {
	static Logger logger = LoggerFactory.getLogger(NTLMAVPairs.class);
	//
	public final static int MSV_AV_EOL = 0x0000;
	public final static int MSV_AV_NB_COMPUTER_NAME = 0x0001;
	public final static int MSV_AV_NB_DOMAIN_NAME = 0x0002;
	public final static int MSV_AV_DNS_COMPUTER_NAME = 0x0003;
	public final static int MSV_AV_DNS_DOMAIN_NAME = 0x0004;
	public final static int MSV_AV_DNS_TREE_NAME = 0x0005;
	public final static int MSV_AV_FLAGS = 0x0006;
	public final static int MSV_AV_TIMESTAMP = 0x0007;
	public final static int MSV_AV_SINGLE_HOST = 0x0008;
	public final static int MSV_AV_TARGET_NAME = 0x0009;
	public final static int MSV_AV_CHANNEL_BINDINGS = 0x000A;
	//
	public final static int MSV_AV_FLAG_CONSTAINRAINED = 0x00000001;
	public final static int MSV_AV_FLAG_INTEGRITY = 0x00000002;
	public final static int MSV_AV_FLAG_SPN = 0x00000004;
	//
	private String nbComputerName;
	private String nbDomainName;
	private String dnsComputerName;
	private String dnsDomainName;
	private String dnsTreeName;
	private int flags;
	private long timestamp;
	private NTLMSingleHost singleHost;
	private String targetName;
	private byte[] channelHash;

	public NTLMAVPairs(byte[] data) throws IOException {
		Packet pkt = new Packet(data);
		pkt.setPosition(0);
		read(pkt);
	}

	@Override
	public Packet write() throws IOException {
		Packet packet = new Packet(1024);
		for (int avId : new int[] { MSV_AV_NB_COMPUTER_NAME, MSV_AV_NB_DOMAIN_NAME, MSV_AV_DNS_COMPUTER_NAME,
				MSV_AV_DNS_DOMAIN_NAME, MSV_AV_DNS_TREE_NAME, MSV_AV_FLAGS, MSV_AV_TIMESTAMP, MSV_AV_SINGLE_HOST,
				MSV_AV_TARGET_NAME, MSV_AV_CHANNEL_BINDINGS, }) {
			byte[] data = null;
			switch (avId) {
			case MSV_AV_NB_COMPUTER_NAME:
				if (StringUtils.isNotBlank(nbComputerName))
					data = nbComputerName.getBytes(NTLMState.UNICODE_ENCODING);
				break;
			case MSV_AV_NB_DOMAIN_NAME:
				if (StringUtils.isNotBlank(nbDomainName))
					data = nbDomainName.getBytes(NTLMState.UNICODE_ENCODING);
				break;
			case MSV_AV_DNS_COMPUTER_NAME:
				if (StringUtils.isNotBlank(dnsComputerName))
					data = dnsComputerName.getBytes(NTLMState.UNICODE_ENCODING);
				break;
			case MSV_AV_DNS_DOMAIN_NAME:
				if (StringUtils.isNotBlank(dnsDomainName))
					data = dnsDomainName.getBytes(NTLMState.UNICODE_ENCODING);
				break;
			case MSV_AV_DNS_TREE_NAME:
				if (StringUtils.isNotBlank(dnsTreeName))
					data = dnsTreeName.getBytes(NTLMState.UNICODE_ENCODING);
				break;
			case MSV_AV_FLAGS:
				if (flags > 0) {
					packet.setLittleEndian16(MSV_AV_FLAGS);
					packet.setLittleEndian16(4);
					packet.setLittleEndian32(flags);
				}
				break;
			case MSV_AV_TIMESTAMP:
				if (timestamp > 0) {
					packet.setLittleEndian16(MSV_AV_TIMESTAMP);
					packet.setLittleEndian16(8);
					packet.setLittleEndian64((System.currentTimeMillis() + NTLMState.MILLISECONDS_BETWEEN_1970_AND_1601) * 10000L);
				}
				break;
			case MSV_AV_SINGLE_HOST:
				if (singleHost != null) {
					data = singleHost.write().getBytes();
				}
				break;
			case MSV_AV_TARGET_NAME:
				if (StringUtils.isNotBlank(targetName))
					data = targetName.getBytes(NTLMState.UNICODE_ENCODING);
				break;
			case MSV_AV_CHANNEL_BINDINGS:
				data = channelHash;
				break;
			default:
				continue;
			}
			if (data != null) {
				packet.setLittleEndian16(avId);
				packet.setLittleEndian16(data.length);
				packet.setArray(data);
			}
		}
		packet.setLittleEndian16(MSV_AV_EOL);
		packet.setLittleEndian16(0);
		packet.markEnd();
		return packet;
	}

	public void reset() {
		nbComputerName = null;
		nbDomainName = null;
		dnsComputerName = null;
		dnsDomainName = null;
		dnsTreeName = null;
		flags = 0;
		timestamp = 0;
		singleHost = null;
		targetName = null;
		channelHash = null;
	}

	@Override
	public void read(Packet packet) throws IOException {
		while (true) {
			int id = packet.getLittleEndian16();
			logger.debug(String.format("Read %s (%d, %02x)", toName(id), id, id));
			switch (id) {
			case MSV_AV_EOL:
				if (id != 0)
					throw new IOException("Protocol error.");
				return;
			case MSV_AV_NB_COMPUTER_NAME:
				nbComputerName = readUnicodeString(packet, packet.getLittleEndian16());
				break;
			case MSV_AV_NB_DOMAIN_NAME:
				nbDomainName = readUnicodeString(packet, packet.getLittleEndian16());
				break;
			case MSV_AV_DNS_COMPUTER_NAME:
				dnsComputerName = readUnicodeString(packet, packet.getLittleEndian16());
				break;
			case MSV_AV_DNS_DOMAIN_NAME:
				dnsDomainName = readUnicodeString(packet, packet.getLittleEndian16());
				break;
			case MSV_AV_DNS_TREE_NAME:
				dnsTreeName = readUnicodeString(packet, packet.getLittleEndian16());
				break;
			case MSV_AV_FLAGS:
				if (packet.getLittleEndian16() != 4)
					throw new IOException("Protocol error.");
				flags = packet.getLittleEndian32();
				break;
			case MSV_AV_TIMESTAMP:
				if (packet.getLittleEndian16() != 8)
					throw new IOException("Protocol error.");
				timestamp = (packet.getLittleEndian64() / 10000L) - NTLMState.MILLISECONDS_BETWEEN_1970_AND_1601;
				break;
			case MSV_AV_SINGLE_HOST:
				singleHost = new NTLMSingleHost();
				singleHost.read(packet);
				break;
			case MSV_AV_TARGET_NAME:
				targetName = readUnicodeString(packet, packet.getLittleEndian16());
				break;
			case MSV_AV_CHANNEL_BINDINGS:
				channelHash = new byte[packet.getLittleEndian16()];
				packet.copyToByteArray(channelHash, 0, packet.getPosition(), channelHash.length);
				packet.incrementPosition(channelHash.length);
				break;
			default:
				int len = packet.getLittleEndian16();
				logger.warn(String.format("Unknown AvId %d (%02x), skipping %d bytes.", id, id, len));
				packet.incrementPosition(len);
				break;
			}
		}
	}
	
	public String toName(int id) {
		switch (id) {
		case MSV_AV_EOL:
			return "MSV_AL_EOL";
		case MSV_AV_NB_COMPUTER_NAME:
			return "MSV_AV_NB_COMPUTER_NAME";
		case MSV_AV_NB_DOMAIN_NAME:
			return "MSV_AV_NB_DOMAIN_NAME";
		case MSV_AV_DNS_COMPUTER_NAME:
			return "MSV_AV_DNS_COMPUTER_NAME";
		case MSV_AV_DNS_DOMAIN_NAME:
			return "MSV_AV_DNS_DOMAIN_NAME";
		case MSV_AV_DNS_TREE_NAME:
			return "MSV_AV_DNS_TREE_NAME";
		case MSV_AV_FLAGS:
			return "MSV_AV_FLAGS";
		case MSV_AV_TIMESTAMP:
			return "MSV_AV_TIMESTAMP";
		case MSV_AV_SINGLE_HOST:
			return "MSV_AV_SINGLE_HOST";
		case MSV_AV_TARGET_NAME:
			return "MSV_AV_TARGET_NAME";
		case MSV_AV_CHANNEL_BINDINGS:
			return "MSV_AV_CHANNEL_BINDINGS";
		default:
			return "MSV_UNKNOWN";
		}
	}

	public String getNbComputerName() {
		return nbComputerName;
	}

	public void setNbComputerName(String nbComputerName) {
		this.nbComputerName = nbComputerName;
	}

	public String getNbDomainName() {
		return nbDomainName;
	}

	public void setNbDomainName(String nbDomainName) {
		this.nbDomainName = nbDomainName;
	}

	public String getDnsComputerName() {
		return dnsComputerName;
	}

	public void setDnsComputerName(String dnsComputerName) {
		this.dnsComputerName = dnsComputerName;
	}

	public String getDnsDomainName() {
		return dnsDomainName;
	}

	public void setDnsDomainName(String dnsDomainName) {
		this.dnsDomainName = dnsDomainName;
	}

	public String getDnsTreeName() {
		return dnsTreeName;
	}

	public void setDnsTreeName(String dnsTreeName) {
		this.dnsTreeName = dnsTreeName;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public NTLMSingleHost getSingleHost() {
		return singleHost;
	}

	public void setSingleHost(NTLMSingleHost singleHost) {
		this.singleHost = singleHost;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public byte[] getChannelHash() {
		return channelHash;
	}

	public void setChannelHash(byte[] channelHash) {
		this.channelHash = channelHash;
	}

	@Override
	public String toString() {
		return "NTLMAVPairs [nbComputerName=" + nbComputerName + ", nbDomainName=" + nbDomainName + ", dnsComputerName="
				+ dnsComputerName + ", dnsDomainName=" + dnsDomainName + ", dnsTreeName=" + dnsTreeName + ", flags=" + flags
				+ ", timestamp=" + timestamp + ", timestampTime=" + DateFormat.getDateTimeInstance().format(new Date(timestamp))
				+ ", singleHost=" + singleHost + ", targetName=" + targetName + ", channelHash=" + Arrays.toString(channelHash)
				+ "]";
	}

	private String readUnicodeString(Packet packet, int len) {
		try {
			byte[] b = new byte[len];
			packet.copyToByteArray(b, 0, packet.getPosition(), len);
			packet.incrementPosition(len);
			return new String(b, NTLMState.UNICODE_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException("Unsupported encoding.", uee);
		}
	}
}
