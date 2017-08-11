package ca.noxid.lab.tile;

import ca.noxid.lab.BlConfig;
import ca.noxid.lab.SignifUndoableEdit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Created by Noxid on 10-Aug-17.
 */
public class TileLayer {
	// for historical reasons, this is stored as [y][x].
	// please be aware of this.
	// furthermore avoid exposing this gross implementation
	// outside of this class
	private int[][] tileData;
	private boolean tilesVisible;
	private boolean typesVisbile;
	private String name;
	private BufferedImage tileset;
	private BlConfig config;

	private BufferedImage displayBuffer;

	public enum LAYER_TYPE {
		TILE_LAYER, GRADIENT_LAYER
	}
	private LAYER_TYPE layerType;

	public TileLayer(String name, int w, int h, BlConfig config, BufferedImage tileset) {
		if (w <= 0) w = 1;
		if (h <= 0) h = 1;
		this.name = name;
		tileData = new int[h][w];
		tilesVisible = true;
		typesVisbile = false;
		this.config = config;
		this.tileset = tileset;
		displayBuffer = new BufferedImage(w*config.getTileSize(), h*config.getTileSize(), BufferedImage.TYPE_INT_ARGB);
		updateBuffer(0, 0, w, h);
	}

	public TileLayer(String name, int[][] data, BlConfig config, BufferedImage tileset) {
		this.name = name;
		tileData = data;
		tilesVisible = true;
		typesVisbile = false;
		this.config = config;
		this.tileset = tileset;
		int w = data[0].length;
		int h = data.length;
		displayBuffer = new BufferedImage(w*config.getTileSize(), h*config.getTileSize(), BufferedImage.TYPE_INT_ARGB);
		updateBuffer(0, 0, w, h);
	}

	/**
	 * resize the map to new dimensions keeping existing tile data where possible
	 * @param newW
	 * @param newH
	 * @return previous tiledata for creating undoable edits
	 */
	public SignifUndoableEdit resize(int newW, int newH) {
		int oldH = tileData.length;
		int oldW = tileData[0].length;
		int[][] newData = new int[newH][newW];
		for (int c = 0;
				c < ((c < oldH) ? newH : oldH);
				c++) {
			for (int r = 0;
					r < ((r < oldW) ? newW : oldW);
					r++) {
				try {
					newData[c][r] = tileData[c][r];
				} catch (ArrayIndexOutOfBoundsException err) {
					System.out.println(String.format(
							"Couldn't resize tile at (%d, %d) on layer %s", r, c, name));
				}
			}
		}
		int[][] oldData = tileData;
		tileData = newData;

		return new LayerEdit(oldData, newData);
	}

	public SignifUndoableEdit shiftLayer(int dx, int dy, int options) {

		int[][] layerDat = tileData;
		int w = layerDat[0].length;
		int h = layerDat.length;
		int[][] oldDat;
		oldDat = new int[h][w];
		for (int i = 0; i < oldDat.length; i++) {
			oldDat[i] = Arrays.copyOf(layerDat[i], layerDat[i].length);
		}
		//<Wistil> was being lazy here, using memory to make all the math go away
		//also I stole this from CE basically
		int i, j;
		int[][] newTiles;
		newTiles = new int[h][w];

		for (i = 0; i < h; i++)
		{
			for (j = 0; j < w; j++)
			{
				int newX = (j+dx+w*100)%w;
				int newY = (i+dy+h*100)%h;
				try {
					if ((options & ShiftDialog.OPTION_WRAP) != 0) {//well some math anyways...
						newTiles[newY][newX] = layerDat[i][j];
					} else {
						if (j+dx < 0 || j+dx >= w || i+dy < 0 || i+dy >= h)
							newTiles[newY][newX] = 0;
						else
							newTiles[newY][newX] = layerDat[i][j];
					}
				} catch (IndexOutOfBoundsException err) {
					System.out.println(String.format(
							"Couldnt shift layer %s. dx:%d dy:%d (%d, %d)", name, dx, dy, j,  i));
				}
			}
		}
		tileData = newTiles;
		return new LayerEdit(oldDat, newTiles);
	}

	public int getTile(int x, int y) {
		return tileData[y][x];
	}

