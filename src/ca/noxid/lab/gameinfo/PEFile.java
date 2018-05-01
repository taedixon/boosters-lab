package ca.noxid.lab.gameinfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Generic PE EXE handling classes, used to patch games.
 * The hope is that having something this generic will reduce the chance of any
 * faults going unseen.
 * THE FULL LIST OF ASSUMPTIONS:
 * 1. Tools expect (but we don't) sections to be in virtual order (this is my
 * fault -- PEONS dev)
 * 2. Tools and us expect .text to be at FA 0x1000, and for .text/.rdata/.data
 * to be linearized.
 * If we can't ensure this, we fail for safety. (The value is adjustible
 * per-game, but cannot be eliminated)
 * 3. Old-Style Relocations and Line Numbers never happen. If we encounter them,
 * we fail for safety.
 * 4. The executable is assumed to not have relocation data. If it does, we
 * ignore it - it's assumed we already know and can handle it.
 * 5. The resource chunk starts at its section and continues to the end of the
 * chunk.
 * If this is not the case, then Xin probably started cannibalizing it and we
 * should just pretend it's not a resource section,
 * but we do NOT have to fail in this case so long as that is kept in mind.
 * 
 * @author 20kdc
 */
public class PEFile {
	public static final int SECCHR_CODE = 0x20;
	public static final int SECCHR_INITIALIZED_DATA = 0x40;
	public static final int SECCHR_UNINITIALIZED_DATA = 0x80;
	public static final int SECCHR_EXECUTE = 0x20000000;
	public static final int SECCHR_READ = 0x40000000;
	public static final int SECCHR_WRITE = 0x80000000;
	
	// NOTE: The section headers in this are not to be trusted.
	// They get rewritten on write().
	// Also note, this implicitly defines the expectedTex.
	public final byte[] earlyHeader;
	// Always use headerPosition(x) before any sequence of operations,
	// and always keep in LITTLE_ENDIAN order
	public final ByteBuffer earlyHeaderBB;
	// The complete list of sections.
	// write() updates earlyHeader with this before writeout.
	public final LinkedList<Section> sections = new LinkedList<>();

	public PEFile(ByteBuffer source, int expectedTex) throws IOException {
		source.order(ByteOrder.LITTLE_ENDIAN);
		source.clear();
		earlyHeader = new byte[expectedTex];
		source.get(earlyHeader);
		earlyHeaderBB = ByteBuffer.wrap(earlyHeader);
		earlyHeaderBB.order(ByteOrder.LITTLE_ENDIAN);
		earlyHeaderBB.position(getNtHeaders());
		if (earlyHeaderBB.getInt() != 0x00004550)
			throw new IOException("Not a PE file.");
		earlyHeaderBB.getShort(); // We don't actually need to check the machine - if anything is wrong, see
									// opt.header magic
		int sectionCount = earlyHeaderBB.getShort() & 0xFFFF;
		earlyHeaderBB.getInt();
		earlyHeaderBB.getInt(); // symtab address
		if (earlyHeaderBB.getInt() != 0)
			throw new IOException(
					"This file was linked with a symbol table. Since we don't want to accidentally destroy it, you get this error instead.");
		int optHeadSize = earlyHeaderBB.getShort() & 0xFFFF;
		earlyHeaderBB.getShort(); // characteristics
		// -- optional header --
		int optHeadPoint = earlyHeaderBB.position();
		if (optHeadSize < 0x78)
			throw new IOException("Optional header size is under 0x78 (RESOURCE table info)");
		if (earlyHeaderBB.getShort() != 0x010B)
			throw new IOException("Unknown optional header type");
		// Check that size of headers is what we thought
		if (getOptionalHeaderInt(0x3C) != expectedTex)
			throw new IOException("Size of headers must be as expected due to linearization fun");
		// Everything verified - load up the image sections
		source.clear(); // Remove this line and things will fail...
		source.position(optHeadPoint + optHeadSize);
		for (int i = 0; i < sectionCount; i++) {
			Section s = new Section();
			s.read(source);
			sections.add(s);
		}
		test(true);
	}

