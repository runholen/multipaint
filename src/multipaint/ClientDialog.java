package multipaint;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ClientDialog extends JDialog implements Runnable, WindowListener{

	private Socket sock;
	private BufferedReader in;
	private BufferedWriter out;
	private PaintPanel paintPanel;
	private PlayersPanel playersPanel;
	private ToolsPanel toolsPanel;
	private int mode = 0; //1-started, 2-disconnected
	private int drawMode = 1; //1-draw,2-line,3-circle,4-rect
	private Vector<Player> players = new Vector<>();
	private HashMap<Integer,Player> playerMap = new HashMap<>();
	private Color lastSentColor = null;
	private int lastSentSize = 1;
	private Vector<String> commandsSent = new Vector<>();
	
	public ClientDialog(Window parent, Socket sock, String name, BufferedReader in, BufferedWriter out) throws Exception{
		super(parent,"MultiPaint, "+name,ModalityType.APPLICATION_MODAL);
		setResizable(false);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		this.sock = sock;
		this.in = in;
		this.out = out;
		new Thread(this).start();
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(paintPanel = new PaintPanel(),BorderLayout.CENTER);
		cp.add(playersPanel = new PlayersPanel(),BorderLayout.EAST);
		cp.add(toolsPanel = new ToolsPanel(),BorderLayout.SOUTH);
		pack();
		setVisible(true);
	}
	private class Player{
		private BufferedImage image = new BufferedImage(1200,700,BufferedImage.TYPE_INT_ARGB);
		private int nr;
		private String name;
		private boolean active = true;
		private Graphics2D g;
		private boolean hide = false;
		private Player(int nr, String name){
			this.nr = nr;
			this.name = name;
			this.g = image.createGraphics();
			this.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
	}
	private class PaintPanel extends JPanel implements MouseMotionListener, MouseListener{
		private Point start = null;
		private Vector<Point> intermediates = new Vector<>(); 
		private Point i1 = null, i2 = null;
		private PaintPanel(){
			setPreferredSize(new Dimension(800,500));
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		public void paint(Graphics g){
			g.setColor(Color.white);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.black);
			if (mode == 0) g.drawString("Waiting for server",250,getHeight()/2);
			else{
				synchronized(ClientDialog.this){
					for (Player p : players){
						if (p.hide) continue;
						g.drawImage(p.image, 0, 0, null);
					}
				}
				if (mode == 2){
					g.setFont(g.getFont().deriveFont((float)24));
					g.setColor(Color.black);
					g.drawString("Disconnected",250,330);
					g.setColor(Color.red);
					g.drawString("Disconnected",248,328);
				}
				if (drawMode > 1 && i1 != null && i2 != null){
					g.setColor(toolsPanel.currentColor);
					((Graphics2D)g).setStroke(new BasicStroke((float)toolsPanel.currentSize));
					if (drawMode == 2){
						g.drawLine(i1.x, i1.y, i2.x, i2.y);
					}
					if (drawMode == 3){
						if (toolsPanel.fill.isSelected()) g.fillOval(i1.x, i1.y, i2.x-i1.x, i2.y-i1.y);
						else g.drawOval(i1.x, i1.y, i2.x-i1.x, i2.y-i1.y);
					}
					if (drawMode == 4){
						if (toolsPanel.fill.isSelected()) g.fillRect(i1.x, i1.y, i2.x-i1.x, i2.y-i1.y);
						else g.drawRect(i1.x, i1.y, i2.x-i1.x, i2.y-i1.y);
					}
				}
				if (drawMode == 1 && start != null && intermediates.size() > 0){
					g.setColor(toolsPanel.currentColor);
					((Graphics2D)g).setStroke(new BasicStroke((float)toolsPanel.currentSize));
					Point old = start;
					for (int t = 0; t < intermediates.size(); t++){
						Point p = intermediates.elementAt(t);
						g.drawLine(old.x, old.y, p.x, p.y);
						old = p;
					}
				}
			}
		}
		@Override
		public void mouseClicked(MouseEvent e) {
		}
		@Override
		public void mousePressed(MouseEvent e) {
			start = e.getPoint();
			intermediates.clear();
			i1 = null; i2 = null;
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			if (i1 != null && i2 != null && drawMode > 1){
				sendCommand(drawMode, paintPanel.i1, paintPanel.i2, null);
				i1 = null;
				i2 = null;
				intermediates.clear();
				repaint();
			}
			else if (drawMode == 1){
				if (intermediates.size() > 0 && start != null){
					intermediates.add(0,start);
					sendCommand(1, null, null, intermediates);
				}
				intermediates.clear();
				repaint();
			}
		}
		@Override
		public void mouseEntered(MouseEvent e) {
		}
		@Override
		public void mouseExited(MouseEvent e) {
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (start != null && start.equals(e.getPoint()) == false){
				//if (drawMode == 1){
				//	paintPanel.setIntermediate(null,null);
				//	sendCommand(1, start, e.getPoint());
				//	start = e.getPoint();
				//}
				//else {
					paintPanel.setIntermediate(start,e.getPoint());
				//}
			}
		}
		private void setIntermediate(Point i1, Point i2) {
			this.i1 = i1;
			this.i2 = i2;
			if (drawMode == 1) intermediates.add(i2);
			repaint();
		}
		@Override
		public void mouseMoved(MouseEvent e) {
		}
	}
	private class PlayersPanel extends JPanel implements MouseListener{
		private PlayersPanel(){
			setBackground(Color.black);
			setPreferredSize(new Dimension(200,-1));
			addMouseListener(this);
		}
		public void paint(Graphics g){
			super.paint(g);
			g.setFont(g.getFont().deriveFont(Font.BOLD,(float)18));
			//g.setColor(Color.black);
			//g.drawLine(0, 0, 0, getHeight());
			for (int t = 0; t < players.size(); t++){
				Player p = players.elementAt(t);
				int y = t*20;
				g.setColor(Color.white);
				g.fillRect(2, y+2, 18, 18);
				g.setColor(Color.black);
				if (p.hide){
					g.drawLine(2, y+2, 20, y+18);
					g.drawLine(2, y+18, 20, y+2);
				}
				if (p.active) g.setColor(Color.green);
				else g.setColor(Color.red);
				g.drawString(p.name, 22, y+18);
			}
		}
		@Override
		public void mouseClicked(MouseEvent e) {
		}
		@Override
		public void mousePressed(MouseEvent e) {
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getX() >= 2 && e.getX() <= 20){
				int t = e.getY() / 20;
				if (t < players.size()){
					players.elementAt(t).hide = !players.elementAt(t).hide;
					repaint();
					paintPanel.repaint();
				}
			}
		}
		@Override
		public void mouseEntered(MouseEvent e) {
		}
		@Override
		public void mouseExited(MouseEvent e) {
		}
	}
	private class ToolsPanel extends JPanel implements ActionListener{
		JButton[] colors = new JButton[20];
		JPanel[] cps = new JPanel[colors.length];
		int currentIndex = 0;
		Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
		Border selectedBorder = BorderFactory.createLineBorder(Color.blue, 3);
		Color currentColor = Color.black;
		JButton clr = new JButton("CLR");
		JRadioButton s1 = new JRadioButton("S1",true);
		JRadioButton s2 = new JRadioButton("S2");
		JRadioButton s3 = new JRadioButton("S3");
		JRadioButton s4 = new JRadioButton("S4");
		JRadioButton s5 = new JRadioButton("S5");
		JRadioButton draw = new JRadioButton("Draw",true);
		JRadioButton line = new JRadioButton("Line");
		JRadioButton circle = new JRadioButton("Circle");
		JRadioButton rectangle = new JRadioButton("Rect");
		JCheckBox fill = new JCheckBox("Fill");
		JButton moveToFront = new JButton("To front");
		JButton moveToBack = new JButton("To back");
		JButton undo = new JButton("Undo");
		JButton save = new JButton("Save");
		AbstractButton[] tools = {draw,line,circle,rectangle,fill,s1,s2,s3,s4,s5,clr,moveToFront,moveToBack,undo,save};
		int currentSize = 1;		
		
		private ToolsPanel(){
			setLayout(new GridLayout(2,1));
			JPanel p1 = new JPanel(); p1.setLayout(new GridLayout(1,colors.length));
			for (int t = 0; t < colors.length; t++){
				colors[t] = new JButton();
				int r = (t * 170) % 255;
				int g = (t * 145) % 255;
				int b = (t * 75) % 255;
				colors[t].setBackground(new Color(r,g,b));
				colors[t].setPreferredSize(new Dimension(-1,40));
				colors[t].addActionListener(this);
				cps[t] = new JPanel();
				cps[t].setBorder(t == 0?selectedBorder:emptyBorder);
				cps[t].setLayout(new BorderLayout());
				cps[t].add(colors[t],BorderLayout.CENTER);
				p1.add(cps[t]);
			}
			add(p1);
			JPanel p2 = new JPanel(); p2.setLayout(new FlowLayout());
			for (AbstractButton tool : tools){
				p2.add(tool);
				tool.addActionListener(this);
			}
			add(p2);
			ButtonGroup sizes = new ButtonGroup();
			sizes.add(s1); sizes.add(s2); sizes.add(s3); sizes.add(s4); sizes.add(s5);
			ButtonGroup mode = new ButtonGroup();
			mode.add(draw); mode.add(line); mode.add(circle); mode.add(rectangle);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try{
				for (int t = 0; t < colors.length; t++){
					if (e.getSource() == colors[t]){
						cps[currentIndex].setBorder(emptyBorder);
						cps[t].setBorder(selectedBorder);
						currentColor = colors[t].getBackground();
						currentIndex = t;
						break;
					}
				}
				if (e.getSource() == draw){
					drawMode = 1;
				}
				if (e.getSource() == line){
					drawMode = 2;
				}
				if (e.getSource() == circle){
					drawMode = 3;
				}
				if (e.getSource() == rectangle){
					drawMode = 4;
				}
				if (e.getSource() == s1){
					currentSize = 1;
				}
				if (e.getSource() == s2){
					currentSize = 2;				
				}
				if (e.getSource() == s3){
					currentSize = 3;
				}
				if (e.getSource() == s4){
					currentSize = 5;
				}
				if (e.getSource() == s5){
					currentSize = 10;
				}
				if (e.getSource() == clr){
					int ok = JOptionPane.showConfirmDialog(ClientDialog.this, "Are you sure?");
					if (ok == JOptionPane.OK_OPTION){
						sendCommand(10, null, null, null);
					}
				}
				if (e.getSource() == moveToFront){
					sendCommand(11, null, null, null);
				}
				if (e.getSource() == moveToBack){
					sendCommand(12, null, null, null);
				}
				if (e.getSource() == undo && commandsSent.size() > 0){
					out.write("CLR\n");
					for (int t = 0; t < commandsSent.size() - 1; t++){
						out.write(commandsSent.elementAt(t));
					}
					out.flush();
					commandsSent.removeElementAt(commandsSent.size()-1);
				}
				if (e.getSource() == save){
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setDialogTitle("Specify a file to save");
					fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.png", "png"));
					fileChooser.setAcceptAllFileFilterUsed(false);
					int userSelection = fileChooser.showSaveDialog(ClientDialog.this);
					if (userSelection != JFileChooser.APPROVE_OPTION) return;
				    File f = fileChooser.getSelectedFile();
				    if (f.getName().toLowerCase().endsWith(".png") == false){
				    	f = new File(f.getParentFile(),f.getName()+".png");
				    }
				    BufferedImage img = new BufferedImage(paintPanel.getWidth(),paintPanel.getHeight(),BufferedImage.TYPE_INT_RGB);
				    Graphics2D g = img.createGraphics();
				    paintPanel.paint(g);
				    ImageIO.write(img, "png", f);
				    JOptionPane.showMessageDialog(ClientDialog.this, "File written");
				}
			}catch(Exception ex){
				ex.printStackTrace();
				String error = ex.toString();
				if (error.length() > 200) error = error.substring(0,200)+"..";
				JOptionPane.showMessageDialog(this, error);		
			}
		}	
	}
	public void sendCommand(int mode, Point from, Point to, Vector<Point> points){ //writeCommand
		try{
			Color col = toolsPanel.currentColor;
			int size = toolsPanel.currentSize ;
			if (lastSentColor == null || lastSentColor.equals(col) == false){
				String s = "COL:"+col.getRed()+","+col.getGreen()+","+col.getBlue()+"\n";
				out.write(s);
				out.flush();
				lastSentColor = col;
				commandsSent.add(s);
			}
			if (lastSentSize != size){
				String s = "S:"+size+"\n";
				out.write(s);
				out.flush();
				lastSentSize = size;
				commandsSent.add(s);
			}
			if (mode == 1){
				StringBuffer sb = new StringBuffer("D:");
				for (int t = 0; t < points.size(); t++){
					Point p = points.elementAt(t);
					if (t > 0) sb.append(",");
					sb.append(p.x+","+p.y);
				}
				sb.append("\n");
				out.write(sb.toString());
				//Old way
				//out.write("D:"+from.x+","+from.y+","+to.x+","+to.y+"\n");
				out.flush();
				commandsSent.add(sb.toString());
			}
			if (mode >= 2 && mode < 10){
				String s = "P"+mode+":"+(toolsPanel.fill.isSelected()?1:0)+":"+from.x+","+from.y+","+to.x+","+to.y+"\n";
				out.write(s);
				out.flush();
				commandsSent.add(s);
			}
			if (mode == 10){
				String s = "CLR\n";
				out.write(s);
				out.flush();
				commandsSent.add(s);
			}
			if (mode == 11){
				String s = "MF\n";
				out.write(s);
				out.flush();
				commandsSent.add(s);
			}
			if (mode == 12){
				String s = "MB\n";
				out.write(s);
				out.flush();
				commandsSent.add(s);
			}
			if (mode == 20){
				out.write("disconnect\n");
				out.flush();
			}
		}catch(Exception ex){
			ex.printStackTrace();
			String error = ex.toString();
			if (error.length() > 200) error = error.substring(0,200)+"..";
			JOptionPane.showMessageDialog(this, error);
		}
	}
	public void run(){
		try{
			while(true){
				String s = in.readLine();
				//System.out.println("M:"+mode+": "+s);
				if (mode == 0){
					if (s.startsWith("start:")){
						s = s.substring(6);
						String[] el = s.split(":",-1);
						for (int t = 0; t < el.length; t++){
							String[] ee = el[t].split(",",2);
							Player p = new Player(Integer.parseInt(ee[0]),ee[1]);
							players.add(p);
							playerMap.put(p.nr, p);
						}
						mode = 1;
						repaint();
						System.out.println("Started");
					}
				}
				else{
					//System.out.println("C: "+s);
					paintCommand(s);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
			String error = ex.toString();
			if (error.length() > 200) error = error.substring(0,200)+"..";
			JOptionPane.showMessageDialog(this, error);
			mode = 2;
		}
	}
	private void paintCommand(String s) {
		String[] el = s.split(":",-1);
		int nr = Integer.parseInt(el[0]);
		Player p = playerMap.get(nr);
		if (p != null){
			String command = el[1];
			//if (command.equals("D") == false) System.out.println("Command received: "+s);
			System.out.println("Command received: "+s);
			if (command.equals("COL")){
				int[] i = getArray(el[2]);
				p.g.setColor(new Color(i[0],i[1],i[2]));
			}
			else if (command.equals("D")){
				int[] i = getArray(el[2]);
				int[] old = {i[0],i[1]};
				for (int t = 2; t < i.length; t += 2){
					int[] point = {i[t],i[t+1]};
					p.g.drawLine(old[0], old[1], point[0], point[1]);
					old = point;
				}
				//Old way
				//p.g.drawLine(i[0], i[1], i[2], i[3]);
				paintPanel.repaint();
			}
			else if (command.startsWith("P")){
				int drawMode = Integer.parseInt(command.substring(1, 2));
				boolean fill = el[2].equals("1");
				int[]i = getArray(el[3]);
				if (drawMode == 2){
					p.g.drawLine(i[0], i[1], i[2], i[3]);
				}
				if (drawMode == 3){
					if (fill) p.g.fillOval(i[0], i[1], i[2]-i[0], i[3]-i[1]);
					else p.g.drawOval(i[0], i[1], i[2]-i[0], i[3]-i[1]);
				}
				if (drawMode == 4){
					if (fill) p.g.fillRect(i[0], i[1], i[2]-i[0], i[3]-i[1]);
					else p.g.drawRect(i[0], i[1], i[2]-i[0], i[3]-i[1]);
				}
				paintPanel.repaint();
			}
			else if (command.equals("S")){
				int size = Integer.parseInt(el[2]);
				p.g.setStroke(new BasicStroke((float)size));
			}
			else if (command.equals("CLR")){
				p.g.setBackground(new Color(255, 255, 255, 0));
				p.g.clearRect(0, 0, p.image.getWidth(), p.image.getHeight());
				paintPanel.repaint();
			}
			else if (command.equalsIgnoreCase("disconnect")){
				p.active = false;
				playersPanel.repaint();
				System.out.println("Player "+p.nr+" reported disconnected.");
			}
			else if (command.equals("MF") || command.equals("MB")){
				synchronized (ClientDialog.this){
					int i = players.indexOf(p);
					if (command.equals("MB") && i > 0){
						players.remove(i);
						players.add(0,p);
					}
					if (command.equals("MF") && i >= 0 && i < players.size() - 1){
						players.remove(i);
						players.add(p);
					}
					paintPanel.repaint();
					playersPanel.repaint();
				}
			}
			else{
				System.out.println("Unhandled command: "+s);
			}
		}
	}
	private int[] getArray(String s) {
		String[] el = s.split(",");
		int[] i = new int[el.length];
		for (int t = 0; t < i.length; t++){
			i[t] = Integer.parseInt(el[t]);
		}
		return i;
	}
	@Override
	public void windowOpened(WindowEvent e) {
	}
	@Override
	public void windowClosing(WindowEvent e) {
		int ok = JOptionPane.showConfirmDialog(this, "Do you want to exit?");
		if (ok != JOptionPane.OK_OPTION) return;
		if (mode == 0 || mode == 1) sendCommand(20, null, null, null);
		System.exit(0);
	}
	@Override
	public void windowClosed(WindowEvent e) {
	}
	@Override
	public void windowIconified(WindowEvent e) {
	}
	@Override
	public void windowDeiconified(WindowEvent e) {
	}
	@Override
	public void windowActivated(WindowEvent e) {
	}
	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
