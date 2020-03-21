package ca.noxid.lab.gameinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import com.carrotlord.string.StrTools;

import ca.noxid.lab.Messages;

/*
 * //**** offsets into .text section **** -0x001000
 * 
 * #define CS_TITLE_MAP_X_OFFSET 0x00E761//unsigned char
 * #define CS_TITLE_MAP_Y_OFFSET 0x00E75F//unsigned char
 * #define CS_TITLE_MAP_EVENT_OFFSET 0x00E763//unsigned char
 * #define CS_TITLE_MAP_OFFSET 0x00E765//unsigned char
 * 
 * #define CS_START_HEALTH_CURRENT_OFFSET 0x013BCF//unsigned short int
 * #define CS_START_HEALTH_MAX_OFFSET 0x013BD8//unsigned short int
 * #define CS_START_PLAYER_FACING 0x013B74//int
 * 
 * #define CS_MAXIMUM_MAP_COUNT_OFFSET 0x013B24//unsigned char
 * 
 * #define CS_START_UNKOWN1 0x013B6D//unsigned char
 * 
 * #define CS_START_PLAYER_X_OFFSET 0x01C592//unsigned char
 * #define CS_START_PLAYER_Y_OFFSET 0x01C590//unsigned char
 * #define CS_START_EVENT_OFFSET 0x01C594//int?
 * #define CS_START_MAP_OFFSET 0x01C599//unsigned char
 * 
 * #define CS_SCRIPT_LIMT 0x020545 //0x021545
 * 
 * //These are the most important offsets
 * //these offsets reference the map data and must be updated to expand the map
 * area
 * #define CS_MAP_JMP1_OFFSET1 0x020C2F
 * #define CS_MAP_JMP1_OFFSET2 0x020C73
 * 
 * //these reference 0x0937D0 not 0x0937B0 (file name)
 * #define CS_MAP_JMP2_OFFSET1 0x020CB5
 * #define CS_MAP_JMP2_OFFSET2 0x020CF6
 * #define CS_MAP_JMP2_OFFSET3 0x020D38
 * 
 * //these reference 0x0937F4 not 0x0937B0 (background)
 * #define CS_MAP_JMP3_OFFSET1 0x020D7A
 * 
 * //these reference 0x0937F0 not 0x0937B0 (background type)
 * #define CS_MAP_JMP8_OFFSET1 0x020D9E
 * 
 * //these reference 0x093814 not 0x0937B0 (npc tileset 1)
 * #define CS_MAP_JMP4_OFFSET1 0x020DD9
 * 
 * //these reference 0x093834 not 0x0937B0 (npc tileset 2)
 * #define CS_MAP_JMP5_OFFSET1 0x020E1C
 * 
 * //these reference 0x093855 not 0x0937B0 (caption)
 * #define CS_MAP_JMP6_OFFSET1 0x020E6A
 * 
 * //these reference 0x093854 not 0x0937B0 (boss number)
 * #define CS_MAP_JMP7_OFFSET1 0x020EA8
 * 
 * (C)Pixel offset 0x08C4D8
 */

public class CSExe {

	private File location;

	// The csmap section must always exist,
	// and the PEFile must always be the same.
	private final PEFile peData;
	private final PEFile.Section csmapSection;

	private boolean modified = false;
	long lastModify;

	public boolean isModified() {
		return modified;
	}

	private static final int pMapdata = 0x20C2F; // address of the pointer to map data
	// private static final int mapdataLoc = 0x937B0; // address of the original
	// mapdata