	// Returns file size if justOrderAndOverlap is given
	private int test(boolean justOrderAndOverlap) throws IOException {
		// You may be wondering: "Why so many passes?"
		// The answer: It simplifies the code.

		// -- Test virtual integrity
		Collections.sort(sections, new Comparator<Section>() {
			@Override
			public int compare(Section section, Section t1) {
				if (section.virtualAddrRelative < t1.virtualAddrRelative)
					return -1;
				if (section.virtualAddrRelative == t1.virtualAddrRelative)
					return 0; // Impossible if they are different, but...
				return 1;
			}
		});
		// Sets the minimum RVA we can use. This works due to the virtual sorting stuff.
		int rvaHeaderFloor = 0;
		for (Section s : sections) {
			if (uCompare(s.virtualAddrRelative) < uCompare(rvaHeaderFloor))
				throw new IOException("Section RVA Overlap, " + s);
			rvaHeaderFloor = s.virtualAddrRelative + s.virtualSize;
		}
		if (justOrderAndOverlap)
			return -1;
		// -- Allocate file addresses
		int sectionAlignment = getOptionalHeaderInt(0x20);
		int fileAlignment = getOptionalHeaderInt(0x24);
		LinkedList<AllocationSpan> map = new LinkedList<>();
		// Disallow collision with the primary header
		map.add(new AllocationSpan(0, earlyHeader.length));
		for (int i = 0; i < 2; i++) {
			for (Section s : sections) {
				if (s.metaLinearize != (i == 0))
					continue;
				boolean ok = false;
				if (s.metaLinearize) {
					if (alignForward(s.virtualAddrRelative, fileAlignment) != s.virtualAddrRelative)
						System.err.println(
								"Warning: File alignment being broken for linearization. This isn't a critical error, but it's not a good thing.");
					ok = checkAllocation(map, new AllocationSpan(s.virtualAddrRelative, s.rawData.length));
					s.cachedFutureFileAddress = s.virtualAddrRelative;
				}
				if (!ok) {
					int position = 0;
					while (!checkAllocation(map, new AllocationSpan(position, s.rawData.length)))
						position += fileAlignment;
					s.cachedFutureFileAddress = position;
				}
			}
		}
		// -- Set Section Count / Rewrite section headers
		// 4: signature
		// 2: field: number of sections
		headerPosition(getNtHeaders() + 4 + 0x02);
		earlyHeaderBB.putShort((short) sections.size());
		headerPosition(getSectionHeaders());
		for (Section s : sections)
			s.writeHead(earlyHeaderBB);
		// -- Image size is based on virtual size, not phys.
		int imageSize = calculateImageSize();
		setOptionalHeaderInt(0x38, alignForward(imageSize, sectionAlignment));
		// -- File size based on the allocation map
		int fileSize = 0;
		for (AllocationSpan as : map) {
			int v = as.start + as.length;
			if (uCompare(v) > uCompare(fileSize))
				fileSize = v;
		}
		return alignForward(fileSize, fileAlignment);
	}

	// This is also used in cases where we need to know the current end of virtual
	// memory.
	private int calculateImageSize() {
		int imageSize = 0;
		for (Section s : sections) {
			int v = s.virtualAddrRelative + s.virtualSize;
			if (uCompare(v) > uCompare(imageSize))
				imageSize = v;
		}
		return imageSize;
	}

	private boolean checkAllocation(LinkedList<AllocationSpan> map, AllocationSpan allocationSpan) {
		for (AllocationSpan as : map)
			if (as.collides(allocationSpan))
				return false;
		map.add(allocationSpan);
		return true;
	}

	public static int alignForward(int virtualAddrRelative, int fileAlignment) {
		int mod = virtualAddrRelative % fileAlignment;
		if (mod != 0)
			virtualAddrRelative += fileAlignment - mod;
		return virtualAddrRelative;
	}

	public byte[] write() throws IOException {
		byte[] data = new byte[test(false)];
		ByteBuffer d = ByteBuffer.wrap(data);
		d.order(ByteOrder.LITTLE_ENDIAN);
		d.put(earlyHeader);
		for (Section s : sections) {
			d.clear();
			d.position(s.cachedFutureFileAddress);
			d.put(s.rawData, 0, s.rawData.length);
		}
		return data;
	}

	public void headerPosition(int i) {
		earlyHeaderBB.clear();
		earlyHeaderBB.position(i);
	}

	/* Structure Finders, modifies earlyHeaderBB.position */

	public int getNtHeaders() {
		headerPosition(0x3C);
		return earlyHeaderBB.getInt();
	}

