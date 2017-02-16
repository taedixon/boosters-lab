package ca.noxid.lab.entity;

import ca.noxid.lab.Changeable;
import ca.noxid.lab.EditorApp;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.uiComponents.BgList;
import ca.noxid.uiComponents.BgPanel;
import ca.noxid.uiComponents.DragScrollAdapter;
import ca.noxid.uiComponents.UpdateTextField;
import com.carrotlord.string.StrTools;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

/**
 * work in progress
 */
public class SpritesheetOptimizer extends JFrame implements Changeable {
	
	private static final long serialVersionUID = 9186488628058309359L;
	private static final String PREF_LASTDIR = "sso_last_directory";
	
	private JList<Sprite> spriteList;
	@SuppressWarnings("unused")
	private File imgFile;
	private SpritesheetPane spritePane;
	private File lastDir;
	
	private UpdateTextField leftField;
	private UpdateTextField upField;
	private UpdateTextField wField;
	private UpdateTextField hField;
	private UpdateTextField nameField;
	
	private boolean unsaved = false;
	
	private double zoom = 1.0;
	
	SpritesheetOptimizer() {
		this.setTitle("Spritesheet Organizer");
		if (EditorApp.blazed)
			this.setCursor(ResourceManager.cursor);
		this.buildMenu();
		this.addContentsToPane(this.getContentPane(), null);
		this.setSize(new Dimension(640, 800));
		this.setVisible(true);
		getPrefs();
	}
	
	public SpritesheetOptimizer(ResourceManager r) {
		this.setTitle("Spritesheet Optimizer");
		this.buildMenu();
		this.addContentsToPane(this.getContentPane(), r);
		this.setSize(new Dimension(640, 800));
		this.setVisible(true);
		getPrefs();
	}
	
