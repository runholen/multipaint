package multipaint;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.*;

import java.io.*;

public class StartFrame extends JFrame implements ActionListener{
	private JTextField nameField = new JTextField();
	private JTextField myIpField = new JTextField();
	private JTextField ipField = new JTextField();
	private JButton startServer = new JButton("Start new server");
	private JButton connectToServer = new JButton("Connect to server");
	
	public static void main(String[] args){
		/*
		if (new java.sql.Date(System.currentTimeMillis()).toString().equals("2022-06-06")){
			StartFrame f1 = new StartFrame();
			StartFrame f2 = new StartFrame();
			StartFrame f3 = new StartFrame();		
			f1.setLocation(20,20);
			f2.setLocation(20,220);
			f3.setLocation(20,420);
			f2.nameField.setText("Appelsin1");
			f3.nameField.setText("Appelsin2");
			
		}
		else{
			new StartFrame();
		}
		*/
		new StartFrame();
	}
	public StartFrame(){
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("MultiPaint v 1.0");
		setLocation(50, 100);
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new GridLayout(4, 2));
		cp.add(new JLabel("Your name")); cp.add(nameField);
		cp.add(startServer); cp.add(myIpField);
		cp.add(connectToServer); cp.add(ipField);
		startServer.addActionListener(this);
		connectToServer.addActionListener(this);
		myIpField.setEditable(false);
		try{ 
			myIpField.setText(InetAddress.getLocalHost().getHostAddress()); 
			File f = new File("multipaint.ini");
			if (f.exists()){
				BufferedReader in = new BufferedReader(new FileReader(f));
				String name = null;
				String ip = null;
				while(true){
					String s = in.readLine();
					if (s == null) break;
					if (s.contains("=") == false) continue;
					String[] el = s.split("=",-1);
					if (el[0].equals("name")) name = el[1];
					if (el[0].equals("ip")) ip = el[1];
				}
				in.close();
				if (name != null) nameField.setText(name);
				if (ip != null) ipField.setText(ip);
			}
		} catch(Exception ex){
			ex.printStackTrace();
		}
		pack();
		setSize(getSize().width+150,getSize().height);
		setVisible(true);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		try{
			String name = nameField.getText().trim();
			String ip = ipField.getText().trim();
			if (name.equals("")){
				JOptionPane.showMessageDialog(this, "Please enter a name");
				return;
			}
			saveIniFile(name,ip);
			if (e.getSource() == startServer){
				ServerDialog server = new ServerDialog(this);
			}
			else if (e.getSource() == connectToServer){
				if (ip.equals("")){
					JOptionPane.showMessageDialog(this, "Please enter an ip address");
					return;
				}
				Socket sock = new Socket(ip,ServerDialog.port);
				BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
				out.write("name="+name+"\n"); out.flush();
				new ClientDialog(this,sock,name,in,out);
			}
		}catch(Exception ex){
			ex.printStackTrace();
			String error = ex.toString();
			if (error.length() > 200) error = error.substring(0,200)+"...";
			JOptionPane.showMessageDialog(this, error);
		}
	}
	private void saveIniFile(String name, String ip) throws Exception{
		File f = new File("multipaint.ini");
		BufferedWriter out = new BufferedWriter(new FileWriter(f));
		out.write("name="+name+"\r\n");
		out.write("ip="+ip+"\r\n");
		out.flush(); out.close();
	}
}
