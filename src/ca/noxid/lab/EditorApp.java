package ca.noxid.lab;

import ca.noxid.lab.entity.EntityData;
import ca.noxid.lab.entity.EntityPane;
import ca.noxid.lab.entity.NpcTblEditor;
import ca.noxid.lab.entity.SpritesheetOptimizer;
import ca.noxid.lab.gameinfo.*;
import ca.noxid.lab.mapdata.MapInfo;
import ca.noxid.lab.mapdata.Mapdata;
import ca.noxid.lab.mapdata.MapdataDialog;
import ca.noxid.lab.mapdata.MapdataPane;
import ca.noxid.lab.rsrc.BlSound;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.lab.script.TscBuilder;
import ca.noxid.lab.script.TscDialog;
import ca.noxid.lab.script.TscPane;
import ca.noxid.lab.tile.MapPane;
import ca.noxid.lab.tile.TilesetPane;
import ca.noxid.uiComponents.*;
import com.carrotlord.string.StrTools;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;
import java.util.logging.*;
import java.util.prefs.Preferences;
// import javax.swing.filechooser.FileNameExtensionFilter;

// @SuppressWarnings("rawtypes")
public class EditorApp extends JFrame implements ActionListener {
	private static final long serialVersionUID = -2975049719856443233L;

	private static final boolean disable_logging = true;
	public static boolean blazed = false;

	// about dialog
	private static final String VER_NUM = "0.5.1.1"; //$NON-NLS-1$
	private static final String TITLE_STR = "Booster's Lab V" + VER_NUM;
	private static final String ABOUT_STR = Messages.getString("EditorApp.1") + VER_NUM + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
			"By Noxid & Open Source Contributors - 10/03/2019\n" + //$NON-NLS-1$
			Messages.getString("EditorApp.4") + //$NON-NLS-1$
			Messages.getString("EditorApp.5"); //$NON-NLS-1$

	public static int EDITOR_MODE = 0;
	/*
	 * 0 = regular CS
	 * 1 = CS w/ layers
	 * 2 = Moustache
	 */

	private static final Icon ABOUT_ICON = getAboutIcon();

	// Property strings
	// private static final String PREF_TILESIZE = "tilesize"; <16 or 32>
	private static final String PREF_DIR = "last_directory"; //$NON-NLS-1$
	// private static final String PREF_LASTFILE = "last_proj";
	private static final String PREF_TILE_ZOOM = "tileset_zoom"; //$NON-NLS-1$
	private static final String PREF_MAP_ZOOM = "map_zoom"; //$NON-NLS-1$
	private static final String PREF_TILE_COL = "tileset_background_colour"; //$NON-NLS-1$
	private static final String PREF_HELPER = "helper_window_visible"; //$NON-NLS-1$
	private static final String PREF_SCRIPT = "script_window_visible"; //$NON-NLS-1$
	private static final String PREF_NOTES = "notes_text"; //$NON-NLS-1$
	private static final String PREF_COM = "tsc_show_commands"; //$NON-NLS-1$
	private static final String PREF_ENTITY = "entity_list_window_visible"; //$NON-NLS-1$
	private static final String PREF_SPRITESHEETWINDOW = "spritesheet_window_visible";
	private static final String PREF_MISC = "misc_display_options"; //$NON-NLS-1$

	public static final String PERSPECTIVE_TILE = "Tile"; //$NON-NLS-1$
	public static final String PERSPECTIVE_ENTITY = "Entity"; //$NON-NLS-1$
	public static final String PERSPECTIVE_TSC = "Script"; //$NON-NLS-1$
	public static final String PERSPECTIVE_MAPDATA = "Mapdata"; //$NON-NLS-1$
	private static final int NUM_DRAWMODE = 5;

	// globally accessible components
	protected JPanel opsPanel;
	protected JTabbedPane mapTabs;
	protected JList<String> mapList;
	protected JPopupMenu mapPopup;
	private JPopupMenu mapMovePopup;
	private ResourceManager iMan = new ResourceManager();
	private JDialog tilesetWindow;
	private boolean showTileWindow;
	private JDialog scriptWindow;
	private boolean showScriptWindow;
	private JDialog entityWindow;
	private boolean showEntityWindow;
	private JDialog npcTblWindow;
	private boolean showSpritesheetWindow;
	private JFrame helpWindow;
	private JFrame spritesheetWindow;
	private JTabbedPane scriptTabs;
	private JTextPane notes;
	private JTextField tscSearch;
	private String notesText = Messages.getString("EditorApp.19"); //$NON-NLS-1$
	private Vector<AbstractButton> buttonsToEnableOnProjectLoad = new Vector<>();
	private Vector<AbstractButton> buttonsToEnableOnExeLoad = new Vector<>();

	// Global variables
	public static String activePerspective = PERSPECTIVE_TILE;
	private static File lastDir = null;
	private final int SCALE_OPTIONS = 5;
	private final static String[] SCALE_NAMES = { "0.25x", "0.5x", "1x", "2x", "4x" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	public static double mapScale = 1; // represents the scale to draw the map at
	public static double tilesetScale = 1;
	private Vector<TabOrganizer> componentVec = new Vector<>();
	private boolean movingMapdata = false; // for right click map list -> move
	private int moveIndex = 0;
	public static Logger logger;

	private GameInfo exeData;

	// drawing related maybe?
	public static int NUM_LAYER;
	public static final int PHYSICAL_LAYER = 5;
	public static final int GRADIENT_LAYER = 4;
	private final static String[] LAYER_NAMES = { Messages.getString("EditorApp.25"), //$NON-NLS-1$
			Messages.getString("EditorApp.26"), Messages.getString("EditorApp.27"), Messages.getString("EditorApp.28"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"Gradient", Messages.getString("EditorApp.29"), }; //$NON-NLS-1$ //$NON-NLS-2$

	private boolean[] visibleLayers; // array of which layers should be shown
	private int activeLayer; // current layer to draw to
	private final static String[] TILEOP_DRAWMODES = { Messages.getString("EditorApp.30"),
			Messages.getString("EditorApp.31"), Messages.getString("EditorApp.32"), Messages.getString("EditorApp.33"),
			Messages.getString("EditorApp.34") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	protected int drawMode; // The current draw mode
	public static final int DRAWMODE_DRAW = 0;
	public static final int DRAWMODE_FILL = 1;
	public static final int DRAWMODE_REPLACE = 2;
	public static final int DRAWMODE_RECT = 3;
	public static final int DRAWMODE_COPY = 4;
	private final int TILEOP_NUM_OTHER_OPT = 5;
	private final static String[] TILEOP_OTHER_OPTS = { Messages.getString("EditorApp.35"),
			Messages.getString("EditorApp.36"), Messages.getString("EditorApp.37"),
			// $NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Messages.getString("EditorApp.38"), Messages.getString("EditorApp.39") }; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String ACTION_SAVE = "FileMenu_Save";

	private static final String ACTION_SAVEALL = "FileMenu_SaveAll";
	private boolean[] otherDrawOpts = new boolean[TILEOP_NUM_OTHER_OPT];

	// script-related
	private boolean showingCommands;

	// entities
	private JList<String> categoryList;
	private JList<String> subcatList;

	/*
	 * Getters and setters
	 */
	public void setDrawMode(int d) {
		drawMode = d;
	}

	public int getDrawMode() {
		return drawMode;
	}

	public int getActiveLayer() {
		if (EDITOR_MODE == 0)
			return -1;
		return activeLayer;
	}

	public void setActiveLayer(int layer) {
		if (layer != activeLayer) {
			activeLayer = layer;
			if (activeLayer == PHYSICAL_LAYER) {
				switchPerspective(activePerspective);
			}
		}
	}

	public boolean[] getVisibleLayers() {
		return visibleLayers;
	}

	public boolean[] getOtherDrawOptions() {
		return otherDrawOpts;
	}

	public ResourceManager getImageManager() {
		return iMan;
	}

	public GameInfo getGameInfo() {
		return exeData;
	}

	private static Icon getAboutIcon() {

		java.net.URL iconURL;
		if (blazed) {
			iconURL = EditorApp.class.getResource("rsrc/weed_aboutIcon.png"); //$NON-NLS-1$
		} else {
			iconURL = EditorApp.class.getResource("rsrc/aboutIcon.png"); //$NON-NLS-1$
		}
		return new ImageIcon(iconURL, "meow"); //$NON-NLS-1$
	}

	/*
	 * Begin functions
	 */
	EditorApp() {

		// blazed
		if (!new File("nofun").exists()) {
			Calendar cal = Calendar.getInstance();
			if (new File("weed").exists() || cal.get(Calendar.MONTH) == Calendar.APRIL && cal.get(Calendar.DAY_OF_MONTH) == 20)
				blazed = true;
		}

		// setups
		if (EDITOR_MODE == 2) {
			NUM_LAYER = 6;
		} else {
			NUM_LAYER = 4;
		}
		getPrefs();
		visibleLayers = new boolean[NUM_LAYER];

		// Build the window
		this.setTitle(""); //$NON-NLS-1$
		java.net.URL iconURL;
		if (blazed) {
			iconURL = EditorApp.class.getResource("rsrc/weed_AppIcon.png"); //$NON-NLS-1$
			BlSound.playSample(EditorApp.class.getResource("rsrc/weew.wav"));
			BlSound.playSample(EditorApp.class.getResource("rsrc/dr_dre-the_next_episode.mid"));
			this.setCursor(ResourceManager.cursor);
		} else {
			iconURL = EditorApp.class.getResource("rsrc/AppIcon.png"); //$NON-NLS-1$
		}
		Image icon = Toolkit.getDefaultToolkit().createImage(iconURL);
		this.setIconImage(icon);
		// build the UI
		JSplitPane topLevelPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		addComponentsToPane(topLevelPane);
		setGlobalKeyBindings(topLevelPane);
		this.setContentPane(topLevelPane);

		// finalize
		this.setMinimumSize(new Dimension(700, 480));
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.setVisible(true);
		this.getGlassPane().setVisible(true);
		// if [x] pressed
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setPrefs();
				// prompt for unsaved executable chanes
				if (exeData != null) {
					if (!saveAll(true)) {
						return;
					}
				}
				System.exit(0);
			}
		});
		// setup the helper after the main frame so it knows how to position itself
		initHelperWindows();
		/*
		int response = JOptionPane.showConfirmDialog(this, "warning:\n" +
				"this may f*** your shit up", "no really", JOptionPane.YES_NO_OPTION);
		switch (response) {
		case JOptionPane.NO_OPTION:
			System.exit(0);
		}
		*/
	}

	@Override
	public void setTitle(String s) {
		super.setTitle(TITLE_STR + " " + s);
	}

	/**
	 * Prompt for saving all resources
	 *
	 * @return false if save was aborted
	 */
	public boolean saveAll(boolean shouldClose) {
		// try to save all tabs
		if (shouldClose) {
			while (mapTabs.getTabCount() != 0) {
				if (!closeCurrentTab()) {
					return false; // save was aborted
				}
			}
		} else {
			for (int i = 0; i < mapTabs.getTabCount(); i++) {
				TabOrganizer t = componentVec.get(i);
				if (t.isModified()) {
					int r = t.promptSave();
					switch (r) {
					case JOptionPane.YES_OPTION:
						t.save();
						break;
					case JOptionPane.NO_OPTION:
						break;
					default:
						return false;
					}
				}
			}
		}
		if (exeData.areUnsavedChanges()) {
			int response = JOptionPane.showConfirmDialog(rootPane, Messages.getString("EditorApp.44") + //$NON-NLS-1$
					Messages.getString("EditorApp.45"), Messages.getString("EditorApp.46"), JOptionPane.YES_NO_OPTION); // $NON-NLS-1$
																														// //$NON-NLS-2$
			if (response == JOptionPane.YES_OPTION) {
				exeData.commitChanges();
			} else if (response == JOptionPane.CANCEL_OPTION || response == JOptionPane.CLOSED_OPTION) {
				return false; // oops
			}
		}
		return true;
	}

	private void getPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(EditorApp.class);
		// DEFAULT_TILE_SIZE = prefs.getInt(PREF_TILESIZE, 16);
		lastDir = new File(prefs.get(PREF_DIR, System.getProperty("user.dir"))); //$NON-NLS-1$
		EditorApp.tilesetScale = prefs.getDouble(PREF_TILE_ZOOM, 1.0);
		EditorApp.mapScale = prefs.getDouble(PREF_MAP_ZOOM, 1.0);
		TilesetPane.bgCol = Color.decode(prefs.get(PREF_TILE_COL, String.valueOf(Color.DARK_GRAY.getRGB())));
		showTileWindow = prefs.getBoolean(PREF_HELPER, false);
		showScriptWindow = prefs.getBoolean(PREF_SCRIPT, true);
		showEntityWindow = prefs.getBoolean(PREF_ENTITY, false);
		// showSpritesheetWindow = prefs.getBoolean(PREF_SPRITESHEETWINDOW, false);
		showSpritesheetWindow = false;
		notesText = prefs.get(PREF_NOTES, Messages.getString("EditorApp.49")); //$NON-NLS-1$
		showingCommands = prefs.getBoolean(PREF_COM, true);

		int miscDrawOpts = prefs.getInt(PREF_MISC, 0);
		for (int i = 0; i < otherDrawOpts.length; i++) {
			int bit = 1 << i;
			if ((miscDrawOpts & bit) != 0) {
				otherDrawOpts[i] = true;
			}
		}
	}

