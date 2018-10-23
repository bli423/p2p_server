package p2p_server;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

public class Server {
	
	 private static int SERVER_START = 1;

     private static int ALIVE_SERVER = 2;
     private static int CONNECT_SERVER = 3;
     private static int CONNECT_SERVER_YES = 4;
     private static int CONNECT_SERVER_NO = 5;

     private static int ISALIVE_PEAR = 6;
     private static int ISALIVE_PEAR_YES = 7;
     private static int ISALIVE_PEAR_NO = 8;
     
     private static int REACEIVE_PEAR = 9;
     private static int ALIVE_PEAR = 10;
     private static int DEATH_PEAR = 11;

     private static int DATA_VOICE = 12;
     private static int DATA_TEXT = 13;
	
	private NetWorkIO netWorkIO;
	private Queue<DatagramPacket> receiveQue = new LinkedList<DatagramPacket>();
	private HashMap<Integer, User> UserList = new HashMap<Integer, User>();
	
	
	
	
	private PacketReceiveHandler packetReceiveHandler = new PacketReceiveHandler() {
		public void receivePacket(DatagramPacket dp) {
			synchronized (receiveQue) {
				receiveQue.add(dp);
				receiveQue.notify();
			}
		}
	};
	
	
	
	
	
	
	public Server() {
		netWorkIO = new NetWorkIO(packetReceiveHandler);
		
		ReceiveThread receiveThread = new ReceiveThread();		
		TimerThread timerThread = new TimerThread();
		
		receiveThread.start();
		timerThread.start();
	}
	
	
	private void PacketProcessing(DatagramPacket dp) {
		TYPE_HEADER type = new TYPE_HEADER(dp.getData(),0);

		
		if(type.type == CONNECT_SERVER) {		
			
			CONNECT_SERVER_HEADER connect_server = new CONNECT_SERVER_HEADER(dp.getData(),0);
			
			if(UserList.containsKey(connect_server.id)) {				
				TYPE_HEADER connect_server_no = new TYPE_HEADER(CONNECT_SERVER_NO,connect_server.id);
				byte[] data = connect_server_no.getByte();			
				
				netWorkIO.sendData(data, data.length, dp.getAddress(), dp.getPort());
			}
			else {
				long public_ip = byteToUnsingedInt(dp.getAddress().getAddress(),0);
				User user = new User(connect_server.id, public_ip, dp.getPort(), connect_server.private_ip, connect_server.private_port);
				UserListPut(user);
				
				TYPE_HEADER connect_server_yes = new TYPE_HEADER(CONNECT_SERVER_YES,connect_server.id);
				byte[] data = connect_server_yes.getByte();				
				
				netWorkIO.sendData(data, data.length, dp.getAddress(), dp.getPort());
				
				sendPearInfo(user);				
			}		
		}
		else if(type.type == ALIVE_SERVER) {
			if(UserList.containsKey(type.id)) {
				
				User user = UserList.get(type.id);
				user.timerSet();
				
				TYPE_HEADER connect_server_no = new TYPE_HEADER(ALIVE_SERVER,type.id);
				byte[] data = connect_server_no.getByte();
				
				netWorkIO.sendData(data, data.length, dp.getAddress(), dp.getPort());
			}			
		}
		else if(type.type == ISALIVE_PEAR) {
			if(UserList.containsKey(type.id)) {				
				
				TYPE_HEADER isalive_pear_yes = new TYPE_HEADER(ISALIVE_PEAR_YES,type.id);
				byte[] data = isalive_pear_yes.getByte();
				
				netWorkIO.sendData(data, data.length, dp.getAddress(), dp.getPort());
			}	
			else {
				TYPE_HEADER isalive_pear_yes = new TYPE_HEADER(ISALIVE_PEAR_NO,type.id);
				byte[] data = isalive_pear_yes.getByte();
				
				netWorkIO.sendData(data, data.length, dp.getAddress(), dp.getPort());
			}
		}
		
	}
	
	
	private void sendPearInfo(User user) {	
		synchronized (UserList) {
			Iterator<Entry<Integer, User>> itor =  UserList.entrySet().iterator();
			while(itor.hasNext()) {
				Entry<Integer, User> node = itor.next();
				if(node.getKey() != user.id) {
					if(user.public_ip == node.getValue().public_ip) {
						
						// public ip가 같으면 내부 네트워크로 패킷경로 지정
						PEAR_INFO_HEADER receive_pear_toOther = new PEAR_INFO_HEADER(REACEIVE_PEAR,user.id,user.private_ip,user.private_port);
						byte[] data_pear_toOther = receive_pear_toOther.getByte();		
						netWorkIO.sendData(data_pear_toOther, data_pear_toOther.length, node.getValue().public_ip, node.getValue().public_port);	
						
						PEAR_INFO_HEADER receive_pear = new PEAR_INFO_HEADER(REACEIVE_PEAR,node.getValue().id,node.getValue().private_ip,node.getValue().private_port);
						byte[] data_pear = receive_pear.getByte();
						netWorkIO.sendData(data_pear, data_pear.length, user.public_ip, user.public_port);	
					}
					else {
						// public ip가 다르면 public 주소로 패킷경로 지정
						PEAR_INFO_HEADER receive_pear_toOther = new PEAR_INFO_HEADER(REACEIVE_PEAR,user.id,user.public_ip,user.public_port);
						byte[] data_pear_toOther = receive_pear_toOther.getByte();	
						netWorkIO.sendData(data_pear_toOther, data_pear_toOther.length, node.getValue().public_ip, node.getValue().public_port);
						
						PEAR_INFO_HEADER receive_pear = new PEAR_INFO_HEADER(REACEIVE_PEAR,node.getValue().id,node.getValue().public_ip,node.getValue().public_port);
						byte[] data_pear = receive_pear.getByte();
						netWorkIO.sendData(data_pear, data_pear.length, user.public_ip, user.public_port);
					}
				}				
			}
		}		
	}
	
