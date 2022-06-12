package multipaint;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.*;

public class ServerDialog extends JDialog implements Runnable, ActionListener{

	private ServerSocket ss;
	private JList<PlayerSocket> list = new JList<>();
	private DefaultListModel<PlayerSocket> m = new DefaultListModel<PlayerSocket>();
	private JButton cancel = new JButton("Cancel");
	private JButton start = new JButton("Start");
	private boolean running = true;
	public static final int port = 52341;
	private Server server = null;
	
	public ServerDialog(Window parent) throws Exception{
		super(parent,"Waiting for players",ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		ss = new ServerSocket(port);
		new Thread(this).start();
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(cancel,BorderLayout.NORTH);
		cp.add(list,BorderLayout.CENTER);
		cp.add(start,BorderLayout.SOUTH);
		list.setModel(m);
		setSize(250,300);
		setLocation(parent.getLocation().x+20, parent.getLocation().y+20);
		start.addActionListener(this);
		cancel.addActionListener(this);
		setVisible(true);
	}
	public void run(){
		while(running){
			try{
				Socket sock = ss.accept();
				if (running == false) break;
				PlayerSocket p = new PlayerSocket(sock);
				m.addElement(p);
				SwingUtilities.invokeLater(()->{list.updateUI();});
			}catch(Exception ex){
				if (running == false) return;
				ex.printStackTrace();
				String error = ex.toString();
				if (error.length() > 200) error = error.substring(0,200)+"...";
				JOptionPane.showMessageDialog(this, error);
			}
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel){
			for (int t = 0; t < m.getSize(); t++){
				m.getElementAt(t).disconnect();
			}
			try{
				running = false;
				ss.close();
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, ex.toString());
			}
			dispose();
		}
		else if (e.getSource() == start){
			try{
				if (m.getSize() == 0){
					JOptionPane.showMessageDialog(this, "No players");
					return;
				}
				String s = "start";
				for (int t = 0; t < m.getSize(); t++){
					PlayerSocket p = m.getElementAt(t);
					if (p.isDisconnected()) continue;
					p.setNr(t+1);
					if (p.getPlayerName().contains(":")){
						JOptionPane.showMessageDialog(this, "Player name contained ':'\n"+p.getPlayerName());
						return;
					}
					s += ":"+p.getNr()+","+p.getPlayerName();
				}
				start.setEnabled(false);
				for (int t = 0; t < m.getSize(); t++){
					PlayerSocket p = m.getElementAt(t);
					if (p.isDisconnected()) continue;
					p.write(s);
				}
				running = false;
				server = new Server(this,m);
				cancel.setText("Close server");
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, ex.toString());
			}
		}
	}
	public void playerDisconnected(PlayerSocket p) {
		SwingUtilities.invokeLater(()->{
			list.updateUI();
		});
	}
}