	CSExe(File inFile) throws IOException {
		location = inFile;
		lastModify = location.lastModified();
		// File tblFile;

		FileInputStream inStream = new FileInputStream(inFile);
		FileChannel chan = inStream.getChannel();
		long l = chan.size();
		if (l > 0x7FFFFFFF) {
			inStream.close();
			throw new IOException("Too big!");
		}
		ByteBuffer bb = ByteBuffer.allocate((int) l);
		if (chan.read(bb) != l) {
			inStream.close();
			throw new IOException("Didn't read whole file.");
		}
		inStream.close();

		peData = new PEFile(bb, 0x1000);

		int mapSection = peData.getSectionIndexByTag(".csmap");

		if (mapSection == -1) {
			int sueSection = peData.getSectionIndexByTag(".swdata");
			if ((sueSection != -1) && (sueSection == (peData.sections.size() - 1))) {
				// Sue's Workshop has interfered. Fix this.
				int response = JOptionPane.showConfirmDialog(null, Messages.getString("CSExe.5"), //$NON-NLS-1$
						Messages.getString("CSExe.8"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
				if (response != JOptionPane.YES_OPTION) {
					// ;.;
					System.exit(4);
				}
				PEFile.Section removeMe = peData.sections.get(sueSection);
				csmapSection = universalMapDataPortMechanism(true);
				peData.sections.remove(removeMe);
				peData.malloc(csmapSection);
				updateMapdataRVA(csmapSection.virtualAddrRelative);
				// oh shit
				StrTools.msgBox(Messages.getString("CSExe.1")); //$NON-NLS-1$
				commit();
			} else {
				// good ol vanilla CS
				csmapSection = universalMapDataPortMechanism(false);
				peData.malloc(csmapSection);
				updateMapdataRVA(csmapSection.virtualAddrRelative);
				commit();
			}
		} else
			// .csmap already exists, just grab it
			csmapSection = peData.sections.get(mapSection);

		fixVirtualLayoutGaps();

		// check (C)Pixel
		ByteBuffer pBuf = read(0x08C4D8, 1);
		if (pBuf.get(0) != 0) {
			pBuf.put((byte) 0, (byte) 0);
			patch(pBuf, 0x08C4D8);
			StrTools.msgBox(Messages.getString("CSExe.0")); //$NON-NLS-1$
		}
	}

	// The map porting mechanism. Creates .csmap, but does not set as such.
	// DO NOT call if a .csmap already exists!
	private PEFile.Section universalMapDataPortMechanism(boolean allAvailableMaps) throws IOException {
		if (csmapSection != null)
			throw new IOException(
					"What kind of prank are you trying to pull, mister? *throws cake* This one is much better!"
				  + "\n(tried to create .csmap when one already existed)");
		int mapCount = 95;
		int mapDataPtr = peData.setupRVAPoint(pMapdata).getInt() - 0x400000;
		// System.out.println("Porting via : " + Integer.toHexString(mapDataPtr));
		ByteBuffer bb = peData.setupRVAPoint(mapDataPtr);
		int area = bb.capacity() - bb.position();
		int areaMapCount = area / 200;
		if ((areaMapCount < mapCount) || allAvailableMaps)
			mapCount = areaMapCount;
		byte[] data = new byte[mapCount * 200];
		bb.get(data);
		PEFile.Section s = new PEFile.Section();
		s.encodeTag(".csmap");
		s.rawData = data;
		s.virtualSize = data.length;
		s.metaLinearize = false;
		s.characteristics = PEFile.SECCHR_INITIALIZED_DATA | PEFile.SECCHR_READ;
		return s;
	}

	public void updateExcode() throws IOException {
		if (csmapSection == null)
			throw new IOException("\".csmap\" section not found!");
		removeFillerSections();
		int codeSectionID = peData.getSectionIndexByTag(".excode");
		PEFile.Section codeSection = null;
		if (codeSectionID != -1) {
			codeSection = peData.sections.get(codeSectionID);
			for (PEFile.Section seg : peData.sections) {
				String segN = seg.decodeTag();
				if (segN.equals(".rsrc") || segN.equals(".csmap"))
					continue;
				if (codeSection.virtualAddrRelative < seg.virtualAddrRelative) {
					StrTools.msgBox(Messages.getString("CSExe.11") + segN + " " + Messages.getString("CSExe.12")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					peData.sections.remove(codeSectionID);
					codeSectionID = -1;
					break;
				}
			}
		}
		if (codeSectionID == -1) {
			byte[] data = new byte[0x100000];
			if (codeSection != null)
				copySectionTo(codeSection, data);
			codeSection = new PEFile.Section();
			codeSection.encodeTag(".excode");
			codeSection.rawData = data;
			codeSection.virtualSize = data.length;
			codeSection.metaLinearize = true;
			codeSection.characteristics = PEFile.SECCHR_CODE | PEFile.SECCHR_INITIALIZED_DATA | PEFile.SECCHR_EXECUTE
					| PEFile.SECCHR_READ | PEFile.SECCHR_WRITE;
			peData.malloc(codeSection);
			modified = true;
			StrTools.msgBox(Messages.getString("CSExe.13") //$NON-NLS-1$
					+ Integer.toHexString(codeSection.virtualAddrRelative + 0x400000).toUpperCase()
					+ Messages.getString("CSExe.14")); //$NON-NLS-1$
		}
		int newSize = codeSection.virtualSize;
		while (true) {
			String valStr = JOptionPane.showInputDialog(Messages.getString("CSExe.24") //$NON-NLS-1$
					+ Integer.toHexString(codeSection.virtualAddrRelative + 0x400000).toUpperCase()
					+ Messages.getString("CSExe.15"), Integer.toHexString(newSize).toUpperCase()); //$NON-NLS-1$
			if (valStr == null) {
				StrTools.msgBox(Messages.getString("CSExe.23")); //$NON-NLS-1$
				break;
			}
			Integer newVal = newSize;
			try {
				newVal = Integer.parseInt(valStr, 16);
			} catch (NumberFormatException e) {
				StrTools.msgBox(Messages.getString("CSExe.16")); //$NON-NLS-1$
				continue;
			}
			if (newVal <= 0) {
				StrTools.msgBox(Messages.getString("CSExe.17")); //$NON-NLS-1$
				continue;
			}
			if (newVal == newSize) {
				StrTools.msgBox(Messages.getString("CSExe.23")); //$NON-NLS-1$
				break;
			}
			newSize = newVal;
			if (newSize < codeSection.virtualSize) {
				int confirm = JOptionPane.showConfirmDialog(null, String.format(Messages.getString("CSExe.18"), //$NON-NLS-1$
						Integer.toHexString(codeSection.virtualSize - newSize)), Messages.getString("CSExe.19"), //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
				if (confirm != JOptionPane.YES_OPTION)
					break;
			}
			peData.sections.remove(codeSection);
			byte[] data = new byte[newSize];
			copySectionTo(codeSection, data);
			codeSection.rawData = data;
			codeSection.virtualSize = data.length;
			peData.malloc(codeSection);
			modified = true;
			StrTools.msgBox(Messages.getString("CSExe.22") + Integer.toHexString(newSize).toUpperCase()); //$NON-NLS-1$
			break;
		}
		fixVirtualLayoutGaps();
	}

	private void copySectionTo(PEFile.Section section, byte[] data) {
		// attempt to copy as much data as possible from old section
		ByteBuffer oldData = ByteBuffer.allocate(section.virtualSize);
		oldData.put(section.rawData);
		oldData.flip();
		int len = oldData.remaining();
		if (len > data.length)
			len = data.length;
		oldData.get(data, 0, len);
	}

	private static final Comparator<PEFile.Section> sortByRVA = new Comparator<PEFile.Section>() {

		@Override
		public int compare(PEFile.Section o1, PEFile.Section o2) {
			int rva1 = PEFile.uCompare(o1.virtualAddrRelative);
			int rva2 = PEFile.uCompare(o2.virtualAddrRelative);
			if (rva1 < rva2)
				return -1;
			if (rva1 > rva2)
				return 1;
			return 0;
		}

	};
	
	private String getFillerSectionTag(int flrNum) {
		String flrNumTag = Integer.toHexString(flrNum).toUpperCase();
		while (flrNumTag.length() < 4)
			flrNumTag = "0" + flrNumTag; //$NON-NLS-1$
		if (flrNumTag.length() > 4)
			flrNumTag = flrNumTag.substring(0, 4);
		return ".flr" + flrNumTag; //$NON-NLS-1$
	}
	
	private boolean isFillerSection(PEFile.Section s) {
		String sTag = s.decodeTag();
		if (sTag.length() == 8 && sTag.startsWith(".flr")) {
			try {
				Integer.parseInt(sTag.substring(4), 16);
			} catch (NumberFormatException e) {
				return false;
			}
			return (s.characteristics & PEFile.SECCHR_UNINITIALIZED_DATA) != 0;
		}
		return false;
	}
	
	private void removeFillerSections() {
		LinkedList<PEFile.Section> secsToRem = new LinkedList<PEFile.Section>();
		for (PEFile.Section s : peData.sections) {
			if (isFillerSection(s))
				secsToRem.add(s);
		}
		peData.sections.removeAll(secsToRem);
	}
	
	private void mallocFiller(int num, int addr, int size) {
		PEFile.Section filler = new PEFile.Section();
		filler.encodeTag(getFillerSectionTag(num));
		filler.virtualAddrRelative = addr;
		filler.virtualSize = size;
		filler.characteristics = PEFile.SECCHR_UNINITIALIZED_DATA | PEFile.SECCHR_READ | PEFile.SECCHR_WRITE;
		peData.malloc(filler);
		modified = true;
	}

	// PEFile.alignForward, but with special handling for RVA == 0
	// hopefully, this should fix empty .csmap nukage
	private int alignForwardFiller(int virtualAddrRelative, int fileAlignment) {
		int mod = virtualAddrRelative % fileAlignment;
		if (virtualAddrRelative == 0 || mod != 0)
			virtualAddrRelative += fileAlignment - mod;
		return virtualAddrRelative;
	}

	private void fixVirtualLayoutGaps() {
		removeFillerSections(); // remove any existing filler sections
		final int sectionAlignment = peData.getOptionalHeaderInt(0x20);
		// sort the sections so we go by RVA
		LinkedList<PEFile.Section> sectionsSorted = new LinkedList<>(peData.sections);
		sectionsSorted.sort(sortByRVA);
		int flrNum = 0;
		int lastAddress = 0;
		String lastSeg = null;
		// if a section's RVA does not equal lastAddress, that means there's a gap in
		// the virtual layout
		// for some reason Windows 10 and apparently *only* Windows 10 hates virtual
		// layout gaps
		// so we plug the gaps up using uninitialized filler sections (.flrXXXX)
		for (PEFile.Section s : sectionsSorted) {
			String curSeg = s.decodeTag();
			if (lastAddress != 0) {
				if (s.virtualAddrRelative != lastAddress) {
					int confirm = JOptionPane.showConfirmDialog(null, String.format(Messages.getString("CSExe.26"), //$NON-NLS-1$
							lastSeg, curSeg), Messages.getString("CSExe.19"), //$NON-NLS-1$
							JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
					if (confirm != JOptionPane.YES_OPTION) {
						lastSeg = curSeg;
						lastAddress = alignForwardFiller(s.virtualAddrRelative + s.virtualSize, sectionAlignment);
						continue;
					}
					// create new filler section
					mallocFiller(flrNum++, lastAddress, s.virtualAddrRelative - lastAddress);
				}
			}
			lastSeg = curSeg;
			lastAddress = PEFile.alignForward(s.virtualAddrRelative + s.virtualSize, sectionAlignment);
		}
	}

	// Gets a ByteBuffer for passing to Mapdata
	public ByteBuffer loadMaps() {
		byte[] dataCopy = new byte[getMapdataSize() * 200];
		System.arraycopy(csmapSection.rawData, 0, dataCopy, 0, dataCopy.length);
		ByteBuffer bb = ByteBuffer.wrap(dataCopy);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb;
	}

	// Takes a Mapdata-output ByteBuffer and puts it in the executable
	public void saveMap(ByteBuffer bytes, int mapNum) {
		int pos = mapNum * 200;
		if (csmapSection.rawData.length <= (pos + 199))
			setMapdataSize(mapNum + 1);
		patch(bytes, pos + csmapSection.virtualAddrRelative);
	}
	
	public void prepareToDeleteMaps() {
		// remove all filler sections
		removeFillerSections();
	}

	public void setMapdataSize(int nMaps) {
		// csmap section no longer valid! get rid of it
		peData.sections.remove(csmapSection);
		byte[] sectionData = new byte[nMaps * 200];
		System.arraycopy(csmapSection.rawData, 0, sectionData, 0,
				Math.min(csmapSection.rawData.length, sectionData.length));
		csmapSection.virtualSize = sectionData.length;
		csmapSection.rawData = sectionData;
		// reinstall csmap section now we've fixed it
		peData.malloc(csmapSection);
	}
	
	public void doneDeletingMaps() {
		// reinstall filler sections
		fixVirtualLayoutGaps();
		// update mapdata RVA
		updateMapdataRVA(csmapSection.virtualAddrRelative);
	}

	public int getMapdataSize() {
		return csmapSection.rawData.length / 200;
	}

	public void patch(ByteBuffer data, int offset) {
		// int shift = 0;
		if (offset >= 0x400000)
			offset -= 0x400000;
		ByteBuffer d = peData.setupRVAPoint(offset);
		if (d != null) {
			// it's within this section
			data.position(0);
			d.put(data);
			modified = true;
		}
	}

	private String int2Hex(int i) {
		return "0x" + Integer.toHexString(i).toUpperCase();
	}

	public ByteBuffer read(int imgStrOffset1, int size) {
		ByteBuffer retVal = null;
		if (imgStrOffset1 >= 0x400000)
			imgStrOffset1 -= 0x400000;
		ByteBuffer d = peData.setupRVAPoint(imgStrOffset1);
		if (d != null) {
			// make sure we don't overflow
			int available = d.capacity() - d.position();
			size = Math.min(size, available);
			// it's within this section
			byte[] data = new byte[size];
			d.get(data);
			retVal = ByteBuffer.wrap(data);
		}
		if (retVal == null)
			System.err.println("READ FAIL! OFF=" + int2Hex(imgStrOffset1) + ",SZE=" + int2Hex(size));
		return retVal;
	}

	// The newPos given is an RVA
	private void updateMapdataRVA(int newPos) {
		newPos += 0x400000;
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(0, newPos);
		patch(buf, pMapdata);
		patch(buf, pMapdata + 0x44);
		buf.putInt(0, newPos + 0x20);
		patch(buf, pMapdata + 0x86);
		patch(buf, pMapdata + 0xC7);
		patch(buf, pMapdata + 0x109);
		buf.putInt(0, newPos + 0x44);
		patch(buf, pMapdata + 0x14B);
		buf.putInt(0, newPos + 0x40);
		patch(buf, pMapdata + 0x16F);
		buf.putInt(0, newPos + 0x64);
		patch(buf, pMapdata + 0x1AA);
		buf.putInt(0, newPos + 0x84);
		patch(buf, pMapdata + 0x1ED);
		buf.putInt(0, newPos + 0xA4);
		patch(buf, pMapdata + 0x279);
		buf.putInt(0, newPos + 0xA5);
		patch(buf, pMapdata + 0x23B);
	}

	public void commit() {
		File outloc = location;
		if (outloc.lastModified() > lastModify) {
			// the file has been changed since we last wrote it
			int choice = JOptionPane.showConfirmDialog(null, Messages.getString("CSExe.6"), //$NON-NLS-1$
					Messages.getString("CSExe.7"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$

			if (choice != JOptionPane.YES_OPTION) {
				outloc = new File(location + ".blalt");
				StrTools.msgBox(Messages.getString("CSExe.3") //$NON-NLS-1$
						+ outloc);
			}
		}

		FileOutputStream oStream;

		if (outloc == location) {
			// backup existing file
			try {
				File backuploc = new File(location + ".blbkp"); //$NON-NLS-1$
				FileInputStream iStream = new FileInputStream(location);
				FileChannel cInput = iStream.getChannel();
				oStream = new FileOutputStream(backuploc);
				FileChannel c = oStream.getChannel();
				ByteBuffer exeDat = ByteBuffer.allocate(iStream.available());
				cInput.read(exeDat);
				exeDat.flip();
				c.write(exeDat);
				iStream.close();
				oStream.close();
			} catch (IOException err) {
				err.printStackTrace();
				StrTools.msgBox(Messages.getString("CSExe.10")); //$NON-NLS-1$
			}
		}
		// actually write to file
		try {
			// Make sure to do this here so we don't damage anything if the file turns out
			// to fail tests
			byte[] b = peData.write();
			oStream = new FileOutputStream(outloc);
			oStream.write(b);
			oStream.close();
		} catch (IOException err) {
			err.printStackTrace();
			StrTools.msgBox(Messages.getString("CSExe.2")); //$NON-NLS-1$
		}
		if (outloc == location) {
			modified = false;
			lastModify = location.lastModified();
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