	private void checkUserTimer() {
		
		Queue<User> deleteList = new LinkedList<User>();
		
		synchronized (UserList) {
			Iterator<Entry<Integer, User>> itor =  UserList.entrySet().iterator();
			while(itor.hasNext()) {
				Entry<Integer, User> node = itor.next();
				User user = node.getValue();				
				int time = user.count();
				
				if(time<=0) {					
					deleteList.add(user);
				}				
			}
			
			while(!deleteList.isEmpty()) {
				User user = deleteList.poll();			
				UserList.remove(user.id);
			}
		}
	}
	
	private void UserListPut( User user) {
		synchronized (UserList) {
			UserList.put(user.id,user);
		}
	}
	
	
	private class ReceiveThread extends Thread{	
		public void run() {
			DatagramPacket dp;
			while(true) {
				
				
				synchronized (receiveQue) {
					while(receiveQue.size() == 0) {
						try {
							receiveQue.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					dp = receiveQue.poll();
					PacketProcessing(dp);
				}
			}
		}
	}
	private class TimerThread extends Thread{	
		public void run() {
			while(true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				checkUserTimer();
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	class TYPE_HEADER {
		static final int header_size = 4;
		int type;
		int id;
		
		public TYPE_HEADER(int type,int id) {
			this.type = type;
			this.id = id;
		}
		public TYPE_HEADER(byte[] data,int start) {
			type = byteToUsingedShort(data,start);
			id = byteToUsingedShort(data,start+2);
		}
		public byte[] getByte() {
			byte[] data = new byte[4];		

			System.arraycopy(usingedShortToByte(type), 0, data, 0, 2);
			System.arraycopy(usingedShortToByte(id), 0, data, 2, 2);
			
			return data;
		}
	}
	
	class CONNECT_SERVER_HEADER {
		static final int header_size = 12;
		int type;
		int id;
		long private_ip;
		int private_port;
		
		public CONNECT_SERVER_HEADER(int type,int id,long private_ip,int private_port) {
			this.type = type;
			this.id = id;
			this.private_ip = private_ip;
			this.private_port = private_port;
		}
		public CONNECT_SERVER_HEADER(byte[] data,int start) {
			type = byteToUsingedShort(data,start);
			id = byteToUsingedShort(data,start+2);
			private_ip = byteToUnsingedInt(data,start+4);
			private_port = byteToInt(data,start+8);
		}
		public byte[] getByte() {
			byte[] data = new byte[header_size];		

			System.arraycopy(usingedShortToByte(type), 0, data, 0, 2);
			System.arraycopy(usingedShortToByte(id), 0, data, 2, 2);
			System.arraycopy(unsingedIntToByte(private_ip), 0, data, 4, 4);
			System.arraycopy(intToByte(private_port), 0, data, 8, 4);
			
			return data;
		}
	}
	
	class PEAR_INFO_HEADER {
		static final int header_size = 12;
		int type;
		int id;
		long ip;
		int port;
		
		public PEAR_INFO_HEADER(int type,int id,long ip,int port) {
			this.type = type;
			this.id = id;
			this.ip = ip;
			this.port = port;
		}
		public PEAR_INFO_HEADER(byte[] data,int start) {
			type = byteToUsingedShort(data,start);
			id = byteToUsingedShort(data,start+2);
			ip = byteToUnsingedInt(data,start+4);
			port = byteToInt(data,start+8);
		}
		public byte[] getByte() {
			byte[] data = new byte[header_size];		

			System.arraycopy(usingedShortToByte(type), 0, data, 0, 2);
			System.arraycopy(usingedShortToByte(id), 0, data, 2, 2);
			System.arraycopy(unsingedIntToByte(ip), 0, data, 4, 4);
			System.arraycopy(intToByte(port), 0, data, 8, 4);
			
			return data;
		}
	}
	
	
	
	
	
	
	
	
	
	private byte[] unsingedIntToByte(long n) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE /8);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt((int)n);
		return buf.array();	
	}
	
	private byte[] intToByte(int n) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE /8);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(n);
		return buf.array();	
	}
	
	private int byteToInt(byte[] arr,int start) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE /8);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.put(arr,start,4);
		buf.flip();
		return buf.getInt();	
	}
	private long byteToUnsingedInt(byte[] arr,int start) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE /8);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.put(arr,start,4);
		buf.flip();
		long result = buf.getInt();
		
		if(result<0) {
			result = (long)0x100000000L + result;
		}
		return result;	
	}
	private byte[] usingedShortToByte(int n) {
		ByteBuffer buf = ByteBuffer.allocate(Short.SIZE /8);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short)n);
		return buf.array();	
	}
	
	private int byteToUsingedShort(byte[] arr,int start) {
		ByteBuffer buf = ByteBuffer.allocate(Short.SIZE /8);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.put(arr,start,2);
		buf.flip();
		int result = buf.getShort();
		
		if(result<0) {
			result = (int)0x10000 + result;
		}
		return result;	
	}
	
}


