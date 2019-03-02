package ca.noxid.lab.entity;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.mapdata.MapInfo;
import ca.noxid.lab.mapdata.MapInfo.EntityEdit;
import ca.noxid.lab.mapdata.MapInfo.PxeEntry;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.lab.tile.MapPane;
import ca.noxid.uiComponents.BgPanel;
import ca.noxid.uiComponents.DragScrollAdapter;
import ca.noxid.uiComponents.FormattedUpdateTextField;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;


public class EntityPane extends MapPane implements ListSelectionListener, ClipboardOwner {
	private static Clipboard clip = new Clipboard("entity");
	private static final long serialVersionUID = -7864463786928806116L;

	private Set<PxeEntry> selectionList;
	
	private JList<EntityData> entityDisplay = new JList<>();

	public JList<EntityData> getEntityList() {return entityDisplay;}
	private Point mouseLoc =  new Point();
	private Rectangle dragSelectRect;
	
	private EntitySettings editPane;
	public JPanel getEditPane() {return editPane;}

	private JMenuItem popup_addEntity;
	
	public EntityPane(EditorApp p, MapInfo data) {
		super(p);
		this.iMan = p.getImageManager();
		this.parent = p;
		dataHolder = data;
		selectionList = new HashSet<>();
		this.initMouse();
		this.buildPopup();
		entityDisplay.setListData(p.getEntityList());
		EntityListRender cellRender = new EntityListRender(data, iMan, p.getGameInfo());
		entityDisplay.setCellRenderer(cellRender);
		entityDisplay.addMouseListener(new EntityListMouseListener());
		entityDisplay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		editPane = new EntitySettings(selectionList);
		this.setFocusable(true); //for keys?
	}
	
	@Override
	protected void drawEntities(Graphics2D g2d) {
		int drawflag = PxeEntry.DRAW_ALL;
		g2d = (Graphics2D) g2d.create();
		Rectangle view = g2d.getClipBounds();
		//int nDraws = 0;
		Iterator<PxeEntry> pxeIt = dataHolder.getPxeIterator();
		while (pxeIt.hasNext()) {
			PxeEntry cEntity = pxeIt.next();
			if (view.intersects(cEntity.getDrawArea())) {
				if (selectionList.contains(cEntity)) {
					///NOTE possibly loop3x and draw sprite -> box -> name if conflicts arise
					cEntity.draw(g2d, drawflag | PxeEntry.DRAW_SELECTED);
				} else {
					cEntity.draw(g2d, drawflag);
				}
				//nDraws++;
			}
		}
		//System.out.println("Redraw " + nDraws + " entity");
		
		if (dragSelectRect != null) {
			g2d.setXORMode(Color.WHITE);
			
			int rx = dragSelectRect.x;
			int rw = dragSelectRect.width;
			int ry = dragSelectRect.y;
			int rh = dragSelectRect.height;
			
			if (rw < 0) {
				rx += rw;
				rw *= -1;
			} 
			if (rh < 0) {
				ry += rh;
				rh *= -1;
			}
			
			g2d.drawRoundRect(rx, ry, rw, rh, 8, 8);
		}
	}
	
