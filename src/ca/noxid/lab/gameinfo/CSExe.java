package ca.noxid.lab.gameinfo;

import ca.noxid.lab.Messages;
import com.carrotlord.string.StrTools;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Vector;


/*
 * //**** offsets into .text section ****   -0x001000

#define CS_TITLE_MAP_X_OFFSET            0x00E761//unsigned char
#define CS_TITLE_MAP_Y_OFFSET            0x00E75F//unsigned char
#define CS_TITLE_MAP_EVENT_OFFSET        0x00E763//unsigned char
#define CS_TITLE_MAP_OFFSET              0x00E765//unsigned char

#define CS_START_HEALTH_CURRENT_OFFSET   0x013BCF//unsigned short int
#define CS_START_HEALTH_MAX_OFFSET       0x013BD8//unsigned short int
#define CS_START_PLAYER_FACING           0x013B74//int

#define CS_MAXIMUM_MAP_COUNT_OFFSET      0x013B24//unsigned char

#define CS_START_UNKOWN1                 0x013B6D//unsigned char

#define CS_START_PLAYER_X_OFFSET         0x01C592//unsigned char
#define CS_START_PLAYER_Y_OFFSET         0x01C590//unsigned char
#define CS_START_EVENT_OFFSET            0x01C594//int?
#define CS_START_MAP_OFFSET              0x01C599//unsigned char

#define CS_SCRIPT_LIMT                   0x020545  //0x021545

//These are the most important offsets
//these offsets reference the map data and must be updated to expand the map area
#define CS_MAP_JMP1_OFFSET1              0x020C2F
#define CS_MAP_JMP1_OFFSET2              0x020C73

//these reference 0x0937D0 not 0x0937B0 (file name)
#define CS_MAP_JMP2_OFFSET1              0x020CB5
#define CS_MAP_JMP2_OFFSET2              0x020CF6
#define CS_MAP_JMP2_OFFSET3              0x020D38

//these reference 0x0937F4 not 0x0937B0 (background)
#define CS_MAP_JMP3_OFFSET1              0x020D7A

//these reference 0x0937F0 not 0x0937B0 (background type)
#define CS_MAP_JMP8_OFFSET1              0x020D9E

//these reference 0x093814 not 0x0937B0 (npc tileset 1)
#define CS_MAP_JMP4_OFFSET1              0x020DD9

//these reference 0x093834 not 0x0937B0 (npc tileset 2)
#define CS_MAP_JMP5_OFFSET1              0x020E1C

//these reference 0x093855 not 0x0937B0 (caption)
#define CS_MAP_JMP6_OFFSET1              0x020E6A

//these reference 0x093854 not 0x0937B0 (boss number)
#define CS_MAP_JMP7_OFFSET1              0x020EA8

 (C)Pixel offset 0x08C4D8
 */

public class CSExe {

	private File location;
	private ExeSec[] headers;
	private ByteBuffer peHead;

	private boolean modified = false;
	long lastModify;

	public boolean isModified() {return modified;}

	private static final int pMapdata   = 0x20C2F; //address of the pointer to map data
	private static final int mapdataLoc = 0x937B0; //address of the original mapdata
	private static final byte[] csmapHead = {
		0x2E, 0x63, 0x73, 0x6D, 0x61, 0x70, 0x00, 0x00, 0x38, 0x4A,
		0x00, 0x00, 0x00, (byte) 0xF0, 0x0B, 0x00, 0x38, 0x4A, 0x00, 0x00,
		0x00, (byte) 0xA0, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, (byte) 0xC0
	};

