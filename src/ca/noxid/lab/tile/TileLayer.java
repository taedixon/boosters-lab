package ca.noxid.lab.tile;

import ca.noxid.lab.SignifUndoableEdit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
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

	public TileLayer(String name, int w, int h) {
		if (w <= 0) w = 1;
		if (h <= 0) h = 1;
		this.name = name;
		tileData = new int[h][w];
		tilesVisible = true;
		typesVisbile = false;
	}

	public TileLayer(String name, int[][] data) {
		this.name = name;
		tileData = data;
		tilesVisible = true;
		typesVisbile = false;
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
