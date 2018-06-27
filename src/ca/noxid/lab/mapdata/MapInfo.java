package ca.noxid.lab.mapdata;

import ca.noxid.lab.BlConfig;
import ca.noxid.lab.Changeable;
import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.entity.EntityData;
import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.lab.tile.LineSeg;
import ca.noxid.lab.tile.MapPoly;
import ca.noxid.lab.tile.ShiftDialog;
import com.carrotlord.string.StrTools;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.List;

public class MapInfo implements Changeable {

	private static final int MAX_TILES_X = 9001;
	private static final int MAX_TILES_Y = 9001;
	private int mapX;

	public int getMapX() {
		return mapX;
	}

	private int mapY;

	public int getMapY() {
		return mapY;
	}

	private int mapNumber;

	public int getMapNumber() {
		return mapNumber;
	}

	protected int[][][] map;

	public int[][][] getMap() {
		return map;
	}

	// MR lines
	private LinkedList<LineSeg> nodeVec = new LinkedList<>();

	public LinkedList<LineSeg> getLines() {
		return nodeVec;
	}

	public ArrayList<LineSeg> getSelectedNodes() {
		ArrayList<LineSeg> rv = new ArrayList<>();
		for (LineSeg l : nodeVec) {
			if (l.isSelected())
				rv.add(l);
		}
		return rv;
	}

	private List<MapPoly> polygons = new LinkedList<>();

	public List<MapPoly> getPolys() {
		return polygons;
	}

	private PropertyChangeSupport pcs;

	private LinkedList<PxeEntry> pxeList;

	public Iterator<PxeEntry> getPxeIterator() {
		return pxeList.iterator();
	}

	// UNDO/redo
	private UndoManager undoMan;

	// Retrieve these values from the ImageManager
	private File tileset;

	public File getTileset() {
		return tileset;
	}

	private File bgImage;

	public File getBG() {
		return bgImage;
	}

	private File npcImage1;

	public File getNpc1() {
		return npcImage1;
	}

	private File npcImage2;

	public File getNpc2() {
		return npcImage2;
	}

	private File pxaFile;

	public File getPxa() {
		return pxaFile;
	}

	// changeable
	private boolean changed = false;

	public final boolean isTemp;

	private ResourceManager iMan;
	private GameInfo exeData;

	// convenience method
	public BlConfig getConfig() {
		return exeData.getConfig();
	}

	public MapInfo(GameInfo eDat, ResourceManager r, int mapNum) {
		this(eDat, r, eDat.getMapdata(mapNum), false);
	}

	public MapInfo(GameInfo eDat, ResourceManager r, Mapdata d) {
		this(eDat, r, d, true);
	}
	
	private MapInfo(GameInfo eDat, ResourceManager r, Mapdata d, boolean temp) {
		pcs = new PropertyChangeSupport(this);
		isTemp = temp;
		mapNumber = d.getMapnum();
		iMan = r;
		exeData = eDat;
		File directory = exeData.getDataDirectory();

		loadImageResource(d, directory);

		File pxa = new File(directory + "/Stage/" + d.getTileset() + ".pxa"); //$NON-NLS-1$ //$NON-NLS-2$
		pxaFile = ResourceManager.checkBase(pxa);
		if (EditorApp.EDITOR_MODE != 0) {
			int tilesetW = iMan.getImgW(tileset) / getConfig().getTileSize();
			int tilesetH = iMan.getImgH(tileset) / getConfig().getTileSize();
			iMan.addPxa(pxaFile, tilesetW * tilesetH);
		} else {
			iMan.addPxa(pxaFile, 256);
		}

		loadMap(d);

		// load the pxe
		getEntities(d, directory);
		undoMan = new UndoManager();
		undoMan.setLimit(1000);
	}

	private void loadImageResource(Mapdata d, File directory) {
		// load each image resource
		tileset = new File(directory + "/Stage/Prt" + d.getTileset() + exeData.getImgExtension()); //$NON-NLS-1$
		iMan.addImage(tileset, 1);

		bgImage = new File(directory + "/" + d.getBG() + exeData.getImgExtension()); //$NON-NLS-1$
		iMan.addImage(bgImage, 0);
		npcImage1 = new File(directory + "/Npc/Npc" + d.getNPC1() + exeData.getImgExtension()); //$NON-NLS-1$
		iMan.addImage(npcImage1, 1);
		npcImage2 = new File(directory + "/Npc/Npc" + d.getNPC2() + exeData.getImgExtension()); //$NON-NLS-1$
		iMan.addImage(npcImage2, 1);
	}

