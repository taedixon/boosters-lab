package ca.noxid.lab.mapdata;

import ca.noxid.lab.Changeable;
import ca.noxid.lab.Messages;
import ca.noxid.lab.gameinfo.GameInfo.MOD_TYPE;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.carrotlord.string.StrTools;
/*
typedef struct {
	   char tileset[32];
	   char filename[32];
	   char scrollType[4];
	   char bgName[32];
	   char npc1[32];
	   char npc2[32];
	   char bossNum;
	   char mapName[35];
	}MapData;
*/
public class Mapdata implements Changeable {	
	private PropertyChangeSupport PCS = new PropertyChangeSupport(this);
	
	public static final String P_NAME = "map name"; //$NON-NLS-1$
	public static final String P_FILE = "file name"; //$NON-NLS-1$
	public static final String P_TILE = "tileset"; //$NON-NLS-1$
	public static final String P_NPC1 = "npc set 1"; //$NON-NLS-1$
	public static final String P_NPC2 = "npc set 2"; //$NON-NLS-1$
	public static final String P_BGIMG = "background img"; //$NON-NLS-1$
	public static final String P_SCROLL = "background scroll"; //$NON-NLS-1$
	public static final String P_BOSS = "boss type"; //$NON-NLS-1$
	public static final String P_NUM = "map num"; //$NON-NLS-1$
	
	/*
	public static final String FORMAT_CEMAP = "cemap";
	public static final String FORMAT_KS = "kingstory";
	public static final String FORMAT_CSPLUS = "csplus";
	*/
	
	private String tilesetName;
	public void setTileset(String s) {
		if (!tilesetName.equals(s) && s != null) {
			markChanged();
			String old = tilesetName;
			tilesetName = s;
			this.firePropertyChange(P_TILE, old, s);
		}
	}
	public String getTileset() {return tilesetName;}
	
	private String fileName;
	public void setFile(String s) {
		if (!fileName.equals(s) && s != null) {
			markChanged();
			String old = fileName;
			fileName = s;
			this.firePropertyChange(P_FILE, old, s);
		}
	}
	public String getFile() {return fileName;}
	
	private String bgName;
	public void setBG(String s) {
		if (!bgName.equals(s) && s != null) {
			markChanged();
			String old = bgName;
			bgName = s;
			this.firePropertyChange(P_BGIMG, old, s);
		}
	}
	public String getBG() {return bgName;}
	
	private String npcSet1;
	public void setNPC1(String s) {
		if (!npcSet1.equals(s) && s != null) {
			markChanged();
			String old = npcSet1;
			npcSet1 = s;
			this.firePropertyChange(P_NPC1, old, s);
		}
	}
	public String getNPC1() {return npcSet1;}
	
	private String npcSet2;
	public void setNPC2(String s) {
		if (!npcSet2.equals(s) && s != null) {
			markChanged();
			String old = npcSet2;
			npcSet2 = s;
			this.firePropertyChange(P_NPC2, old, s);
		}
	}
	public String getNPC2() {return npcSet2;}
	
	private String mapName;
	public void setMapname(String s) {
		if (!mapName.equals(s) && s != null) {
			markChanged();
			String old = mapName;
			mapName = s;
			this.firePropertyChange(P_NAME, old, s);
		}
	}
	public String getMapname() {return mapName;}
	
	private byte[] jpName;
	public void setJpName(byte[] b){
		markChanged();
		jpName = b.clone();
	}
	public byte[] getJpName() {return jpName.clone();}
	
	private int scrollType;
	public void setScroll(int n) {
		if (n != scrollType) {
			markChanged();
			int old = scrollType;
			scrollType = n;
			this.firePropertyChange(P_SCROLL, old, n);
		}
	}
	public int getScroll() {return scrollType;}
	
	private int bossNum;
	public void setBoss(int n) {
		if (n != bossNum) {
			markChanged();
			int old = bossNum;
			bossNum = n;
			this.firePropertyChange(P_BOSS, old, n);
		}
	}
	public int getBoss() {return bossNum;}
	
	private int mapNum;
	public void setMapnum(int n) {
		if (n != mapNum) {
			markChanged();
			int old = mapNum;
			mapNum = n;
			this.firePropertyChange(P_NUM, old, n);
		}
	}
	public int getMapnum() {return mapNum;}
	
	private boolean changed = false;
	