	public int getSectionHeaders() {
		int nt = getNtHeaders();
		// 4: Signature
		// 0x10: Field : Size Of Optional Header
		headerPosition(nt + 4 + 0x10);
		return nt + 4 + 0x14 + (earlyHeaderBB.getShort() & 0xFFFF);
	}

	/* Value IO, modifies earlyHeaderBB.position */

	public int getOptionalHeaderInt(int ofs) {
		// 4: Signature
		// 0x14: IMAGE_FILE_HEADER
		headerPosition(getNtHeaders() + 4 + 0x14 + ofs);
		return earlyHeaderBB.getInt();
	}

	public void setOptionalHeaderInt(int ofs, int v) {
		// 0x18: Signature + IMAGE_FILE_HEADER
		headerPosition(getNtHeaders() + 0x18 + ofs);
		earlyHeaderBB.putInt(v);
	}

	public ByteBuffer setupRVAPoint(int rva) {
		for (Section s : sections) {
			if (uCompare(rva) >= uCompare(s.virtualAddrRelative)) {
				int rel = rva - s.virtualAddrRelative;
				if (uCompare(rel) < Math.max(uCompare(s.rawData.length), uCompare(s.virtualSize))) {
					ByteBuffer bb = ByteBuffer.wrap(s.rawData);
					bb.order(ByteOrder.LITTLE_ENDIAN);
					bb.position(rel);
					return bb;
				}
			}
		}
		return null;
	}

	/* High-Level Tools */

	// Get the current index of the resource section, or -1 if none/unknown.

	public int getResourcesIndex() {
		int idx = 0;
		int resourcesRVA = getOptionalHeaderInt(0x70);
		for (Section s : sections) {
			if (s.virtualAddrRelative == resourcesRVA)
				return idx;
			idx++;
		}
		return -1;
	}

	// Gets the current index of a given named section, or -1 if none/unknown.

	public int getSectionIndexByTag(String tag) {
		int idx = 0;
		for (Section s : sections) {
			if (s.decodeTag().equals(tag))
				return idx;
			idx++;
		}
		return -1;
	}

	// Add in the given section and ensure the resources section is the latest
	// section.
	public void malloc(Section newS) {
		int sectionAlignment = getOptionalHeaderInt(0x20);
		Section resourcesSection = null;
		int rsI = getResourcesIndex();
		if (rsI != -1)
			resourcesSection = sections.remove(rsI);
		mallocInterior(newS, earlyHeader.length, sectionAlignment);
		sections.add(newS);
		if (resourcesSection != null) {
			// resourcesSection has to be the latest section in the file
			int oldResourcesRVA = resourcesSection.virtualAddrRelative;
			mallocInterior(resourcesSection, calculateImageSize(), sectionAlignment);
			int newResourcesRVA = resourcesSection.virtualAddrRelative;
			resourcesSection.shiftResourceContents(newResourcesRVA - oldResourcesRVA);
			sections.add(resourcesSection);
			setOptionalHeaderInt(0x70, newResourcesRVA);
		}
	}

	// Positions a section in virtual memory starting at a given RVA.
	private void mallocInterior(Section resourcesSection, int i, int sectionAlignment) {
		while (true) {
			i = alignForward(i, sectionAlignment);
			// Is this OK?
			AllocationSpan as = new AllocationSpan(i, alignForward(resourcesSection.virtualSize, sectionAlignment));
			boolean hit = false;
			for (Section s : sections) {
				if (new AllocationSpan(s.virtualAddrRelative, alignForward(s.virtualSize, sectionAlignment))
						.collides(as)) {
					hit = true;
					break;
				}
			}
			if (!hit) {
				resourcesSection.virtualAddrRelative = i;
				return;
			}
			i += sectionAlignment;
		}
	}

	/**
	 * A section, loaded into memory.
	 * Yes, this means temporary higher memory use,
	 * but it's a 2MB EXE at max. and frankly this code has to be as readable as
	 * possible.
	 */
	public static class Section {
		private final static byte[] blankRawData = new byte[0];
		// "Linearization" : File Address == RVA
		// We try to preserve linearization due to Doukutsu Assembler & co.
		// This is forced on for 3 critical sections,
		// and if we detect linearization on anything else,
		// we try to keep it that way for consistency.
		public boolean metaLinearize;
		// These are the fields we support (read: we don't enforce these to be 0):
		// Tag (in Windows-1252)
		public final byte[] tag = new byte[8];
		public int virtualSize, virtualAddrRelative;
		// File address that test(true) wrote into the section header
		private int cachedFutureFileAddress;
		// File address is implied by linearization or automatic rearranging during
		// write()
		// The "Raw Data" - Set position before use.
		public byte[] rawData = blankRawData;
		public int characteristics = 0xE0000040;

