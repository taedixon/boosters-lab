package ca.noxid.lab.tile;

import ca.noxid.lab.Messages;
import ca.noxid.lab.mapdata.MapInfo;
import com.carrotlord.string.StrTools;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Noxid on 17-Aug-17.
 */
public class PxmLoader implements TileLoader {
	public byte pxmVersion;
	private List<TileLayer> layers;
	private List<MapPoly> polygons;
	private List<LineSeg> lines;
	private BufferedImage tileset;
	private MapInfo mapInfo;

	public int width, height;

	private static final String[] defaultLayerNames = {
			"Far Back",
	        "Back",
	        "Front/Physical",
	        "Far Front",
	        "Gradient"
	};

	public PxmLoader() {

	}

	public void loadMap(File pxmFile, MapInfo mapInfo) throws IOException {
		this.mapInfo = mapInfo;
		this.tileset = mapInfo.getTilesetImage();

		layers = new ArrayList<>();
		polygons = new ArrayList<>();
		lines = new ArrayList<>();

		FileInputStream inStream = new FileInputStream(pxmFile);
		FileChannel inChan = inStream.getChannel();
		ByteBuffer hBuf = ByteBuffer.allocate(4);
		hBuf.order(ByteOrder.LITTLE_ENDIAN);
		inChan.read(hBuf);
		//read the filetag
		hBuf.flip();
		byte tagArray[] = new byte[3];
		hBuf.get(tagArray, 0, 3);
		if (!(new String(tagArray).equals("PXM"))) { //$NON-NLS-1$
			inChan.close();
			inStream.close();
			throw new IOException(Messages.getString("MapInfo.9")); //$NON-NLS-1$
		}
		pxmVersion = hBuf.get();
		switch (pxmVersion) {
			case 0x10:
				loadMapClassic(inChan);
				break;
			case 0x20:
				loadMapKss(inChan);
				break;
			case 0x21:
				loadMapKssWide(inChan);
				break;
			case 0x22:
				loadMapMultiLayer(inChan);
				break;
			case 0x30:
				loadMapMR(inChan);
				break;
			case 0x31:
				loadMapTess(inChan);
				break;
			case 0x32:
				loadMapTessPoly(inChan);
				break;
			case 0x33:
				loadMapTessGradient(inChan);
				break;
			default:
				//throw a fit
		}
		inChan.close();
		inStream.close();
	}

	/*
	1 layer of 1-byte tiles
	 */
	private void loadMapClassic(FileChannel channel) throws IOException {
		readClassicHeader(channel);

		TileLayer baseLayer = loadByteLayer(channel, width, height);
		baseLayer.setName("Front");
		TileLayer backLayer = new TileLayer("Back", width, height, mapInfo.getConfig(), tileset);
		TileLayer frontLayer = new TileLayer("Front", width, height, mapInfo.getConfig(), tileset);

		for (int y = 0; y < frontLayer.getHeight(); y++) {
			for (int x = 0; x < frontLayer.getWidth(); x++) {
				int baseTile = baseLayer.getTile(x, y);
				int pxa = mapInfo.calcPxa(baseTile);
				if (pxa < 0x20) {
					backLayer.setTile(x, y, baseTile);
				} else {
					frontLayer.setTile(x, y, baseTile);
				}
			}
		}
		layers.add(backLayer);
		layers.add(frontLayer);
	}

	/*
	4 layers of 1-byte tiles
	 */
	private void loadMapKss(FileChannel channel) throws IOException {
		readClassicHeader(channel);
		for (int i = 0; i < 4; i++) {
			TileLayer layer = loadByteLayer(channel, width, height);
			layer.setName(defaultLayerNames[i]);
			if (i == 2) {
				layer.setLayerType(TileLayer.LAYER_TYPE.TILE_PHYSICAL);
			}
			layers.add(layer);
		}
	}

	/*
	4 layers of 2-byte tiles
	 */
	private void loadMapKssWide(FileChannel channel) throws IOException {
		readClassicHeader(channel);
		for (int i = 0; i < 4; i++) {
			TileLayer layer = loadShortLayer(channel, width, height);
			layer.setName(defaultLayerNames[i]);
			if (i == 2) {
				layer.setLayerType(TileLayer.LAYER_TYPE.TILE_PHYSICAL);
			}
			layers.add(layer);
		}
	}

	/*
	variable number of 2-byte tile layers with names and types
	 */
	private void loadMapMultiLayer(FileChannel channel) throws IOException {
		readClassicHeader(channel);
		ByteBuffer extraBuf;
		extraBuf = ByteBuffer.allocate(4);
		extraBuf.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(extraBuf);
		extraBuf.flip();
		int nLayer = extraBuf.getInt();

		for (int i = 0; i < nLayer; i++) {
			extraBuf = ByteBuffer.allocate(0x24);
			extraBuf.order(ByteOrder.LITTLE_ENDIAN);
			channel.read(extraBuf);
			extraBuf.flip();
			int layerType = extraBuf.getInt();
			byte[] nameArray = new byte[0x20];
			extraBuf.get(nameArray);
			String layerName = StrTools.CString(nameArray);

			TileLayer layer = loadShortLayer(channel, width, height);
			layer.setName(layerName);
			layer.setLayerType(TileLayer.LAYER_TYPE.fromOrdinal(layerType));
			layers.add(layer);
		}
	}

