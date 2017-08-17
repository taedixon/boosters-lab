package ca.noxid.lab.tile;

import ca.noxid.lab.BlConfig;
import ca.noxid.lab.SignifUndoableEdit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Created by Noxid on 10-Aug-17.
 */
public class TileLayer {
	// for historical reasons, this is stored as [y][x].
	// please be aware of this.
	// furthermore avoid exposing this weird implementation
	// outside of this class
	private int[][] tileData;
	private String name;
	private BufferedImage tileset;
	private BufferedImage icon;
	private BlConfig config;

	int tilesChanged;
	int iconRefreshThreshold = 50;

	private BufferedImage displayBuffer;

	public String getName() {
		return name;
	}

	public BufferedImage getIcon() {
		return icon;
	}

	public LAYER_TYPE getType() {
		return layerType;
	}

	public void setName(String name) {
		this.name = name;
	}

	public enum LAYER_TYPE {
		TILE_LAYER, GRADIENT_LAYER, TILE_PHYSICAL
	}
	private LAYER_TYPE layerType;

	public TileLayer(String name, int w, int h, BlConfig config, BufferedImage tileset) {
		if (w <= 0) w = 1;
		if (h <= 0) h = 1;
		this.name = name;
		this.tileData = new int[h][w];
		this.config = config;
		this.tileset = tileset;
		init();
	}

	public TileLayer(String name, int[][] data, BlConfig config, BufferedImage tileset) {
		this.name = name;
		this.tileData = data;
		this.config = config;
		this.tileset = tileset;
		init();

	}

	public TileLayer(TileLayer copyOf) {
		this.name = copyOf.name + " copy";
		this.tileData = copyOf.tileData;
		this.config = copyOf.config;
		this.tileset = copyOf.tileset;
		init();
	}

	private void init() {
		layerType = LAYER_TYPE.TILE_LAYER;
		createDisplayBuffer();
		icon = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		updateIcon();
	}

	private void createDisplayBuffer() {
		if (displayBuffer != null) {
			displayBuffer.flush();
		}
		int w = tileData[0].length;
		int h = tileData.length;
		displayBuffer = new BufferedImage(w*config.getTileSize(), h*config.getTileSize(), BufferedImage.TYPE_INT_ARGB);

		updateBuffer(0, 0, w, h);
	}

	public void setLayerType(LAYER_TYPE t) {
		layerType = t;
	}

	public void merge(TileLayer layer) {
		for (int y = 0; y < tileData.length; y++) {
			for (int x = 0; x < tileData[0].length; x++) {
				int tileval = layer.getTile(x, y);
				if (tileval > 0) {
					tileData[y][x] = tileval;
				}
			}
		}
		updateBuffer(0, 0, tileData[0].length, tileData.length);
		updateIcon();
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
		if (x < tileData[0].length && y < tileData.length) {
			return tileData[y][x];
		} else {
			return 0;
		}
	}

	public void setTile(int x, int y, int val) {
		if (x < tileData[0].length && y < tileData.length) {
			tileData[y][x] = val;
			updateBuffer(x, y, 1, 1);
			if (++tilesChanged > iconRefreshThreshold) {
				updateIcon();
			}
		}
	}

	private void updateBuffer(int startX, int startY, int w, int h) {
		Graphics2D g2d = displayBuffer.createGraphics();
		g2d.setComposite(AlphaComposite.Src);
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

	public void draw(Graphics2D graphics, double scale) {
		AffineTransform transform = new AffineTransform();
		transform.setToScale(scale, scale);
		graphics.drawImage(displayBuffer, transform, null);
	}

	private void updateIcon() {
		Graphics2D g2d = icon.createGraphics();
		g2d.setComposite(AlphaComposite.Src);
		g2d.drawImage(displayBuffer, 0, 0, 100, 100, null);
		tilesChanged = 0;
	}

	private class LayerEdit extends SignifUndoableEdit {
		int[][] oldData, newData;
		boolean shouldRebuildBuffer;
		LayerEdit(int[][] oldm, int[][] newm) {
			oldData = oldm;
			newData = newm;
			//if the new map has a different size, we need to create a new buffer to render it to
			shouldRebuildBuffer = oldm[0].length != newm[0].length || oldm.length != newm.length;
		}

		@Override
		public void undo() throws CannotUndoException {
			tileData = oldData;
			if (shouldRebuildBuffer) {
				createDisplayBuffer();
			} else {
				updateBuffer(0, 0, tileData[0].length, tileData.length);
			}
			updateIcon();
		}

		@Override
		public void redo() throws CannotRedoException {
			tileData = newData;
			if (shouldRebuildBuffer) {
				createDisplayBuffer();
			} else {
				updateBuffer(0, 0, tileData[0].length, tileData.length);
			}
			updateIcon();
		}
	}

}