		public Section() {
		}

		public void read(ByteBuffer bb) throws IOException {
			// NOTE: This occurs on the actual file in order to extract all the yummy data.
			bb.get(tag);
			virtualSize = bb.getInt();
			virtualAddrRelative = bb.getInt();
			rawData = new byte[bb.getInt()];
			int rawDataPointer = bb.getInt();
			metaLinearize = rawDataPointer == virtualAddrRelative;
			int saved = bb.position();
			bb.clear();
			bb.position(rawDataPointer);
			bb.get(rawData);
			bb.clear();
			bb.position(saved);
			bb.getInt();
			bb.getInt();
			if (bb.getShort() != 0)
				throw new IOException("Relocations not allowed");
			if (bb.getShort() != 0)
				throw new IOException("Line numbers not allowed");
			characteristics = bb.getInt();
		}

		public void writeHead(ByteBuffer earlyHeaderBB) {
			earlyHeaderBB.put(tag);
			earlyHeaderBB.putInt(virtualSize);
			earlyHeaderBB.putInt(virtualAddrRelative);
			earlyHeaderBB.putInt(rawData.length);
			earlyHeaderBB.putInt(cachedFutureFileAddress);
			earlyHeaderBB.putInt(0);
			earlyHeaderBB.putInt(0);
			earlyHeaderBB.putShort((short) 0);
			earlyHeaderBB.putShort((short) 0);
			earlyHeaderBB.putInt(characteristics);
		}

		public void encodeTag(String s) {
			byte[] data = s.getBytes(Charset.forName("Windows-1252"));
			for (int i = 0; i < tag.length; i++) {
				if (i < data.length) {
					tag[i] = data[i];
				} else {
					tag[i] = 0;
				}
			}
		}

		public String decodeTag() {
			int maxL = 0;
			for (int i = 0; i < tag.length; i++)
				if (tag[i] != 0)
					maxL = i + 1;
			return new String(tag, 0, maxL, Charset.forName("Windows-1252"));
		}

		public String toString() {
			return decodeTag() + " : RVA " + Integer.toHexString(virtualAddrRelative) + " VS "
					+ Integer.toHexString(virtualSize) + " : RDS " + Integer.toHexString(rawData.length) + " : CH "
					+ Integer.toHexString(characteristics);
		}

		// -- Copied from the old ExeSec, these routines appear reliable so let's not
		// break anything --

		private void shiftDirTable(ByteBuffer data, int amt, int pointer) {
			// get the # of rsrc subdirs indexed by name
			int nEntry = data.getShort(pointer + 12);
			// get the # of rsrc subdirs indexed by id
			nEntry += data.getShort(pointer + 14);

			// read and shift entries
			int pos = pointer + 16;
			for (int i = 0; i < nEntry; i++) {
				rsrcShift(data, amt, pos + i * 8);
			}
		}

		private void rsrcShift(ByteBuffer data, int amt, int pointer) {
			int rva = data.getInt(pointer + 4);
			if ((rva & 0x80000000) != 0) { // if hi bit 1 points to another directory table
				shiftDirTable(data, amt, rva & 0x7FFFFFFF);
			} else {
				int oldVal = data.getInt(rva);
				data.putInt(rva, oldVal + amt);
			}
		}

		public void shiftResourceContents(int amt) {
			ByteBuffer bb = ByteBuffer.wrap(rawData);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			shiftDirTable(bb, amt, 0);
		}
	}

	public static int uCompare(int a) {
		// 0xFFFFFFFF (-1) becomes 0x7FFFFFFF (highest number)
		// 0x00000000 (0) becomes 0x80000000 (lowest number)
		return a ^ 0x80000000;
	}

	private class AllocationSpan {
		public int start, length;

		public AllocationSpan(int fa, int size) {
			start = fa;
			length = size;
		}

		public boolean collides(AllocationSpan as) {
			return within(as.start) || within(as.start + as.length - 1) || as.within(start)
					|| as.within(start + length - 1);
		}

		private boolean within(int target) {
			return (uCompare(target) >= uCompare(start)) && (uCompare(start + length) > uCompare(target));
		}
	}
}