	/*
	4 layers of 1-byte tiles, lines
	 */
	private void loadMapMR(FileChannel channel) throws IOException {
		readClassicHeader(channel);
		for (int i = 0; i < 4; i++) {
			TileLayer layer = loadByteLayer(channel, width, height);
			layer.setName(defaultLayerNames[i]);
			layers.add(layer);
		}
		loadLines(channel);
	}

	/*
	4 layers of 2-byte tiles, lines
	 */
	private void loadMapTess(FileChannel channel) throws IOException {
		readClassicHeader(channel);
		for (int i = 0; i < 4; i++) {
			TileLayer layer = loadShortLayer(channel, width, height);
			layer.setName(defaultLayerNames[i]);
			layers.add(layer);
		}
		loadLines(channel);
	}

	/*
	4 layers of 2-byte tiles, lines, polygons
	 */
	private void loadMapTessPoly(FileChannel channel) throws IOException {
		readClassicHeader(channel);
		for (int i = 0; i < 4; i++) {
			TileLayer layer = loadShortLayer(channel, width, height);
			layer.setName(defaultLayerNames[i]);
			layers.add(layer);
		}
		loadLines(channel);
		loadPolys(channel);
	}

	/*
	4 layers of 2-byte tiles, 1 layer of 2-byte gradient indicators, lines, polygons
	 */
	private void loadMapTessGradient(FileChannel channel) throws IOException {
		readClassicHeader(channel);
		for (int i = 0; i < 5; i++) {
			TileLayer layer = loadShortLayer(channel, width, height);
			layer.setName(defaultLayerNames[i]);
			if (i == 4) {
				layer.setLayerType(TileLayer.LAYER_TYPE.GRADIENT_LAYER);
			}
			layers.add(layer);
		}
		loadLines(channel);
		loadPolys(channel);
	}

	private TileLayer loadByteLayer(FileChannel channel, int w, int h) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(w*h);
		channel.read(buf);
		buf.flip();
		int[][] values = new int[h][w];

		for (int y = 0; y < values.length; y++) {
			for (int x = 0; x < values[y].length; x++) {
				values[y][x] = buf.get() & 0xFF;
			}
		}

		TileLayer layer = new TileLayer("Layer", values, mapInfo.getConfig(), tileset);
		return layer;
	}

	private TileLayer loadShortLayer(FileChannel channel, int w, int h) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(w*h*2);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(buf);
		buf.flip();
		int[][] values = new int[h][w];

		for (int y = 0; y < values.length; y++) {
			for (int x = 0; x < values[y].length; x++) {
				values[y][x] = buf.getShort() & 0xFFFF;
			}
		}

		TileLayer layer = new TileLayer("Layer", values, mapInfo.getConfig(), tileset);
		return layer;
	}

	private void readClassicHeader(FileChannel channel) throws IOException {
		ByteBuffer headerBuf = ByteBuffer.allocate(4);
		headerBuf.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(headerBuf);
		headerBuf.flip();

		width = headerBuf.getShort() & 0xFFFF;
		height = headerBuf.getShort() & 0xFFFF;
	}

	private void loadLines(FileChannel channel) throws IOException {
		ByteBuffer lineBuf;
		ByteBuffer lineCount = ByteBuffer.allocate(4);
		lineCount.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(lineCount);
		int nLines = lineCount.getInt(0);
		lineBuf = ByteBuffer.allocate(nLines*20); //5 ints per line, 4 bytes per int
		lineBuf.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(lineBuf);
		lineBuf.flip();

		while (lineBuf.hasRemaining()) {
			Point p1 = new Point(lineBuf.getInt(), lineBuf.getInt());
			Point p2 = new Point(lineBuf.getInt(), lineBuf.getInt());
			LineSeg seg = new LineSeg(p1, p2, lineBuf.getInt());
			lines.add(seg);
		}
	}

	private void loadPolys(FileChannel channel) throws IOException {
		ByteBuffer polyBuf;
		ByteBuffer polyCount = ByteBuffer.allocate(8);
		polyCount.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(polyCount);
		polyCount.flip();
		polyBuf = ByteBuffer.allocate(polyCount.getInt());
		polyBuf.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(polyBuf);
		polyBuf.flip();

		while (polyBuf.hasRemaining()) {
			short type = polyBuf.getShort();
			short event = polyBuf.getShort();
			short pointCount = polyBuf.getShort();
			MapPoly current = new MapPoly(
					new Point(polyBuf.getInt(), polyBuf.getInt()),
					ca.noxid.lab.tile.TypeConfig.getType(type));
			for (int i = 1; i < pointCount; i++) {
				current.extend(new Point(polyBuf.getInt(), polyBuf.getInt()));
			}
			current.setEvent(event);
			polygons.add(current);
		}
	}

	@Override
	public List<TileLayer> getLayers() {
		return layers;
	}

	@Override
	public List<LineSeg> getLines() {
		return lines;
	}

	@Override
	public List<MapPoly> getPolygons() {
		return polygons;
	}
}