	public Mapdata(int num, ByteBuffer buf, MOD_TYPE format, String charEncoding) {
		mapNum = num;
		switch (format) {
		case MOD_CS: // from exe
			/*
			typedef struct {
				   char tileset[32];
				   char filename[32];
				   char scrollType[4];
				   char bgName[32];
				   char npc1[32];
				   char npc2[32];
				   char bossNum;
				   char mapName[35];
				}nMapData;
				*/
			byte[] buffer = new byte[0x23];
			buf.get(buffer, 0, 0x20);
			tilesetName = StrTools.CString(buffer, charEncoding);
			buf.get(buffer, 0, 0x20);
			fileName = StrTools.CString(buffer, charEncoding);
			scrollType = buf.getInt() & 0xFF;						
			buf.get(buffer, 0, 0x20);
			bgName = StrTools.CString(buffer, charEncoding);
			buf.get(buffer, 0, 0x20);
			npcSet1 = StrTools.CString(buffer, charEncoding);
			buf.get(buffer, 0, 0x20);
			npcSet2 = StrTools.CString(buffer, charEncoding);
			bossNum = buf.get();
			buf.get(buffer, 0, 0x23);
			mapName = StrTools.CString(buffer, charEncoding);
			break;
		case MOD_CS_PLUS: // from stage.tbl
			/*
			typedef struct {
				   char tileset[32];
				   char filename[32];
				   char scrollType[4];
				   char bgName[32];
				   char npc1[32];
				   char npc2[32];
				   char bossNum;
				   char jpName[32];
				   char mapName[32];
				}nMapData;
				*/
			byte[] buf32 = new byte[32];
			buf.get(buf32);
			tilesetName = StrTools.CString(buf32, charEncoding);
			buf.get(buf32);
			fileName = StrTools.CString(buf32, charEncoding);
			scrollType = buf.getInt();
			buf.get(buf32);
			bgName = StrTools.CString(buf32, charEncoding);
			buf.get(buf32);
			npcSet1 = StrTools.CString(buf32, charEncoding);
			buf.get(buf32);
			npcSet2 = StrTools.CString(buf32, charEncoding);
			bossNum = buf.get();
			buf.get(buf32);
			jpName = buf32.clone();
			buf.get(buf32);
			mapName = StrTools.CString(buf32, charEncoding);
			break;
		case MOD_MR:
		case MOD_KS:
			// from bin
			byte[] buf16 = new byte[16];
			byte[] nameBuf = new byte[34];
			buf.get(buf16);
			tilesetName = StrTools.CString(buf16, charEncoding);
			buf.get(buf16);
			fileName = StrTools.CString(buf16, charEncoding);
			scrollType = buf.get();
			buf.get(buf16);
			bgName = StrTools.CString(buf16, charEncoding);
			buf.get(buf16);
			npcSet1 = StrTools.CString(buf16, charEncoding);
			buf.get(buf16);
			npcSet2 = StrTools.CString(buf16, charEncoding);
			bossNum = buf.get();
			buf.get(nameBuf);
			mapName = StrTools.CString(nameBuf, charEncoding);
			break;
		case MOD_GUXT:
		case DUMMY:
		default:
			// unknown/unused
			break;
		}
	}
	
	public Mapdata(int num) {
		tilesetName = "0"; //$NON-NLS-1$
		fileName = "myMap";		 //$NON-NLS-1$
		scrollType = 0;
		bgName = "bk0"; //$NON-NLS-1$
		npcSet1 = "0"; //$NON-NLS-1$
		npcSet2 = "0"; //$NON-NLS-1$
		bossNum = 0;
		mapName = Messages.getString("Mapdata.0");		 //$NON-NLS-1$
		mapNum = num;
		jpName = new byte[0x20];
	}

