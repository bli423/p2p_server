package p2p_server;

import java.net.DatagramPacket;

public interface PacketReceiveHandler {
	public void receivePacket(DatagramPacket dp);
}