	public void setTile(int x, int y, int val) {
		tileData[y][x] = val;
	}

	private void updateBuffer(int startX, int startY, int w, int h) {
		Graphics2D g2d = displayBuffer.createGraphics();
		int tilesz = config.getTileSize();
		int tilesetW = config.getTilesetWidth();
		for (int x = startX; x < startX + w; x++) {
			for (int y = startY; y < startY + h; y++) {
				int tileID = tileData[y][x];
				int dstX = x*tilesz;
				int dstY = y*tilesz;
				int srcX = (tileID%tilesetW) * tilesz;
				int srcY = (tileID/tilesetW) * tilesz;

				g2d.drawImage(tileset,
						dstX, dstY, dstX + tilesz, dstY + tilesz,
						srcX, srcY, srcX + tilesz, srcY + tilesz, null);
			}
		}
	}

	public void draw(Graphics graphics) {
		graphics.drawImage(displayBuffer, 0, 0, null);


//		BlConfig conf = dataHolder.getConfig();
//		int mapX = dataHolder.getMapX();
//		int mapY = dataHolder.getMapY();
//		int scale = (int) (conf.getTileSize() * EditorApp.mapScale);
//		int srcScale = conf.getTileSize();
//		BufferedImage tileImg = iMan.getImg(dataHolder.getTileset());
//		Rectangle r = g.getClipBounds();
//		int startX = 0;
//		int startY = 0;
//		int endX = mapX;
//		int endY = mapY;
//		if (r != null) {
//			//System.out.println(r);
//			startX = r.x / scale;
//			if (startX < 0) startX = 0;
//			startY = r.y / scale;
//			if (startY < 0) startY = 0;
//			endX = startX + r.width / scale + 2;
//			if (endX < 2) endX = 2;
//			if (endX > mapX) endX = mapX;
//			endY = startY + r.height / scale + 2;
//			if (endY < 2) endY = 2;
//			if (endY > mapY) endY = mapY;
//		}
//
//		int setWidth = conf.getTilesetWidth();
//		if (setWidth <= 0) {
//			//get width as actual fittable tiles
//			setWidth = tileImg.getWidth() / conf.getTileSize();
//		}
//		if (layerType == LAYER_TYPE.GRADIENT_LAYER) {
//			float alpha = dataHolder.getConfig().getGradientAlpha() / 100f;
//			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
//		}
//		//int tDrawn = 0;
//		//System.out.println("startY: " + startY + " endY: " + endY +
//		//		" startX: " + startX + " endX: " + endX);
//		for (int i = startY; i < endY; i++) {
//			for (int j = startX; j < endX; j++) {
//				//tDrawn++;
//				int xPixel = scale * j;
//				int yPixel = scale * i;
//
//				int tileNum = dataHolder.getTile(j, i, layer);
//				//if (tileNum < 0) System.out.println("num-" + tileNum + ", ");
//				int sourceX = (tileNum % setWidth) * srcScale;
//				int sourceY = (tileNum / setWidth) * srcScale;
//				g.drawImage(tileImg,
//						xPixel,
//						yPixel,
//						xPixel + scale,
//						yPixel + scale,
//						sourceX,
//						sourceY,
//						sourceX + srcScale,
//						sourceY + srcScale,
//						null);
//			}
//		}
		//System.out.println("draw " + tDrawn + " tiles");
	}
//
//	public TileRegion getRegion(Rectangle area) {
//		return getRegion(area.x, area.y, area.width, area.height);
//	}
//
//	public TileRegion getRegion(int x, int y, int w, int h) {
//
//	}
//
//	public RegionEdit setRegion(TileRegion region) {
//
//	}

	private class LayerEdit extends SignifUndoableEdit {
		int[][] oldData, newData;
		LayerEdit(int[][] oldm, int[][] newm) {
			oldData = oldm;
			newData = newm;
		}

		@Override
		public void undo() throws CannotUndoException {
			tileData = oldData;
		}

		@Override
		public void redo() throws CannotRedoException {
			tileData = newData;
		}

		@Override
		public boolean canRedo() {
			return true;
		}
	}

	private class RegionEdit extends SignifUndoableEdit {

	}


}
