package multipaint;

import java.util.Vector;
import javax.swing.DefaultListModel;

public class Server {

	Vector<PlayerSocket> v = new Vector<>();
	Vector<String> commands = new Vector<>();
	ServerDialog sd;
	
	public Server(ServerDialog sd, DefaultListModel<PlayerSocket> m) {
		this.sd = sd;
		for (int t = 0; t < m.getSize(); t++){
			PlayerSocket p = m.getElementAt(t);
			v.addElement(p);
			p.setServer(this);
		}
	}
	public synchronized void commandReceived(PlayerSocket p, String s) {
		try{
			s = p.getNr()+":"+s;
			for (PlayerSocket pl : v){
				if (pl.isDisconnected()) continue;
				pl.write(s);
			}
			commands.addElement(s);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	public void playerDisconnected(PlayerSocket p) {
		sd.playerDisconnected(p);
	}
}
