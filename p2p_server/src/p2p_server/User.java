package p2p_server;

public class User {
	public static final int TIME = 5;
	
	public int id;

	
	public long public_ip;
	public int public_port;
	public long private_ip;
	public int private_port;
	
	private int timer_count = TIME;
	
	public User(int id,long public_ip,int public_port,long private_ip,int private_port) {
		this.id= id;		
		
		this.public_ip = public_ip;
		this.public_port = public_port;
		this.private_ip = private_ip;
		this.private_port = private_port;
	}
	
	
	public void timerSet() {
		timer_count = TIME;
	}
	public int count() {
		timer_count -= 1;
		return timer_count;
	}
}