	public ByteBuffer toBuf(MOD_TYPE format, String charEncoding) throws UnsupportedEncodingException {
		ByteBuffer retVal;
		switch (format) {
		case MOD_CS:
			retVal = ByteBuffer.allocate(200);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tilesetName.getBytes(charEncoding), 0, tilesetName.length() < 0x20 ? tilesetName.length() : 0x1F);
			retVal.position(0x20);
			retVal.put(fileName.getBytes(charEncoding), 0, fileName.length() < 0x20 ? fileName.length() : 0x1F);
			retVal.position(0x40);
			retVal.putInt(scrollType);
			retVal.put(bgName.getBytes(charEncoding), 0, bgName.length() < 0x20 ? bgName.length() : 0x1F);
			retVal.position(0x64);
			retVal.put(npcSet1.getBytes(charEncoding), 0, npcSet1.length() < 0x20 ? npcSet1.length() : 0x1F);
			retVal.position(0x84);
			retVal.put(npcSet2.getBytes(charEncoding), 0, npcSet2.length() < 0x20 ? npcSet2.length() : 0x1F);
			retVal.position(0xA4);
			retVal.put((byte) bossNum);
			try {
				byte[] letters = mapName.getBytes(charEncoding);
				retVal.put(letters, 0, letters.length < 0x23 ? letters.length : 0x22);
			} catch (UnsupportedEncodingException ignored) {
			}	
			break;
		case MOD_CS_PLUS:
			retVal = ByteBuffer.allocate(229);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tilesetName.getBytes(charEncoding), 0, tilesetName.length() < 0x20 ? tilesetName.length() : 0x1F);
			retVal.position(0x20);
			retVal.put(fileName.getBytes(charEncoding), 0, fileName.length() < 0x20 ? fileName.length() : 0x1F);
			retVal.position(0x40);
			retVal.putInt(scrollType);
			retVal.put(bgName.getBytes(charEncoding), 0, bgName.length() < 0x20 ? bgName.length() : 0x1F);
			retVal.position(0x64);
			retVal.put(npcSet1.getBytes(charEncoding), 0, npcSet1.length() < 0x20 ? npcSet1.length() : 0x1F);
			retVal.position(0x84);
			retVal.put(npcSet2.getBytes(charEncoding), 0, npcSet2.length() < 0x20 ? npcSet2.length() : 0x1F);
			retVal.position(0xA4);
			retVal.put((byte) bossNum);
			retVal.position(0xA5);
			if (jpName == null)
				jpName = new byte[0];
			retVal.put(jpName, 0, (jpName.length < 0x20) ? jpName.length : 0x1F);
			retVal.position(0xC5);
			try {
				byte[] letters = mapName.getBytes(charEncoding);
				retVal.put(letters, 0, letters.length < 0x23 ? letters.length : 0x22);
			} catch (UnsupportedEncodingException ignored) {
			}
			break;
		case MOD_KS:
		case MOD_MR:
			retVal = ByteBuffer.allocate(116);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tilesetName.getBytes(charEncoding), 0, tilesetName.length() < 0x10 ? tilesetName.length() : 0xF);
			retVal.position(0x10);
			retVal.put(fileName.getBytes(charEncoding), 0, fileName.length() < 0x10 ? fileName.length() : 0xF);
			retVal.position(0x20);
			retVal.put((byte) scrollType);
			retVal.put(bgName.getBytes(charEncoding), 0, bgName.length() < 0x10 ? bgName.length() : 0xF);
			retVal.position(0x31);
			retVal.put(npcSet1.getBytes(charEncoding), 0, npcSet1.length() < 0x10 ? npcSet1.length() : 0xF);
			retVal.position(0x41);
			retVal.put(npcSet2.getBytes(charEncoding), 0, npcSet2.length() < 0x10 ? npcSet2.length() : 0xF);
			retVal.position(0x51);
			retVal.put((byte) bossNum);
			try {
				byte[] letters = mapName.getBytes(charEncoding);
				retVal.put(letters, 0, letters.length < 0x22 ? letters.length : 0x21);
			} catch (UnsupportedEncodingException ignored) {
			}
			break;
		default:
			throw new IllegalArgumentException("MOD_TYPE parameter invalid or not accounted for" + //$NON-NLS-1$
					" in Mapdata.toBuf()"); //$NON-NLS-1$
		}
		retVal.position(0);
		return retVal;
	}
	
	public String toString() {
		return mapNum + " - " + mapName + " - " + fileName; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public boolean isModified() {
		return changed;
	}

	@Override
	public void markUnchanged() {
		changed = false;
	}

	@Override
	public void markChanged() {
		changed = true;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		PCS.addPropertyChangeListener(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l) {
		PCS.removePropertyChangeListener(l);
	}
	
	protected void firePropertyChange(String title, Object old, Object changed) {
		PCS.firePropertyChange(title, old, changed);
	}
	
	protected void firePropertyChange(String title, int old, int changed) {
		PCS.firePropertyChange(title, old, changed);
	}
	
	public Mapdata clone() {
		Mapdata nd = new Mapdata(-1);
		nd.bgName = this.bgName;
		nd.bossNum = this.bossNum;
		nd.fileName = this.fileName;
		nd.mapName = this.mapName;
		nd.npcSet1 = this.npcSet1;
		nd.npcSet2 = this.npcSet2;
		nd.scrollType = this.scrollType;
		nd.tilesetName = this.tilesetName;
		
		String mn = this.fileName;
		String nstr = "0"; //$NON-NLS-1$
		String lstr = ""; //$NON-NLS-1$
		for (int i = mn.length(); i >= 0; i--) {
			char c = mn.charAt(i-1);
			if (c >= '0' && c <= '9') {
				nstr = nstr + c;
			} else {
				lstr = mn.substring(0,  i);
				break;
			}
		}
		int copynum = Integer.parseInt(nstr);
		nd.fileName = lstr + ++copynum;
		
		return nd;
	}
		
}