	@SuppressWarnings("unused")
	CSExe(File inFile) {
		location = inFile;
		lastModify = location.lastModified();
		//File tblFile;
		FileInputStream inStream;
		FileChannel chan;
		try {
			//read operations
			inStream = new FileInputStream(inFile);
			chan = inStream.getChannel();
			//read PE header
			peHead = ByteBuffer.allocate(0x208);
			peHead.order(ByteOrder.nativeOrder());
			chan.read(peHead);
			peHead.flip();
			ByteBuffer uBuf = ByteBuffer.allocate(2);
			uBuf.order(ByteOrder.LITTLE_ENDIAN);

			//find how many sections
			chan.position(0x116);
			chan.read(uBuf);
			uBuf.flip();
			int numSection = uBuf.getShort();
			//read each segment
			//find the .csmap or .swdata segment
			//String[] secHeaders = new String[numSection];
			Vector<ByteBuffer> sections = new Vector<>();
			chan.position(0x208);
			int mapSec = -1, dataSec = -1, rsrcSec = -1, rdataSec = -1;
			for (int i = 0; i < numSection; i++)
			{
				ByteBuffer nuBuf = ByteBuffer.allocate(0x28);
				nuBuf.order(ByteOrder.nativeOrder());
				chan.read(nuBuf);
				nuBuf.flip();
				sections.add(nuBuf);
				String segStr = new String(nuBuf.array());

				if (segStr.contains(".csmap")) //$NON-NLS-1$
					mapSec = i;
				else if (segStr.contains(".swdata")) //$NON-NLS-1$
					mapSec = i;
				else if (segStr.contains(".data")) //$NON-NLS-1$
					dataSec = i;
				else if (segStr.contains(".rsrc"))  //$NON-NLS-1$
					rsrcSec = i;
				else if (segStr.contains(".rdata")) //$NON-NLS-1$
					rdataSec = i;
				//secHeaders[i] = segStr;
			}
			headers = new ExeSec[sections.size()];
			for (int i = 0; i < sections.size(); i++) {
				headers[i] = new ExeSec(sections.get(i), chan);
			}
			if (mapSec == -1) { //there is no map section yet, so we must create it.
				ExeSec[] newHead = new ExeSec[headers.length + 1];
				//create the .csmap section. To do this, fudge it a bit.
				ByteBuffer csHead = ByteBuffer.wrap(csmapHead);
				csHead.order(ByteOrder.LITTLE_ENDIAN);
				csHead.putInt(0x14, mapdataLoc);
				ExeSec csmapSec = new ExeSec(csHead, chan);
				if (headers.length - 1 != rsrcSec) {
					StrTools.msgBox(Messages.getString("CSExe.9")); //$NON-NLS-1$
					System.exit(5);
				}
				ExeSec rsrc = headers[headers.length-1];
				//copy the 'good' segments into their proper place
				System.arraycopy(headers, 0, newHead, 0, headers.length - 1);
				headers = newHead;
				//adjust the indices
				rsrcSec++;
				mapSec = rsrcSec - 1;
				//relocate
				csmapSec.rAddr = rsrc.getPos();
				int rsrcShift = (csmapSec.getLen() + 0xFFF) / 0x1000 * 0x1000;
				rsrc.shift(rsrcShift);
				headers[mapSec] = csmapSec;
				headers[rsrcSec] = rsrc;

				//update PE Header
				int tmpInt = peHead.getInt(0x198); //rsrc table address
				tmpInt += rsrcShift;
				peHead.putInt(0x198, tmpInt);
				peHead.put(0x116, (byte) newHead.length);
				peHead.putShort(0x161, (short) 0x1930); //section list size(?)

				moveMapdata(csmapSec.getPosV());

				modified = true;
			}
			chan.close();
			inStream.close();

			//check (C)Pixel
			ByteBuffer pBuf = read(0x08C4D8, 1);
			if (pBuf.get(0) != 0) {
				pBuf.put((byte)0, (byte)0);
				patch(pBuf, 0x08C4D8);
				StrTools.msgBox(Messages.getString("CSExe.0")); //$NON-NLS-1$
			}

			//check for sue's
			if (mapSec == (headers.length - 1)) {
				int response = JOptionPane.showConfirmDialog(null, Messages.getString("CSExe.5"), Messages.getString("CSExe.8"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$ //$NON-NLS-2$
				if (response != JOptionPane.YES_OPTION) {
					System.exit(4);
				}
				int mapShift = headers[rsrcSec].getPos() - headers[mapSec].getPos();
				int rsrcShift = (headers[mapSec].getLen() + 0xFFF) / 0x1000 * 0x1000;
				headers[mapSec].shift(mapShift);
				headers[rsrcSec].shift(rsrcShift);
				headers[mapSec].setTag(".csmap"); //$NON-NLS-1$

				//swap
				ExeSec tmpSec = headers[rsrcSec];
				headers[rsrcSec] = headers[mapSec];
				headers[mapSec] = tmpSec;
				int tmpInt = rsrcSec;
				rsrcSec = mapSec;
				mapSec = tmpInt;

				//update PE Header
				tmpInt = peHead.getInt(0x198); //rsrc table address
				tmpInt += rsrcShift;
				peHead.putInt(0x198, tmpInt);

				//update mapdata position
				moveMapdata(headers[mapSec].getPosV());

				//oh shit
				StrTools.msgBox(Messages.getString("CSExe.1")); //$NON-NLS-1$
				commit();
			}
		} catch (IOException err) {
			err.printStackTrace();
		}
	}

	public void saveMap(ByteBuffer bytes, int mapNum) {
		int pos = mapNum * 200;
		int csmapLoc = 0;
		//make sure the map will fit
		int rsrcShift = 0;
		for (ExeSec s : headers) {
			s.shift(rsrcShift);
			if (s.getTag().equals(".csmap")) { //$NON-NLS-1$
				if (s.getLen() <= pos) {
					rsrcShift = s.resize(pos + 200);
				}
				csmapLoc = s.getPos();
			} else if (s.getTag().equals(".rsrc")) { //$NON-NLS-1$
				//update PE Header
				int tmpInt = peHead.getInt(0x198); //rsrc table address
				tmpInt += rsrcShift;
				peHead.putInt(0x198, tmpInt);
			}
		}
		patch(bytes, pos+csmapLoc);
	}

	public void setMapdataSize(int nMaps) {
		int rsrcShift = 0;
		for (ExeSec s : headers) {
			s.shift(rsrcShift);
			if (s.getTag().equals(".csmap")) { //$NON-NLS-1$
				if (s.getLen() != nMaps*200) {
					rsrcShift = s.resize(nMaps*200);
				}
			} else if (s.getTag().equals(".rsrc")) { //$NON-NLS-1$
				//update PE Header
				int tmpInt = peHead.getInt(0x198); //rsrc table address
				tmpInt += rsrcShift;
				peHead.putInt(0x198, tmpInt);
			}
		}
	}

	public void patch(ByteBuffer data, int offset) {
		//int shift = 0;
		if (offset >= 0x400000) offset -= 0x400000;
		for (ExeSec s : headers) {
			if (offset >= s.getPos() &&
					offset < s.getPos() + s.getLen()) {
				//it's within this section
				ByteBuffer d = s.getData();
				d.position(offset - s.getPos());
				data.position(0);
				d.put(data);
				modified = true;
				break;
			}
		}
	}

	public ByteBuffer read(int imgStrOffset1, int size) {
		ByteBuffer retVal = null;
		if (imgStrOffset1 >= 0x400000) imgStrOffset1 -= 0x400000;
		for (ExeSec s : headers) {
			if (imgStrOffset1 >= s.getPos() &&
					imgStrOffset1 < s.getPos() + s.getLen()) {
				//it's within this section
				//make sure we don't overflow
				size = (size < s.getPos() + s.getLen() - imgStrOffset1) ? size :
					s.getPos() + s.getLen() - imgStrOffset1;
				ByteBuffer d = s.getData();
				byte[] dat = new byte[ size];
				d.position((imgStrOffset1 - s.getPos()));
				d.get(dat);
				retVal = ByteBuffer.wrap(dat);
				break;
			}
		}
		return retVal;
	}

	private void moveMapdata(int newPos) {
		newPos += 0x400000;
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(0, newPos);
		patch(buf, pMapdata);
		patch(buf, 0x020C73);
		buf.putInt(0, newPos + 0x20);
		patch(buf, 0x020CB5);
		patch(buf, 0x020CF6);
		patch(buf, 0x020D38);
		buf.putInt(0, newPos + 0x44);
		patch(buf, 0x020D7A);
		buf.putInt(0, newPos + 0x40);
		patch(buf, 0x020D9E);
		buf.putInt(0, newPos + 0x64);
		patch(buf, 0x020DD9);
		buf.putInt(0, newPos + 0x84);
		patch(buf, 0x020E1C);
		buf.putInt(0, newPos + 0xA4);
		patch(buf, 0x020EA8);
		buf.putInt(0, newPos + 0xA5);
		patch(buf, 0x020E6A);
	}

	public void commit() {
		File backuploc = null;
		if (location.lastModified() > lastModify) {
			//the file has been changed since we last wrote it
			int choice = JOptionPane.showConfirmDialog(null, "EXE has been modified since last save/load.\n"
					+ "It may contain changes that will be lost if overwritten.\n"
					+ "Do you want to overwrite?" , "EXE on file newer than in memory", JOptionPane.YES_NO_OPTION);

			if (choice != JOptionPane.YES_OPTION) {
				JOptionPane.showMessageDialog(null, "Select 'Load Last' from the File menu\n"
						+ "to discard changes, or try saving again later.\n"
						+ "Current state has been written to:\n"
						+ location + ".bkp");
				backuploc = new File(location + ".blbkp");
			}
		}

		FileOutputStream oStream;
		FileChannel c;
		//update the SIZE_OF_IMAGE optional value in the header
		//this is really important
		int head_sz = 0;
		for (ExeSec sc : headers) {
			head_sz += (sc.getLenV() + 0xFFF) / 0x1000 * 0x1000;
		}
		peHead.putInt(0x160, head_sz + 0x1000);

		try {
			if (backuploc != null) {
				oStream = new FileOutputStream(backuploc);
			} else {
				oStream = new FileOutputStream(location);
			}
			c = oStream.getChannel();
			peHead.position(0);
			c.write(peHead);
			//write section headers
			for (ExeSec s : headers) {
				c.write(s.toBuf());
			}
			//write section data
			for (ExeSec s : headers) {
				c.position(s.getPos());
				ByteBuffer buf = s.getData();
				buf.position(0);
				c.write(buf);
			}
			c.close();
			oStream.close();
		} catch (IOException err) {
			err.printStackTrace();
			StrTools.msgBox(Messages.getString("CSExe.2") + //$NON-NLS-1$
					Messages.getString("CSExe.3")); //$NON-NLS-1$
		}
		if (backuploc == null) {
			modified = false;
			lastModify = location.lastModified();
		}
	}

	/*
	 * PE segment descriptor
	 * 0-7 tag
	 * 8-B virtual size
	 * C-F virtual address
	 * 10-13 size of raw data
	 * 14-17 raw data pointer
	 * 18-1B relocations pointer
	 * 1C-1F line numbers pointer
	 * 20-21 # of relocations
	 * 22-23 # of line #s
	 * 24-27 characteristics
	 */

	public class ExeSec {
		private String tag;
		private int vSize;
		private int vAddr;
		private int rSize;
		private int rAddr;
		private int pReloc;
		private int pLine;
		private short numReloc;
		private short numLine;
		private int attrib;

		private ByteBuffer data;

		public String getTag() {return tag;}
		public void setTag(String t) {tag = t;}
		public int getPos() {return rAddr;}
		public int getLen() {return rSize;}
		public int getPosV() {return vAddr;}
		public ByteBuffer getData() {return data;}
		public int getLenV() {return vSize;}

		ExeSec(ByteBuffer in, FileChannel f) {
			in.position(0);
			byte[] tagArray = new byte[8];
			in.get(tagArray);
			tag = new String(tagArray);
			tag = tag.replaceAll("\0", ""); //$NON-NLS-1$ //$NON-NLS-2$
			vSize = in.getInt();
			vAddr = in.getInt();
			rSize = in.getInt();
			rAddr = in.getInt();
			pReloc = in.getInt();
			pLine = in.getInt();
			numReloc = in.getShort();
			numLine = in.getShort();
			attrib = in.getInt();

			data = ByteBuffer.allocate(rSize);
			data.order(ByteOrder.nativeOrder());
			try {
				if (tag.equals(".swdata")) { //$NON-NLS-1$
					attrib = 0xC0000040;
					f.position(rAddr + 0x10);
					f.read(data);
					data.flip();
					//count the maps
					int size = 0;
					while(true) {
						if (data.getInt(size) != -1) {
							size += 200;
						} else {
							break;
						}
					}
					rSize = size;
					vSize = size;
					data = ByteBuffer.allocate(rSize);
					data.order(ByteOrder.nativeOrder());
					f.position(rAddr + 0x10);
					f.read(data);
					data.flip();
				} else {
					f.position(rAddr);
					f.read(data);
					data.flip();
				}
			} catch (IOException err) {
				err.printStackTrace();
			}
		}

		public ByteBuffer toBuf() {
			ByteBuffer retVal = ByteBuffer.allocate(0x28);
			retVal.order(ByteOrder.nativeOrder());

			byte[] tagDat = java.util.Arrays.copyOf(tag.getBytes(), 8);
			retVal.put(tagDat);
			retVal.putInt(vSize);
			retVal.putInt(vAddr);
			retVal.putInt(rSize);
			retVal.putInt(rAddr);
			retVal.putInt(pReloc);
			retVal.putInt(pLine);
			retVal.putShort(numReloc);
			retVal.putShort(numLine);
			retVal.putInt(attrib);
			retVal.flip();
			return retVal;
		}

		public void shift(int amt) {
			if (tag.equals(".rsrc")) { //$NON-NLS-1$
				shiftDirTable(amt, 0);
			}
			vAddr += amt;
			rAddr += amt;
		}

		private void shiftDirTable(int amt, int pointer) {
			//get the # of rsrc subdirs indexed by name
			int nEntry = data.getShort(pointer + 12);
			//get the # of rsrc subdirs indexed by id
			nEntry += data.getShort(pointer + 14);

			//read and shift entries
			int pos = pointer + 16;
			for (int i = 0; i < nEntry; i++) {
				rsrcShift(amt, pos + i*8);
			}
		}
		private void rsrcShift(int amt, int pointer) {
			int rva = data.getInt(pointer + 4);
			if ((rva & 0x80000000) != 0) { //if hi bit 1 points to another directory table
				shiftDirTable(amt, rva & 0x7FFFFFFF);
			} else {
				int oldVal = data.getInt(rva);
				data.putInt(rva, oldVal + amt);
			}
		}

		/**
		 * @param newSize
		 * @return how much future sections need to be shifted
		 */
		public int resize(int newSize) {
			int oldSize = data.capacity();
			int diff = newSize - oldSize;
			ByteBuffer newDat = ByteBuffer.allocate(newSize);
			data.position(0);
			//transfer as many bytes as possible into the new array from the old
			while (data.hasRemaining()) {
				if (newDat.hasRemaining())
					newDat.put(data.get());
				else
					break;
			}
			data = newDat;
			rSize = newSize;
			vSize = newSize;
			if (diff != 0)
				modified = true;
			return ((newSize+0xFFF) / 0x1000 * 0x1000)-((oldSize+0xFFF) / 0x1000 * 0x1000);
		}
	}

	public void execute() {

		File gameLoc = location.getParentFile();
		Runtime rt = Runtime.getRuntime();
		try {
			rt.exec(location.toString(), null, gameLoc);
		} catch (IOException e) {
			StrTools.msgBox(Messages.getString("CSExe.4")); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	public File getFile() {
		return location;
	}
}
