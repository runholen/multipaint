package multipaint;

import java.io.*;
import java.net.Socket;

public class PlayerSocket extends Thread{

	private String ip = null;
	private String playerName = null;
	private Socket sock;
	private BufferedReader in;
	private BufferedWriter out;
	private boolean disconnected = false;
	private int nr;
	private Server server;
	
	public PlayerSocket(Socket sock) throws Exception{
		this.sock = sock;
		in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		ip = sock.getInetAddress().toString();
		start();
		for (int t = 0; t < 20; t++){
			Thread.sleep(100);
			if (playerName != null) break;
		}
		if (playerName == null) throw new Exception("Timeout getting playerName from player.");
	}
	public void run(){
		while(disconnected == false){
			try{
				String s = in.readLine();
				System.out.println(nr+": "+s);
				if (s.startsWith("name=")) playerName = s.substring(5);
				if (s.startsWith("disconnect")){
					disconnected = true;
					in.close();
					System.out.println("Player "+playerName+" disconnected.");
				}
				if (server != null){
					server.commandReceived(PlayerSocket.this,s);
				}
			}catch(Exception ex){
				ex.printStackTrace();
				disconnected = true;
				break;
			}
		}
		server.playerDisconnected(this);
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getPlayerName() {
		return playerName;
	}
	public void setPlayerName(String name) {
		this.playerName = name;
	}
	public boolean isDisconnected() {
		return disconnected;
	}
	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}
	public int getNr() {
		return nr;
	}
	public void setNr(int nr) {
		this.nr = nr;
	}
	public String toString(){
		StringBuffer sb = new StringBuffer("<html><font color=gray>"+ip+"</font> ");
		if (disconnected) sb.append("<font color=red>");
		sb.append(playerName);
		if (disconnected) sb.append("</font>");
		sb.append("</html>");
		return sb.toString();
	}
	public void disconnect() {
		try{
			out.write("disconnect\n");
			out.flush(); out.close();
			sock.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		disconnected = true;
	}
	public void write(String s) throws Exception{
		out.write(s);
		out.write("\n");
		out.flush();
	}
	public void setServer(Server server) {
		this.server = server;
	}
}
