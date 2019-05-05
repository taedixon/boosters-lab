package ca.noxid.lab.tile;

import ca.noxid.lab.BlConfig;
import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.mapdata.MapInfo;
import ca.noxid.lab.mapdata.MapInfo.PxeEntry;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.lab.script.TscPane;
import ca.noxid.lab.tile.MapPoly.xPoint;
import ca.noxid.uiComponents.BgList;
import ca.noxid.uiComponents.BgPanel;
import ca.noxid.uiComponents.DragScrollAdapter;
import ca.noxid.uiComponents.FormattedUpdateTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Queue;


public class MapPane extends BgPanel {

	private static final long serialVersionUID = 0x1337FFFF34972L;

	//byte[] pxaArray;
	//private LinkedList<LineSeg> selectedNodes = new LinkedList<LineSeg>();

	private static TileBuffer tilePen; //holds an array of tiles to be written in draw/fill/rectangle mode

	public TileBuffer getPen() {
		return tilePen;
	}

	protected MapInfo dataHolder;

	protected ResourceManager iMan;
	///GameInfo exeData;

	private Rectangle cursorLoc;

	private ShiftDialog shiftDialog;
	private ResizeDialog resizeDialog;

	protected EditorApp parent;
	public TilesetPane tilePane;
	protected PreviewPane preview;
	private LinetypePane linePane;

	private MouseAdapter lineAdapter = new LineMouseAdapter();
	private PolyMouseAdapter polyAdapter = new PolyMouseAdapter();
	
	private int activeLayer;
	private JList<TileLayer> layerSelect;
	private JPanel layerPanel;
	protected boolean isSoloLayerView = false;
	protected boolean isFadeUnfocusedLayers = true;

	public JPanel getTilePane() {
		//TODO: re-implement logic for displaying the line pane instead of the tile pane
//		if (activeLayer != EditorApp.PHYSICAL_LAYER) {
//			return tilePane;
//		} else {
//			return linePane;
//		}
		return tilePane;
	}

	public PreviewPane getPreviewPane() {
		return preview;
	}

	public Component getLayerPane() {
		return layerPanel;
	}

	protected JPopupMenu popup;
	private JMenuItem popup_tilescript;
	private JMenuItem popup_tra;
	protected MapUndo undo = new MapUndo();
	protected MapRedo redo = new MapRedo();


	public String toString() {
		return dataHolder.toString() + " :: mapPane"; //$NON-NLS-1$
	}

	protected MapPane(EditorApp p) {
		super(p.getImageManager().getImg(ResourceManager.rsrcBgBrown)); //$NON-NLS-1$
		//implicit superconstructor for entityPane
		this.setFocusable(true);
	}

	public MapPane(EditorApp p, MapInfo data) {
		super(p.getImageManager().getImg(ResourceManager.rsrcBgBrown)); //$NON-NLS-1$
		dataHolder = data;
		parent = p;
		iMan = p.getImageManager();
		//exeData = p.getGameInfo();
		//File currentFile = null;
		initMouse();
		initLayerSelect();

		//create the tileset panel and preview
		tilePane = new TilesetPane(this, iMan, dataHolder);
		linePane = new LinetypePane(iMan.getImg(ResourceManager.rsrcBgWhite));
		preview = new PreviewPane(iMan, this, dataHolder);

		this.setPreferredSize(new Dimension(
				dataHolder.getMapX() * dataHolder.getConfig().getTileSize(),
				dataHolder.getMapY() * dataHolder.getConfig().getTileSize()));

		//setup the pen
		if (tilePen == null) {
			tilePen = new TileBuffer();
			tilePen.data = new int[1][1];
			tilePen.data[0][0] = 0;
		}

		//first time draw
		//mapCanvas = new BufferedImage(mapX * dataHolder.getConfig().getTileSize(), mapY * dataHolder.getConfig().getTileSize(), BufferedImage.TYPE_INT_ARGB_PRE);
		//fillCanvas((Graphics2D)mapCanvas.getGraphics());

		//setup the undo/redo menu
		this.buildPopup();
		resizeDialog = new ResizeDialog(parent, dataHolder.getMapX(), dataHolder.getMapY());
		shiftDialog = new ShiftDialog(parent);
		this.setFocusable(true);
	}