	protected void loadMap(Mapdata d) {
		// load the map data
		byte pxmVersion;
		ByteBuffer mapBuf;
		ByteBuffer lineBuf = null;
		ByteBuffer polyBuf = null;
		File directory = exeData.getDataDirectory();
		try {
			File currentFile;
			if (EditorApp.EDITOR_MODE == 2) {
				currentFile = new File(directory + "/Stage/" + d.getFile() + ".nxm"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				currentFile = new File(directory + "/Stage/" + d.getFile() + ".pxm"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			currentFile = ResourceManager.checkBase(currentFile);

			if (!currentFile.exists())
				writeDummyPxm(currentFile);

			FileInputStream inStream = new FileInputStream(currentFile);
			FileChannel inChan = inStream.getChannel();
			ByteBuffer hBuf = ByteBuffer.allocate(8);
			hBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(hBuf);
			// read the filetag
			hBuf.flip();
			byte tagArray[] = new byte[3];
			hBuf.get(tagArray, 0, 3);
			if (!(new String(tagArray).equals("PXM"))) { //$NON-NLS-1$
				inChan.close();
				inStream.close();
				throw new IOException(Messages.getString("MapInfo.9")); //$NON-NLS-1$
			}
			pxmVersion = hBuf.get();
			mapX = hBuf.getShort();
			mapY = hBuf.getShort();

			switch (pxmVersion) {
			case 0x10:
				mapBuf = ByteBuffer.allocate(mapY * mapX);
				break;
			case 0x20:
				mapBuf = ByteBuffer.allocate(mapY * mapX * 4);
				break;
			case 0x30:
				mapBuf = ByteBuffer.allocate(mapY * mapX * 4);
				break;
			case 0x31:
			case 0x32:
			case 0x21:
				mapBuf = ByteBuffer.allocate(mapY * mapX * 4 * 2); // shorts
				break;
			case 0x33:
				mapBuf = ByteBuffer.allocate(mapY * mapX * 5 * 2); // shorts
				break;
			default:
				mapBuf = ByteBuffer.allocate(mapY * mapX);
			}
			mapBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(mapBuf);
			if (pxmVersion >= 0x30) {
				ByteBuffer lineCount = ByteBuffer.allocate(4);
				lineCount.order(ByteOrder.LITTLE_ENDIAN);
				inChan.read(lineCount);
				int nLines = lineCount.getInt(0);
				lineBuf = ByteBuffer.allocate(nLines * 20); // 5 ints per line, 4 bytes per int
				lineBuf.order(ByteOrder.LITTLE_ENDIAN);
				inChan.read(lineBuf);
				lineBuf.flip();
			}
			if (pxmVersion >= 0x32) {
				ByteBuffer polyCount = ByteBuffer.allocate(8);
				polyCount.order(ByteOrder.LITTLE_ENDIAN);
				inChan.read(polyCount);
				polyCount.flip();
				polyBuf = ByteBuffer.allocate(polyCount.getInt());
				polyBuf.order(ByteOrder.LITTLE_ENDIAN);
				inChan.read(polyBuf);
				polyBuf.flip();

			}
			inChan.close();
			inStream.close();
			mapBuf.flip();

		} catch (IOException e) {
			StrTools.msgBox(Messages.getString("MapInfo.10") + directory + "/Stage/" + d.getFile() //$NON-NLS-1$ //$NON-NLS-2$
					+ ".pxm"); //$NON-NLS-1$
			mapX = 21;
			mapY = 16;
			if (EditorApp.EDITOR_MODE == 0) {
				pxmVersion = 0x10;
				mapBuf = ByteBuffer.allocate(mapY * mapX);
			} else {
				pxmVersion = 0x20;
				mapBuf = ByteBuffer.allocate(mapY * mapX * 4);
			}
			System.err.println(Messages.getString("MapInfo.0")); //$NON-NLS-1$

		}
		map = new int[EditorApp.NUM_LAYER][mapY][mapX];
		switch (pxmVersion) {
		case 0x10: // original PXM
		// needs pxa file
		{
			for (int y = 0; y < mapY; y++)
				for (int x = 0; x < mapX; x++) {
					int next = 0xFF & mapBuf.get();
					if (calcPxa(next) < 0x20)
						map[1][y][x] = next;
					else
						map[2][y][x] = next;

					// map[4][y][x] = (byte) tilePane.calcPxa(next);
				}
		}
			break;
		case 0x20: // KS PXM V1 w/ 4 layers
		{
			// needs pxa file
			for (int layer = 0; layer < 4; layer++) {
				for (int y = 0; y < mapY; y++) {
					for (int x = 0; x < mapX; x++)
						map[layer][y][x] = mapBuf.get() & 0xFF;
				}
			}
		}
			break;
		case 0x21: {
			// needs pxa file
			for (int layer = 0; layer < 4; layer++) {
				for (int y = 0; y < mapY; y++) {
					for (int x = 0; x < mapX; x++)
						map[layer][y][x] = mapBuf.getShort() & 0xFFFF;
				}
			}
		}
			break;
		case 0x30: // MR PXM w/ 4 layers + lines
			// doesn't need pxa file
			for (int layer = 0; layer < 4; layer++) {
				for (int y = 0; y < mapY; y++) {
					for (int x = 0; x < mapX; x++)
						map[layer][y][x] = mapBuf.get() & 0xFF;
				}
			}
			while (lineBuf.hasRemaining()) {
				Point p1 = new Point(lineBuf.getInt(), lineBuf.getInt());
				Point p2 = new Point(lineBuf.getInt(), lineBuf.getInt());
				LineSeg seg = new LineSeg(p1, p2, lineBuf.getInt());
				nodeVec.add(seg);
			}
			break;
		case 0x31:
		case 0x32:
			// doesn't need pxa file
			for (int layer = 0; layer < 4; layer++) {
				for (int y = 0; y < mapY; y++) {
					for (int x = 0; x < mapX; x++)
						map[layer][y][x] = mapBuf.getShort() & 0xFFFF;
				}
			}
			while (lineBuf != null && lineBuf.hasRemaining()) {
				Point p1 = new Point(lineBuf.getInt(), lineBuf.getInt());
				Point p2 = new Point(lineBuf.getInt(), lineBuf.getInt());
				LineSeg seg = new LineSeg(p1, p2, lineBuf.getInt());
				nodeVec.add(seg);
			}
			if (pxmVersion >= 0x32) {
				while (polyBuf != null && polyBuf.hasRemaining()) {
					short type = polyBuf.getShort();
					short event = polyBuf.getShort();
					short pointCount = polyBuf.getShort();
					MapPoly current = new MapPoly(new Point(polyBuf.getInt(), polyBuf.getInt()),
							ca.noxid.lab.tile.TypeConfig.getType(type));
					for (int i = 1; i < pointCount; i++) {
						current.extend(new Point(polyBuf.getInt(), polyBuf.getInt()));
					}
					current.setEvent(event);
					polygons.add(current);
				}
			}
			break;
		case 0x33:
			for (int layer = 0; layer < 5; layer++) {
				for (int y = 0; y < mapY; y++) {
					for (int x = 0; x < mapX; x++)
						map[layer][y][x] = mapBuf.getShort() & 0xFFFF;
				}
				while (lineBuf != null && lineBuf.hasRemaining()) {
					Point p1 = new Point(lineBuf.getInt(), lineBuf.getInt());
					Point p2 = new Point(lineBuf.getInt(), lineBuf.getInt());
					LineSeg seg = new LineSeg(p1, p2, lineBuf.getInt());
					nodeVec.add(seg);
				}
				while (polyBuf != null && polyBuf.hasRemaining()) {
					short type = polyBuf.getShort();
					short event = polyBuf.getShort();
					short pointCount = polyBuf.getShort();
					MapPoly current = new MapPoly(new Point(polyBuf.getInt(), polyBuf.getInt()),
							ca.noxid.lab.tile.TypeConfig.getType(type));
					for (int i = 1; i < pointCount; i++) {
						current.extend(new Point(polyBuf.getInt(), polyBuf.getInt()));
					}
					current.setEvent(event);
					polygons.add(current);
				}
			}
		default: // Unknown
			break;
		}
	}

	private void writeDummyPxm(File currentFile) throws IOException {
		FileOutputStream out = new FileOutputStream(currentFile);
		FileChannel chan = out.getChannel();
		byte[] pxmTag = { 'P', 'X', 'M', 0x10 };
		ByteBuffer mapBuf;
		switch (EditorApp.EDITOR_MODE) {
		default: // default
			mapBuf = ByteBuffer.allocate(21 * 16 + 4);
			mapBuf.order(ByteOrder.LITTLE_ENDIAN);
			mapBuf.putShort(0, (short) 21);
			mapBuf.putShort(2, (short) 16);
			break;
		case 1: // cs+ / ks
			pxmTag[3] = 0x20;
			mapBuf = ByteBuffer.allocate(21 * 16 * 4 + 4);
			mapBuf.order(ByteOrder.LITTLE_ENDIAN);
			mapBuf.putShort(0, (short) 21);
			mapBuf.putShort(2, (short) 16);
			break;
		case 2: // mr
			pxmTag[3] = 0x30;
			mapBuf = ByteBuffer.allocate(21 * 16 * 4 + 8);
			mapBuf.order(ByteOrder.LITTLE_ENDIAN);
			mapBuf.putShort(0, (short) 21);
			mapBuf.putShort(2, (short) 16);
		}
		ByteBuffer tagBuf = ByteBuffer.wrap(pxmTag);
		chan.write(tagBuf);
		chan.write(mapBuf);
		out.close();
	}

	// load pxe file
	private void getEntities(Mapdata d, File directory) {

		pxeList = new LinkedList<>();
		try {
			File currentFile = new File(directory + "/Stage/" + d.getFile() + ".pxe"); //$NON-NLS-1$ //$NON-NLS-2$
			currentFile = ResourceManager.checkBase(currentFile);
			FileInputStream inStream = new FileInputStream(currentFile);
			FileChannel inChan = inStream.getChannel();
			ByteBuffer hBuf = ByteBuffer.allocate(6);
			hBuf.order(ByteOrder.LITTLE_ENDIAN);

			inChan.read(hBuf);
			hBuf.flip();
			int nEnt;
			ByteBuffer eBuf;
			switch (hBuf.get(3)) {
			case 0: // original pxe
				nEnt = hBuf.getShort(4);
				eBuf = ByteBuffer.allocate(nEnt * 12 + 2);
				eBuf.order(ByteOrder.LITTLE_ENDIAN);
				inChan.read(eBuf);
				eBuf.flip();
				eBuf.getShort(); // discard this value
				for (int i = 0; i < nEnt; i++) {
					int pxeX = eBuf.getShort();
					int pxeY = eBuf.getShort();
					int pxeFlagID = eBuf.getShort();
					int pxeEvent = eBuf.getShort();
					int pxeType = eBuf.getShort();
					int pxeFlags = eBuf.getShort() & 0xFFFF;
					PxeEntry p = new PxeEntry(pxeX, pxeY, pxeFlagID, pxeEvent, pxeType, pxeFlags, 1);
					p.filePos = i;
					pxeList.add(p);
				}
				break;
			case 0x10: // kss pxe
				nEnt = hBuf.getShort(4);
				eBuf = ByteBuffer.allocate(nEnt * 13);
				eBuf.order(ByteOrder.LITTLE_ENDIAN);
				inChan.read(eBuf);
				eBuf.flip();
				for (int i = 0; i < nEnt; i++) {
					int pxeX = eBuf.getShort();
					int pxeY = eBuf.getShort();
					int pxeFlagID = eBuf.getShort();
					int pxeEvent = eBuf.getShort();
					int pxeType = eBuf.getShort();
					int pxeFlags = eBuf.getShort();
					byte pxeLayer = eBuf.get();
					PxeEntry p = new PxeEntry(pxeX, pxeY, pxeFlagID, pxeEvent, pxeType, pxeFlags, pxeLayer);
					p.filePos = i;
					pxeList.add(p);
				}
				break;
			default:
				System.err.println(Messages.getString("MapInfo.15")); //$NON-NLS-1$
				inChan.close();
				inStream.close();
				throw new IOException();
			}
			inChan.close();
			inStream.close();
		} catch (IOException e) {
			System.err.println(Messages.getString("MapInfo.16") + directory + "/Stage/" + d.getFile()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	// yes.
	public EntityData getEntityInfo(int eNum) {
		return exeData.getEntityInfo(eNum);
	}

	public class PxeEntry implements Comparable<PxeEntry>, Cloneable {
		private short xTile;

		public int getX() {
			return xTile;
		}

		private short yTile;

		public int getY() {
			return yTile;
		}

		private short flagID;

		public int getFlagID() {
			return flagID;
		}

		public void setFlagID(int id) {
			flagID = (short) id;
			markChanged();
		}

		private short eventNum;

		public int getEvent() {
			return eventNum;
		}

		public void setEvent(int num) {
			eventNum = (short) num;
			markChanged();
		}

		private short entityType;

		public int getType() {
			return entityType;
		}

		// set method below
		private short flags;

		public int getFlags() {
			return flags;
		}

		public void setFlags(int num) {
			flags = (short) num;
			markChanged();
		}

		private byte layer;
		private int filePos;

		public int getOrder() {
			return filePos;
		}

		public void setOrder(int num) {
			filePos = num;
			markChanged();
		}

		public static final int DRAW_SPRITE = 1;
		public static final int DRAW_BOX = 2;
		public static final int DRAW_NAME = 4;
		public static final int DRAW_ALL = 7;
		public static final int DRAW_SELECTED = 8;

		// TODO this is bad
		private EntityData inf;

		public EntityData getInfo() {
			return inf;
		}

		PxeEntry(int pxeX, int pxeY, int pxeFlagID, int pxeEvent, int pxeType, int pxeFlags, int pxeLayer) {
			xTile = (short) pxeX;
			yTile = (short) pxeY;
			flagID = (short) pxeFlagID;
			eventNum = (short) pxeEvent;
			entityType = (short) pxeType;
			flags = (short) pxeFlags;
			layer = (byte) pxeLayer;
			filePos = -1;

			inf = exeData.getEntityInfo(entityType);
			if (inf == null) {
				StrTools.msgBox("Warning! There is an entity on your map"
						+ " with an ID that does not exist in the entity table <" + entityType + ">");
			}
		}

		public PxeEntry clone() {
			return new PxeEntry(this.xTile, this.yTile, this.flagID, this.eventNum, this.entityType, this.flags,
					this.layer);
		}

		public void draw(Graphics2D g2d, int flags) {

			int scale = (int) (exeData.getConfig().getEntityRes() * EditorApp.mapScale);
			int tileScale = (int) (exeData.getConfig().getTileSize() * EditorApp.mapScale);
			int rectX = xTile * scale;
			int rectY = yTile * scale;
			if (((flags & DRAW_SPRITE) != 0) && (inf != null)) {
				Rectangle frameRect = inf.getFramerect();
				BufferedImage srcImg;
				int tilesetNum = inf.getTileset();
				if (tilesetNum == 0x15)
					srcImg = iMan.getImg(npcImage1);
				else if (tilesetNum == 0x16)
					srcImg = iMan.getImg(npcImage2);
				else if (tilesetNum == 0x14) // npc sym
					srcImg = iMan.getImg(exeData.getNpcSym());
				else if (tilesetNum == 0x17) // npc regu
					srcImg = iMan.getImg(exeData.getNpcRegu());
				else if (tilesetNum == 0x2) // map tileset
					srcImg = iMan.getImg(tileset);
				else if (tilesetNum == 0x10) // npc myChar
					srcImg = iMan.getImg(exeData.getMyCharFile());
				else
					srcImg = null;

				if (srcImg != null) {
					// TODO make an entityInfo.txt that has the right frames... or maybe not
					int srcX = frameRect.x;
					int srcY = frameRect.y;
					int srcX2 = frameRect.width;
					int srcY2 = frameRect.height;
					if (exeData.getConfig().getTileSize() == 16) {
						srcX /= 2;
						srcY /= 2;
						srcX2 /= 2;
						srcY2 /= 2;
					}
					Rectangle dest = getDrawArea();
					g2d.drawImage(srcImg, dest.x, dest.y, dest.x + dest.width, dest.y + dest.height, srcX, srcY, srcX2,
							srcY2, null);
				}
			} // draw sprite

			if ((flags & DRAW_BOX) != 0) {
				Graphics2D gBox = (Graphics2D) g2d.create();
				if ((flags & DRAW_SELECTED) != 0) {
					gBox.setColor(Color.cyan);
				} else {
					gBox.setColor(Color.green);
				}

				gBox.drawRect(rectX + 1, rectY + 1, tileScale - 2, tileScale - 2);
				if (tileScale >= 32) {
					gBox.drawRect(rectX + 2, rectY + 2, scale - 4, scale - 4);
				}

				if ((flags & DRAW_SELECTED) != 0) {
					// 'sprite frame' rect
					gBox.setColor(Color.yellow);
					Rectangle area = getDrawArea();
					gBox.drawRect(area.x, area.y, area.width, area.height);
					// sprite hitbox rect
					gBox.setColor(Color.red);
					area = getHitArea();
					gBox.drawRect(area.x, area.y, area.width, area.height);
				}

			} // draw box

			if ((flags & DRAW_NAME) != 0) {
				Graphics2D gTxtWhi = (Graphics2D) g2d.create();
				Graphics2D gTxtBlk = (Graphics2D) g2d.create();
				gTxtWhi.setFont(new Font("Small Fonts", Font.PLAIN, tileScale / 3)); //$NON-NLS-1$
				gTxtWhi.setColor(Color.white);
				gTxtBlk.setFont(new Font("Small Fonts", Font.PLAIN, tileScale / 3)); //$NON-NLS-1$
				gTxtBlk.setColor(Color.DARK_GRAY);
				if (inf != null) {
					gTxtBlk.drawString(inf.getShort1(), rectX + 4, rectY + tileScale / 3 + 1);
					gTxtBlk.drawString(inf.getShort2(), rectX + 4, rectY + 2 * tileScale / 3 + 1);
					gTxtWhi.drawString(inf.getShort1(), rectX + 3, rectY + tileScale / 3);
					gTxtWhi.drawString(inf.getShort2(), rectX + 3, rectY + 2 * tileScale / 3);
				}
			} // draw name
		}

		public ByteBuffer toBuf() {
			int size = 12;
			// ! comment out for Tyrone's builds
			// if (EditorApp.EDITOR_MODE >= 1)
			// size++;
			ByteBuffer retVal = ByteBuffer.allocate(size);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.putShort(xTile);
			retVal.putShort(yTile);
			retVal.putShort(flagID);
			retVal.putShort(eventNum);
			retVal.putShort(entityType);
			retVal.putShort(flags);
			// if (EditorApp.EDITOR_MODE >= 1)
			// retVal.put(layer);
			retVal.flip();
			return retVal;
		}

		@Override
		public int compareTo(PxeEntry other) {
			Integer fp = filePos;
			return fp.compareTo(other.filePos);
		}

		public Rectangle getDrawArea() {
			int sc = exeData.getConfig().getTileSize();
			int res = exeData.getConfig().getEntityRes();
			int big = res;
			if (big < exeData.getConfig().getTileSize()) {
				big = exeData.getConfig().getTileSize();
			}
			double mapsc = EditorApp.mapScale;
			Rectangle offset;
			if (inf != null) {
				offset = inf.getDisplay();
			} else {
				offset = new Rectangle(16, 16, 16, 16);
			}
			int offL = offset.x;
			int offU = offset.y;
			int offR = offset.width;
			int offD = offset.height;
			int destW = offR + offL;
			destW *= mapsc;
			int destH = offD + offU;
			destH *= mapsc;
			offU -= sc / 2;
			offL -= sc / 2;
			int destX = xTile * res - offL;
			destX *= mapsc;
			int destY = yTile * res - offU;
			destY *= mapsc;

			Rectangle area = new Rectangle(destX, destY, destW, destH);
			area.add(new Rectangle((int) (xTile * res * mapsc), (int) (yTile * res * mapsc), (int) (big * mapsc + 2),
					(int) (big * mapsc + 2)));
			return area;
		}

		private Rectangle getHitArea() {
			int sc = exeData.getConfig().getTileSize();
			int res = exeData.getConfig().getEntityRes();
			Rectangle offset;
			if (inf == null) {
				offset = new Rectangle(8, 8, 8, 8);
			} else {
				offset = inf.getHit();
			}
			int offL = offset.x;
			int offU = offset.y;
			int offR = offset.width;
			int offD = offset.height;
			if (sc != 16) {
				offL *= 2;
				offU *= 2;
				offR *= 2;
				offD *= 2;
			}
			int destW = offR + offL;
			destW *= EditorApp.mapScale;
			int destH = offD + offU;
			destH *= EditorApp.mapScale;
			offU -= sc / 2;
			offL -= sc / 2;
			int destX = xTile * res - offL;
			destX *= EditorApp.mapScale;
			int destY = yTile * res - offU;
			destY *= EditorApp.mapScale;

			return new Rectangle(destX, destY, destW, destH);
		}

		public void shift(int x, int y) {
			xTile += x;
			yTile += y;
		}

		public void setType(int id) {
			entityType = (short) id;
			inf = exeData.getEntityInfo(id);
		}
	}

	@Override
	public boolean isModified() {
		return changed;
	}

	@Override
	public void markUnchanged() {
		if (changed) {
			changed = false;
			pcs.firePropertyChange(Changeable.PROPERTY_EDITED, true, false);
		}
	}

	@Override
	public void markChanged() {
		if (!changed) {
			changed = true;
			pcs.firePropertyChange(Changeable.PROPERTY_EDITED, false, true);
		}
	}

	public void resizeMap(int x, int y) {
		int minX = getConfig().getMapMinX();
		int minY = getConfig().getMapMinY();
		if (x < minX)
			x = minX;
		else if (x > MAX_TILES_X)
			x = MAX_TILES_X;
		if (y < minY)
			y = minY;
		else if (y > MAX_TILES_Y)
			y = MAX_TILES_Y;
		int[][][] newArray = new int[EditorApp.NUM_LAYER][y][x];
		for (int l = 0; l < map.length; l++) {
			for (int c = 0; c < ((c < mapY) ? y : mapY); c++) {
				for (int r = 0; r < ((r < mapX) ? x : mapX); r++) {
					try {
						newArray[l][c][r] = map[l][c][r];
					} catch (ArrayIndexOutOfBoundsException err) {
						System.out.println("nope " + l + " " + r + " " + c); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		}
		mapX = x;
		mapY = y;
		undoMan.addEdit(new ResizeEdit(map, newArray));
		map = newArray;
	}

	public void shiftMap(int dx, int dy, int options) {

		undoMan.addEdit(new MapEdit(0, 0, null, null, 0));
		// apply shift
		int w = mapX;
		int h = mapY;
		Iterator<PxeEntry> pxeIt = pxeList.iterator();
		if ((options & ShiftDialog.OPTION_ENTITY) != 0) {
			while (pxeIt.hasNext()) {

				PxeEntry entity = pxeIt.next();
				if ((options & ShiftDialog.OPTION_WRAP) != 0) {
					int newX = (entity.xTile + dx) % w;
					int newY = (entity.yTile + dy) % h;

					if (newX < 0)
						newX += w;
					if (newY < 0)
						newY += h;

					Point delta = new Point(newX - entity.xTile, newY - entity.yTile);
					entity.xTile += delta.x;
					entity.yTile += delta.y;
					EntityEdit edit = new EntityEdit(EntityEdit.EDIT_MOVE, entity, delta);
					edit.setSignificant(false);
					undoMan.addEdit(edit);

				} else {
					Point lastPos = new Point(entity.xTile, entity.yTile);
					if (entity.xTile + dx < 0) {
						entity.xTile = 0;
					} else {
						if (entity.xTile + dx >= w)
							entity.xTile = (short) (w - 1);
						else
							entity.xTile += dx;
					}

					if (entity.yTile < 0) {
						entity.yTile = 0;
					} else {
						if (entity.yTile + dy >= h)
							entity.yTile = (short) (h - 1);
						else
							entity.yTile += dy;
					}
					Point delta = new Point(entity.xTile - lastPos.x, entity.yTile - lastPos.y);
					EntityEdit edit = new EntityEdit(EntityEdit.EDIT_MOVE, entity, delta);
					edit.setSignificant(false);
					undoMan.addEdit(edit);
				}
			} // for each entity
		}

		if ((options & ShiftDialog.OPTION_TILE) != 0) {
			for (int layer = 0; layer < map.length; layer++) {
				int[][] layerDat = map[layer];
				int[][] oldDat;
				int[][] newDat;
				oldDat = new int[mapX][mapY];
				for (int dx1 = 0; dx1 < mapX; dx1++) {
					for (int dy1 = 0; dy1 < mapY; dy1++) {
						oldDat[dx1][dy1] = getTile(dx1, dy1, layer);
					}
				}
				// Wistil was being lazy here, using memory to make all the math go away
				// also I stole this from CE basically
				int i, j;
				int[][] newTiles;
				newTiles = new int[h][w];

				for (i = 0; i < h; i++) {
					for (j = 0; j < w; j++) {
						int newX = (j + dx + w * 100) % w;
						int newY = (i + dy + h * 100) % h;
						try {
							if ((options & ShiftDialog.OPTION_WRAP) != 0) {// well some math anyways...
								newTiles[newY][newX] = layerDat[i][j];
							} else {
								if (j + dx < 0 || j + dx >= w || i + dy < 0 || i + dy >= h)
									newTiles[newY][newX] = 0;
								else
									newTiles[newY][newX] = layerDat[i][j];
							}
						} catch (IndexOutOfBoundsException err) {
							System.out.println(layer + ": (" + newX + ", " + newY + ") - (" + i + ", " + j + ")");
						}
					}
				}
				map[layer] = newTiles;
				newDat = new int[mapX][mapY];
				for (int dx1 = 0; dx1 < mapX; dx1++) {
					for (int dy1 = 0; dy1 < mapY; dy1++) {
						newDat[dx1][dy1] = getTile(dx1, dy1, layer);
					}
				}
				MapEdit edit = new MapEdit(0, 0, oldDat, newDat, layer);
				edit.setSignificant(false);
				undoMan.addEdit(edit);
			} // for each layer
		}

		if ((options & ShiftDialog.OPTION_LINE) != 0) {
			int shiftX = dx * getConfig().getLineRes();
			int shiftY = dy * getConfig().getLineRes();
			for (LineSeg l : nodeVec) {
				Point2D lp1 = l.getP1();
				lp1.setLocation(lp1.getX() + shiftX, lp1.getY() + shiftY);
				Point2D lp2 = l.getP2();
				lp2.setLocation(lp2.getX() + shiftX, lp2.getY() + shiftY);
				l.setLine(lp1.getX(), lp1.getY(), lp2.getX(), lp2.getY());
			}
		}
		// mark edits with a significant edit
		// I don't know why but this is the only thing that works I sware
		undoMan.addEdit(new MapEdit(0, 0, null, null, 0));
	}

	/*
	public byte getTileB(int x, int y, int layer) {
		byte rVal = Byte.MIN_VALUE;
		try {
			if (EditorApp.EDITOR_MODE == 0) {
				rVal = map[1][y][x];
	            rVal += map[2][y][x];
			} else {
				rVal = map[layer][y][x];
			}
		} catch (ArrayIndexOutOfBoundsException err) {
			//ignore
		}
		return rVal;
	}
	*/

	public int getTile(int x, int y, int layer) {
		int rVal = 0;
		try {
			if (EditorApp.EDITOR_MODE == 0 && layer == -1) {
				rVal = map[1][y][x];
				rVal += map[2][y][x];
				// rVal &= 0xFF;
			} else {
				rVal = map[layer][y][x];
			}
		} catch (ArrayIndexOutOfBoundsException err) {
			// err.printStackTrace();
		}
		return rVal;
	}

	public void putTile(int x, int y, int newData, int layer) {
		System.out.println("Putting tile " + newData + " at x:" + x + ",y:" + y + ",layer:" + layer);
		int oldData = -1;
		if (x >= 0 && y >= 0 && x < mapX && y < mapY) {
			if (EditorApp.EDITOR_MODE == 0 && layer == -1) {
				if (calcPxa(newData) < 0x40) {
					oldData = map[1][y][x];
					map[1][y][x] = newData;
					map[2][y][x] = 0;
				} else {
					oldData = map[2][y][x];
					map[1][y][x] = 0;
					map[2][y][x] = newData;
				}
			} else {
				oldData = map[layer][y][x];
				map[layer][y][x] = newData;
			}
		}
		if (oldData != -1 && oldData != newData)
			markChanged();
	}

	public int calcPxa(int tileNum) {
		byte[] pxaData = iMan.getPxa(pxaFile);
		int rval = 0;
		try {
			rval = pxaData[tileNum];
		} catch (ArrayIndexOutOfBoundsException err) {
			err.printStackTrace();
			// StrTools.msgBox(Messages.getString("MapInfo.23") + tileNum + " " + pxaData.length); //$NON-NLS-1$
		}
		return rval & 0xFF;
	}

	public void save() {
		Mapdata d;
		if (!isTemp)
			d = exeData.getMapdata(mapNumber);
		else
			d = exeData.getMapdataTemp(mapNumber);
		File pxmFile;
		if (EditorApp.EDITOR_MODE == 2) {
			pxmFile = new File(exeData.getDataDirectory() + "/Stage/" + d.getFile() + ".nxm"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			pxmFile = new File(exeData.getDataDirectory() + "/Stage/" + d.getFile() + ".pxm"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		File pxeFile = new File(exeData.getDataDirectory() + "/Stage/" + d.getFile() + ".pxe"); //$NON-NLS-1$ //$NON-NLS-2$
		// we can just use our pxaFile field for this, since that's already corrected for CS+
		//File pxaFile = new File(exeData.getDataDirectory() + "/Stage/" + d.getTileset() + ".pxa"); //$NON-NLS-1$ //$NON-NLS-2$
		byte[] pxmTag = { 'P', 'X', 'M', 0x10 };
		byte[] pxeTag = { 'P', 'X', 'E', 0 };
		ByteBuffer headerBuf;
		ByteBuffer mapBuf;

		// save the map
		try {
			FileOutputStream out = new FileOutputStream(pxmFile);
			FileChannel pxmChannel = out.getChannel();
			switch (EditorApp.EDITOR_MODE) {
			case 0: // standard CS editor
				headerBuf = ByteBuffer.wrap(pxmTag);
				pxmChannel.write(headerBuf);
				mapBuf = ByteBuffer.allocate(mapX * mapY + 4);
				mapBuf.order(ByteOrder.LITTLE_ENDIAN);
				mapBuf.putShort((short) mapX);
				mapBuf.putShort((short) mapY);
				for (int y = 0; y < mapY; y++)
					for (int x = 0; x < mapX; x++) {
						mapBuf.put((byte) getTile(x, y, -1));
					}
				break;
			case 1: // KS
				pxmTag[3] = 0x21;
				headerBuf = ByteBuffer.wrap(pxmTag);
				pxmChannel.write(headerBuf);
				mapBuf = ByteBuffer.allocate(mapX * mapY * 4 * 2 + 4);
				mapBuf.order(ByteOrder.LITTLE_ENDIAN);
				mapBuf.putShort((short) mapX);
				mapBuf.putShort((short) mapY);
				for (int layer = 0; layer < 4; layer++)
					for (int y = 0; y < mapY; y++)
						for (int x = 0; x < mapX; x++) {
							mapBuf.putShort((short) getTile(x, y, layer));
						}
				break;
			case 2: // MR
				pxmTag[3] = 0x33;
				headerBuf = ByteBuffer.wrap(pxmTag);
				pxmChannel.write(headerBuf);
				ByteBuffer preBuf = ByteBuffer.allocate(mapX * mapY * 5 * 2 + 4);
				preBuf.order(ByteOrder.LITTLE_ENDIAN);
				preBuf.putShort((short) mapX);
				preBuf.putShort((short) mapY);
				for (int layer = 0; layer < 5; layer++)
					for (int y = 0; y < mapY; y++)
						for (int x = 0; x < mapX; x++) {
							preBuf.putShort((short) getTile(x, y, layer));
						}
				preBuf.flip();
				// save lines
				LinkedList<LineSeg> processed = new LinkedList<>();
				for (LineSeg i : nodeVec) {
					if (i.getP2() != null && !processed.contains(i)) {
						processed.add(i);
					}
				}
				ByteBuffer lineBuf = ByteBuffer.allocate(preBuf.capacity() + processed.size() * 20 + 4);
				lineBuf.order(ByteOrder.LITTLE_ENDIAN);
				lineBuf.put(preBuf);
				lineBuf.putInt(processed.size());
				for (LineSeg i : processed) {
					lineBuf.putInt((int) i.getX1());
					lineBuf.putInt((int) i.getY1());
					lineBuf.putInt((int) i.getX2());
					lineBuf.putInt((int) i.getY2());
					lineBuf.putInt(i.getType());
				}
				lineBuf.flip();
				// save polygons
				LinkedList<MapPoly> savablePolys = new LinkedList<>();
				int polySize = 8;
				for (MapPoly p : polygons) {
					if (p.getPoints().size() > 2) {
						savablePolys.add(p);
						polySize += 6 + p.getPoints().size() * 8;
					}
				}
				ByteBuffer polyBuf = ByteBuffer.allocate(lineBuf.capacity() + polySize);
				polyBuf.order(ByteOrder.LITTLE_ENDIAN);
				polyBuf.put(lineBuf);
				polyBuf.putInt(savablePolys.size());
				polyBuf.putInt(polySize - 8);
				for (MapPoly p : savablePolys) {
					polyBuf.putShort(p.getType());
					polyBuf.putShort(p.getEvent());
					polyBuf.putShort((short) p.getPoints().size());
					if (p.isFlipped()) {
						for (int i = p.getPoints().size(); --i >= 0;) {
							polyBuf.putInt(p.getPoints().get(i).x);
							polyBuf.putInt(p.getPoints().get(i).y);
						}
					} else {
						for (Point point : p.getPoints()) {
							polyBuf.putInt(point.x);
							polyBuf.putInt(point.y);
						}
					}
				}
				mapBuf = polyBuf;
				break;
			default:
				out.close();
				return;
			}
			mapBuf.flip();
			pxmChannel.write(mapBuf);
			pxmChannel.close();
			out.close();
		} catch (IOException err) {
			StrTools.msgBox("Error saving .pxm file. (check read-only?)");
			err.printStackTrace();
		}

		// save the entities
		try {
			// sort by order
			Collections.sort(pxeList);
			FileOutputStream out = new FileOutputStream(pxeFile);
			FileChannel pxeChannel = out.getChannel();
			headerBuf = ByteBuffer.wrap(pxeTag);
			pxeChannel.write(headerBuf);
			ByteBuffer dumbBuf = ByteBuffer.allocate(4);
			dumbBuf.order(ByteOrder.LITTLE_ENDIAN);
			dumbBuf.putShort((short) pxeList.size());
			dumbBuf.putShort((short) 0);
			dumbBuf.flip();
			pxeChannel.write(dumbBuf);
			Collections.sort(pxeList);
			for (PxeEntry nextEntry : pxeList) {
				pxeChannel.write(nextEntry.toBuf());
			}
			pxeChannel.close();
			out.close();
		} catch (IOException e) {
			StrTools.msgBox("Error saving .pxe file. (check read-only?)");
			e.printStackTrace();
		}

		// save the pxa
		if (EditorApp.EDITOR_MODE != 2)
			iMan.savePxa(pxaFile);
		markUnchanged();
	}

	public class MapEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = -4946946031855265785L;
		int layer;
		int[][] oldData;
		int[][] newData;
		int xOrigin;
		int yOrigin;

		private boolean signif = true;

		public void setSignificant(boolean b) {
			signif = b;
		}

		/**
		 * An undoable edit that applies to a square section of tiles
		 * 
		 * @param x
		 *            X pos of the block to replace
		 * @param y
		 *            Y pos of the block to replace
		 * @param oldDat
		 *            The data being replaced (for undo purposes)
		 * @param newDat
		 *            The data replacing (for redo purposes)
		 * @param l
		 *            The layer this applies to
		 */
		public MapEdit(int x, int y, int[][] oldDat, int[][] newDat, int l) {
			layer = l;
			oldData = oldDat;
			newData = newDat;
			xOrigin = x;
			yOrigin = y;
			markChanged();
		}

		@Override
		public boolean isSignificant() {
			return signif;
		}

		@Override
		public boolean canRedo() {
			return true;
		}

		@Override
		public void redo() {
			if (newData == null)
				return;
			int w = newData.length;
			int h = newData[0].length;
			for (int dx = 0; dx < w; dx++) {
				for (int dy = 0; dy < h; dy++) {
					putTile(xOrigin + dx, yOrigin + dy, newData[dx][dy], layer);
				}
			}
			// redrawTiles(xOrigin, yOrigin, xOrigin + w, yOrigin + h);
		}

		@Override
		public void undo() {
			if (oldData == null)
				return;
			int w = oldData.length;
			int h = oldData[0].length;
			for (int dx = 0; dx < w; dx++) {
				for (int dy = 0; dy < h; dy++) {
					putTile(xOrigin + dx, yOrigin + dy, oldData[dx][dy], layer);
				}
			}
			// redrawTiles(xOrigin, yOrigin, xOrigin + w, yOrigin + h);
		}
	}

	class ResizeEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = -3009446899803969175L;
		int[][][] oldMap;
		int[][][] newMap;

		private boolean signif = true;

		@Override
		public boolean isSignificant() {
			return signif;
		}

		ResizeEdit(int[][][] old, int[][][] newm) {
			oldMap = old;
			newMap = newm;
			markChanged();
		}

		@Override
		public boolean canRedo() {
			return true;
		}

		public void redo() {
			if (newMap == null)
				return;

			map = newMap;
			mapY = newMap[0].length;
			mapX = newMap[0][0].length;
			// repaint();
		}

		public void undo() {
			if (newMap == null)
				return;

			map = oldMap;
			mapY = oldMap[0].length;
			mapX = oldMap[0][0].length;
			// repaint();
		}
	}

	public class EntityEdit extends AbstractUndoableEdit {

		public static final int EDIT_MOVE = 0;
		public static final int EDIT_PLACE = 1;
		public static final int EDIT_REMOVE = 2;
		public static final int EDIT_MODIFY = 3;

		PxeEntry entry;
		int changeType;
		Object params;

		private boolean signif = true;

		public void setSignificant(boolean b) {
			signif = b;
		}

		private static final long serialVersionUID = -371877823331946549L;

		/**
		 * An edit that applies to a PxeEntry
		 * 
		 * @param editType
		 *            An enumerated type that specifies what kind of edit
		 * @param entry1
		 *            The entity that was edited
		 * @param parameter
		 *            A parameter, depends on the type of edit.
		 * 
		 *            For editType == EDIT_MOVE, parameter is a point
		 *            that specifies the x and y distance moved
		 * 
		 *            For editType == EDIT_PLACE, parameter should be null (ignored)
		 * 
		 *            For editType == EDIT_REMOVE, parameter should also be null
		 * 
		 *            For editType == EDIT_MODIFY, parameter is a PxeEntry that
		 *            represents the previous state of the entity.
		 */
		public EntityEdit(int editType, PxeEntry entry1, Object parameter) {
			entry = entry1;
			changeType = editType;
			params = parameter;
			markChanged();
		}

		@Override
		public boolean isSignificant() {
			return signif;
		}

		@Override
		public boolean canRedo() {
			return true;
		}

		@Override
		public void undo() {
			switch (changeType) {
			case EDIT_MOVE:
				Point delta = (Point) params;
				entry.xTile -= delta.x;
				entry.yTile -= delta.y;
				break;
			case EDIT_PLACE:
				pxeList.remove(entry);
				break;
			case EDIT_MODIFY:
				PxeEntry lastState = (PxeEntry) params;
				pxeList.remove(entry);
				pxeList.add(lastState);
				break;
			case EDIT_REMOVE:
				pxeList.add(entry);
				break;
			}
		}

		@Override
		public void redo() {
			switch (changeType) {
			case EDIT_MOVE:
				Point delta = (Point) params;
				entry.xTile += delta.x;
				entry.yTile += delta.y;
				break;
			case EDIT_PLACE:
				pxeList.add(entry);
				break;
			case EDIT_MODIFY:
				PxeEntry lastState = (PxeEntry) params;
				pxeList.add(entry);
				pxeList.remove(lastState);
				break;
			case EDIT_REMOVE:
				pxeList.remove(entry);
				break;
			}
		}
	}

	public void doUndo() {
		if (undoMan.canUndo()) {
			undoMan.undo();
			markChanged();
			EditorApp.airhorn();
		}
	}

	public void doRedo() {
		if (undoMan.canRedo()) {
			undoMan.redo();
			markChanged();
			EditorApp.airhorn();
		}
	}

	public void setTileset(File pxaFile2, File tileFile) {
		pxaFile = pxaFile2;
		tileset = tileFile;
		iMan.addImage(tileFile, 1);
		if (EditorApp.EDITOR_MODE != 0) {
			int tilesetW = iMan.getImgW(tileset) / getConfig().getTileSize();
			int tilesetH = iMan.getImgH(tileset) / getConfig().getTileSize();
			iMan.addPxa(pxaFile, tilesetW * tilesetH);
		} else {
			iMan.addPxa(pxaFile, 256);
		}
	}

	public void setNpc1Img(File npcFile) {
		npcImage1 = npcFile;
		iMan.addImage(npcFile, 1);
	}

	public void setNpc2Img(File npcFile) {
		npcImage2 = npcFile;
		iMan.addImage(npcFile, 1);
	}

	public void setBgImg(File bgFile) {
		bgImage = bgFile;
		iMan.addImage(bgFile, 1);
	}

	public void addEdit(UndoableEdit e) {
		if (!e.isSignificant() && !undoMan.canUndo())
			undoMan.addEdit(new MapEdit(0, 0, null, null, 0));
		undoMan.addEdit(e);
		EditorApp.airhorn();
	}

	public void removeEntities(Set<PxeEntry> selectionList) {
		for (PxeEntry e : selectionList) {
			EntityEdit edit = new EntityEdit(EntityEdit.EDIT_REMOVE, e, null);
			edit.setSignificant(false);
			addEdit(edit);
		}
		addEdit(new MapEdit(0, 0, null, null, 0));// significant
		pxeList.removeAll(selectionList);
		this.markChanged();
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param id
	 * @return the new entity created
	 */
	public PxeEntry addEntity(int x, int y, int id) {
		PxeEntry ent = new PxeEntry(x, y, 0, 0, id, 0, 0);
		return addEntity(ent);
	}

	public PxeEntry addEntity(PxeEntry ent) {
		int max = 0;
		for (PxeEntry e : pxeList) {
			if (max < e.getOrder()) {
				max = e.getOrder();
			}
		}
		ent.setOrder(max + 1);
		pxeList.add(ent);
		addEdit(new EntityEdit(EntityEdit.EDIT_PLACE, ent, null));
		this.markChanged();
		return ent;
	}

	public void addPropertyChangeListener(PropertyChangeListener listen) {
		pcs.addPropertyChangeListener(listen);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void addNode(LineSeg newP) {
		nodeVec.add(newP);
	}

	public void clearEntities() {
		pxeList.clear();
		markChanged();
	}

}