	private void setPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(EditorApp.class);
		if (prefs != null) {
			prefs.put(PREF_DIR, lastDir.toString());
			prefs.putDouble(PREF_MAP_ZOOM, mapScale);
			prefs.putDouble(PREF_TILE_ZOOM, tilesetScale);
			prefs.putBoolean(PREF_HELPER, showTileWindow);
			prefs.putBoolean(PREF_SCRIPT, showScriptWindow);
			prefs.putBoolean(PREF_ENTITY, showEntityWindow);
			prefs.putBoolean(PREF_SPRITESHEETWINDOW, showSpritesheetWindow);
			prefs.put(PREF_NOTES, notes.getText());
			prefs.putBoolean(PREF_COM, showingCommands);

			int misc = 0;
			for (int i = 0; i < otherDrawOpts.length; i++) {
				if (otherDrawOpts[i]) {
					misc |= 1 << i;
				}
			}
			prefs.putInt(PREF_MISC, misc);
		}

		// write window sizes
		File rectFile = new File("editor.rect"); //$NON-NLS-1$
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(rectFile));
			Window[] wArray = { this, tilesetWindow, scriptWindow, helpWindow, entityWindow, spritesheetWindow };
			for (Window w : wArray) {
				Point l = w.getLocation();
				Dimension d = w.getSize();
				out.write(l.x + " " + l.y + " "); //$NON-NLS-1$ //$NON-NLS-2$
				out.write(d.width + " " + d.height + " "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void mapZoomIn() {
		if (mapScale < 10) {
			mapScale *= 2.0;
			refreshCurrentMap();
		}
	}

	public void mapZoomOut() {
		if (mapScale > 0.1) {
			mapScale *= 0.5;
			refreshCurrentMap();
		}
	}

	@SuppressWarnings("serial")
	private void setGlobalKeyBindings(JComponent c) {
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoom in"); //$NON-NLS-1$
		c.getActionMap().put("zoom in", //$NON-NLS-1$
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						mapZoomIn();
					}
				});
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoom out"); //$NON-NLS-1$
		c.getActionMap().put("zoom out", //$NON-NLS-1$
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						mapZoomOut();
					}
				});
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS,
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "zoom in tiles"); //$NON-NLS-1$
		c.getActionMap().put("zoom in tiles", //$NON-NLS-1$
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						tilesetScale *= 2.0;
						refreshCurrentMap();
					}
				});
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS,
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "zoom out tiles"); //$NON-NLS-1$
		c.getActionMap().put("zoom out tiles", //$NON-NLS-1$
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						tilesetScale *= 0.5;
						refreshCurrentMap();
					}
				});
	}

	private void addComponentsToPane(JSplitPane pane) {
		// this causes crashes for some people.
		// rather than solve it properly i'm just gonna make this hack to avoid it.
		File lfOverride = new File("love.txt");
		if (!lfOverride.exists()) {
			// make it look like a native application (Carrotlord)
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (InstantiationException | UnsupportedLookAndFeelException | ClassNotFoundException
					| IllegalAccessException exceptionInfo) {
				// Ignore this exception.
			}
		}

		// add the menus
		JMenuBar menuBar = new JMenuBar();

		menuBar.add(buildFileMenu());
		menuBar.add(buildViewMenu());
		menuBar.add(buildActionMenu());
		menuBar.add(buildHelpMenu());

		// add the whole menu now
		this.setJMenuBar(menuBar);

		// panel mania
		JPanel mainPanel = new JPanel(); // This panel holds the tabs, and the opsBar
		JPanel mapsPanel = new JPanel(); // This panel holds the list of maps

		// add the panels to the top level, ensuring the left panel gets any extra space

		pane.add(mapsPanel);
		pane.add(mainPanel);

		// setup the main panel
		mainPanel.setLayout(new GridBagLayout());
		mainPanel.setBackground(Color.decode("0x83A767")); //$NON-NLS-1$
		GridBagConstraints c = new GridBagConstraints();
		// using grid bag. For each component, create it, set instance variables to the
		// contstraints, add
		// mainPanel.setMinimumSize(new Dimension(600, 400));
		// JPanel opsPanel; -- is class variable
		// setup the radio group
		JRadioButton radioTile = new JRadioButton(new PerspectiveAction(PERSPECTIVE_TILE));
		radioTile.setText(Messages.getString("EditorApp.55")); //$NON-NLS-1$
		radioTile.setSelected(true);
		radioTile.setOpaque(false);
		JRadioButton radioEntity = new JRadioButton(new PerspectiveAction(PERSPECTIVE_ENTITY));
		radioEntity.setText(Messages.getString("EditorApp.56")); //$NON-NLS-1$
		radioEntity.setOpaque(false);
		JRadioButton radioScript = new JRadioButton(new PerspectiveAction(PERSPECTIVE_TSC));
		radioScript.setText(Messages.getString("EditorApp.57")); //$NON-NLS-1$
		radioScript.setOpaque(false);
		JRadioButton radioMapdata = new JRadioButton(new PerspectiveAction(PERSPECTIVE_MAPDATA));
		radioMapdata.setText(Messages.getString("EditorApp.58")); //$NON-NLS-1$
		radioMapdata.setOpaque(false);
		ButtonGroup group = new ButtonGroup();
		group.add(radioTile);
		group.add(radioEntity);
		group.add(radioScript);
		group.add(radioMapdata);
		JPanel opsRadio = new JPanel();
		opsRadio.setLayout(new GridBagLayout());
		opsRadio.setBackground(Color.decode("0x83A767")); //$NON-NLS-1$
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		opsRadio.add(radioTile, c);
		c.gridy++;
		opsRadio.add(radioEntity, c);
		c.gridy++;
		opsRadio.add(radioScript, c);
		c.gridy++;
		opsRadio.add(radioMapdata, c);
		// constraints
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.weighty = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.BOTH;
		// add
		mainPanel.add(opsRadio, c);

		// constraints
		c.gridy = 0;
		c.gridx = 2;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1.0;
		c.weighty = 0;
		mainPanel.add(buildOpsPanel(), c);

		// setup the tabbed element
		mapTabs = new SplashTabPane(iMan.getImg(ResourceManager.rsrcSplash1), // $NON-NLS-1$
				iMan.getImg(ResourceManager.rsrcSplash2), // $NON-NLS-1$
				iMan.getImg(ResourceManager.rsrcSplashMid)); // $NON-NLS-1$
		mapTabs.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent eve) {
				if (showTileWindow && mapTabs.getSelectedIndex() != -1 && activePerspective.equals(PERSPECTIVE_TILE)) {
					// System.out.println("swapTile");
					// add current tileset to helper
					TabOrganizer inf = componentVec.get(mapTabs.getSelectedIndex());
					MapPane mapPanel = inf.map;
					JScrollPane tileScroll = new JScrollPane(mapPanel.getTilePane());
					tileScroll.getVerticalScrollBar().setUnitIncrement(5);
					JSplitPane tileSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel.getPreviewPane(),
							tileScroll);
					tileSplit.setDividerLocation(100);
					tilesetWindow.setContentPane(tileSplit);
					tilesetWindow.validate();
				} else if (showEntityWindow && mapTabs.getSelectedIndex() != -1
						&& activePerspective.equals(PERSPECTIVE_ENTITY)) {
					// System.out.println("swapEntity");
					TabOrganizer inf = componentVec.get(mapTabs.getSelectedIndex());
					JScrollPane listScroll = new JScrollPane(inf.getEntity().getEntityList());
					JScrollPane editScroll = new JScrollPane(inf.getEntity().getEditPane());
					JSplitPane windowSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editScroll, listScroll);
					windowSplit.setDividerLocation(120);
					entityWindow.setContentPane(windowSplit);
					entityWindow.validate();
				}
				refreshCurrentMap();
			}

		});
		// constraints
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 0;
		c.weighty = 1.0;
		// add
		mainPanel.add(mapTabs, c);

		// setup the maps panel
		mapsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		mapsPanel.setLayout(new BorderLayout(2, 2));
		mapsPanel.add(new JLabel(Messages.getString("EditorApp.59")), BorderLayout.NORTH); //$NON-NLS-1$
		mapList = new BgList<>(iMan.getImg(ResourceManager.rsrcBgWhite)); // $NON-NLS-1$
		//
		buildMapsPopup();
		mapList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					airhorn();
					int destIndex = Integer.parseInt(mapList.getSelectedValue().split("\\s+")[0]);
					if (!movingMapdata) {
						addMapTab(destIndex);
					} else {
						exeData.moveMap(moveIndex, destIndex, EditorApp.this);
						mapList.setListData(exeData.getMapNames());
						movingMapdata = false;
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (!movingMapdata) {
						mapPopup.show(e.getComponent(), e.getX(), e.getY());
					} else {
						mapMovePopup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (!movingMapdata) {
						mapPopup.show(e.getComponent(), e.getX(), e.getY());
					} else {
						mapMovePopup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}

		});
		mapList.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent eve) {
				if (exeData != null) {
					if (eve.getKeyCode() == KeyEvent.VK_DELETE) {
						int result = JOptionPane.showConfirmDialog(EditorApp.this, Messages.getString("EditorApp.24"), //$NON-NLS-1$
								Messages.getString("EditorApp.40"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION);
						if (result == JOptionPane.YES_OPTION) {
							airhorn();
							deleteSelectedMaps();
						}
					} else if (eve.getKeyCode() == KeyEvent.VK_ENTER && !movingMapdata) {
						airhorn();
						int destIndex = Integer.parseInt(mapList.getSelectedValue().split("\\s+")[0]);
						if (!movingMapdata) {
							addMapTab(destIndex);
						}
					}
				}
			}
		});
		JScrollPane mapScroll = new JScrollPane(mapList);
		mapList.setMinimumSize(new Dimension(150, 400));
		mapList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		mapScroll.setPreferredSize(new Dimension(150, 400));
		mapScroll.setMaximumSize(new Dimension(200, 9001));
		// fix size horizontally
		mapsPanel.add(mapScroll, BorderLayout.CENTER);
		notes = new JTextPane();
		notes.setText(notesText);
		mapsPanel.add(notes, BorderLayout.SOUTH);
		pane.setDividerLocation(150);
	}

	private void initHelperWindows() {
		tilesetWindow = new JDialog(this);
		tilesetWindow.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		// if [x] pressed
		tilesetWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				showTileWindow = false;
				((JDialog) e.getSource()).setVisible(false);
				switchPerspective(activePerspective);
			}
		});
		tilesetWindow.setTitle(Messages.getString("EditorApp.60")); //$NON-NLS-1$
		// helperWindow.setAlwaysOnTop(true);
		tilesetWindow.setSize(400, 200);
		Point parentPos = this.getLocation();
		parentPos.x += this.getWidth();
		tilesetWindow.setLocation(parentPos);
		tilesetWindow.setVisible(showTileWindow);

		entityWindow = new JDialog(this);
		entityWindow.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		// if [x] pressed
		entityWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				showEntityWindow = false;
				((JDialog) e.getSource()).setVisible(false);
				switchPerspective(activePerspective);
			}
		});
		entityWindow.setTitle(Messages.getString("EditorApp.61")); //$NON-NLS-1$
		entityWindow.setSize(200, 500);
		entityWindow.setLocation(parentPos);
		entityWindow.setVisible(false);

		scriptWindow = new JDialog(this);
		scriptWindow.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		// if [x] pressed
		// if [x] pressed
		scriptWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				showScriptWindow = false;
				((JDialog) e.getSource()).setVisible(false);
				// switchPerspective(activePerspective);
			}
		});
		scriptWindow.setTitle(Messages.getString("EditorApp.62")); //$NON-NLS-1$
		scriptWindow.setSize(400, 400);
		parentPos.y += tilesetWindow.getHeight();
		scriptWindow.setLocation(parentPos);
		scriptWindow.setVisible(showScriptWindow);
		scriptTabs = new JTabbedPane();
		scriptTabs.getActionMap().put("butts", new AbstractAction() {
			// save
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("butts indeed");
				ActionEvent action = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, EditorApp.ACTION_SAVE);
				EditorApp.this.actionPerformed(action);
			}

		});
		scriptTabs.getActionMap().put("butts2", new AbstractAction() {
			// save all
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("nobutts");
				ActionEvent action = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, EditorApp.ACTION_SAVEALL);
				EditorApp.this.actionPerformed(action);
			}
		});
		scriptTabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "butts");
		scriptTabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "butts2");
		scriptWindow.setName(Messages.getString("EditorApp.63")); //$NON-NLS-1$

		java.net.URL helpLoc = null;
		try {
			File helpDir = new File(System.getProperty("user.dir") + "/help"); //$NON-NLS-1$ //$NON-NLS-2$
			helpLoc = helpDir.toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		helpWindow = new HelpDialog(helpLoc);
		parentPos = this.getLocation();
		parentPos.x += 100;
		parentPos.y += 100;
		helpWindow.setLocation(parentPos);
		helpWindow.setVisible(false);

		spritesheetWindow = new SpritesheetOptimizer(iMan);
		spritesheetWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				showSpritesheetWindow = false;
				((JFrame) e.getSource()).setVisible(false);
				// switchPerspective(activePerspective);
			}
		});
		parentPos.x += 50;
		parentPos.y += 50;
		spritesheetWindow.setLocation(parentPos);
		spritesheetWindow.setVisible(showSpritesheetWindow);

		// Attempt to remember last window sizes and positions
		File winFile = new File("editor.rect"); //$NON-NLS-1$
		try {
			Scanner sc = new Scanner(winFile);
			this.setLocation(sc.nextInt(), sc.nextInt());
			this.setSize(sc.nextInt(), sc.nextInt());
			tilesetWindow.setLocation(sc.nextInt(), sc.nextInt());
			tilesetWindow.setSize(sc.nextInt(), sc.nextInt());
			scriptWindow.setLocation(sc.nextInt(), sc.nextInt());
			scriptWindow.setSize(sc.nextInt(), sc.nextInt());
			helpWindow.setLocation(sc.nextInt(), sc.nextInt());
			helpWindow.setSize(sc.nextInt(), sc.nextInt());
			entityWindow.setLocation(sc.nextInt(), sc.nextInt());
			entityWindow.setSize(sc.nextInt(), sc.nextInt());
			spritesheetWindow.setLocation(sc.nextInt(), sc.nextInt());
			spritesheetWindow.setSize(sc.nextInt(), sc.nextInt());
			sc.close();
		} catch (Exception e1) {
			// do nothing
		}

		if (EditorApp.blazed) {
			tilesetWindow.setCursor(ResourceManager.cursor);
			scriptWindow.setCursor(ResourceManager.cursor);
			helpWindow.setCursor(ResourceManager.cursor);
			entityWindow.setCursor(ResourceManager.cursor);
			spritesheetWindow.setCursor(ResourceManager.cursor);
		}
	}

	private JPopupMenu buildMapsPopup() {
		// add listener things and popup menu
		mapPopup = new JPopupMenu();
		JMenuItem menuItem;
		{
			menuItem = new JMenuItem(Messages.getString("EditorApp.67")); //$NON-NLS-1$
			menuItem.setActionCommand("MapList_Edit"); //$NON-NLS-1$
			menuItem.addActionListener(this);
			// menuItem.setEnabled(false);
			mapPopup.add(menuItem);

			menuItem = new JMenuItem(Messages.getString("EditorApp.69")); //$NON-NLS-1$
			menuItem.setActionCommand("MapList_Delete"); //$NON-NLS-1$
			menuItem.addActionListener(this);
			// menuItem.setEnabled(false);
			mapPopup.add(menuItem);

			menuItem = new JMenuItem(Messages.getString("EditorApp.71")); //$NON-NLS-1$
			menuItem.setActionCommand("MapList_New"); //$NON-NLS-1$
			menuItem.addActionListener(this);
			// menuItem.setEnabled(false);
			mapPopup.add(menuItem);

			menuItem = new JMenuItem(Messages.getString("EditorApp.73")); //$NON-NLS-1$
			menuItem.setActionCommand("MapList_Open"); //$NON-NLS-1$
			menuItem.addActionListener(this);
			// menuItem.setEnabled(false);
			mapPopup.add(menuItem);

			menuItem = new JMenuItem(Messages.getString("EditorApp.75")); //$NON-NLS-1$
			menuItem.setActionCommand("MapList_Duplicate"); //$NON-NLS-1$
			menuItem.addActionListener(this);
			// menuItem.setEnabled(false);
			mapPopup.add(menuItem);

			menuItem = new JMenuItem("Move Map"); //$NON-NLS-1$
			menuItem.setActionCommand("MapList_Move"); //$NON-NLS-1$
			menuItem.addActionListener(this);
			// menuItem.setEnabled(false);
			mapPopup.add(menuItem);
		}
		mapMovePopup = new JPopupMenu();
		menuItem = new JMenuItem("Move Map Here"); //$NON-NLS-1$
		menuItem.setActionCommand("MapList_Move"); //$NON-NLS-1$
		menuItem.addActionListener(this);
		// menuItem.setEnabled(false);
		mapMovePopup.add(menuItem);

		return mapPopup;
	}

	private JMenu buildViewMenu() {
		JMenuItem menuItem;
		JMenu opsMenu = new JMenu(Messages.getString("EditorApp.77")); //$NON-NLS-1$

		// populate the operations menu
		JMenu tilesetScaleMenu = new JMenu(Messages.getString("EditorApp.78")); //$NON-NLS-1$
		JMenu mapScaleMenu = new JMenu(Messages.getString("EditorApp.79")); //$NON-NLS-1$

		for (int i = 0; i < SCALE_OPTIONS; i++) {
			menuItem = new JMenuItem(SCALE_NAMES[i]);
			menuItem.addActionListener(this);
			menuItem.setActionCommand("tileset_scale_" + SCALE_NAMES[i]); //$NON-NLS-1$
			tilesetScaleMenu.add(menuItem);
		}
		for (int i = 0; i < SCALE_OPTIONS; i++) {
			menuItem = new JMenuItem(SCALE_NAMES[i]);
			menuItem.addActionListener(this);
			menuItem.setActionCommand("map_scale_" + SCALE_NAMES[i]); //$NON-NLS-1$
			mapScaleMenu.add(menuItem);
		}
		opsMenu.add(tilesetScaleMenu);
		opsMenu.add(mapScaleMenu);

		JMenuItem pickCol = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = 22747309347145031L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (mapTabs.getComponentCount() <= 0) {
					return;
				}
				TabOrganizer inf = componentVec.get(mapTabs.getSelectedIndex());
				if (inf != null) {
					MapPane map = inf.getMap();
					Color newCol = JColorChooser.showDialog(map, Messages.getString("EditorApp.82"), TilesetPane.bgCol); //$NON-NLS-1$
					if (newCol != null) {
						TilesetPane.bgCol = newCol;
						Preferences prefs = Preferences.userNodeForPackage(EditorApp.class);
						prefs.put(PREF_TILE_COL, String.valueOf(TilesetPane.bgCol.getRGB()));
						map.tilePane.repaint();
					}
				}
			}

		});
		pickCol.setText(Messages.getString("EditorApp.83")); //$NON-NLS-1$
		opsMenu.add(pickCol);

		JMenuItem editorConfig = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = 22747309347145031L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				new BlIniDialog(EditorApp.this, iMan.getImg(ResourceManager.rsrcBgWhite2));
			}

		});
		editorConfig.setText("Edit Project Config Settings"); //$NON-NLS-1$
		editorConfig.setEnabled(false);
		opsMenu.add(editorConfig);
		this.buttonsToEnableOnProjectLoad.add(editorConfig);

		return opsMenu;
	}

	private JMenu buildFileMenu() {
		JMenuItem menuItem;
		JMenu fileMenu = new JMenu(Messages.getString("EditorApp.84")); //$NON-NLS-1$

		// populate the file menu
		menuItem = new JMenuItem(Messages.getString("EditorApp.85")); //$NON-NLS-1$
		menuItem.setActionCommand("FileMenu_New"); //$NON-NLS-1$
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);
		menuItem = new JMenuItem(Messages.getString("EditorApp.87")); //$NON-NLS-1$
		menuItem.setActionCommand("FileMenu_Load"); //$NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);
		menuItem = new JMenuItem(Messages.getString("EditorApp.89")); //$NON-NLS-1$
		menuItem.setActionCommand("FileMenu_Last"); //$NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(this);
		if (lastDir == null) {
			menuItem.setEnabled(false);
		}
		fileMenu.add(menuItem);
		menuItem = new JMenuItem(Messages.getString("EditorApp.91")); //$NON-NLS-1$
		menuItem.setActionCommand(EditorApp.ACTION_SAVE); // $NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(this);
		menuItem.setEnabled(false);
		this.buttonsToEnableOnProjectLoad.add(menuItem);
		fileMenu.add(menuItem);
		menuItem = new JMenuItem(Messages.getString("EditorApp.93")); //$NON-NLS-1$
		menuItem.setActionCommand(EditorApp.ACTION_SAVEALL); // $NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		menuItem.addActionListener(this);
		menuItem.setEnabled(false);
		this.buttonsToEnableOnProjectLoad.add(menuItem);
		fileMenu.add(menuItem);

		JMenu exportMenu = new JMenu(Messages.getString("EditorApp.97"));
		exportMenu.setEnabled(false);
		menuItem = new JMenuItem(new AbstractAction() {
			/**
			 *
			 */
			private static final long serialVersionUID = 8626085285993575830L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (exeData != null && exeData.canPatch()) {
					try {
						exeData.exportMapdata("stage.tbl", GameInfo.MOD_TYPE.MOD_CS_PLUS);
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(EditorApp.this,
								Messages.getString("EditorApp.174"),
								Messages.getString("EditorApp.172"),
								JOptionPane.ERROR_MESSAGE);
					}
					StrTools.msgBox(Messages.getString("EditorApp.95")); //$NON-NLS-1$
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.96")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.170")); //$NON-NLS-1$
		exportMenu.add(menuItem);
		menuItem = new JMenuItem(new AbstractAction() {
			/**
			 *
			 */
			private static final long serialVersionUID = 8626085285993575830L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (exeData != null && exeData.canPatch()) {
					try {
						exeData.exportMapdata("csmap.bin", GameInfo.MOD_TYPE.MOD_CS);
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(EditorApp.this,
								Messages.getString("EditorApp.174"),
								Messages.getString("EditorApp.172"),
								JOptionPane.ERROR_MESSAGE);
					}
					StrTools.msgBox(Messages.getString("EditorApp.173")); //$NON-NLS-1$
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.96")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.171")); //$NON-NLS-1$
		exportMenu.add(menuItem);
		menuItem = new JMenuItem(new AbstractAction() {
			/**
			 *
			 */
			private static final long serialVersionUID = 8626085285993575830L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (exeData != null && exeData.canPatch()) {
					try {
						exeData.exportMapdata("mrmap.bin", GameInfo.MOD_TYPE.MOD_MR);
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(EditorApp.this,
								Messages.getString("EditorApp.174"),
								Messages.getString("EditorApp.172"),
								JOptionPane.ERROR_MESSAGE);
					}
					StrTools.msgBox(Messages.getString("EditorApp.176")); //$NON-NLS-1$
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.96")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.175")); //$NON-NLS-1$
		exportMenu.add(menuItem);
		this.buttonsToEnableOnExeLoad.add(exportMenu);
		fileMenu.add(exportMenu);

		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -2516642407926834624L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				closeCurrentTab();
			}
		});
		menuItem.setText(Messages.getString("EditorApp.98")); //$NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		menuItem.setEnabled(true);
		fileMenu.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -487836554946003024L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (saveAll(false)) {
					GameExporter export = new GameExporter(exeData);

					JFileChooser fc = new JFileChooser(lastDir);
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

					int result = fc.showSaveDialog(EditorApp.this);

					switch (result) {
					case JFileChooser.APPROVE_OPTION:
						try {
							export.exportTo(fc.getSelectedFile());
						} catch (IOException e) {
							e.printStackTrace();
							StrTools.msgBox(Messages.getString("EditorApp.165")); //$NON-NLS-1$
							return;
						}
						StrTools.msgBox(Messages.getString("EditorApp.166")); //$NON-NLS-1$
						break;
					default:
						break;
					}
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.90")); //$NON-NLS-1$
		menuItem.setEnabled(false);
		this.buttonsToEnableOnProjectLoad.addElement(menuItem);
		fileMenu.add(menuItem);

		return fileMenu;
	}

	@SuppressWarnings("serial")
	private JMenu buildActionMenu() {

		JMenuItem menuItem;
		JMenu ops = new JMenu(Messages.getString("EditorApp.99")); //$NON-NLS-1$
		// populate
		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -5667436162643974389L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				airhorn();
				if (exeData != null) {
					try {
						exeData.generateFlagList();
					} catch (IOException e) {
						e.printStackTrace();
						StrTools.msgBox(Messages.getString("EditorApp.41")); //$NON-NLS-1$
					}
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.103")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.100")); //$NON-NLS-1$
		menuItem.setEnabled(false);
		this.buttonsToEnableOnProjectLoad.add(menuItem);
		ops.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -5667436162643974389L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				airhorn();
				if (exeData != null) {
					try {
						exeData.generateTRAList();
					} catch (IOException e) {
						e.printStackTrace();
						StrTools.msgBox(Messages.getString("EditorApp.41")); //$NON-NLS-1$
					}
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.103")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.101")); //$NON-NLS-1$
		menuItem.setEnabled(false);
		this.buttonsToEnableOnProjectLoad.add(menuItem);
		ops.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -4839459771566018919L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				airhorn();
				showScriptWindow = !showScriptWindow;
				scriptWindow.setVisible(showScriptWindow);
			}
		});
		menuItem.setText(Messages.getString("EditorApp.102")); //$NON-NLS-1$
		ops.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -5667436162643974389L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				airhorn();
				if (exeData != null && exeData.canPatch()) {
					JDialog patcher = new HexDialog(EditorApp.this, exeData);
					patcher.dispose();
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.103")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.104")); //$NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
		menuItem.setEnabled(false);
		this.buttonsToEnableOnExeLoad.add(menuItem);
		ops.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -5667436162643974389L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				airhorn();
				if (exeData != null && exeData.canPatch()) {
					// TODO this I think ????
					@SuppressWarnings("unused")
					JDialog patcher = new HackDialog(EditorApp.this, exeData.getExe(), iMan);
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.103")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.110")); //$NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
		menuItem.setEnabled(false);
		this.buttonsToEnableOnExeLoad.add(menuItem);
		ops.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			/**
			 *
			 */
			private static final long serialVersionUID = -6692081319209085736L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (exeData != null && exeData.canPatch()) {
					try {
						exeData.getExe().updateExcode();
					} catch (IOException e1) {
						e1.printStackTrace();
						StrTools.msgBox(Messages.getString("EditorApp.162")); //$NON-NLS-1$
					}
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.163")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.164")); //$NON-NLS-1$
		menuItem.setEnabled(false);
		this.buttonsToEnableOnExeLoad.add(menuItem);
		ops.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				airhorn();
				if (exeData != null) {
					switch (exeData.type) {
					case MOD_CS:
						if (saveAll(false)) {
							exeData.execute();
						}
						break;
					case MOD_CS_PLUS:
						if (saveAll(false)) {
							String path = System.getenv("programfiles(x86)"); //$NON-NLS-1$
							if (path == null) {
								path = System.getenv("programfiles"); //$NON-NLS-1$
							}
							Runtime rt = Runtime.getRuntime();
							try {
								rt.exec(path + "/Steam/Steam.exe -applaunch 200900"); //$NON-NLS-1$
							} catch (IOException e) {
								e.printStackTrace();
								StrTools.msgBox(Messages.getString("EditorApp.50")); //$NON-NLS-1$
							}
						}
						break;
					case MOD_KS:
						if (saveAll(false)) {
							exeData.execute();
						}
						break;
					case MOD_MR:
						StrTools.msgBox(Messages.getString("EditorApp.51")); //$NON-NLS-1$
						break;
					default:
						break;
					}
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.52")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.53")); //$NON-NLS-1$
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		menuItem.setEnabled(false);
		this.buttonsToEnableOnProjectLoad.add(menuItem);
		ops.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent eve) {
				airhorn();
				if (exeData == null) {
					StrTools.msgBox(Messages.getString("EditorApp.54")); //$NON-NLS-1$
					return;
				}
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileNameExtensionFilter(Messages.getString("EditorApp.64"), "tsc")); //$NON-NLS-1$ //$NON-NLS-2$
				if (exeData != null) {
					fc.setCurrentDirectory(exeData.getDataDirectory());
				}
				int rval = fc.showOpenDialog(EditorApp.this);
				if (rval == JFileChooser.APPROVE_OPTION) {
					TscPane t = new TscPane(exeData, fc.getSelectedFile(), iMan);
					new TscDialog(EditorApp.this, fc.getSelectedFile().getName(), t);
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.66")); //$NON-NLS-1$
		ops.add(menuItem);

		// Spritesheet Organizer
		/*
		menuItem = new JMenuItem(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				airhorn();
				showSpritesheetWindow = !showSpritesheetWindow;
				spritesheetWindow.setVisible(showSpritesheetWindow);
			}
		});
		menuItem.setText("toggle sprite thing");
		ops.add(menuItem);
		*/

		menuItem = new JMenuItem(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (exeData != null) {
					airhorn();
					new FileCaseDialog(exeData);
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.103")); //$NON-NLS-1$
				}
			}
		});
		menuItem.setText(Messages.getString("EditorApp.167"));
		menuItem.setEnabled(false);
		this.buttonsToEnableOnProjectLoad.add(menuItem);
		ops.add(menuItem);
		
		// OOB Flag Dialog
		menuItem = new JMenuItem(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				airhorn();
				new OOBFlagDialog(EditorApp.this, iMan);
			}
		});
		menuItem.setText(Messages.getString("EditorApp.168"));
		ops.add(menuItem);

		return ops;
	}

	private JMenu buildHelpMenu() {
		JMenuItem menuItem;
		JMenu help = new JMenu(Messages.getString("EditorApp.105")); //$NON-NLS-1$

		// populate
		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -6358071959738865808L;

			@Override
			public void actionPerformed(ActionEvent e) {
				airhorn();
				helpWindow.setVisible(true);
			}
		});
		menuItem.setText(Messages.getString("EditorApp.106")); //$NON-NLS-1$
		help.add(menuItem);

		menuItem = new JMenuItem(new AbstractAction() {
			private static final long serialVersionUID = -4839459771566018919L;

			@Override
			public void actionPerformed(ActionEvent e) {
				airhorn();
				JOptionPane.showMessageDialog((Component) e.getSource(), ABOUT_STR, Messages.getString("EditorApp.107"), //$NON-NLS-1$
						JOptionPane.INFORMATION_MESSAGE, ABOUT_ICON);
			}
		});
		menuItem.setText(Messages.getString("EditorApp.108")); //$NON-NLS-1$
		help.add(menuItem);

		return help;
	}

	// setup the ops panel
	private JPanel buildOpsPanel() {
		opsPanel = new JPanel(new CardLayout());
		opsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.decode("0x7D9E63"), //$NON-NLS-1$
				Color.decode("0x4F633E"))); //$NON-NLS-1$

		opsPanel.add(buildTileOps(), PERSPECTIVE_TILE);

		opsPanel.add(buildEntityOps(), PERSPECTIVE_ENTITY);

		opsPanel.add(buildScriptOps(), PERSPECTIVE_TSC);

		opsPanel.add(buildMapdataOps(), PERSPECTIVE_MAPDATA);

		return opsPanel;
	}

	// Set up the "Entity" panel
	private JPanel buildEntityOps() {
		// local vars for setting up ops panel
		JPanel tempPanel;
		JLabel kittyLabel;
		java.net.URL kittenURL;
		ImageIcon catImg;
		GridBagConstraints c = new GridBagConstraints();
		// ButtonGroup group;
		tempPanel = new BgPanel(new GridBagLayout(), iMan.getImg(ResourceManager.rsrcBackdrop)); // $NON-NLS-1$
		// ActionListener oListen = new TileOpsListener();
		c.anchor = GridBagConstraints.LINE_START;
		if (blazed) {
			kittenURL = EditorApp.class.getResource("rsrc/weed_EntityCat.gif"); //$NON-NLS-1$
		} else {
			kittenURL = EditorApp.class.getResource("rsrc/EntityCat.gif"); //$NON-NLS-1$
		}
		catImg = new ImageIcon(kittenURL, "pic"); //$NON-NLS-1$
		kittyLabel = new JLabel(catImg);
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = GridBagConstraints.BOTH;
		c.weightx = 0;
		tempPanel.add(kittyLabel, c);

		c.weightx = 0.2;
		c.gridx++;
		// c.fill = GridBagConstraints.BOTH;
		// categoryList = new JList(exeData.getEntityCategories());
		categoryList = new BgList<>(iMan.getImg(ResourceManager.rsrcBgWhite)); // $NON-NLS-1$
		subcatList = new BgList<>(iMan.getImg(ResourceManager.rsrcBgWhite)); // $NON-NLS-1$
		categoryList.setPrototypeCellValue("Long Category"); //$NON-NLS-1$
		subcatList.setPrototypeCellValue("Long Category Name"); //$NON-NLS-1$
		JScrollPane catScroll = new JScrollPane(categoryList);
		categoryList.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void mouseClicked(MouseEvent eve) {
				if (exeData != null && eve.getClickCount() == 2) {
					subcatList
							.setListData(exeData.getEntitySubcat(((JList<String>) eve.getSource()).getSelectedValue()));
				}
			}
		});
		subcatList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent eve) {
				if (eve.getClickCount() == 2) {
					int selected = EditorApp.this.mapTabs.getSelectedIndex();
					if (selected != -1) {
						TabOrganizer currentTab = componentVec.get(selected);
						EntityPane ep = currentTab.getEntity();
						Vector<EntityData> eVec = exeData.getEntityList(categoryList.getSelectedValue(),
								subcatList.getSelectedValue());
						ep.getEntityList().setListData(eVec);
					} // if there is a selected tab
				} // if doubleclick
			}
		});

		catScroll.setMinimumSize(new Dimension(120, 120));
		tempPanel.add(catScroll, c);

		c.gridx++;
		// subcatList = new JList(exeData.getEntitySubcat("All"));
		JScrollPane subcatScroll = new JScrollPane(subcatList);
		subcatScroll.setMinimumSize(new Dimension(120, 120));
		subcatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		tempPanel.add(subcatScroll, c);

		// npc.tbl editor button
		npcTblWindow = new NpcTblEditor(this);
		c.gridx++;
		c.weightx = 0.2;
		JButton npcTblButton = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 5301785830920994513L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				airhorn();
				if (exeData != null) {
					((NpcTblEditor) npcTblWindow).populate(exeData);
					npcTblWindow.setVisible(true);
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.113")); //$NON-NLS-1$
				}
			}
		});
		npcTblButton.setText(Messages.getString("EditorApp.114")); //$NON-NLS-1$
		npcTblButton.setOpaque(false);
		tempPanel.add(npcTblButton, c);

		// buffer space
		c.gridx++;
		c.gridy = 0;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.weightx = 0.8;
		tempPanel.add(new JLabel(), c);

		return tempPanel;
	}

	// Set up the "Script" panel
	private JPanel buildScriptOps() {
		// local vars for setting up ops panel
		JPanel tempPanel;
		JLabel kittyLabel;
		java.net.URL kittenURL;
		ImageIcon catImg;
		GridBagConstraints c = new GridBagConstraints();
		// ButtonGroup group;
		tempPanel = new BgPanel(new GridBagLayout(), iMan.getImg(ResourceManager.rsrcBackdrop)); // $NON-NLS-1$
		// ActionListener oListen = new TileOpsListener();
		c.anchor = GridBagConstraints.LINE_START;
		if (blazed) {
			kittenURL = EditorApp.class.getResource("rsrc/weed_ScriptCat.gif"); //$NON-NLS-1$
		} else {
			kittenURL = EditorApp.class.getResource("rsrc/ScriptCat.gif"); //$NON-NLS-1$
		}
		catImg = new ImageIcon(kittenURL, "pic"); //$NON-NLS-1$
		kittyLabel = new JLabel(catImg);
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		tempPanel.add(kittyLabel, c);
		c.gridheight = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx++;
		tscSearch = new JTextField(16);
		tscSearch.setAction(new ScriptSearchAction());
		tempPanel.add(tscSearch, c);
		c.anchor = GridBagConstraints.WEST;
		c.gridy++;
		JButton findButton = new JButton(new ScriptSearchAction());
		findButton.setOpaque(false);
		findButton.setText(Messages.getString("EditorApp.117")); //$NON-NLS-1$
		tempPanel.add(findButton, c);
		c.gridx++;
		c.gridy = 0;

		ButtonGroup g = new ButtonGroup();
		JRadioButton comButton = new JRadioButton(new AbstractAction() {
			private static final long serialVersionUID = -6730103666544430394L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				// rebuild script pane
				airhorn();
				if (TscPane.getComPanel() != null) {
					JPanel p = new JPanel(new BorderLayout());
					p.add(scriptTabs, BorderLayout.CENTER);
					p.add(TscPane.getComPanel(), BorderLayout.WEST);
					scriptWindow.setContentPane(p);
					scriptWindow.validate();
				}
			}
		});
		comButton.setOpaque(false);
		comButton.setText(Messages.getString("EditorApp.118")); //$NON-NLS-1$
		g.add(comButton);
		comButton.setSelected(showingCommands);
		tempPanel.add(comButton, c);
		c.gridy++;
		JRadioButton defButton = new JRadioButton(new AbstractAction() {
			private static final long serialVersionUID = -6730103666544420394L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				// rebuild script pane
				airhorn();
				JPanel p = new JPanel(new BorderLayout());
				p.add(new JScrollPane(TscPane.getDefPanel()), BorderLayout.WEST);
				p.add(scriptTabs, BorderLayout.CENTER);
				scriptWindow.setContentPane(p);
				scriptWindow.validate();
			}
		});
		defButton.setOpaque(false);
		defButton.setText(Messages.getString("EditorApp.119")); //$NON-NLS-1$
		defButton.setSelected(!showingCommands);
		g.add(defButton);
		tempPanel.add(defButton, c);
		c.gridx++;
		c.gridy = 0;

		/*
		JButton flagButton = new JButton("Gen. Flag listing");
		tempPanel.add(flagButton, c);
		c.gridy++;
		JButton traButton = new JButton("Gen. <TRA listing");
		tempPanel.add(traButton, c);
		c.gridx++;
		c.gridy = 0;
		*/

		return tempPanel;
	}

	// Set up the "Mapdata" panel
	private JPanel buildMapdataOps() {
		// local vars for setting up ops panel
		JPanel tempPanel;
		JLabel kittyLabel;
		java.net.URL kittenURL;
		ImageIcon catImg;
		GridBagConstraints c = new GridBagConstraints();
		// ButtonGroup group;
		tempPanel = new BgPanel(new GridBagLayout(), iMan.getImg(ResourceManager.rsrcBackdrop)); // $NON-NLS-1$
		// ActionListener oListen = new TileOpsListener();
		c.anchor = GridBagConstraints.LINE_START;
		if (blazed) {
			kittenURL = EditorApp.class.getResource("rsrc/weed_MapdataCat.gif"); //$NON-NLS-1$
		} else {
			kittenURL = EditorApp.class.getResource("rsrc/MapdataCat.gif"); //$NON-NLS-1$
		}
		catImg = new ImageIcon(kittenURL, "pic"); //$NON-NLS-1$
		kittyLabel = new JLabel(catImg);
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		tempPanel.add(kittyLabel, c);

		return tempPanel;
	}

	// Set up the "Tile" operations panel
	private JPanel buildTileOps() {
		// local vars for setting up ops panel
		JPanel tempPanel;
		JLabel kittyLabel;
		java.net.URL kittenURL;
		ImageIcon catImg;
		GridBagConstraints c = new GridBagConstraints();
		ButtonGroup group;
		tempPanel = new BgPanel(new GridBagLayout(), iMan.getImg(ResourceManager.rsrcBackdrop)); // $NON-NLS-1$
		c.anchor = GridBagConstraints.LINE_START;
		// needs cats
		if (blazed) {
			kittenURL = EditorApp.class.getResource("rsrc/weed_TileCat.gif"); //$NON-NLS-1$
		} else {
			kittenURL = EditorApp.class.getResource("rsrc/TileCat.gif"); //$NON-NLS-1$
		}
		catImg = new ImageIcon(kittenURL, "pic"); //$NON-NLS-1$
		kittyLabel = new JLabel(catImg);
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		tempPanel.add(kittyLabel, c);
		// add a set of checkboxes for visible labels
		JCheckBox check;
		c.gridx = 1;
		c.gridheight = 1;
		tempPanel.add(new JLabel(Messages.getString("EditorApp.124")), c); //$NON-NLS-1$
		for (int i = 0; i < NUM_LAYER; i++) {
			check = new JCheckBox(new VisibleLayerAction(i));
			check.setText(LAYER_NAMES[i]);
			c.gridy++;
			check.setSelected(true);
			check.setOpaque(false);
			check.setMnemonic(KeyEvent.VK_6 + i);
			visibleLayers[i] = true;
			if (EDITOR_MODE == 0 && (i == 0 || i == 3)) {
				check.setEnabled(false);
			}
			tempPanel.add(check, c);
		}
		// add a set of radio buttons for the active layer
		JRadioButton radio;
		group = new ButtonGroup();
		c.gridx = 2;
		c.gridy = 0;
		tempPanel.add(new JLabel(Messages.getString("EditorApp.125")), c); //$NON-NLS-1$
		for (int i = 0; i < NUM_LAYER; i++) {

			radio = new JRadioButton(new ActiveLayerAction(i));
			radio.setText(LAYER_NAMES[i]);
			radio.setOpaque(false);
			radio.setMnemonic(java.awt.event.KeyEvent.VK_1 + i);
			group.add(radio);
			c.gridy++;
			if (i == 2) {
				radio.setSelected(true);
			}
			if (EDITOR_MODE == 0) {
				radio.setEnabled(false);
			}
			tempPanel.add(radio, c);
		}
		setActiveLayer(2);
		// add a set of radio buttons for draw actions
		group = new ButtonGroup();
		c.gridx = 3;
		c.gridy = 0;
		tempPanel.add(new JLabel(Messages.getString("EditorApp.126")), c); //$NON-NLS-1$
		int[] mnemonicArray = { KeyEvent.VK_D, KeyEvent.VK_F, KeyEvent.VK_R, KeyEvent.VK_E, KeyEvent.VK_C, };
		for (int i = 0; i < NUM_DRAWMODE; i++) {
			radio = new JRadioButton(new DrawmodeAction(i));
			radio.setText(TILEOP_DRAWMODES[i]);
			radio.setOpaque(false);
			radio.setMnemonic(mnemonicArray[i]);
			group.add(radio);
			c.gridy++;
			if (i == 0) {
				radio.setSelected(true);
			}
			tempPanel.add(radio, c);
		}
		// add a set of checkboxes for other options
		c.gridx = 4;
		c.gridy = 0;
		tempPanel.add(new JLabel(Messages.getString("EditorApp.127")), c); //$NON-NLS-1$
		for (int i = 0; i < TILEOP_NUM_OTHER_OPT; i++) {
			check = new JCheckBox(new OtherOpAction(i));
			check.setText(TILEOP_OTHER_OPTS[i]);
			check.setOpaque(false);
			check.setSelected(otherDrawOpts[i]);
			c.gridy++;
			tempPanel.add(check, c);
		}

		// add a slider for gradient layer alpha
		if (EDITOR_MODE == 2) {
			c.gridx = 5;
			c.gridy = 0;
			tempPanel.add(new JLabel("Alpha:"), c);
			c.gridy++;
			c.gridheight = GridBagConstraints.REMAINDER;

			final JSlider alphaSlider = new BgSlider(JSlider.VERTICAL, 0, 100, 50,
					iMan.getImg(ResourceManager.rsrcBackdrop));
			alphaSlider.setPreferredSize(new Dimension(60, 120));
			alphaSlider.setMajorTickSpacing(25);

			alphaSlider.setPaintTicks(true);
			Hashtable<Integer, Component> labels = new Hashtable<>();
			labels.put(0, new JLabel("0%"));
			labels.put(50, new JLabel("50%"));
			labels.put(100, new JLabel("100%"));
			alphaSlider.setLabelTable(labels);
			alphaSlider.setPaintLabels(true);

			alphaSlider.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent arg0) {
					if (!alphaSlider.getValueIsAdjusting()) {
						int newValue = alphaSlider.getValue();
						if (exeData != null) {
							BlConfig conf = exeData.getConfig();
							conf.setGradientAlpha(newValue);
							refreshCurrentMap();
						}
					}
				}

			});
			tempPanel.add(alphaSlider, c);
		}

		// buffer space
		c.gridx++;
		c.gridy = 0;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.weightx = 0.5;
		tempPanel.add(new JLabel(), c);

		tempPanel.setMaximumSize(new Dimension(800, 1000));
		return tempPanel;
	}

	public class TabOrganizer implements PropertyChangeListener, Changeable {
		MapInfo data;
		MapPane map;
		EntityPane entity;
		// subject to change
		TscPane TSC;
		MapdataPane mapdata;
		TscBuilder tscBuilder;

		boolean starred = false;

		public MapPane getMap() {
			return map;
		}

		public EntityPane getEntity() {
			return entity;
		}

		public TscPane getTSC() {
			return TSC;
		}

		public JPanel getMapdata() {
			return mapdata;
		}

		TabOrganizer(MapPane m, EntityPane e, TscPane txt, MapdataPane d, MapInfo dat) {
			map = m;
			data = dat;
			data.addPropertyChangeListener(this);
			if (!data.isTemp) {
				exeData.getMapdata(data.getMapNumber()).addPropertyChangeListener(this);
			} else {
				d.getMapdata().addPropertyChangeListener(this);
			}
			entity = e;
			entity.addPropertyChangeListener(this);
			TSC = txt;
			TSC.addPropertyChangeListener(this);
			mapdata = d;
			mapdata.addPropertyChangeListener(this);
			tscBuilder = new TscBuilder(iMan.getImg(ResourceManager.rsrcBgBlue)); // $NON-NLS-1$
		}

		public boolean isModified() {
			return data.isModified() | TSC.isModified() | mapdata.isModified();
		}

		public void markUnchanged() {
			if (starred) {
				if (!data.isModified() && !TSC.isModified() && !mapdata.isModified()) {
					starred = false;
					int myIndex = componentVec.indexOf(this);
					String currentTitle = mapTabs.getTitleAt(myIndex);
					currentTitle = currentTitle.substring(0, currentTitle.length() - 1);
					mapTabs.setTitleAt(myIndex, currentTitle);
				}
			}
		}

		public void markChanged() {
			if (!starred) {
				starred = true;
				int myIndex = componentVec.indexOf(this);
				String currentTitle = mapTabs.getTitleAt(myIndex);
				mapTabs.setTitleAt(myIndex, currentTitle + '*');
			}
		}

		public void save() {

			try {
				data.save();
			} catch (Exception er) {
				er.printStackTrace();
			}
			try {
				TSC.save();
			} catch (Exception err) {
				err.printStackTrace();
			}
			// entity.save();
			if (!data.isTemp) {
				try {
					exeData.saveMapData(data.getMapNumber());
				} catch (IOException e) {
					StrTools.msgBox(Messages.getString("EditorApp.128")); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}

		/**
		 * Show a save confirm dialog with the options Yes, No, Cancel
		 *
		 * @return JOptionPane.<choice>
		 */
		public int promptSave() {
			String tabTitle = Messages.getString("EditorApp.155") //$NON-NLS-1$
					+ mapTabs.getTitleAt(mapTabs.getSelectedIndex()); // $NON-NLS-1$
			return JOptionPane.showConfirmDialog(EditorApp.this, Messages.getString("EditorApp.156") + //$NON-NLS-1$
					Messages.getString("EditorApp.157"), //$NON-NLS-1$
					tabTitle, JOptionPane.YES_NO_CANCEL_OPTION);
		}

		@Override
		public void propertyChange(PropertyChangeEvent eve) {
			if (eve.getPropertyName() == Mapdata.P_NAME && !data.isTemp) {
				String name = (String) eve.getNewValue();
				mapTabs.setToolTipTextAt(mapTabs.getSelectedIndex(), name);
				// attempt to update the list
				mapList.setListData(exeData.getMapNames());
				// attempt to update the TSC majig
				String targetName = mapTabs.getTitleAt(mapTabs.getSelectedIndex());
				for (int i = 0; i < scriptTabs.getComponentCount(); i++) {
					String tabName = scriptTabs.getTitleAt(i);
					if (tabName.equals(targetName)) {
						scriptTabs.setToolTipTextAt(i, name);
						break;
					}
				}
			} else if (eve.getPropertyName() == Mapdata.P_TILE) {
				String n = (String) eve.getNewValue();
				File tileFile = new File(exeData.getDataDirectory() + "/Stage/Prt" + //$NON-NLS-1$
						n + exeData.getImgExtension());
				File pxaFile = new File(exeData.getDataDirectory() + "/Stage/" + //$NON-NLS-1$
						n + ".pxa"); //$NON-NLS-1$
				data.setTileset(pxaFile, tileFile);
				((TilesetPane) this.getMap().getTilePane()).setTileBounds();
			} else if (eve.getPropertyName() == Mapdata.P_NPC1) {
				File npcFile = new File(exeData.getDataDirectory() + "/Npc/Npc" + //$NON-NLS-1$
						eve.getNewValue() + exeData.getImgExtension());
				data.setNpc1Img(npcFile);
			} else if (eve.getPropertyName() == Mapdata.P_NPC2) {
				File npcFile = new File(exeData.getDataDirectory() + "/Npc/Npc" + //$NON-NLS-1$
						eve.getNewValue() + exeData.getImgExtension());
				data.setNpc2Img(npcFile);
			} else if (eve.getPropertyName() == Mapdata.P_BGIMG) {
				File bgFile = new File(
						exeData.getDataDirectory() + File.separator + eve.getNewValue() + exeData.getImgExtension());
				data.setBgImg(bgFile);
			} else if (eve.getPropertyName() == Mapdata.P_FILE && !data.isTemp) {
				if (eve.getOldValue() != null) {
					this.save();
				}
				closeCurrentTab();
				// this must be done prematurely
				// Mapdata d = exeData.getMapdata(data.getMapNumber());
				// d.setFile((String) eve.getNewValue());
				// attempt to update the list
				mapList.setListData(exeData.getMapNames());
				// close the current tab because shenanigans
			} else if (eve.getPropertyName() == Changeable.PROPERTY_EDITED) {
				if ((Boolean) eve.getNewValue()) {
					markChanged();
				} else {
					markUnchanged();
				}
			}
		}

		/**
		 * Frees listeners etc. associated with this tab.
		 */
		public void free() {

			data.removePropertyChangeListener(this);
			if (!data.isTemp) {
				exeData.getMapdata(data.getMapNumber()).removePropertyChangeListener(this);
			}
			entity.removePropertyChangeListener(this);
			TSC.removePropertyChangeListener(this);
			mapdata.removePropertyChangeListener(this);
		}
	}

	// method adds a new map tab to the thing
	private void addMapTab(int selectionNum) {
		// check to see if we are even set up yet
		if (exeData == null) {
			return;
		}
		try {
			// check to see if it exists already
			for (int i = 0; i < mapTabs.getComponentCount(); i++) {
				String tabName = mapTabs.getTitleAt(i);
				if (tabName.endsWith("*")) {
					tabName = tabName.substring(0, tabName.length() - 1);
				}
				if (tabName.equals(exeData.getShortName(selectionNum))
						|| tabName.equals(exeData.getLongName(selectionNum))) {
					mapTabs.setSelectedIndex(i);
					// scriptTabs.setSelectedIndex(i);
					return;
				}
			}

			// set up the data holder
			MapInfo data = new MapInfo(exeData, iMan, selectionNum);
			// set up the map pane
			MapPane mapPanel = new MapPane(this, data);
			EntityPane entityPanel = new EntityPane(this, data);
			TscPane txt = new TscPane(exeData, selectionNum, this, iMan);

			// set up the mapdata pane
			MapdataPane mapdatPanel = new MapdataPane(exeData, selectionNum, iMan.getImg(ResourceManager.rsrcBgBlue),
					true); // $NON-NLS-1$

			// add bits to holder
			TabOrganizer holder = new TabOrganizer(mapPanel, entityPanel, txt, mapdatPanel, data);
			// componentVec.setSize(mapTabs.getTabCount() + 2); //not needed?
			componentVec.add(mapTabs.getTabCount(), holder);

			// finalize
			// layout.show(tabPanel, activePerspective);
			Component contents = buildTabContents(holder);
			mapTabs.insertTab(exeData.getShortName(selectionNum), null, contents, exeData.getLongName(selectionNum),
					mapTabs.getComponentCount());
			mapTabs.setSelectedComponent(contents);
			if (scriptWindow.getName().equals(Messages.getString("EditorApp.2"))) { //$NON-NLS-1$
				// needs to be initialized
				if (showingCommands) {
					scriptWindow.add(TscPane.getComPanel(), BorderLayout.WEST);
				} else {
					JScrollPane defScroll = new JScrollPane(TscPane.getDefPanel());
					scriptWindow.add(defScroll, BorderLayout.WEST);
				}
				scriptWindow.add(scriptTabs, BorderLayout.CENTER);
				scriptWindow.setName("scriptWindow"); //$NON-NLS-1$
			}
			JScrollPane textScroll = new JScrollPane(txt);
			scriptTabs.insertTab(exeData.getShortName(selectionNum), null, textScroll,
					exeData.getLongName(selectionNum), scriptTabs.getComponentCount());
			scriptTabs.setSelectedComponent(textScroll);

		} catch (OutOfMemoryError err) {
			StrTools.msgBox(Messages.getString("EditorApp.136")); //$NON-NLS-1$
		} /*catch (Exception ex) {
			ex.printStackTrace();
			StrTools.msgBox("Failed to add map tab:\n" + ex);
			}*/
	}

	private void addMapTab(File mapfile) {
		// this is a terrible idea
		if (exeData == null) {
			try {
				exeData = new GameInfo(mapfile);
				exeData.deleteMap(0, null);
				mapPopup.setEnabled(false);
			} catch (IOException e) {
				StrTools.msgBox(Messages.getString("EditorApp.109")); //$NON-NLS-1$
				e.printStackTrace();
			}
		}
		MapdataDialog diag = new MapdataDialog(this, mapfile, exeData, iMan.getImg(ResourceManager.rsrcBgBlue)); // $NON-NLS-1$
		Mapdata mapdat = diag.getMapdata();

		MapInfo data = new MapInfo(exeData, iMan, mapdat);

		MapPane mapPanel = new MapPane(this, data);
		EntityPane entityPanel = new EntityPane(this, data);
		TscPane txt = new TscPane(exeData, mapdat, this, iMan);

		// set up the mapdata pane
		MapdataPane mapdatPanel = new MapdataPane(exeData, mapdat, iMan.getImg(ResourceManager.rsrcBgBlue), true); // $NON-NLS-1$

		// add bits to holder
		TabOrganizer holder = new TabOrganizer(mapPanel, entityPanel, txt, mapdatPanel, data);
		// componentVec.setSize(mapTabs.getTabCount() + 2); //not needed?
		componentVec.add(mapTabs.getTabCount(), holder);

		// finalize
		// layout.show(tabPanel, activePerspective);
		Component contents = buildTabContents(holder);
		mapTabs.insertTab(mapdat.getFile(), null, contents, mapdat.getMapname(), mapTabs.getComponentCount());
		mapTabs.setSelectedComponent(contents);
		if (scriptWindow.getName().equals(Messages.getString("EditorApp.2"))) { //$NON-NLS-1$
			// needs to be initialized
			if (showingCommands) {
				scriptWindow.add(TscPane.getComPanel(), BorderLayout.WEST);
			} else {
				JScrollPane defScroll = new JScrollPane(TscPane.getDefPanel());
				scriptWindow.add(defScroll, BorderLayout.WEST);
			}
			scriptWindow.add(scriptTabs, BorderLayout.CENTER);
			scriptWindow.setName("scriptWindow"); //$NON-NLS-1$
		}
		JScrollPane textScroll = new JScrollPane(txt);
		scriptTabs.insertTab(mapdat.getFile(), null, textScroll, mapdat.getMapname(), scriptTabs.getComponentCount());
		scriptTabs.setSelectedComponent(textScroll);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// radio buttons for card layout
		FileNameExtensionFilter filter;
		int selectedMap = -1;
		String selectedMapStr = mapList.getSelectedValue();
		if (selectedMapStr != null) {
			selectedMap = Integer.parseInt(selectedMapStr.split("\\s+")[0]);
		}
		JFileChooser fc = new JFileChooser();
		int retVal;
		if (e.getActionCommand().equals("MapList_Edit")) { //$NON-NLS-1$
			airhorn();
			// don't do if we ain't loaded
			if (exeData == null) {
				return;
			}
			Mapdata d = exeData.getMapdata(selectedMap);
			// check tabs
			boolean success = removeTab(d);
			// update
			if (success) {
				new MapdataDialog(this, d.getMapnum(), exeData, iMan.getImg(ResourceManager.rsrcBgBlue)); // $NON-NLS-1$
				mapList.setListData(exeData.getMapNames());
			}
		} else if (e.getActionCommand().equals("MapList_Delete")) { //$NON-NLS-1$
			airhorn();
			// don't do if we ain't loaded
			if (exeData == null) {
				return;
			}
			int choice = JOptionPane.showConfirmDialog(this, Messages.getString("EditorApp.24"),
					Messages.getString("EditorApp.40"), JOptionPane.YES_NO_OPTION);
			if (choice != JOptionPane.YES_OPTION) {
				return;
			}
			deleteSelectedMaps();
		} else if (e.getActionCommand().equals("MapList_New")) { //$NON-NLS-1$
			airhorn();
			// don't do if we ain't loaded
			if (exeData == null) {
				return;
			}
			Mapdata d = exeData.addMap();
			new MapdataDialog(this, d.getMapnum(), exeData, iMan.getImg(ResourceManager.rsrcBgBlue)); // $NON-NLS-1$
			mapList.setListData(exeData.getMapNames());
		} else if (e.getActionCommand().equals("MapList_Open")) { //$NON-NLS-1$
			airhorn();
			this.addMapTab(selectedMap);
		} else if (e.getActionCommand().equals("MapList_Duplicate")) { //$NON-NLS-1$
			airhorn();
			// don't do if we ain't loaded
			if (exeData == null) {
				return;
			}
			exeData.duplicateMap(selectedMap);
			mapList.setListData(exeData.getMapNames());
		} else if (e.getActionCommand().equals("MapList_Move")) { //$NON-NLS-1$
			airhorn();
			if (exeData == null) {
				return;
			}
			if (!movingMapdata) {
				movingMapdata = true;
				moveIndex = selectedMap;
				mapList.setToolTipText(Messages.getString("EditorApp.129") + //$NON-NLS-1$
						selectedMap);
			} else {
				mapList.setToolTipText(null);
				exeData.moveMap(moveIndex, selectedMap, this);
				mapList.setListData(exeData.getMapNames());
				movingMapdata = false;
			}
		} else if (e.getActionCommand().equals("FileMenu_Load")) { //$NON-NLS-1$
			airhorn();
			if (exeData != null)
				if (!saveAll(true))
					return;

			filter = new FileNameExtensionFilter(Messages.getString("EditorApp.143"), "bin", "exe", "tbl", "pxm"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			fc.setFileFilter(filter);

			fc.setCurrentDirectory(lastDir);
			retVal = fc.showOpenDialog(this);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				File selected = fc.getSelectedFile();
				if (selected.toString().endsWith(".pxm")) { //$NON-NLS-1$
					// load single file
					addMapTab(selected);
				} else {
					try {
						loadFile(selected);
						lastDir = selected;
					} catch (IOException e1) {
						System.err.println(Messages.getString("EditorApp.147") + fc.getSelectedFile().getName()); //$NON-NLS-1$
					}
				}
			}
		} else if (e.getActionCommand().equals("FileMenu_New")) { //$NON-NLS-1$
			airhorn();
			if (exeData != null)
				if (!saveAll(true))
					return;
			// purge existing memory
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			StrTools.msgBox(Messages.getString("EditorApp.132") + //$NON-NLS-1$
					Messages.getString("EditorApp.133")); //$NON-NLS-1$
			retVal = fc.showOpenDialog(this);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				try {
					File dir = fc.getSelectedFile();
					if (!dir.exists()) {
						// noinspection ResultOfMethodCallIgnored
						dir.mkdirs();
					}
					File tblFile = new File(dir + "/dsmap.bin"); //$NON-NLS-1$
					if (tblFile.exists()) {
						int response = JOptionPane.showConfirmDialog(this, Messages.getString("EditorApp.135"), //$NON-NLS-1$
								Messages.getString("EditorApp.137"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION);
						if (response != JOptionPane.YES_OPTION) {
							return;
						}
					}
					GameInfo.writeDefaultFiles(tblFile);
					loadFile(tblFile);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		} else if (e.getActionCommand().equals("FileMenu_Last")) { //$NON-NLS-1$
			airhorn();
			if (exeData != null)
				if (!saveAll(true))
					return;
			try {
				if (lastDir.exists()) {
					loadFile(lastDir);
				} else {
					StrTools.msgBox(Messages.getString("EditorApp.138") + lastDir); //$NON-NLS-1$
				}
			} catch (IOException err) {
				err.printStackTrace();
			}

		} else if (e.getActionCommand().equals(EditorApp.ACTION_SAVE)) { // $NON-NLS-1$
			airhorn();
			if (mapTabs.getComponentCount() <= 0) {
				return;
			}
			TabOrganizer inf = componentVec.get(mapTabs.getSelectedIndex());
			inf.save();
		} else if (e.getActionCommand().equals(EditorApp.ACTION_SAVEALL)) { // $NON-NLS-1$
			airhorn();

			for (TabOrganizer i : componentVec) {
				i.save();
			}
			exeData.commitChanges();
		} else {
			airhorn();
			for (int i = 0; i < SCALE_OPTIONS; i++) {
				if (e.getActionCommand().equals("tileset_scale_" + SCALE_NAMES[i])) { //$NON-NLS-1$
					tilesetScale = 0.25 * Math.pow(2, i);
					refreshCurrentMap();
				}
			}
			for (int i = 0; i < SCALE_OPTIONS; i++) {
				if (e.getActionCommand().equals("map_scale_" + SCALE_NAMES[i])) { //$NON-NLS-1$
					mapScale = 0.25 * Math.pow(2, i);
					refreshCurrentMap();
				}
			}
		}
	}

	private void deleteSelectedMaps() {
		List<String> mapstrs = mapList.getSelectedValuesList();
		int[] indices = new int[mapstrs.size()];
		for (int i = 0; i < mapstrs.size(); i++) {
			indices[i] = Integer.parseInt(mapstrs.get(i).split("\\s+")[0]);
		}
		java.util.Arrays.sort(indices);
		exeData.prepareToDeleteMaps();
		// remove top-down so as not to disturb the natural order.
		for (int i = indices.length - 1; i >= 0; i--) {
			Mapdata d = exeData.getMapdata(indices[i]);
			// check tabs
			boolean success = removeTab(d);
			// remove
			if (success) {
				exeData.deleteMap(indices[i], this);
			}
		}
		exeData.doneDeletingMaps();
		mapList.setListData(exeData.getMapNames());
	}

	private void purgeData() {
		iMan.purge();
		mapTabs.removeAll();
		scriptTabs.removeAll();
		componentVec.clear();
		mapList.removeAll();
	}

	public void loadFile(File selected) throws IOException {
		purgeData(); // clean up any old tabs
		exeData = new GameInfo(selected);
		exeData.loadImages(iMan);
		mapList.setListData(exeData.getMapNames());
		categoryList.setListData(exeData.getEntityCategories());
		subcatList.setListData(exeData.getEntitySubcat(Messages.getString("EditorApp.154"))); //$NON-NLS-1$
		((NpcTblEditor) npcTblWindow).populate(exeData);
		TscPane.initDefines(exeData.getDataDirectory());
		for (AbstractButton b : buttonsToEnableOnProjectLoad) {
			b.setEnabled(true);
		}
		if (exeData.canPatch()) {
			for (AbstractButton b : buttonsToEnableOnExeLoad) {
				b.setEnabled(true);
			}
		}
	}

	private class OtherOpAction extends AbstractAction {
		/**
		 *
		 */
		private static final long serialVersionUID = 2590795299419022003L;
		int index;

		OtherOpAction(int n) {
			index = n;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			otherDrawOpts[index] = !otherDrawOpts[index];
			airhorn();
			refreshCurrentMap();
		}
	}

	private class DrawmodeAction extends AbstractAction {
		/**
		 *
		 */
		private static final long serialVersionUID = -3828837252245691218L;
		int mode;

		DrawmodeAction(int n) {
			mode = n;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setDrawMode(mode);
			airhorn();
		}
	}

	private class ActiveLayerAction extends AbstractAction {
		/**
		 *
		 */
		private static final long serialVersionUID = 8292845970444285006L;
		int layer;

		ActiveLayerAction(int n) {
			layer = n;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setActiveLayer(layer);
			airhorn();

		}
	}

	private class VisibleLayerAction extends AbstractAction {
		/**
		 *
		 */
		private static final long serialVersionUID = 5009351875420439556L;
		int layer;

		VisibleLayerAction(int n) {
			layer = n;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			visibleLayers[layer] = !visibleLayers[layer];
			refreshCurrentMap();
			airhorn();
		}
	}

	private void switchPerspective(String perspective) {
		activePerspective = perspective;
		switch (perspective) {
		case PERSPECTIVE_TILE: {
			// airhorn();
			tilesetWindow.setVisible(showTileWindow);
			entityWindow.setVisible(false);
			CardLayout layout = (CardLayout) opsPanel.getLayout();
			layout.show(opsPanel, PERSPECTIVE_TILE);
			// update the tabs
			for (int i = 0; i < mapTabs.getComponentCount(); i++) {
				// for each tab
				// JPanel currentPanel = (JPanel)mapTabs.getComponent(i);
				TabOrganizer inf = componentVec.get(i);
				// currentPanel.removeAll();
				mapTabs.setComponentAt(i, buildTabContents(inf));
				// currentPanel.revalidate();
			}
			refreshCurrentMap();
			break;
		}
		case PERSPECTIVE_ENTITY: {
			tilesetWindow.setVisible(false);
			entityWindow.setVisible(showEntityWindow);
			CardLayout layout = (CardLayout) opsPanel.getLayout();
			layout.show(opsPanel, PERSPECTIVE_ENTITY);

			// update the tabs
			for (int i = 0; i < mapTabs.getComponentCount(); i++) {
				// for each tab
				// JPanel currentPanel = (JPanel)mapTabs.getComponent(i);
				TabOrganizer inf = componentVec.get(i);
				// currentPanel.removeAll();
				mapTabs.setComponentAt(i, buildTabContents(inf));
				// currentPanel.revalidate();
			}
			refreshCurrentMap();
			break;
		}
		case PERSPECTIVE_TSC: {
			tilesetWindow.setVisible(false);
			entityWindow.setVisible(false);
			scriptWindow.setVisible(true);
			showScriptWindow = true;
			CardLayout layout = (CardLayout) opsPanel.getLayout();
			layout.show(opsPanel, PERSPECTIVE_TSC);
			// update the tabs
			for (int i = 0; i < mapTabs.getComponentCount(); i++) {
				// for each tab
				// JPanel currentPanel = (JPanel)mapTabs.getComponent(i);
				TabOrganizer inf = componentVec.get(i);
				// currentPanel.removeAll();
				mapTabs.setComponentAt(i, buildTabContents(inf));
				// currentPanel.revalidate();
			}
			mapTabs.repaint();
			break;
		}
		case PERSPECTIVE_MAPDATA: {
			tilesetWindow.setVisible(false);
			entityWindow.setVisible(false);
			CardLayout layout = (CardLayout) opsPanel.getLayout();
			layout.show(opsPanel, PERSPECTIVE_MAPDATA);
			// update the tabs
			for (int i = 0; i < mapTabs.getComponentCount(); i++) {
				// for each tab
				/*
				JPanel currentPanel = (JPanel)mapTabs.getComponent(i);
				layout = (CardLayout)currentPanel.getLayout();
				layout.show(currentPanel, PERSPECTIVE_MAPDATA);
				*/
				// JPanel currentPanel = (JPanel)mapTabs.getComponent(i);
				TabOrganizer inf = componentVec.get(i);
				// currentPanel.removeAll();
				mapTabs.setComponentAt(i, buildTabContents(inf));
				// currentPanel.revalidate();
			}
			mapTabs.repaint();
			break;
		}
		}
	}

	/**
	 * Attempt to close the currently open map
	 *
	 * @return true if tab was successfully closed
	 */
	private boolean closeCurrentTab() {
		if (mapTabs.getComponentCount() > 0) {
			TabOrganizer tab = componentVec.get(mapTabs.getSelectedIndex());
			if (tab.isModified()) {
				int r = tab.promptSave();
				switch (r) {
				case JOptionPane.YES_OPTION:
					tab.save();
				case JOptionPane.NO_OPTION:
					break;
				case JOptionPane.CANCEL_OPTION:
				case JOptionPane.CLOSED_OPTION:
					return false;
				}
			}
			tab.free();
			scriptTabs.remove(mapTabs.getSelectedIndex());
			componentVec.remove(tab);
			mapTabs.remove(mapTabs.getSelectedIndex());
			if (showTileWindow && mapTabs.getSelectedIndex() == -1) {
				tilesetWindow.setContentPane(new JPanel());
				tilesetWindow.validate();
			}
			if (showEntityWindow && mapTabs.getSelectedIndex() == -1) {
				entityWindow.setContentPane(new JPanel());
				entityWindow.validate();
			}
		}
		return true;
	}

	/**
	 * @param d
	 *            - the mapdata of the tab to remove. Used to get title.
	 * @return success of removal
	 */
	private boolean removeTab(Mapdata d) {
		for (int i = 0; i < mapTabs.getComponentCount(); i++) {
			String title = mapTabs.getTitleAt(i);
			String titlestar = title.substring(0, title.length() - 1);
			if (title.equals(d.getFile()) || title.equals(d.getMapname()) || titlestar.equals(d.getFile())
					|| titlestar.equals(d.getMapname())) {
				TabOrganizer tab = componentVec.get(i);
				if (tab.isModified()) {
					int r = JOptionPane.showConfirmDialog(this, Messages.getString("EditorApp.158") + //$NON-NLS-1$
							Messages.getString("EditorApp.159"), Messages.getString("EditorApp.160"),
							JOptionPane.YES_NO_CANCEL_OPTION); // $NON-NLS-1$ //$NON-NLS-2$
					switch (r) {
					case JOptionPane.YES_OPTION:
						tab.save();
					case JOptionPane.NO_OPTION:
						break;
					case JOptionPane.CANCEL_OPTION:
						return false;
					}
				}
				tab.free();
				mapTabs.remove(i);
				componentVec.remove(tab);
				scriptTabs.remove(i);
				break;
			}
		}
		return true;
	}

	public void dockOrUndockTileset() {
		showTileWindow = !showTileWindow;
		switchPerspective(activePerspective);
	}

	public void dockOrUndockEntityList() {
		showEntityWindow = !showEntityWindow;
		switchPerspective(activePerspective);
	}

	private Component buildTabContents(TabOrganizer inf) {
		switch (activePerspective) {
		case PERSPECTIVE_TILE:
			MapPane mapPanel = inf.map;
			JScrollPane mapScroll = new JScrollPane(mapPanel);
			mapScroll.addMouseWheelListener(new ScrollZoomAdapter(this, mapPanel));
			mapScroll.getVerticalScrollBar().setUnitIncrement(10);
			JScrollPane tileScroll = new JScrollPane(mapPanel.getTilePane());
			tileScroll.getVerticalScrollBar().setUnitIncrement(5);
			if (showTileWindow) {
				// only if this is the active tab
				if (componentVec.indexOf(inf) == mapTabs.getSelectedIndex()) {
					JSplitPane tileSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel.getPreviewPane(),
							tileScroll);
					tileSplit.setDividerLocation(100);
					// System.out.println(mapPanel);
					tilesetWindow.setContentPane(tileSplit);
					tilesetWindow.validate();
				}
				return mapScroll;
			} else {
				JSplitPane tileSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tileScroll,
						mapPanel.getPreviewPane());
				tileSplit.setDividerLocation(350);
				JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tileSplit, mapScroll);
				mainSplit.setDividerLocation(100);
				return mainSplit;
			} // if not showing helper window
		case PERSPECTIVE_ENTITY:
			if (showEntityWindow) {
				// only if this is the active tab
				mapScroll = new JScrollPane(inf.getEntity());
				mapScroll.getVerticalScrollBar().setUnitIncrement(10);
				if (componentVec.indexOf(inf) == mapTabs.getSelectedIndex()) {
					JScrollPane listScroll = new JScrollPane(inf.getEntity().getEntityList());
					JScrollPane editScroll = new JScrollPane(inf.getEntity().getEditPane());
					JSplitPane windowSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editScroll, listScroll);
					windowSplit.setDividerLocation(120);
					entityWindow.setContentPane(windowSplit);
					entityWindow.validate();
				}
				return mapScroll;
			} else {
				JScrollPane editScroll = new JScrollPane(inf.getEntity().getEditPane());
				editScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				mapScroll = new JScrollPane(inf.getEntity());
				mapScroll.getVerticalScrollBar().setUnitIncrement(10);
				JScrollPane listScroll = new JScrollPane(inf.getEntity().getEntityList());

				JPanel jp = new JPanel(new BorderLayout());
				@SuppressWarnings("serial")
				JButton entityDockButton = new JButton(new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						dockOrUndockEntityList();
					}
				});
				entityDockButton.setText("Undock");
				jp.add(entityDockButton, BorderLayout.PAGE_START);
				jp.add(listScroll, BorderLayout.CENTER);

				JSplitPane miniSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editScroll, jp);
				miniSplit.setDividerLocation(120);
				JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, miniSplit, mapScroll);
				split.setDividerLocation(160);
				return split;
			}
		case PERSPECTIVE_TSC:
			return inf.tscBuilder;
		case PERSPECTIVE_MAPDATA:
			return inf.mapdata;
		default:
			// bad action
			return new JLabel(Messages.getString("EditorApp.161")); //$NON-NLS-1$

		}
	}

	private void refreshCurrentMap() {
		if (componentVec.size() > 0 && mapTabs.getSelectedIndex() >= 0) {
			TabOrganizer inf = componentVec.get(mapTabs.getSelectedIndex());
			if (inf != null) {
				inf.getMap().repaint();
			}
			// maybe not needed...?
			// inf.getEntity().redraw();
			mapTabs.repaint();
		}
	}

	public TscPane getSelectedScript() {
		if (scriptTabs.getComponentCount() > 0) {
			JScrollPane jsp = (JScrollPane) scriptTabs.getSelectedComponent();
			JViewport jvp = jsp.getViewport();
			return (TscPane) jvp.getComponent(0);
		} else {
			return null;
		}
	}

	private class PerspectiveAction extends AbstractAction {
		private static final long serialVersionUID = 8203245293658208169L;
		String perspective = PERSPECTIVE_TILE;

		PerspectiveAction(String p) {
			if (p != null) {
				perspective = p;
			}
		}

		@Override
		public void actionPerformed(ActionEvent eve) {
			airhorn();
			switchPerspective(perspective);
		}
	}

	private class ScriptSearchAction extends AbstractAction {
		private static final long serialVersionUID = 9174867054600985368L;

		@Override
		public void actionPerformed(ActionEvent eve) {
			airhorn();
			TscPane selectedText = getSelectedScript();
			if (selectedText != null) {
				String searchTxt = tscSearch.getText();
				StrTools.findAndSelectTextInTextComponent(selectedText, searchTxt, true, false, true);
			}
		}
	}

	public Vector<EntityData> getEntityList() {
		String category = categoryList.getSelectedValue();
		String subcat = subcatList.getSelectedValue();
		return exeData.getEntityList(category, subcat);
	}

	private Vector<TscPane> getOpenScripts() {
		Vector<TscPane> rVal = new Vector<>();
		for (TabOrganizer t : componentVec) {
			rVal.add(t.TSC);
		}
		return rVal;
	}

	public void updateOpenScripts(Vector<Integer> oldNums, Vector<Integer> newNums) {
		Vector<TscPane> paneVec = getOpenScripts();
		for (TscPane pane : paneVec) {
			String contents = pane.getText();
			boolean modified = false;
			for (int i = 0; i < oldNums.size(); i++) {
				String oldTRA = String.format("<TRA%04d", oldNums.get(i)); //$NON-NLS-1$
				String newTRA = String.format("<TRA<<%04d", newNums.get(i)); //$NON-NLS-1$
				if (contents.contains(oldTRA)) {
					modified = true;
					contents = contents.replaceAll(oldTRA, newTRA);
				}
			} // for each mapnum
			if (modified) {
				for (int i = 0; i < oldNums.size(); i++) {
					String oldTRA = String.format("<TRA<<%04d", newNums.get(i)); //$NON-NLS-1$
					String newTRA = String.format("<TRA%04d", newNums.get(i)); //$NON-NLS-1$
					if (contents.contains(oldTRA)) {
						contents = contents.replaceAll(oldTRA, newTRA);
					}
				} // for each mapnum
				pane.setText(contents);
			}
		} // for each pane
	}

	public static void airhorn() {
		if (blazed) {
			BlSound.playSample(EditorApp.class.getResource("rsrc/horn.wav"));
		}
	}

	public class LoadMapAction {
		public void loadMap(int mapNum) {
			addMapTab(mapNum);
		}
	}

	public static void main(String[] args) {

		// parse args
		for (String s : args) {
			if (s.startsWith("MODE=")) {
				String val = s.split("=", 2)[1];
				try {
					EDITOR_MODE = Integer.parseInt(val);
				} catch (NumberFormatException ignored) {

				}
			}
		}

		// https://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and
		// initialize logging to go to rolling log file
		// noinspection PointlessBooleanExpression,ConstantConditions
		if (!disable_logging) {
			LogManager logManager = LogManager.getLogManager();
			logManager.reset();

			// log file max size 10K, 3 rolling files, append-on-open
			Handler fileHandler;
			try {
				fileHandler = new FileHandler("log", 100000, 3, true); //$NON-NLS-1$
				fileHandler.setFormatter(new SimpleFormatter());
				Logger.getLogger("").addHandler(fileHandler); //$NON-NLS-1$
				// now rebind stdout/stderr to logger
				LoggingOutputStream los;

				logger = Logger.getLogger("stdout"); //$NON-NLS-1$
				los = new LoggingOutputStream(logger, StdOutErrLevel.STDOUT);
				System.setOut(new PrintStream(los, true));

				logger = Logger.getLogger("stderr"); //$NON-NLS-1$
				los = new LoggingOutputStream(logger, StdOutErrLevel.STDERR);
				System.setErr(new PrintStream(los, true));
			} catch (SecurityException | IOException e) {
				e.printStackTrace();
			}
		}
		// Use the event dispatch thread to build the UI for thread-safety.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new EditorApp();
			}
		});
	}
}