	private void getPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(SpritesheetOptimizer.class);
		String ld = prefs.get(PREF_LASTDIR, null);
		lastDir = (ld == null) ? null : new File(ld);
	}
	
	private void setPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(SpritesheetOptimizer.class);
		prefs.put(PREF_LASTDIR, lastDir.toString());
	}
	
	private void buildMenu() {
		JMenuBar mb = new JMenuBar();
		JMenu menu = new JMenu("File");
		mb.add(menu);
		//
		//LOAD SPRITESHEET ACTION
		//
		JMenuItem item = new JMenuItem(new AbstractAction() {


			private static final long serialVersionUID = 2489719410511675935L;

			@Override
			public void actionPerformed(ActionEvent e) {
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Image files", "png", "bmp");
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(filter);
				fc.setCurrentDirectory(lastDir);
				int retVal = fc.showOpenDialog(SpritesheetOptimizer.this);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					File ss = fc.getSelectedFile();
					lastDir = ss.getParentFile();
					setPrefs();
					imgFile = ss;
					loadImage(ss);
				}
			}			
		});
		item.setText("Load Spritesheet");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		menu.add(item);
		
		//
		//SAVE SPRITESHEET ACTION
		//
		item = new JMenuItem(new AbstractAction() {

			private static final long serialVersionUID = 3599541737380390419L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveImage();
			}
		});
		item.setText("Save Spritesheet");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		menu.add(item);
		
		
		this.setJMenuBar(mb);
	}

	private void addContentsToPane(Container c, ResourceManager r) {
		c.setLayout(new BorderLayout());		
		//left
		if (r != null) {
			spritePane = new SpritesheetPane(r.getImg(ResourceManager.rsrcBgBrown));
		} else {
			spritePane = new SpritesheetPane(null);
		}
		spritePane.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				// TODO Auto-generated method stub
				System.out.print("+");
			}

			@Override
			public void focusLost(FocusEvent e) {
				// TODO Auto-generated method stub
				System.out.print("-");				
			}
			
		});
		JScrollPane jsp = new JScrollPane(spritePane);
		jsp.getVerticalScrollBar().setUnitIncrement(20);
		DragScrollAdapter drag = new DragScrollAdapter();
		spritePane.addMouseListener(drag);
		spritePane.addMouseMotionListener(drag);
		c.addKeyListener(new ZoomControl());
		c.add(jsp, BorderLayout.CENTER);
		
		//right
		JPanel rightPane;
		if (r != null) {
			rightPane = new BgPanel(new BorderLayout(), r.getImg(ResourceManager.rsrcBgBlue));
		} else {
			rightPane = new JPanel(new BorderLayout());
		}
		c.add(rightPane, BorderLayout.EAST);
		if (r != null) {
			spriteList = new BgList<Sprite>(r.getImg(ResourceManager.rsrcBgWhite));
		} else {
			spriteList = new JList<Sprite>();
		}
		spriteList.addListSelectionListener(new SpriteListListener());
		jsp = new JScrollPane(spriteList);
		rightPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		rightPane.add(jsp, BorderLayout.CENTER);
		JPanel controlPane;
		if (r != null) {
			controlPane = new BgPanel( r.getImg(ResourceManager.rsrcBgBlue));
		} else {
			controlPane = new JPanel();
		}
		GridLayout lay = new GridLayout(0,2);
		lay.setHgap(2);
		lay.setVgap(4);
		controlPane.setLayout(lay);
		controlPane.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
		
		buildControlPane(controlPane);
		
		rightPane.add(controlPane, BorderLayout.SOUTH);
	}
	
	private void buildControlPane(JPanel controlPane) {
		//CONTROL BUTTONS
		//MOVE SPRITES UP LIST
		JLabel lbl;
		ActionListener al = new SpriteListListener();
		JButton btn = new JButton(new AbstractAction() {

			private static final long serialVersionUID = 3599541737380390419L;
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] selected = spriteList.getSelectedIndices();
				List<Integer> sList = new ArrayList<Integer>();
				for (int i : selected) {
					sList.add(i);
					if (i > 0 && !sList.contains(i-1)) {
						Sprite moving = spritePane.sprites.get(i);
						spritePane.sprites.remove(moving);
						spritePane.sprites.add(i-1, moving);
						sList.remove((Integer)i); //oops, autoboxing has failed me
						sList.add((Integer)i-1);
					}
				}
				spritePane.indexSprites();
				spriteList.setListData(spritePane.sprites.toArray(new Sprite[0]));
				spriteList.repaint();
				for (int i = 0; i < selected.length; i++) {
					selected[i] = sList.get(i);
				}
				spriteList.setSelectedIndices(selected);
			}
			
		});
		btn.setText("⇧");
		btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
		btn.setMargin(new Insets(0, 6, 0, 6));
		controlPane.add(btn);
		
		//MOVE SPRITES DOWN LIST
		btn = new JButton(new AbstractAction() {

			private static final long serialVersionUID = -6830329589902672849L;

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("donw");
			}
			
		});
		btn.setText("⇩");
		btn.setMargin(new Insets(0, 6, 0, 6));
		btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
		controlPane.add(btn);
		
		lbl = new JLabel("Left:" );
		lbl.setOpaque(false);
		controlPane.add(lbl);
		leftField = new UpdateTextField(12);
		leftField.addActionListener(al);
		leftField.setFireActionOnFocusSwitch(false);
		controlPane.add(leftField);
		
		lbl = new JLabel("Up:" );
		lbl.setOpaque(false);
		controlPane.add(lbl);
		upField = new UpdateTextField(12);
		upField.addActionListener(al);
		upField.setFireActionOnFocusSwitch(false);
		controlPane.add(upField);
		
		lbl = new JLabel("Width:" );
		lbl.setOpaque(false);
		controlPane.add(lbl);
		wField = new UpdateTextField(12);
		wField.addActionListener(al);
		wField.setFireActionOnFocusSwitch(false);
		controlPane.add(wField);

		lbl = new JLabel("Height:" );
		lbl.setOpaque(false);
		controlPane.add(lbl);
		hField = new UpdateTextField(12);
		hField.addActionListener(al);
		hField.setFireActionOnFocusSwitch(false);
		controlPane.add(hField);
		
		lbl = new JLabel("Name:" );
		lbl.setOpaque(false);
		controlPane.add(lbl);
		nameField = new UpdateTextField(12);
		nameField.addActionListener(al);
		nameField.setFireActionOnFocusSwitch(false);
		controlPane.add(nameField);
	}
	
	public void loadImage(File newImg) {
		spritePane.setSheet(newImg);
		if (spritePane.sheet != null) {
			spriteList.setListData(spritePane.sprites.toArray(new Sprite[0]));
		}
		this.revalidate();
		this.repaint();
	}
	
	public void saveImage() {
		//TODO save
		markUnchanged();
	}
	
	@SuppressWarnings("unused")
	private void packSprites() {
		Sprite[] sprits = spritePane.sprites.toArray(new Sprite[0]);
		Arrays.sort(sprits);
		Area pack = new Area();
		
		for (Sprite s : sprits) {
			
		}
	}
	
	/*******************************************************
	 * 	CLASSES
	 *******************************************************/	
	private class Sprite implements Comparable<Sprite> {
		private int left;
		public int getLeft() {return left;}
		public void setLeft(int l) {
			left = l;
			update();
		}
		private int up;
		public int getUp() {return up;}
		public void setUp(int u) {
			up = u;
			update();
		}
		private int w;
		public int getW() {return w;}
		public void setW(int w) {
			this.w = w;
			update();
		}
		private int h;
		public int getH() {return h;}
		public void setH(int hh) {
			h = hh;
			update();
		}
		private String name;
		public String getName() {return name;}
		public void setName(String n) {
			name = n;
			update();
		}
		private int index;
		
		Sprite (int l, int u, int ww, int hh, String n, int i) {
			left = l;
			up = u;
			w = ww;
			h = hh;
			name = n;
			index = i;
		}
		
		private void update() {
			markChanged();
		}
		
		@Override
		public String toString() {
			return index + " " + name;
		}
		
		@Override
		public int compareTo(Sprite o) {
			// I want them sorted ascending
			// so i'm intentinally doing this backward
			Integer bigness = o.w*o.h;
			return bigness.compareTo(w*h);
		}
	}

	@Override
	public boolean isModified() {
		return unsaved;
	}

	@Override
	public void markUnchanged() {
		unsaved = false;		
	}

	@Override
	public void markChanged() {
		unsaved = true;
	}
	
	private class SpritesheetPane extends BgPanel {
		
		public SpritesheetPane(BufferedImage bg) {
			super(bg);
			this.setFocusable(true);
		}

		public void indexSprites() {
			int index = 1;
			for (Sprite s : sprites) {
				s.index = index++;
			}
		}

		private static final long serialVersionUID = -2350090820667199188L;
		BufferedImage sheet = null;
		private ArrayList<Sprite> sprites;
		
		void setSheet(File newsheet) {
			sprites = new ArrayList<Sprite>();
			try {
				sheet = ImageIO.read(newsheet);
				resize();
				
				File rectfile = new File(newsheet + ".rect");
				if (rectfile.exists()) {
					Scanner sc = new Scanner(rectfile);
					sc.nextLine();//discard the first line idgaf
					int index = 1;
					while (sc.hasNextLine()) {
						int l = sc.nextInt();
						int u = sc.nextInt();
						int w = sc.nextInt();
						int h = sc.nextInt();
						String n = sc.nextLine();
						sprites.add(new Sprite(l,u,w,h,n, index++));
					}
					sc.close();
				}
			} catch (IOException e) {
				StrTools.msgBox("Couldn't read " + newsheet);
				sheet = null;
			}
		}
		
		@Override
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			if (sheet != null) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.drawImage(sheet, 0, 0,
						(int)(sheet.getWidth()*zoom),
						(int)(sheet.getHeight()*zoom),
						this);
				
				Area clip = new Area(g2d.getClip());
				Area bound = new Area();
				List<Sprite> selected = spriteList.getSelectedValuesList();
				for (Sprite s : sprites) {
					if (selected.contains(s)) {
						g2d.setColor(Color.YELLOW);
					} else {
						g2d.setColor(Color.green);
					}
					bound.add(new Area(new Rectangle(
							s.getLeft(), s.getUp(), s.getW(), s.getH())));
					g2d.drawRect(s.getLeft(), s.getUp(), s.getW()-1, s.getH()-1);
				}
				bound.intersect(clip); //we want the area where they overlap
				clip.exclusiveOr(bound); //to be removed from view
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) 0.4));
				g2d.setColor(Color.gray);
				g2d.fill(clip);
				
				g2d.dispose();
			}
		}
		
		public void resize() {
			this.setPreferredSize(new Dimension(
					(int)(sheet.getWidth() * zoom), 
					(int)(sheet.getHeight() * zoom)
					)
			);
			this.revalidate();
		}
	}
	
	private class ZoomControl extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent eve) {
			SpritesheetPane src = (SpritesheetPane) eve.getSource();
			if ((eve.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
				if (eve.getKeyCode() == KeyEvent.VK_MINUS) {
					zoom *= 0.5;
					//System.out.println("minus");
					src.resize();
					SpritesheetOptimizer.this.repaint();
				}
				if (eve.getKeyCode() == KeyEvent.VK_EQUALS) {
					zoom *= 2;
					//System.out.println("plus");
					src.resize();
					SpritesheetOptimizer.this.repaint();
				}
			}
		}
	}
	
	private class SpriteListListener implements ListSelectionListener, ActionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() == false) {
				int index = spriteList.getSelectedIndex();
				if (index != -1) {
					Sprite s = spriteList.getSelectedValue();
					leftField.setText(s.getLeft() + "");
					upField.setText(s.getUp() +"");
					wField.setText(s.getW() + "");
					hField.setText(s.getH() + "");
					nameField.setText(s.getName());
				}
				spritePane.repaint();
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			try {
				if (src == leftField) {
					int l = Integer.parseInt(leftField.getText());
					for (Sprite s : spriteList.getSelectedValuesList() ) {
						s.setLeft(l);
					}
				} else if (src == upField){
					int u = Integer.parseInt(upField.getText());
					for (Sprite s : spriteList.getSelectedValuesList()) {
						s.setUp(u);
					}
				} else if (src == wField) {
					int w = Integer.parseInt(wField.getText());
					for (Sprite s : spriteList.getSelectedValuesList()) {
						s.setW(w);
					}
				} else if (src == hField) {
					int h = Integer.parseInt(hField.getText());
					for (Sprite s : spriteList.getSelectedValuesList()) {
						s.setH(h);
					}
				} else if (src == nameField) {
					String n = nameField.getText();
					n = n.replaceAll(" ", "_");
					for (Sprite s : spriteList.getSelectedValuesList()) {
						s.setName(n);
					}
				}
				spriteList.repaint();
				spritePane.repaint();
			} catch (NumberFormatException err) {
				StrTools.msgBox("There was an error parsing your change");
			}
		}
		
	}
}