	@SuppressWarnings("serial")
	@Override
	protected void buildPopup() {
		popup = new JPopupMenu();
		JMenuItem undoItem;
		undoItem = new JMenuItem(undo);
		undoItem.setText(Messages.getString("EntityPane.0")); //$NON-NLS-1$
		undoItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Z,
				java.awt.event.InputEvent.CTRL_DOWN_MASK));
		JMenuItem redoItem;
		redoItem = new JMenuItem(redo);
		redoItem.setText(Messages.getString("EntityPane.1")); //$NON-NLS-1$
		redoItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Y,
				java.awt.event.InputEvent.CTRL_DOWN_MASK));
		JMenuItem deleteItem = new JMenuItem();
		deleteItem.setAction(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelectedEntities();
			}
		});
		deleteItem.setText("Delete");
		
		JMenuItem secret_undoItem;
		secret_undoItem = new JMenuItem(undo);
		secret_undoItem.setText(Messages.getString("EntityPane.2")); //$NON-NLS-1$
		secret_undoItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Z,
				java.awt.event.InputEvent.CTRL_DOWN_MASK));
		JMenuItem secret_redoItem;
		secret_redoItem = new JMenuItem(redo);
		secret_redoItem.setText(Messages.getString("EntityPane.3")); //$NON-NLS-1$
		secret_redoItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Y,
				java.awt.event.InputEvent.CTRL_DOWN_MASK));
		JPopupMenu fakePopup = new JPopupMenu();
		fakePopup.add(secret_undoItem);
		fakePopup.add(secret_redoItem);
		
		popup.add(undoItem);
		popup.add(redoItem);

		popup_addEntity = new JMenuItem(Messages.getString("EntityPane.9"));
		popup.add(popup_addEntity);

		popup.addSeparator();

		popup.add(deleteItem);
		JMenuItem clearall = new JMenuItem(new javax.swing.AbstractAction() {
			private static final long serialVersionUID = -1148394342964407843L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				selectionList.clear();
				dataHolder.clearEntities();
				EntityPane.this.repaint();
			}
		});
		clearall.setText(Messages.getString("EntityPane.8"));
		popup.add(clearall);
		this.add(fakePopup);
		this.add(popup);
	}
	
	@Override
	protected void initMouse() {
		EntMouseAdapter ema = new EntMouseAdapter();
		DragScrollAdapter dsa = new DragScrollAdapter();
		this.addMouseListener(ema);
		this.addMouseMotionListener(ema);
		this.addKeyListener(new EntityKeyAdapter());
		this.addMouseListener(dsa);
		this.addMouseMotionListener(dsa);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		//System.out.println("Entity list selection");
	}

	private void deleteSelectedEntities() {
		if (selectionList.isEmpty()) return;
		dataHolder.removeEntities(selectionList);
		Rectangle r = selectionList.iterator().next().getDrawArea();
		for (PxeEntry e : selectionList) {
			r.add(e.getDrawArea());
		}
		selectionList.clear();
		r.grow(1, 1);
		repaint(r);
	}
	
	private void showStackedEntitySelectionPopup(List<PxeEntry> pxeStack, int x, int y) {
		JPopupMenu selectMenu = new JPopupMenu();
		
		for (PxeEntry e : pxeStack) {
			JMenuItem item = new JMenuItem(new EntitySelectionToggleAction(e));
			EntityData inf = dataHolder.getEntityInfo(e.getType());
			String name = "????";
			if (inf != null ) {
				name = inf.getName();
			}				
			String title = e.getOrder() + " - " + name.trim() + "#" + e.getEvent();
			if (selectionList.contains(e)) {
				title = "* " + title;
			} else {
				title = "  " + title;
			}
			item.setText(title);
			selectMenu.add(item);
		}
		
		selectMenu.show(this, x, y);
	}
	
	
	/////////////////////////////////////////////////////////////////////// 
	// 	Classes
	///////////////////////////////////////////////////////////////////////
	
	class EntMouseAdapter extends MouseAdapter {
		Point anchorTile;
		Point currentTile;
		boolean allowDrag = false;
		Point origin;
		
		@Override
		public void mouseClicked(MouseEvent eve) {
			//System.out.println("click");
			boolean selectionListChanged = false;
			boolean shiftHeld = (eve.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
			int realScale = (int) (dataHolder.getConfig().getEntityRes()
					* EditorApp.mapScale);
			int tileScale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
			//int canvasScale = EditorApp.DEFAULT_TILE_SIZE;
			//Graphics2D g2d = (Graphics2D) pane.getGraphics();
			//System.out.println("EntityPane mouseClicked event");
			if ((eve.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0) {
				//ctrl key not pressed
				selectionList.clear();
				selectionListChanged = true;
			}
			Iterator<PxeEntry> pIt = dataHolder.getPxeIterator();
			List<PxeEntry> entityStack = new LinkedList<>();
			while (pIt.hasNext()) {
				PxeEntry cEntry = pIt.next();
				int pxeL = cEntry.getX() * realScale;
				int pxeU = cEntry.getY() * realScale;
				
				if ((eve.getX() > pxeL)
					&& (eve.getY() > pxeU)
					&& (eve.getX() < pxeL + tileScale)
					&& (eve.getY() < pxeU + tileScale) ) {	
					entityStack.add(cEntry);
					if (!selectionList.contains(cEntry)) {
						if (shiftHeld) {
							Iterator<PxeEntry> it2 = dataHolder.getPxeIterator();
							while (it2.hasNext()) {
								PxeEntry p = it2.next();
								if (p.getType() == cEntry.getType() 
										&& !selectionList.contains(p)) {
									selectionList.add(p);
									selectionListChanged = true;
								}
							}
							repaint();
							break;
						}//if shift held
					}
				}
				dragSelectRect = null;
			}
			if (entityStack.size() == 1 && !shiftHeld) {
				PxeEntry select = entityStack.get(0);
				selectionListChanged = true;
				if (selectionList.contains(select)) {
					selectionList.remove(select);
				} else {
					selectionList.add(select);
				}
			} else if (entityStack.size() > 1 && !shiftHeld) {
				showStackedEntitySelectionPopup(entityStack, eve.getX(), eve.getY());
			}
			if (selectionListChanged) {
				editPane.listChanged();
				repaint();
			}
		}

		@Override
		public void mouseEntered(MouseEvent eve) {
			// TODO Auto-generated method stub
			EntityPane.this.requestFocusInWindow();
		}

		@Override
		public void mouseExited(MouseEvent eve) {
			mouseLoc = null;
		}

		@Override
		public void mousePressed(MouseEvent eve) {
			//log current position
			int scale = (int) (dataHolder.getConfig().getEntityRes() * EditorApp.mapScale);
			anchorTile = new Point(eve.getX() / scale, eve.getY() / scale);
			if (eve.isPopupTrigger()) {
				//popup
				setupPopupMenu(eve);
				return;
			}
			origin = eve.getPoint();
			//System.out.println("press");
			requestFocus(); //for keys?????????? ??
			currentTile = new Point(anchorTile);
			allowDrag = false;
			for (PxeEntry e : selectionList) {
				if (e.getDrawArea().contains(eve.getPoint())) {
					allowDrag = true;
					/*set up origin list
					originList = new ArrayList<Point>();
					for (PxeEntry e621 : selectionList) {
						originList.add(new Point(e621.getX(), e621.getY()));
					}
					*/
					break;
				}
			}
			if (!allowDrag) {
				dragSelectRect = new Rectangle(eve.getX(), eve.getY(), 1, 1);
			}
		}

		@Override
		public void mouseReleased(MouseEvent eve) {
			if (eve.isPopupTrigger()) {
				//popup
				setupPopupMenu(eve);
				return;
			}
			if (allowDrag && (!anchorTile.equals(currentTile))) {//if entities have been dragged
				Point p = new Point(currentTile.x - anchorTile.x, currentTile.y - anchorTile.y);
				for (PxeEntry e : selectionList) {
					EntityEdit edit = dataHolder.new EntityEdit(EntityEdit.EDIT_MOVE, e, p);
					edit.setSignificant(false);
					dataHolder.addEdit(edit);
				}
				dataHolder.addEdit(dataHolder.new MapEdit(0, 0, null, null, 0));//signif
			}
			if (eve.getPoint().equals(origin)) {
				dragSelectRect = null;
			}
			if (dragSelectRect != null) {
				if (!eve.isControlDown()) {
					selectionList.clear();
				}
				Rectangle tmpRect = dragSelectRect.getBounds();
				if (tmpRect.width < 0) {
					tmpRect.x += tmpRect.width;
					tmpRect.width *= -1;
				}
				if (tmpRect.height < 0) {
					tmpRect.y += tmpRect.height;
					tmpRect.height *= -1;
				}
				Iterator<PxeEntry> pxeIt = dataHolder.getPxeIterator();
				while (pxeIt.hasNext()) {
					PxeEntry pxe = pxeIt.next();
					if (pxe.getDrawArea().intersects(tmpRect)) {
						selectionList.add(pxe);
					}
				}
				editPane.listChanged();
				dragSelectRect = null;
				repaint();
			}
		}

		@Override
		public void mouseDragged(MouseEvent eve) {			
			if (allowDrag) {
				int scale = (int) (dataHolder.getConfig().getEntityRes() * EditorApp.mapScale);
				int tileScale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.mapScale);
				Point myLoc = new Point(eve.getX() / scale, eve.getY() / scale);
				if (!myLoc.equals(currentTile)) {

					parent.setTitle("(" + eve.getX() / tileScale +
							"," + eve.getY() / tileScale + ")");
					int xShift = myLoc.x - currentTile.x;
					int yShift = myLoc.y - currentTile.y;
					Iterator<PxeEntry> pxeIt = selectionList.iterator();
					Rectangle redrawSpace = null;
					if (pxeIt.hasNext()) { //if there are entities to check
						while (pxeIt.hasNext()) {
							PxeEntry e = pxeIt.next();
							//redraw the area it was
							if (redrawSpace == null) {
								redrawSpace = e.getDrawArea();
							} else {
								redrawSpace.add(e.getDrawArea());
							}
							e.shift(xShift, yShift);
							//redraw the area it will be
							redrawSpace.add(e.getDrawArea());
						}
						assert redrawSpace != null;
						redrawSpace.grow(1, 1);
						//System.out.println(redrawSpace);
						repaint(redrawSpace);
						dataHolder.markChanged();
						//System.out.println("shift entities");
					}
					currentTile = myLoc;
					EntityPane.this.mouseLoc = myLoc;
				}//if in diff. tile
			} else {
				if (dragSelectRect  != null) {
					dragSelectRect.width = eve.getX() - dragSelectRect.x;
					dragSelectRect.height = eve.getY() - dragSelectRect.y;
				}
				repaint();
			}
		}

		@Override
		public void mouseMoved(MouseEvent eve) {
			int scale = (int) (dataHolder.getConfig().getEntityRes() * EditorApp.mapScale);
			Point myLoc = new Point(eve.getX() / scale, eve.getY() / scale);
			if (!myLoc.equals(currentTile)) {
				parent.setTitle("(" + myLoc.x + "," + myLoc.y + ")");
				currentTile = myLoc;
				EntityPane.this.mouseLoc = myLoc;
			}
		}

		private void setupPopupMenu(MouseEvent eve) {
			EntityData selectedNpc = entityDisplay.getSelectedValue();
			if (selectedNpc != null) {
				popup_addEntity.setEnabled(true);
				popup_addEntity.setAction(new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						addNpcAtPoint(anchorTile.x, anchorTile.y);
					}
				});
				popup_addEntity.setText(
						Messages.getString("EntityPane.10")
								.replace("@@@", selectedNpc.getName()));
			} else {
				popup_addEntity.setEnabled(false);
			}
			popup.show(eve.getComponent(), eve.getX(), eve.getY());
		}
	}
	
	class EntityKeyAdapter extends KeyAdapter{
		 
		@Override
		public void keyPressed(KeyEvent eve) {
			//System.out.println("key");
			if (eve.getKeyCode() == KeyEvent.VK_DELETE) {
				deleteSelectedEntities();
			} else if (eve.getKeyCode() == KeyEvent.VK_C && eve.isControlDown()) {
				//copy
				EntitySelection content = new EntitySelection(selectionList);
				clip.setContents(content, EntityPane.this);				
			} else if (eve.getKeyCode() == KeyEvent.VK_V && eve.isControlDown()) {
				Transferable contents = clip.getContents(EntityPane.this);
				try {
					@SuppressWarnings("unchecked")
					HashSet<PxeEntry> content = (HashSet<PxeEntry>) contents.getTransferData(
							new DataFlavor(PxeEntry.class, "java/serializable/pxeEntry"));
					Integer dx = null;
					Integer dy = null;
					for (PxeEntry p: content) {
						if (dx == null) {
							dx = mouseLoc.x - p.getX();
							
						}
						if (dy == null) {
							dy = mouseLoc.y - p.getY();
						}
						PxeEntry duple = p.clone();
						duple.shift(dx, dy);
						
						dataHolder.addEntity(duple);
					}
					EntityPane.this.repaint();
				} catch (UnsupportedFlavorException | IOException e) {
					e.printStackTrace();
				}
			} else if (eve.getKeyCode() == KeyEvent.VK_I) {
				if (mouseLoc == null) {
					return;
				}
				addNpcAtPoint(mouseLoc.x, mouseLoc.y);
			} else if (eve.getKeyCode() == KeyEvent.VK_A && eve.isControlDown()) {
				selectionList.clear();
				Iterator<PxeEntry> pxeIt = dataHolder.getPxeIterator();
				while (pxeIt.hasNext()) {
					selectionList.add(pxeIt.next());
				}
				editPane.listChanged();
				repaint();
				
			}
		}
	}

	private void addNpcAtPoint(int x, int y) {
		int entID = 0;
		if (entityDisplay.getSelectedValue() != null) {
			entID = entityDisplay.getSelectedValue().getID();
		}
		PxeEntry newEnt = dataHolder.addEntity(x, y, entID);
		repaint(newEnt.getDrawArea());
	}
	
	
	class EntitySelection implements Transferable {
		Set<PxeEntry> data;
		
		DataFlavor supportedFlav = new DataFlavor(PxeEntry.class, "java/serializable/pxeEntry");
		EntitySelection(Set<PxeEntry> selectionList) {
			data = new HashSet<>(selectionList);
		}
		
		@Override
		public Object getTransferData(DataFlavor arg0)
				throws UnsupportedFlavorException, IOException {
			if (arg0.isMimeTypeEqual(supportedFlav))
				return data;
			else
				throw new UnsupportedFlavorException(arg0);
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[]{supportedFlav};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor arg0) {
			return arg0.equals(supportedFlav);
		}
	}
	
	class EntitySettings extends BgPanel implements ActionListener {
		private static final long serialVersionUID = 4649521655943734465L;
		
		private FormattedUpdateTextField flagIDInput;
		private FormattedUpdateTextField eventInput;
		private FormattedUpdateTextField orderInput;
		private JLabel selectCountLabel;
		private JLabel flagLabel;
		
		private JCheckBox[] flagArray;
		boolean active = false;
		
		private final NumberFormat lFormat = 
				FormattedUpdateTextField.getNumberOnlyFormat(1, 4);
		
		
		
		Set<PxeEntry> entityList;
		
		EntitySettings(Set<PxeEntry> selectionList) {
			super(iMan.getImg(ResourceManager.rsrcBgBlue)); //$NON-NLS-1$
			entityList = selectionList;
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.anchor = GridBagConstraints.WEST;
			c.ipadx = 2;
			selectCountLabel = new JLabel("");
			this.add(selectCountLabel, c);
			c.gridy++;
			this.add(new JLabel(Messages.getString("EntityPane.4")), c); //$NON-NLS-1$
			c.gridy++;
			this.add(new JLabel(Messages.getString("EntityPane.5")), c); //$NON-NLS-1$
			c.gridy++;
			this.add(new JLabel(Messages.getString("EntityPane.6")), c); //$NON-NLS-1$
			c.gridy++;
			this.add(new JLabel(Messages.getString("EntityPane.7")), c); //$NON-NLS-1$
			c.gridy = 1;
			c.gridx = 1;
			flagIDInput = new FormattedUpdateTextField(lFormat);
			flagIDInput.setColumns(4);
			flagIDInput.addActionListener(this);
			this.add(flagIDInput, c);
			c.gridy++;
			eventInput = new FormattedUpdateTextField(lFormat);
			eventInput.setColumns(4);
			eventInput.addActionListener(this);
			this.add(eventInput, c);
			c.gridy++;
			orderInput = new FormattedUpdateTextField(lFormat);
			orderInput.setColumns(4);
			orderInput.addActionListener(this);
			this.add(orderInput, c);
			c.gridy++;
			flagLabel = new JLabel("0000"); //$NON-NLS-1$
			this.add(flagLabel, c);
			
			//create flag value checkboxes
			c.gridx = 0;
			c.gridy++;
			c.gridwidth = GridBagConstraints.REMAINDER;
			flagArray = new JCheckBox[16];
			for (int i = 0; i < flagArray.length; i++) {
				flagArray[i] = new JCheckBox(EntityData.flagNames[i]);
				flagArray[i].addActionListener(this);
				flagArray[i].setOpaque(false);
				this.add(flagArray[i], c);
				c.gridy++;
			}
			switchAll(false);
		}
		
		private void switchAll(boolean state) {
			if (active != state) {
				flagIDInput.setEnabled(state);
				eventInput.setEnabled(state);
				orderInput.setEnabled(state);
				for (JCheckBox c : flagArray) {
					c.setEnabled(state);
				}
				active = state;
			}			
		}
		
		private void listChanged() {
			if (entityList.isEmpty()) {
				selectCountLabel.setText("");
				switchAll(false);
			} else if (entityList.size() == 1){
				selectCountLabel.setText("");
				switchAll(true);
				PxeEntry p = entityList.iterator().next();
				flagIDInput.setText(String.valueOf(p.getFlagID()));
				eventInput.setText(String.valueOf(p.getEvent()));
				orderInput.setText(String.valueOf(p.getOrder()));
				int flags = p.getFlags();
				flagLabel.setText(String.format("0x%04X", flags)); //$NON-NLS-1$
				int bit = 1;
				for (int i = 0; i < flagArray.length; i++) {
					if ((flags & bit<<i) != 0) {
						flagArray[i].setSelected(true);
					} else {
						flagArray[i].setSelected(false);
					}
					flagArray[i].setText(EntityData.flagNames[i]);
				} //check each bit in the flag
				entityDisplay.setSelectedValue(p.getInfo(), true);
			} else {//if size > 1
				selectCountLabel.setText("Selected: " + entityList.size());
				switchAll(true);
				orderInput.setEnabled(false);
				String flagID = null;
				String event = null;
				int[] flagsArray = new int[flagArray.length];
				for (PxeEntry p : entityList) {
					if (flagID == null) {
						flagID = String.valueOf(p.getFlagID());						
					} else {
						if (!String.valueOf(p.getFlagID()).equals(flagID)) {
							flagID = "****"; //$NON-NLS-1$
						}
					}
					if (event == null) {
						event = String.valueOf(p.getEvent());						
					} else {
						if (!String.valueOf(p.getEvent()).equals(event)) {
							event = "****"; //$NON-NLS-1$
						}
					}
					for (int i = 0; i < flagArray.length; i++) {
						if ((p.getFlags() & 1 << i) != 0) { //if the flag set
							flagsArray[i]++;
						}
					}
				}//for each entity in the list
				flagIDInput.setText(flagID);
				eventInput.setText(event);
				for (int i = 0; i < flagArray.length; i++) {
					if (flagsArray[i] == 0) {
						flagArray[i].setSelected(false);
						flagArray[i].setText(EntityData.flagNames[i]);
					} else {
						flagArray[i].setSelected(true);
						flagArray[i].setText(EntityData.flagNames[i] + "*" + flagsArray[i]); //$NON-NLS-1$
					}
				}
			} //if size  > 1
		}//listChanged();

		@Override
		public void actionPerformed(ActionEvent eve) {
			if (!entityList.isEmpty()) {
				Object src = eve.getSource();
				if (src == flagIDInput) {
					int flag = 0;
					try {
						flag = Integer.parseInt(flagIDInput.getText());
					} catch (NumberFormatException err) {
						flagIDInput.setText("0");
					}
					for (PxeEntry e : entityList) {
						e.setFlagID(flag);
					}
				} else if (src == eventInput) {
					int event = 0;
					try {
						event = Integer.parseInt(eventInput.getText());
					} catch (NumberFormatException err) {
						eventInput.setText("0");
					}
					for (PxeEntry e : entityList) {
						e.setEvent(event);
					}
				} else if (src == orderInput) {
					int order = 0;
					try {
						order = Integer.parseInt(orderInput.getText());
					} catch (NumberFormatException err) {
						orderInput.setText("0");
					}
					entityList.iterator().next().setOrder(order);
				} else {
					//check flags
					for (int i = 0; i < flagArray.length; i++) {
						if (src == flagArray[i]) {
							int selected = (flagArray[i].isSelected()) ? 1 : 0;
							for (PxeEntry e : entityList) {
								int oldFlags = e.getFlags();
								oldFlags &= ~(1 << i);
								oldFlags |= selected << i;
								e.setFlags(oldFlags);
							}//for each entry in selection list
							listChanged();
							break;
						}//if event source is this element in flag array
					}//for each flag in array
				}//check flags
			}//if entity list is not empty
		}//actionperformed
	}//::EntitySettings

	class EntityListMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent eve) {
			if (eve.getClickCount() == 2) {
				if (!selectionList.isEmpty()) {
					Rectangle repaintRect = null;
					EntityData dat = entityDisplay.getSelectedValue();
					if (dat != null) {
						for (PxeEntry entity : selectionList) {
							if (repaintRect == null) {
								repaintRect = entity.getDrawArea();
							} else {
								repaintRect.add(entity.getDrawArea());
							}
							entity.setType(dat.getID());
							repaintRect.add(entity.getDrawArea());
						}
						assert repaintRect != null;
						repaintRect.grow(1, 1);
						repaint(repaintRect);
					} //if there is a selected value
				}
			}
		}
	}
	
	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// TODO Auto-generated method stub
		
	}
	
	class EntitySelectionToggleAction extends AbstractAction {
		private static final long serialVersionUID = -860779077029978392L;
		PxeEntry select;
		EntitySelectionToggleAction(PxeEntry e) {
			select = e;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (selectionList.contains(select)) {
				selectionList.remove(select);
			} else {
				selectionList.add(select);
			}
			repaint();
		}
	}
}
