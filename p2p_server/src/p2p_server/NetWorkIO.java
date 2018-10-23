package p2p_server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

public class NetWorkIO {
	public static int BUFF_SIZE  =1024;
	private DatagramSocket  socket;
	private int port = 7989;
	private PacketReceiveHandler packetReceiveHandler;
	private Queue<DatagramPacket> sendQue  = new LinkedList<DatagramPacket>();
	
	
	public NetWorkIO(PacketReceiveHandler packetReceiveHandler) {
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
	
			e.printStackTrace();
		}
		this.packetReceiveHandler = packetReceiveHandler;
		
		ReceiveThread receiveThread = new ReceiveThread();
		SendThread sendThread = new SendThread();
		
		receiveThread.start();
		sendThread.start();
	}
	
	
	public void sendData(byte[] data,int length,InetAddress ip, int port) {		
		synchronized (sendQue) {
			DatagramPacket sendDp = new DatagramPacket(data,0,length,ip,port);		
			sendQue.add(sendDp);
			sendQue.notifyAll();
		}
	}
	public void sendData(byte[] data,int length,long ip, int port) {	
		InetAddress ip_address;
		
		try {
			ip_address = InetAddress.getByName(toIpString(ip));

			
			synchronized (sendQue) {
				DatagramPacket sendDp = new DatagramPacket(data,0,length,ip_address,port);						

				sendQue.add(sendDp);
				sendQue.notifyAll();
			}
		} catch (UnknownHostException e) {		
			e.printStackTrace();
		}
		
	}
	
	private class ReceiveThread extends Thread{
		
		public void run() {
			while(true) {
				byte[] data = new byte[BUFF_SIZE];
				DatagramPacket dp = new DatagramPacket(data, data.length);

			
				try {
					socket.receive(dp);
					
					
					
					packetReceiveHandler.receivePacket(dp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private String toIpString(long ip) {
		String result = "";
		result += (ip & 0xFF) + ".";
		result += ((ip>>8) & 0xFF) + ".";
		result += ((ip>>16) & 0xFF) + ".";
		result += ((ip>>24) & 0xFF);
		return result;
	}
	
	private class SendThread extends Thread{
		public void run() {
			while(true) {
				DatagramPacket dp;
				
				synchronized (sendQue) {
					while(sendQue.size() == 0) {
						try {
							sendQue.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					dp = sendQue.poll();					
				}
				
				try {					
					socket.send(dp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