	protected void buildPopup() {
		popup = new JPopupMenu();
		JMenuItem undoItem;
		undoItem = new JMenuItem(undo);
		undoItem.setText(Messages.getString("MapPane.1")); //$NON-NLS-1$
		undoItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		JMenuItem redoItem;
		redoItem = new JMenuItem(redo);
		redoItem.setText(Messages.getString("MapPane.2")); //$NON-NLS-1$
		redoItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
		JMenuItem secret_undoItem;
		secret_undoItem = new JMenuItem(undo);
		secret_undoItem.setText(Messages.getString("MapPane.1")); //$NON-NLS-1$
		secret_undoItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		JMenuItem secret_redoItem;
		secret_redoItem = new JMenuItem(redo);
		secret_redoItem.setText(Messages.getString("MapPane.2")); //$NON-NLS-1$
		secret_redoItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
		@SuppressWarnings("serial")
		JMenuItem shiftItem = new JMenuItem(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent eve) {
				shiftDialog.setVisible(true);
				shiftDialog.init();
				shiftDialog.setTitle("shift tiles");
				if (shiftDialog.accepted()) {
					dataHolder.shiftMap(shiftDialog.getShiftX(),
							shiftDialog.getShiftY(),
							shiftDialog.getOptions());
				}

				MapPane.this.repaint();
			}

		});
		shiftItem.setText(Messages.getString("MapPane.5")); //$NON-NLS-1$
		@SuppressWarnings("serial")
		JMenuItem resizeItem = new JMenuItem(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent eve) {
				resizeDialog.init(dataHolder.getMapX(), dataHolder.getMapY());
				resizeDialog.setVisible(true);
				if (resizeDialog.accepted()) {
					dataHolder.resizeMap(resizeDialog.getSizeX(), resizeDialog.getSizeY());
					resizeMap();
				}
				//resizeDialog.setVisible(false);

				redrawTiles(0, 0, dataHolder.getMapX(), dataHolder.getMapY());
				repaint();
			}

		});
		resizeItem.setText(Messages.getString("MapPane.6")); //$NON-NLS-1$

		JPopupMenu fakePopup = new JPopupMenu();
		fakePopup.add(secret_undoItem);
		fakePopup.add(secret_redoItem);
		popup.add(undoItem);
		popup.add(redoItem);
		popup.add(shiftItem);
		popup.add(resizeItem);
		popup_tilescript = new JMenuItem(Messages.getString("MapPane.8")); //$NON-NLS-1$
		popup.add(popup_tilescript);
		popup_tra = new JMenuItem(Messages.getString("MapPane.9")); //$NON-NLS-1$
		popup.add(popup_tra);
		this.add(fakePopup);
		this.add(popup);
	}

	protected void initLayerSelect() {
		layerSelect = new BgList<TileLayer>(iMan.getImg(ResourceManager.rsrcBgWhite2));
		layerSelect.setListData(dataHolder.getMap().toArray(new TileLayer[dataHolder.getMap().size()]));
		layerSelect.setCellRenderer(new TileListRender());
		LayerListListener lll = new LayerListListener();
		layerSelect.addListSelectionListener(lll);
		layerSelect.addKeyListener(lll);
		layerSelect.setSelectedIndex(activeLayer);
		//create layer control panel
		JPanel layerControl = new BgPanel(iMan.getImg(ResourceManager.rsrcBgBlue));
		JTextField txtfield;
		layerControl.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
//		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		JCheckBox check;
		check = new JCheckBox(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
				isSoloLayerView = ((JCheckBox)e.getSource()).isSelected();
			}
		});
		check.setText("Solo layer mode");
		check.setOpaque(false);
		layerControl.add(check, gbc);
		gbc.gridy++;
		check = new JCheckBox(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
				isFadeUnfocusedLayers = ((JCheckBox)e.getSource()).isSelected();
			}
		});
		check.setText("Fade unselected layers");
		check.setOpaque(false);
		check.setSelected(true);
		layerControl.add(check, gbc);
		gbc.gridy++;
		//list control buttons
		JButton button;
		JPanel buttonStrip = new JPanel();
		ActionListener buttonListener = new LayerListActionListener();
		buttonStrip.setLayout(new BoxLayout(buttonStrip, BoxLayout.X_AXIS));
		Icon[] btnIcons = {
				new ImageIcon(iMan.getImg(ResourceManager.rsrcIcAddLayer)),
				new ImageIcon(iMan.getImg(ResourceManager.rsrcIcDelLayer)),
				new ImageIcon(iMan.getImg(ResourceManager.rsrcIcCopyLayer)),
				new ImageIcon(iMan.getImg(ResourceManager.rsrcIcLayerUp)),
				new ImageIcon(iMan.getImg(ResourceManager.rsrcIcLayerDown)),
				new ImageIcon(iMan.getImg(ResourceManager.rsrcIcMergeLayer)),
				new ImageIcon(iMan.getImg(ResourceManager.rsrcIcLayerProp)),
		};
		String[] btnCommands = {
				"add",
		        "delete",
		        "copy",
		        "move_up",
		        "move_down",
		        "merge",
		        "property",
		};
		String[] btnTooltips = {
				"Add Layer",
		        "Delete Layer",
		        "Copy Layer",
		        "Move Layer Up",
		        "Move Layer Down",
		        "Merge Layer Down",
		        "Edit Properties",
		};
		for (int i = 0; i < btnCommands.length; i++) {
			button = new JButton();
			button.addActionListener(buttonListener);
			button.setActionCommand(btnCommands[i]);
			button.setIcon(btnIcons[i]);
			button.setToolTipText(btnTooltips[i]);
			button.setBorder(new EmptyBorder(4,4,4,4));
			buttonStrip.add(button);
		}
		layerControl.add(buttonStrip, gbc);
		gbc.gridy++;


		//add them all into one container for passing out to the main application
		layerPanel = new JPanel();
		layerPanel.setLayout(new BorderLayout());
		JScrollPane jsp = new JScrollPane(layerSelect);
		layerPanel.add(layerControl, BorderLayout.NORTH);
		layerPanel.add(jsp, BorderLayout.CENTER);
	}

	protected void resizeMap() {
		this.repaint();
	}

	protected void initMouse() {
		MapMouseAdapter mma = new MapMouseAdapter();
		DragScrollAdapter dsa = new DragScrollAdapter();
		PointDragAdapter pda = new PointDragAdapter();
		this.addMouseListener(mma);
		this.addMouseMotionListener(mma);
		this.addMouseListener(dsa);
		this.addMouseMotionListener(dsa);
		this.addMouseListener(pda);
		this.addMouseMotionListener(pda);

		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent eve) {
				List<LineSeg> ll;
				switch (eve.getExtendedKeyCode()) {
				case KeyEvent.VK_DELETE:
					ll = dataHolder.getLines();
					ListIterator<LineSeg> it = ll.listIterator();
					while (it.hasNext()) {
						if (it.next().isSelected()) {
							it.remove();
						}
					}
					LinkedList<MapPoly> expired = new LinkedList<>();
					for (MapPoly p : dataHolder.getPolys()) {
						p.remove(p.getSelected());
						if (p.getPoints().size() <= 0) {
							expired.add(p);
						}
					}
					dataHolder.getPolys().removeAll(expired);
					MapPane.this.repaint();
					TileBuffer pen = getPen();
					if (pen != null) {
						pen.toggleEraser();
						preview.repaint();
					}
					break;
				case KeyEvent.VK_H:
					ll = dataHolder.getLines();
					for (LineSeg seg : ll) {
						if (seg.isSelected()) {
							seg.setLine(seg.getP2(), seg.getP1());
						}
					}
					for (MapPoly p : dataHolder.getPolys()) {
						if (p.getSelected().size() > 0) {
							p.flip();
						}
					}
					repaint();
					break;
				case KeyEvent.VK_A:
					if (dataHolder.getSelectedNodes().size() == dataHolder.getLines().size()) {
						for (LineSeg s : dataHolder.getLines()) {
							s.setSelection(LineSeg.NONE, false);
						}
					} else {
						for (LineSeg s : dataHolder.getLines()) {
							s.setSelection(LineSeg.LINE, false);
						}
					}
					repaint();
					break;
				case KeyEvent.VK_I:
					for (MapPoly p : dataHolder.getPolys()) {
						for (xPoint xp : p.getSelected()) {
							p.insert(xp);
						}
					}
					repaint();
					break;
				case KeyEvent.VK_MINUS:
				case KeyEvent.VK_SUBTRACT:
					if (activeLayer > 0) {
						activeLayer--;
						layerSelect.setSelectedIndex(activeLayer);
					}
					break;
				case KeyEvent.VK_EQUALS:
				case KeyEvent.VK_ADD:
					if (activeLayer < dataHolder.getMap().size()-1) {
						activeLayer++;
						layerSelect.setSelectedIndex(activeLayer);
					}
					break;
				}
			}
		});
	}

	@Override
	public void paintComponent(Graphics g) {
		int tileScale = (int) (EditorApp.mapScale * dataHolder.getConfig().getTileSize());
		this.setPreferredSize(new Dimension(
				dataHolder.getMapX() * tileScale,
				dataHolder.getMapY() * tileScale));
		this.revalidate();
		Graphics2D g2d = (Graphics2D) g.create();
		if ((dataHolder.getMapX() * dataHolder.getMapY()) < 25000)
		//this can  be a costly operation for extremely large maps
		{
			super.paintComponent(g2d);
		}
		//draw corner decal
		BufferedImage corner = iMan.getImg(ResourceManager.rsrcCorner); //$NON-NLS-1$
		Dimension d = this.getSize();
		g2d.clipRect(tileScale / 2, tileScale / 2, tileScale * (dataHolder.getMapX() - 1),
				tileScale * (dataHolder.getMapY() - 1));
		g2d.drawImage(corner, d.width - corner.getWidth(), d.height - corner.getHeight(), null);
		fillCanvas(g2d);
	}

	protected void fillCanvas(Graphics2D g2d) {
		int scale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
		g2d.setBackground(Color.black);
		drawBackground((Graphics2D) g2d.create());
		List<TileLayer> map = dataHolder.getMap();
		for (int i = 0; i < map.size(); i++) {
			TileLayer layer = map.get(i);
			if (isSoloLayerView && activeLayer != i) {
				continue;
			}
			Graphics2D layerGfx = (Graphics2D) g2d.create();
			if (isFadeUnfocusedLayers && activeLayer != i) {
				AlphaComposite composite = AlphaComposite.SrcOver.derive(0.5f);
				layerGfx.setComposite(composite);
			}
			layer.draw(layerGfx, EditorApp.mapScale);
			//draw tile types if applicable
			if (parent.getOtherDrawOptions()[0]
					&& layer.getType() == TileLayer.LAYER_TYPE.TILE_PHYSICAL) {
				drawTileTypes((Graphics2D) g2d.create(), layer);
			}
		}
		//draw entities if applicable
		drawEntities(g2d);
		//draw grid if applicable
		int mapX = dataHolder.getMapX();
		int mapY = dataHolder.getMapY();
		if (parent.getOtherDrawOptions()[1]) {
			Graphics2D g2 = (Graphics2D) g2d.create();
			g2.setColor(Color.black);
			g2.setXORMode(Color.white);
			for (int x = 0; x < mapX; x++) {
				g2.drawLine(x * scale, 0, x * scale, mapY * scale);
			}
			for (int y = 0; y < mapY; y++) {
				g2.drawLine(0, y * scale, mapX * scale, y * scale);
			}
		}
		//draw pink outline, just like daddy CE used to do 
		g2d.setColor(Color.MAGENTA);
		int buf = scale / 2;
		g2d.drawRect(buf, buf, (mapX - 1) * scale - 1, (mapY - 1) * scale - 1);
		drawCursor(g2d);
	}

	protected void drawEntities(Graphics2D g2d) {
		int drawflag = 0;
		Rectangle view = g2d.getClipBounds();
		if (parent.getOtherDrawOptions()[3]) drawflag |= PxeEntry.DRAW_SPRITE;
		if (parent.getOtherDrawOptions()[4]) drawflag |= PxeEntry.DRAW_NAME;
		if (parent.getOtherDrawOptions()[2]) drawflag |= PxeEntry.DRAW_BOX;
		//int nDraws = 0;
		if (drawflag != 0) {
			Iterator<PxeEntry> pxeIt = dataHolder.getPxeIterator();
			while (pxeIt.hasNext()) {
				PxeEntry cEntity = pxeIt.next();
				if (view.intersects(cEntity.getDrawArea())) {
					cEntity.draw(g2d, drawflag);
					//nDraws++;
				}
			}
		}
		//System.out.println("Redraw " + nDraws + " entity");
	}


	protected void drawTileTypes(Graphics2D g, TileLayer layer) {
		int mapX = dataHolder.getMapX();
		int mapY = dataHolder.getMapY();

		int scale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
		//int tileScale = dataHolder.getConfig().getTileSize();
		//BufferedImage tileImg = iMan.getImg(tileset);
		BufferedImage legend = iMan.getImg(ResourceManager.rsrcTiles); //$NON-NLS-1$
		Rectangle r = g.getClipBounds();
		int startX = 0;
		int startY = 0;
		int endX = mapX;
		int endY = mapY;
		if (r != null) {
			//System.out.println(r);
			startX = r.x / scale;
			if (startX < 0) startX = 0;
			startY = r.y / scale;
			if (startY < 0) startY = 0;
			endX = startX + r.width / scale + 2;
			if (endX < 2) endX = 2;
			if (endX > mapX) endX = mapX;
			endY = startY + r.height / scale + 2;
			if (endY < 2) endY = 2;
			if (endY > mapY) endY = mapY;
		}
		for (int i = startY; i < endY; i++) {
			for (int j = startX; j < endX; j++) {
				int sourceX, sourceY;
				int xPixel = scale * j;
				int yPixel = scale * i;
				int tileType = dataHolder.calcPxa(layer.getTile(j, i));
				if (legend != null) {
					//if (tileType < 0) System.out.println("type-" + tileType + ", ");
					sourceX = (tileType % 0x10) * 16;
					sourceY = (tileType / 0x10) * 16;
					g.drawImage(legend,
							xPixel,
							yPixel,
							xPixel + scale,
							yPixel + scale,
							sourceX,
							sourceY,
							sourceX + 16,
							sourceY + 16,
							this);
				}
			}
		}
	}

	protected void drawBackground(Graphics2D g) {
		BufferedImage background = iMan.getImg(dataHolder.getBG());
		int scale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
		int mapX = dataHolder.getMapX();
		int mapY = dataHolder.getMapY();
		int bgw = (int) (background.getWidth() * EditorApp.mapScale);
		int bgh = (int) (background.getHeight() * EditorApp.mapScale);
		Rectangle r = g.getClipBounds();
		int startX = 0;
		int startY = 0;
		int endX = mapX * scale;
		int endY = mapY * scale;
		if (r != null) {
			startX = r.x - r.x % bgw;
			if (startX < 0) startX = 0;
			startY = r.y - r.y % bgh;
			if (startY < 0) startY = 0;
			endX = startX + r.width + bgw;
			if (endX < 2) endX = 2;
			endY = startY + r.height + bgh;
			if (endY < 2) endY = 2;
		}

		for (int y = startY; y < endY; y += bgh) {
			for (int x = startX; x < endX; x += bgw) {
				g.drawImage(background, x, y,
						x + bgw,
						y + bgh,
						0,
						0,
						background.getWidth(),
						background.getHeight(),
						this);
			}
		}

	}

	private void drawMapPhysical(Graphics2D g) {
		for (LineSeg l : dataHolder.getLines()) {
			l.draw(g);
		}
		for (MapPoly p : dataHolder.getPolys()) {
			p.draw(g);
		}
	}


	class TileBuffer {
		int[][] data;
		int dx;
		int dy;
		private boolean eraser = false;

		private void toggleEraser() {
			eraser = !eraser;
		}

		/**
		 * @param x x
		 * @param y y
		 * @return the data at this location
		 */
		public int get(int x, int y) {
			if (eraser) return 0;
			return (data[y][x]);
		}

		public int getH() {
			return data.length;
		}

		public int getW() {
			return data[0].length;
		}
	}

	public void updatePen(TileBuffer p) {
		tilePen = p;
		preview.repaint();
	}

	protected void drawPen(int x, int y, int maxX, int maxY) {
		int layer = activeLayer;
		for (int xOff = 0; xOff < tilePen.getW(); xOff++) {
			for (int yOff = 0; yOff < tilePen.getH(); yOff++) {
				int locX = x - tilePen.dx + xOff;
				if (locX >= maxX) {
					continue;
				}
				int locY = y - tilePen.dy + yOff;
				if (locY >= maxY) {
					continue;
				}
				try {
					dataHolder.putTile(locX, locY, tilePen.get(xOff, yOff), layer);
				} catch (IndexOutOfBoundsException e) {
					//ignore this
				}
			}
		}
	}

	protected Rectangle fillPen(Rectangle cursor) {
		int currentLayer = activeLayer;
		int width = dataHolder.getMapX();
		int height = dataHolder.getMapY();
		Queue<Point> q = new LinkedList<>();
		q.add(new Point(cursor.x, cursor.y));
		boolean[][] painted = new boolean[height][width];
		int[][] cursorSample = new int[cursor.height][cursor.width];
		for (int cy = 0; cy < cursor.height; cy++) {
			for (int cx = 0; cx < cursor.width; cx++) {
				cursorSample[cy][cx] = dataHolder.getTile(cursor.x + cx, cursor.y + cy, currentLayer);
			}
		}

		int trackMinX = cursor.x;
		int trackMinY = cursor.y;
		int trackMaxX = cursor.x;
		int trackMaxY = cursor.y;

		while (!q.isEmpty()) {
			Point p = q.remove();
			if ((p.x >= 0) && (p.x < width) &&
					(p.y >= 0) && (p.y < height)) {

				int difX = p.x - cursor.x;
				int difY = p.y - cursor.y;

				int targetX = (difX % cursor.width) + cursor.width;
				int targetY = (difY % cursor.height) + cursor.height;

				int targetType = cursorSample[targetY % cursor.height][targetX % cursor.width];

				if (!painted[p.y][p.x] &&
						(dataHolder.getTile(p.x, p.y, currentLayer) == targetType)) {
					painted[p.y][p.x] = true;
					System.out.println(String.format("%s calculated difference: (%d, %d)", p.toString(), difX, difY));

					if (p.x < trackMinX) {
						trackMinX = p.x;
					}
					if (p.x > trackMaxX) {
						trackMaxX = p.x;
					}
					if (p.y < trackMinY) {
						trackMinY = p.y;
					}
					if (p.y > trackMaxY) {
						trackMaxY = p.y;
					}
					difX = ((difX - tilePen.dx) % tilePen.getW()) + tilePen.getW();
					difY = ((difY - tilePen.dy) % tilePen.getH()) + tilePen.getH();
					int blockTile = tilePen.get(difX % tilePen.getW(), difY % tilePen.getH());
					dataHolder.putTile(p.x, p.y, blockTile, activeLayer);

					q.add(new Point(p.x + 1, p.y));
					q.add(new Point(p.x - 1, p.y));
					q.add(new Point(p.x, p.y + 1));
					q.add(new Point(p.x, p.y - 1));
				}
			}
		}
		Rectangle affectedArea = new Rectangle();
		affectedArea.x = trackMinX;
		affectedArea.y = trackMinY;
		affectedArea.width = trackMaxX - trackMinX + 1;
		affectedArea.height = trackMaxY - trackMinY + 1;
		return affectedArea;
	}

	/**
	 * Contrary to standards, this returns a rect of L U R D
	 *
	 */
	public Rectangle replacePen(Rectangle cursor) {
		int currentLayer = activeLayer;
		int[][] cursorSample = new int[cursor.height][cursor.width];
		for (int cy = 0; cy < cursor.height; cy++) {
			for (int cx = 0; cx < cursor.width; cx++) {
				cursorSample[cy][cx] = dataHolder.getTile(cursor.x + cx, cursor.y + cy, currentLayer);
			}
		}
		Rectangle retVal = new Rectangle(0, 0, dataHolder.getMapX() - 1, dataHolder.getMapY() - 1);
		for (int tX = 0; tX < dataHolder.getMapX(); tX++) {
			for (int tY = 0; tY < dataHolder.getMapY(); tY++) {
				int difX = tX - cursor.x;
				int difY = tY - cursor.y;

				int targetX = (difX % cursor.width) + cursor.width;
				int targetY = (difY % cursor.height) + cursor.height;

				int targetType = cursorSample[targetY % cursor.height][targetX % cursor.width];

				if (dataHolder.getTile(tX, tY, activeLayer) == targetType) {
					//replace
					difX = ((difX - tilePen.dx) % tilePen.getW()) + tilePen.getW();
					difY = ((difY - tilePen.dy) % tilePen.getH()) + tilePen.getH();
					dataHolder.putTile(tX, tY,
							tilePen.get(difX % tilePen.getW(), difY % tilePen.getH()),
							activeLayer);
				}
			}
		}
		return retVal;
	}

	class MapMouseAdapter extends MouseAdapter {
		int lastX = -1;
		int lastY = -1;
		int baseX = -1;
		int baseY = -1;
		int selW = -1;
		int selH = -1;
		int[][] prevLayerState;
		boolean dragging = false;

		@Override
		public void mouseEntered(MouseEvent eve) {
			requestFocus();
		}

		@Override
		public void mouseExited(MouseEvent eve) {
			//TODO do nothing if we are editing the line layer
//			if (dataHolder.getMap().get(activeLayer) instanceof LineLayer) {
//				return;
//			}
			//int viewScale = (int) (EditorApp.mapScale * dataHolder.getConfig().getTileSize());
			//int tileSize = dataHolder.getConfig().getTileSize();
			//MapPane pane = (MapPane) eve.getSource();
			//Graphics2D g2d = (Graphics2D) pane.getGraphics();
			switch (parent.getDrawMode()) {

			case EditorApp.DRAWMODE_DRAW:
			case EditorApp.DRAWMODE_FILL:
			case EditorApp.DRAWMODE_REPLACE:
				((MapPane) eve.getSource()).moveCursor(null);
				break;
			case EditorApp.DRAWMODE_RECT:
			case EditorApp.DRAWMODE_COPY:
				//only if mouse isn't pressed
				if ((eve.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
					((MapPane) eve.getSource()).moveCursor(null);
				}
				break;
			default:
				break;
			}
		}

		@Override
		public void mousePressed(MouseEvent eve) {
			int mapX = dataHolder.getMapX();
			int mapY = dataHolder.getMapY();
			Point mousePoint = eve.getPoint();
			int viewScale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
			int currentX = mousePoint.x / viewScale;
			int currentY = mousePoint.y / viewScale;
			//TODO do nothing if we are editing the line layer
//			if (activeLayer == EditorApp.PHYSICAL_LAYER) {
//				return;
//			}
			if (eve.isPopupTrigger()) {
				//this needs to be copied to isrelease
				popup_tilescript.setAction(new TilescriptAction(currentX,
						currentY, tilePen.get(0, 0), activeLayer));
				popup_tilescript.setText(Messages.getString(
						"MapPane.11") + mapX + "," + mapY + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				popup_tra.setAction(new TraScriptAction(currentX,
						currentY, dataHolder.getMapNumber()));
				popup_tra.setText(Messages.getString("MapPane.14")); //$NON-NLS-1$
				popup.show(eve.getComponent(), eve.getX(), eve.getY());
				return;
			}
			if ((eve.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
				//middle click
				//sample a tile
				tilePen.data = new int[1][1];
				tilePen.data[0][0] = dataHolder.getTile(currentX, currentY, activeLayer);
				tilePen.dx = 0;
				tilePen.dy = 0;
				preview.repaint();
				return;
			}
			if ((eve.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
				return; //do nothing if right mouse
			}
			if ((eve.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
				return; //do nothing if dragging
			} else {
				dragging = false;
			}

			prevLayerState = new int[mapY][mapX];
			for (int x = 0; x < mapX; x++) {
				for (int y = 0; y < mapY; y++) {
					prevLayerState[y][x] = dataHolder.getTile(x, y, activeLayer);
				}
			}
			
			
			/* http://www.cavestory.org/forums/index.php?/topic/3893-boosters-lab-beta-bitches/page-30#entry173320
			if (currentX >= mapX)
				currentX = mapX-1;
			if (currentY >= mapY)
				currentY = mapY-1;
				*/
			if (currentX >= mapX || currentY >= mapY) return;
			//MapPane pane = (MapPane)eve.getSource();
			//Graphics2D g2d = (Graphics2D)pane.getGraphics();
			switch (parent.getDrawMode()) {
			case EditorApp.DRAWMODE_DRAW:
				//Rectangle oldCursorRect = new Rectangle(lastX - tilePen.dx, lastY - tilePen.dy, tilePen.getW(), tilePen.getH());
				Rectangle newCursorRect = new Rectangle(currentX - tilePen.dx, currentY - tilePen.dy, tilePen.getW(),
						tilePen.getH());
				drawPen(currentX, currentY, currentX + tilePen.getW(), currentY + tilePen.getH());
				redrawTiles(currentX - tilePen.dx,
						currentY - tilePen.dy,
						tilePen.getW(),
						tilePen.getH());
				moveCursor(newCursorRect);
				selW = currentX + tilePen.getW();
				selH = currentY + tilePen.getH();
				break;
			case EditorApp.DRAWMODE_RECT:
			case EditorApp.DRAWMODE_COPY:
			case EditorApp.DRAWMODE_FILL:
			case EditorApp.DRAWMODE_REPLACE:
				selW = 1;
				selH = 1;
				break;
			default:
				break;
			}
			lastX = currentX;
			lastY = currentY;
			baseX = currentX;
			baseY = currentY;
		}

		@Override
		public void mouseReleased(MouseEvent eve) {
			int mapX = dataHolder.getMapX();
			int mapY = dataHolder.getMapY();
			Point mousePoint = eve.getPoint();
			int viewScale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
			int cursorX, cursorY;

			//TODO do nothing if we are editing the line layer
//			if (activeLayer == EditorApp.PHYSICAL_LAYER) {
//				return;
//			}
			if (eve.isPopupTrigger()) {
				//this needs to be copied to isRelease
				cursorX = mousePoint.x / viewScale;
				cursorY = mousePoint.y / viewScale;
				popup_tilescript.setAction(
						new TilescriptAction(cursorX, cursorY, tilePen.get(0, 0),
								activeLayer));
				popup_tilescript.setText(Messages.getString(
						"MapPane.11") + cursorX + "," + cursorY + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				popup_tra.setAction(new TraScriptAction(cursorX, cursorY, dataHolder.getMapNumber()));
				popup_tra.setText(Messages.getString("MapPane.14")); //$NON-NLS-1$
				popup.show(eve.getComponent(), eve.getX(), eve.getY());
				return;
			}
			if ((eve.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
				//middle click
				return;
			}
			if ((eve.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
				return; //do nothing if right mouse button
			}
			if (dragging) {
				//setCursor(null);
				return;
			}
			int[][] oldDat;
			int[][] newDat;
			Rectangle newCursorRect;
			int w, h;
			//Graphics2D g2d = (Graphics2D)((MapPane)eve.getSource()).getGraphics();
			switch (parent.getDrawMode()) {
			case EditorApp.DRAWMODE_RECT:
				if (selW < 0) {
					cursorX = baseX + selW + 1;
				} else {
					cursorX = baseX;
				}
				//absolutize to make calculations easy
				//(it no longer needs to be updated until next click)
				selW = Math.abs(selW);
				if (selH < 0) {
					cursorY = baseY + selH + 1;
				} else {
					cursorY = baseY;
				}
				//see above
				selH = Math.abs(selH);
				int tmpx = tilePen.dx;
				int tmpY = tilePen.dy;
				tilePen.dx = 0;
				tilePen.dy = 0;
				//capture the previous state
				oldDat = new int[selH][selW];
				for (int dx = 0; dx < selW; dx++) {
					for (int dy = 0; dy < selH; dy++) {
						oldDat[dy][dx] = dataHolder.getTile(cursorX + dx, cursorY + dy, activeLayer);
					}
				}
				//draw over it
				for (int dx = 0; dx < selW; dx += tilePen.getW()) {
					for (int dy = 0; dy < selH; dy += tilePen.getH()) {
						drawPen(cursorX + dx, cursorY + dy, cursorX + selW, cursorY + selH);
					}
				}
				//capture the new state
				newDat = new int[selH][selW];
				for (int dx = 0; dx < selW; dx++) {
					for (int dy = 0; dy < selH; dy++) {
						newDat[dy][dx] = dataHolder.getTile(cursorX + dx, cursorY + dy, activeLayer);
					}
				}
				redrawTiles(cursorX, cursorY, selW, selH);
				tilePen.dx = tmpx;
				tilePen.dy = tmpY;
				selW = 1;
				selH = 1;
				dataHolder.addEdit(dataHolder.new MapEdit(cursorX, cursorY, oldDat, newDat, activeLayer));
				break;
			case EditorApp.DRAWMODE_COPY:
				if (selW < 0) {
					cursorX = baseX + selW + 1;
				} else {
					cursorX = baseX;
				}
				//absolutize to make calculations easy
				//(it no longer needs to be updated until next click)
				selW = Math.abs(selW);
				if (selH < 0) {
					cursorY = baseY + selH + 1;
				} else {
					cursorY = baseY;
				}
				//see above
				selH = Math.abs(selH);

				//create a pen
				tilePen = new TileBuffer();
				tilePen.dx = baseX - cursorX;
				tilePen.dy = baseY - cursorY;
				tilePen.data = new int[selH][selW];
				for (int x = 0; x < selW; x++) {
					for (int y = 0; y < selH; y++) {
						tilePen.data[y][x] = dataHolder.getTile(cursorX + x, cursorY + y, activeLayer);
					}
				}
				redrawTiles(cursorX, cursorY, selW, selH);
				preview.repaint();
				selW = 1;
				selH = 1;
				break;
			case EditorApp.DRAWMODE_FILL:
				int currentX = mousePoint.x / viewScale;
				int currentY = mousePoint.y / viewScale;
				if (currentX >= mapX) {
					currentX = mapX - 1;
				}
				if (currentX < 0) {
					currentX = 0;
				}
				if (currentY >= mapY) {
					currentY = mapY - 1;
				}
				if (currentY < 0) {
					currentY = 0;
				}
				Rectangle affected = fillPen(cursorLoc);
				//System.out.println(tracker);
				redrawTiles(affected.x, affected.y,
						affected.x + affected.width,
						affected.height + affected.y);

				//oldCursorRect = new Rectangle(lastX - tilePen.dx, lastY - tilePen.dy, tilePen.getW(), tilePen.getH());
				newCursorRect = new Rectangle(currentX - tilePen.dx, currentY - tilePen.dy, tilePen.getW(),
						tilePen.getH());
				//put the cursor back
				moveCursor(newCursorRect);
				oldDat = new int[affected.height][affected.width];
				for (int dy = 0; dy < affected.height; dy++) {
					System.arraycopy(prevLayerState[affected.y + dy], affected.x, oldDat[dy], 0, affected.width);
				}
				//capture the new state
				newDat = new int[affected.height][affected.width];
				for (int dx = 0; dx < affected.width; dx++) {
					for (int dy = 0; dy < affected.height; dy++) {
						newDat[dy][dx] = dataHolder.getTile(affected.x + dx, affected.y + dy, activeLayer);
					}
				}
				//create the edit
				dataHolder.addEdit(dataHolder.new MapEdit(affected.x, affected.y,
						oldDat, newDat, activeLayer));
				break;
			case EditorApp.DRAWMODE_REPLACE:
				currentX = eve.getX() / viewScale;
				currentY = eve.getY() / viewScale;
				if (currentX >= mapX) {
					currentX = mapX - 1;
				}
				if (currentX < 0) {
					currentX = 0;
				}
				if (currentY >= mapY) {
					currentY = mapY - 1;
				}
				if (currentY < 0) {
					currentY = 0;
				}
				Rectangle r = replacePen(cursorLoc);
				r.width += 1;
				r.height += 1;
				redrawTiles(r.x, r.y, r.width, r.height);
				//put the cursor back
				//oldCursorRect = new Rectangle(lastX - tilePen.dx, lastY - tilePen.dy, tilePen.getW(), tilePen.getH());
				newCursorRect = new Rectangle(currentX - tilePen.dx, currentY - tilePen.dy, tilePen.getW(),
						tilePen.getH());
				moveCursor(newCursorRect);

				w = r.width - r.x;
				h = r.height - r.y;
				oldDat = new int[h][w];
				for (int dy = 0; dy < h; dy++) {
					System.arraycopy(prevLayerState[r.y + dy], r.x, oldDat[dy], 0, w);
				}
				//capture the new state
				newDat = new int[h][w];
				for (int dx = 0; dx < w; dx++) {
					for (int dy = 0; dy < h; dy++) {
						newDat[dy][dx] = dataHolder.getTile(r.x + dx, r.y + dy, activeLayer);
					}
				}
				//create the edit
				dataHolder.addEdit(dataHolder.new MapEdit(r.x, r.y, oldDat,
						newDat, activeLayer));
				break;
			case EditorApp.DRAWMODE_DRAW:
				//capture the previous state
				//if (selW > mapX)
				//	selW = mapX;
				//if (selH > mapY)
				//	selH = mapY;
				if (baseX < 0) {
					baseX = 0;
				}
				if (baseY < 0) {
					baseY = 0;
				}
				w = selW - baseX;
				h = selH - baseY;
				if (w < 1 || h < 1) {
					return;
				}
				oldDat = new int[h][w];
				for (int dx = 0; dx < w; dx++) {
					for (int dy = 0; dy < h; dy++) {
						try {
							oldDat[dy][dx] = prevLayerState
									[baseY + dy - tilePen.dy][baseX + dx - tilePen.dx];
						} catch (IndexOutOfBoundsException ignored) {
						}
					}
				}
				//capture the new state
				newDat = new int[h][w];
				for (int dx = 0; dx < w; dx++) {
					for (int dy = 0; dy < h; dy++) {
						newDat[dy][dx] = dataHolder.getTile(
								baseX + dx - tilePen.dx,
								baseY + dy - tilePen.dy,
								activeLayer);
					}
				}
				//create the edit
				dataHolder.addEdit(dataHolder.new MapEdit(baseX - tilePen.dx, baseY - tilePen.dy, oldDat,
						newDat, activeLayer));
				break;
			default:
				break;
			}
		}

		@Override
		public void mouseDragged(MouseEvent eve) {
			int mapX = dataHolder.getMapX();
			int mapY = dataHolder.getMapY();
			//TODO do nothing if we are editing the line layer
//			if (activeLayer == EditorApp.PHYSICAL_LAYER) {
//				return;
//			}
			if ((eve.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
				//middle click
				return;
			}
			if ((eve.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
				return;
			}
			if ((eve.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0) {
				Rectangle newCursorRect;
				Point mousePoint = eve.getPoint();
				int viewScale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
				int currentX = mousePoint.x / viewScale;
				int currentY = mousePoint.y / viewScale;
				boolean oob = false;
				if (currentX >= mapX) {
					currentX = mapX - 1;
					oob = true;
				}
				if (currentX < 0) {
					currentX = 0;
					oob = true;
				}
				if (currentY >= mapY) {
					currentY = mapY - 1;
					oob = true;
				}
				if (currentY < 0) {
					currentY = 0;
					oob = true;
				}
				parent.setTitle("(" + currentX + "," + currentY + ")");
				//Graphics2D g2d = (Graphics2D)((MapPane)eve.getSource()).getGraphics();
				switch (parent.getDrawMode()) {
				case EditorApp.DRAWMODE_DRAW:
					if (oob) return;
					//oldCursorRect = new Rectangle(lastX - tilePen.dx, lastY - tilePen.dy, tilePen.getW(), tilePen.getH());
					newCursorRect = new Rectangle(currentX - tilePen.dx, currentY - tilePen.dy, tilePen.getW(),
							tilePen.getH());
					if (lastX != currentX || lastY != currentY) {
						//update rects
						if (currentX < baseX) {
							selW += baseX - currentX;
							baseX = currentX;
						}
						if (currentY < baseY) {
							selH += baseY - currentY;
							baseY = currentY;
						}
						if ((currentX + tilePen.getW()) > selW) {
							selW = currentX + tilePen.getW();
						}
						if ((currentY + tilePen.getH()) > selH) {
							selH = currentY + tilePen.getH();
						}
						drawPen(currentX, currentY, currentX + tilePen.getW(), currentY + tilePen.getH());
						/*
						redrawTiles(lastX - tilePen.dx,
								lastY - tilePen.dy,
								tilePen.data.length,
								tilePen.data[0].length,
								g2d);
						*/
						redrawTiles(currentX - tilePen.dx,
								currentY - tilePen.dy,
								tilePen.data.length,
								tilePen.data[0].length);

						moveCursor(newCursorRect);
					}
					lastX = currentX;
					lastY = currentY;
					break;
				case EditorApp.DRAWMODE_FILL:
				case EditorApp.DRAWMODE_REPLACE:
				case EditorApp.DRAWMODE_RECT:
				case EditorApp.DRAWMODE_COPY:
					if (lastX != currentX || lastY != currentY) {
						int cursorX, cursorY;
						//redraw the tiles with line over them
						//could be optimized not to include middley tiles but w/e
						//oldCursorRect = new Rectangle(cursorX, cursorY, Math.abs(selW), Math.abs(selH));
						//update widths and heights
						if (currentX < baseX) {
							selW = currentX - baseX - 1;
						} else {
							selW = currentX - baseX + 1;
						}
						if (currentY < baseY) {
							selH = currentY - baseY - 1;
						} else {
							selH = currentY - baseY + 1;
						}
						//replace the cursor
						if (selW < 0) {
							cursorX = baseX + selW + 1;
						} else {
							cursorX = baseX;
						}
						if (selH < 0) {
							cursorY = baseY + selH + 1;
						} else {
							cursorY = baseY;
						}
						newCursorRect = new Rectangle(cursorX, cursorY, Math.abs(selW), Math.abs(selH));
						moveCursor(newCursorRect);

						lastX = currentX;
						lastY = currentY;
					}
					break;
				default:
					break;
				}
			}
		}

		@Override
		public void mouseMoved(MouseEvent eve) {
			int mapX = dataHolder.getMapX();
			int mapY = dataHolder.getMapY();
			//TODO do nothing if we are editing the line layer
//			if (activeLayer == EditorApp.PHYSICAL_LAYER) {
//				return;
//			}
			Rectangle newCursorRect;
			//MapPane pane = (MapPane) eve.getSource();
			Point mousePoint = eve.getPoint();
			//int tileSize = dataHolder.getConfig().getTileSize();
			int viewScale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
			//Graphics2D g2d = (Graphics2D) pane.getGraphics();
			int currentX = mousePoint.x / viewScale;
			int currentY = mousePoint.y / viewScale;

			if (currentX >= mapX || currentY >= mapY) {
				//clear the cursor
				mouseExited(eve);
				return;
			} else {
				parent.setTitle("(" + currentX + "," + currentY + ")");
			}
			switch (parent.getDrawMode()) {
			case EditorApp.DRAWMODE_DRAW:
			case EditorApp.DRAWMODE_FILL:
			case EditorApp.DRAWMODE_REPLACE:
				//oldCursorRect = new Rectangle(lastX - tilePen.dx, lastY - tilePen.dy, tilePen.getW(), tilePen.getH());
				newCursorRect = new Rectangle(currentX - tilePen.dx, currentY - tilePen.dy, tilePen.getW(),
						tilePen.getH());
				if (currentX != lastX || currentY != lastY) //make sure the thing has changed before we do anything
				{
					//dirty rectangles for efficiency?
					redrawTiles(lastX - tilePen.dx,
							lastY - tilePen.dy,
							tilePen.getW(),
							tilePen.getH());
					//redraw the mouse square
					moveCursor(newCursorRect);
					lastX = currentX;
					lastY = currentY;
				}
				break;
			case EditorApp.DRAWMODE_RECT:
			case EditorApp.DRAWMODE_COPY:
				if (currentX != lastX || currentY != lastY) //make sure the thing has changed before we do anything
				{

					newCursorRect = new Rectangle(currentX, currentY, 1, 1);
					//dirty rectangles for efficiency?
					//redrawTiles(lastX, lastY, 1, 1, g2d);
					//redraw the mouse square
					moveCursor(newCursorRect);
					lastX = currentX;
					lastY = currentY;
				}
				break;
			default:
				break;

			}
		}
	}

	private class PointCorrectingMouseAdapter extends MouseAdapter {

		protected Point correct(Point p) {
			correctZoom(p);
			roundToRes(p);
			return p;
		}

		protected void correctZoom(Point p) {
			p.x /= EditorApp.mapScale;
			p.y /= EditorApp.mapScale;
		}

		protected void roundToRes(Point p) {
			int resolution = dataHolder.getConfig().getLineRes();
			p.x += resolution / 2;
			p.x -= p.x % resolution;
			p.y += resolution / 2;
			p.y -= p.y % resolution;
		}

	}

	private class LineMouseAdapter extends PointCorrectingMouseAdapter {
		LineSeg lastNode = null;

		@Override
		public void mousePressed(MouseEvent eve) {
			//TODO do nothing if we are not editing the line layer
			if (activeLayer != 99999999) {
				return;
			}
			Point here = eve.getPoint();
			correct(here);
			//System.out.println("line press");
		}

		@Override
		public void mouseClicked(MouseEvent eve) {
			//TODO do nothing if we are not editing the line layer
			if (activeLayer != 99999999) {
				return;
			}
			if (eve.isAltDown()) {
				Point p = eve.getPoint();
				correct(p);
				if (lastNode != null) {
					lastNode.setP2(p);
					if (!eve.isShiftDown()) {
						lastNode = null;
					} else {
						//create a new node at this node to continue lining
						lastNode = new LineSeg(new Point(p), linePane.getCurrentType());
						dataHolder.addNode(lastNode);
					}
				} else {
					lastNode = new LineSeg(p, linePane.getCurrentType());
					dataHolder.addNode(lastNode);
				}
			}
			//System.out.println("line click");
		}

		@Override
		public void mouseReleased(MouseEvent eve) {
			//TODO do nothing if we are not editing the line layer
			if (activeLayer != 99999999) {
				return;
			}
			repaint();
		}

	}

	/**
	 * Redraw a selection of tiles without the cursor
	 *
	 * @param x   start tile x
	 * @param y   start tile y
	 * @param w   width in tiles
	 * @param h   height in tiles
	 */
	protected void redrawTiles(int x, int y, int w, int h) {
		//int tileSize = dataHolder.getConfig().getTileSize();
		int viewScale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
		/*
		g2d.setClip(null);
		g2d.clipRect(x * viewScale,
				y * viewScale,
				w * viewScale,
				h * viewScale);
		fillCanvas(g2d);
		*/
		this.repaint(x * viewScale, y * viewScale, w * viewScale, h * viewScale);
	}

	private class PolyMouseAdapter extends PointCorrectingMouseAdapter {
		MapPoly polygon;

		@Override
		public void mouseClicked(MouseEvent eve) {
			//TODO do nothing if we are not editing the line layer
			if (activeLayer != 99999999) {
				return;
			}
			Point p = correct(eve.getPoint());
			if (eve.isAltDown()) {
				if (polygon == null) {
					polygon = new MapPoly(p, TypeConfig.getType(linePane.getCurrentType()));
					polygon.setActive(true);
					dataHolder.getPolys().add(polygon);
				} else {
					polygon.extend(p);
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent eve) {
			//TODO do nothing if we are not editing the line layer
			if (activeLayer != 99999999) {
				return;
			}
			if (polygon != null && polygon.getPoints().size() <= 0) {
				polygon = null;
			}
			if (eve.getButton() == MouseEvent.BUTTON3) {
				if (polygon != null) {
					polygon.setActive(false);
					polygon = null;
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent eve) {
			//TODO do nothing if we are not editing the line layer
			if (activeLayer != 99999999) {
				return;
			}
			repaint();
		}
	}

	protected void drawCursor(Graphics2D g2d) {
		if (g2d == null) {
			g2d = (Graphics2D) this.getGraphics();
		}
		if (cursorLoc == null) {
			return;
		}
		g2d.setColor(Color.black);
		g2d.setXORMode(Color.white);
		g2d.setClip(null);
		int sc = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
		g2d.drawRoundRect(cursorLoc.x * sc + 1,
				cursorLoc.y * sc + 1,
				cursorLoc.width * sc - 2,
				cursorLoc.height * sc - 2, 8, 8);
		//this.repaint(x*viewScale, y*viewScale, w*viewScale, h*viewScale);
	}

	private class PointDragAdapter extends PointCorrectingMouseAdapter {

		Point lastP;

		@Override
		public void mousePressed(MouseEvent eve) {
			Point rawHere = eve.getPoint();
			correctZoom(rawHere);
			lastP = correct(eve.getPoint());

			if (!eve.isControlDown()) {
				for (LineSeg l : dataHolder.getLines()) {
					l.setSelection(LineSeg.NONE, false);
				}
				for (MapPoly poly : dataHolder.getPolys()) {
					poly.clearSelection();
				}
			}
			//determine if any nodes are being selected
			// this formerly iterated over the list in reverse and I cant' remember why
			// so if this breaks something you'll know why
			for (Iterator<LineSeg> it = dataHolder.getLines().iterator();
			     it.hasNext(); ) {
				LineSeg l = it.next();
				int selection;
				if ((selection = l.inRange(rawHere)) != LineSeg.NONE) {
					l.setSelection(selection, eve.isControlDown());
					linePane.setSelectedType(l.getType());
					break;
				}
			}
			for (MapPoly p : dataHolder.getPolys()) {
				if (p.trySelect(rawHere, eve.isControlDown(), eve.isShiftDown())) {
					if (polyAdapter.polygon == null ||
							polyAdapter.polygon != p) {
						polyAdapter.polygon = p;
						p.setActive(true);
						linePane.setSelectedType(p.type.id);
						linePane.setEvent(p.getEvent());
					}
				}

			}
		}

		@Override
		public void mouseDragged(MouseEvent eve) {
			//TODO do nothing if we are not editing the line layer
			if (activeLayer != 99999999) {
				return;
			}
			Point p = eve.getPoint();
			correct(p);
			//System.out.println("~\n" + lastP + "\n" + p);
			if (!p.equals(lastP)) {
				for (LineSeg l : dataHolder.getLines()) {
					l.drag(p.x - lastP.x, p.y - lastP.y);
				}//for each node
				for (MapPoly poly : dataHolder.getPolys()) {
					poly.drag(p.x - lastP.x, p.y - lastP.y);
				}
				repaint();
				//System.out.println("Move " + (p.x - lastP.x) + ", " + (p.y - lastP.y));
				lastP = p;
			}// if the point is not where it was
		}
	}

	protected void moveCursor(Rectangle newCursor) {
		int sc = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
		if (cursorLoc != null) {
			this.repaint(cursorLoc.x * sc,
					cursorLoc.y * sc,
					cursorLoc.width * sc,
					cursorLoc.height * sc);
		}
		cursorLoc = newCursor;
		if (cursorLoc != null) {
			this.repaint(cursorLoc.x * sc,
					cursorLoc.y * sc,
					cursorLoc.width * sc,
					cursorLoc.height * sc);
		}
	}

	public void importTiledJson(File tileFile) {
		dataHolder.importTiledJson(tileFile);
		this.reloadLayerList();
	}

	private class MapUndo extends AbstractAction {
		private static final long serialVersionUID = 6782970146054673885L;

		@Override
		public void actionPerformed(ActionEvent eve) {
			dataHolder.doUndo();
			reloadLayerList();
			repaint();
		}

	}

	private class MapRedo extends AbstractAction {

		private static final long serialVersionUID = -3695712535211760564L;

		@Override
		public void actionPerformed(ActionEvent eve) {
			dataHolder.doRedo();
			reloadLayerList();
			repaint();
		}

	}

	class PreviewPane extends JPanel {
		private static final long serialVersionUID = -1370915835014616734L;
		ResourceManager iMan;
		MapInfo dataHolder;
		MapPane parent;

		PreviewPane(ResourceManager i, MapPane p, MapInfo data) {
			parent = p;
			iMan = i;
			dataHolder = data;
		}

		@Override
		public void paintComponent(Graphics g) {
			if (parent.getPen() != null) {
				BlConfig conf = dataHolder.getConfig();
				int sc = (int) (conf.getTileSize() * EditorApp.mapScale);
				MapPane.TileBuffer pen = parent.getPen();
				this.setPreferredSize(new Dimension(pen.getW() * sc, pen.getH() * sc));
				this.setBackground(Color.black);
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g.create();
				BufferedImage tileImg = iMan.getImg(dataHolder.getTileset());
				int width = conf.getTilesetWidth();
				if (width <= 0) {
					//get width as actual fittable tiles
					width = tileImg.getWidth() / conf.getTileSize();
				}
				for (int x = 0; x < pen.getW(); x++) {
					for (int y = 0; y < pen.getH(); y++) {
						//calculate destination x/y
						int dx1 = x * sc;
						int dx2 = dx1 + sc;
						int dy1 = y * sc;
						int dy2 = dy1 + sc;
						//calculate source x/y
						int sx1 = (pen.get(x, y) % width) * conf.getTileSize();
						int sx2 = sx1 + conf.getTileSize();
						int sy1 = (pen.get(x, y) / width) * conf.getTileSize();
						int sy2 = sy1 + conf.getTileSize();
						g2d.drawImage(tileImg, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
					}
				}
			} else {
				this.setPreferredSize(new Dimension(32, 32));
				g.setColor(Color.red);
				super.paintComponent(g);
				g.drawLine(0, 0, 32, 32);
				g.drawLine(0, 32, 32, 0);
			}
		}
	}

	private class LinetypePane extends BgPanel {
		private static final long serialVersionUID = 2605950963052741569L;
		private JComboBox<TypeConfig> typeSelector = new JComboBox<>(TypeConfig.getTypes());
		private JPanel topcol = new JPanel();
		private JPanel botcol = new JPanel();
		private JTextField textInput = new FormattedUpdateTextField(NumberFormat.getIntegerInstance());
		private int currentType = 1;

		public LinetypePane(BufferedImage bg) {
			super(bg);
			this.add(typeSelector);
			JPanel colourPreview = new JPanel(new GridLayout(2, 2));
			topcol.setMinimumSize(new Dimension(15, 15));
			colourPreview.add(topcol);
			colourPreview.add(new JLabel("Top colour"));
			colourPreview.add(botcol);
			colourPreview.add(new JLabel("Bottom colour"));
			this.add(colourPreview);
			JPanel eventField = new JPanel();
			eventField.add(new JLabel("Event #"));
			textInput.setColumns(6);
			eventField.add(textInput);
			textInput.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent arg0) {
				}

				@Override
				public void focusLost(FocusEvent arg0) {
					try {
						if (polyAdapter.polygon != null) {
							polyAdapter.polygon.setEvent(Integer.parseInt(textInput.getText()));
						}
					} catch (NumberFormatException ignored) {
					}
				}
			});
			this.add(eventField);
			updatePreview();
			typeSelector.setAction(new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					setSelectedType(((TypeConfig) typeSelector.getSelectedItem()).id);
				}

			});
		}

		public int getCurrentType() {
			return currentType;
		}

		public void setEvent(int eve) {
			textInput.setText(eve + "");
		}

		private void updatePreview() {
			TypeConfig type = TypeConfig.getType(currentType);
			if (type != null) {
				topcol.setBackground(type.topColour);
				botcol.setBackground(type.bottomColour);
			} else {
				topcol.setBackground(Color.gray);
				botcol.setBackground(Color.gray);
				return;
			}
			MapPane.this.removeMouseListener(lineAdapter);
			MapPane.this.removeMouseListener(polyAdapter);
			//MapPane.this.removeMouseMotionListener(lineAdapter);
			//MapPane.this.removeMouseMotionListener(polyAdapter);
			switch (type.linemode) {
			case POLYLINE:
				MapPane.this.addMouseListener(polyAdapter);
				break;
			case SEGMENT:
				MapPane.this.addMouseListener(lineAdapter);
				List<LineSeg> selectedNodes = dataHolder.getSelectedNodes();
				for (LineSeg l : selectedNodes) {
					l.setType(currentType);
				}
				break;
			default:
				break;

			}
		}

		public void setSelectedType(int type) {
			currentType = type;
			typeSelector.setSelectedItem(TypeConfig.getType(type));
			updatePreview();
		}
	}

	private class TilescriptAction extends AbstractAction {

		private static final long serialVersionUID = -8925511600300317665L;
		int tx, ty, tile, layer;

		TilescriptAction(int x, int y, int tile, int layer) {
			tx = x;
			ty = y;
			this.tile = tile;
			this.layer = layer;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			TscPane scriptPane = parent.getSelectedScript();
			if (scriptPane != null) {
				String sx = String.format("%04d", tx); //$NON-NLS-1$
				String sy = String.format("%04d", ty); //$NON-NLS-1$
				String stile = String.format("%04d", tile); //$NON-NLS-1$
				String sl = String.format("%04d", layer); //$NON-NLS-1$
				String comStr; //$NON-NLS-1$
				if (EditorApp.EDITOR_MODE == 1) {
					//KSS
					comStr = "<CML" + sl + ":" + sx + ":" + sy + ":" + stile; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} else {
					comStr = "<CMP" + sx + ":" + sy + ":" + stile;             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				scriptPane.insertStringAtCursor(comStr);
			}
		}
	}

	private class TraScriptAction extends AbstractAction {

		private static final long serialVersionUID = -8925511600300317665L;
		int tx, ty, mapnum;

		TraScriptAction(int x, int y, int map) {
			tx = x;
			ty = y;
			mapnum = map;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			TscPane scriptPane = parent.getSelectedScript();
			if (scriptPane != null) {
				String sx = String.format("%04d", tx); //$NON-NLS-1$
				String sy = String.format("%04d", ty); //$NON-NLS-1$
				String sm = String.format("%04d", mapnum); //$NON-NLS-1$
				String comStr; //$NON-NLS-1$
				comStr = "<TRA" + sm + ":EVNT:" + sx + ":" + sy; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				scriptPane.insertStringAtCursor(comStr);
			}
		}
	}

	private class LayerListListener implements ListSelectionListener, KeyListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			activeLayer = layerSelect.getSelectedIndex();
			if (isFadeUnfocusedLayers || isSoloLayerView) {
				repaint();
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {

		}

		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getExtendedKeyCode()) {
			case KeyEvent.VK_MINUS:
			case KeyEvent.VK_SUBTRACT:
				if (activeLayer > 0) {
					activeLayer--;
					layerSelect.setSelectedIndex(activeLayer);
				}
				break;
			case KeyEvent.VK_EQUALS:
			case KeyEvent.VK_ADD:
				if (activeLayer < dataHolder.getMap().size()-1) {
					activeLayer++;
					layerSelect.setSelectedIndex(activeLayer);
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {

		}
	}

	private class LayerListActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			switch(e.getActionCommand().toLowerCase()) {
			case "add":
				dataHolder.addTileLayer(++activeLayer);
				break;
			case "delete":
				dataHolder.removeTileLayer(activeLayer);
				if (activeLayer > 0) {
					activeLayer--;
				}
				break;
			case "copy":
				dataHolder.copyTileLayer(activeLayer);
				activeLayer++;
				break;
			case "move_up":
				dataHolder.swapTileLayer(activeLayer, activeLayer-1);
				if (activeLayer > 0) {
					activeLayer--;
				}
				break;
			case "move_down":
				dataHolder.swapTileLayer(activeLayer, activeLayer+1);
				if (activeLayer < dataHolder.getMap().size()-1) {
					activeLayer++;
				}
				break;
			case "merge":
				dataHolder.mergeTileLayer(activeLayer, activeLayer+1);
				break;
			case "property":
				new LayerPropertyDialog(parent, dataHolder.getMap().get(activeLayer));
				dataHolder.markChanged();
				break;
			}
			reloadLayerList();
		}
	}

	private void reloadLayerList() {
		//setListData creates a listSelectionChanged event, which will reset the activeLayer to 0
		//so, briefly copy it here so we can put it back when it's done
		int l = activeLayer;
		layerSelect.setListData(dataHolder.getMap().toArray(new TileLayer[0]));
		layerSelect.setSelectedIndex(l);
		repaint();
	}
}
