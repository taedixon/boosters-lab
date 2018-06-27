package ca.noxid.lab.tile;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.mapdata.MapInfo;
import ca.noxid.lab.rsrc.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class TilesetPane extends JPanel {

	private static final String[] tileNames = {
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.2"), Messages.getString("TilesetPane.3"),
			Messages.getString("TilesetPane.3"), Messages.getString("TilesetPane.5"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			Messages.getString("TilesetPane.0"), Messages.getString("TilesetPane.0"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			Messages.getString("TilesetPane.33"), Messages.getString("TilesetPane.33"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.65"),
			Messages.getString("TilesetPane.66"), Messages.getString("TilesetPane.67"),
			Messages.getString("TilesetPane.68"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.70"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.80"), Messages.getString("TilesetPane.81"),
			Messages.getString("TilesetPane.82"), Messages.getString("TilesetPane.83"),
			Messages.getString("TilesetPane.84"), Messages.getString("TilesetPane.85"),
			Messages.getString("TilesetPane.86"), Messages.getString("TilesetPane.87"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.96"), Messages.getString("TilesetPane.97"),
			Messages.getString("TilesetPane.98"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.100"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.112"), Messages.getString("TilesetPane.113"),
			Messages.getString("TilesetPane.114"), Messages.getString("TilesetPane.115"),
			Messages.getString("TilesetPane.116"), Messages.getString("TilesetPane.117"),
			Messages.getString("TilesetPane.118"), Messages.getString("TilesetPane.119"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.128"), Messages.getString("TilesetPane.129"),
			Messages.getString("TilesetPane.130"), Messages.getString("TilesetPane.131"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.160"), Messages.getString("TilesetPane.161"),
			Messages.getString("TilesetPane.162"), Messages.getString("TilesetPane.163"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77"),
			Messages.getString("TilesetPane.77"), Messages.getString("TilesetPane.77")
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
	};

	private static final long serialVersionUID = -4160450343759615331L;
	public static Color bgCol = Color.DARK_GRAY;
	ResourceManager iMan;
	//private File pxaFile;
	//private File tileset;
	MapInfo dataHolder;
	JPopupMenu popup;
	JLabel tileidLabel = new JLabel("dummy");
	int popTileX;
	int popTileY;
	int selectionX, selectionY;
	int selectionW, selectionH;
	int maxTilesX, maxTilesY;
	MapPane parent;

	TilesetPane(MapPane p, ResourceManager i, MapInfo inf) {
		dataHolder = inf;
		iMan = i;
		//tileset = tileFile;
		//this.pxaFile = pxaFile;
		parent = p;
		TileMouseAdapter tma = new TileMouseAdapter();
		this.addMouseListener(tma);
		this.addMouseMotionListener(tma);

		this.setPreferredSize(new Dimension(iMan.getImgH(dataHolder.getTileset()),
				iMan.getImgW(dataHolder.getTileset())));
		this.revalidate();
		selectionX = 0;
		selectionY = 0;
		selectionW = 1;
		selectionH = 1;
		setTileBounds();

		//setup popup menu
		popup = new JPopupMenu();
		TileTypePane ttp = new TileTypePane();
		popup.add(tileidLabel);
		popup.add(ttp);
		popup.add(ttp.getLabel());
	}

	public void setTileBounds() {
		maxTilesX = iMan.getImgW(dataHolder.getTileset()) /
				dataHolder.getConfig().getTileSize() - 1;
		maxTilesY = iMan.getImgH(dataHolder.getTileset()) /
				dataHolder.getConfig().getTileSize() - 1;
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
		super.paintComponent(g2d);
		int sc = (int) (dataHolder.getConfig().getTileSize() * EditorApp.tilesetScale);
		//calculate the scaled display size
		int dispW = (int) (iMan.getImgW(dataHolder.getTileset()) * EditorApp.tilesetScale);
		int dispH = (int) (iMan.getImgH(dataHolder.getTileset()) * EditorApp.tilesetScale);
		BufferedImage tileImg = iMan.getImg(dataHolder.getTileset());
		g2d.clipRect(0, 0, dispW, dispH);
		this.setPreferredSize(new Dimension(dispW, dispH));
		//draw the transparent thingy
		Graphics2D gBackLight = (Graphics2D) g2d.create();
		gBackLight.setColor(bgCol.brighter());
		Graphics2D gBackDark = (Graphics2D) g2d.create();
		gBackDark.setColor(bgCol.darker());
		int sqSize = 16;
		int halfSqSize = sqSize / 2;
		for (int x = 0; x < dispW; x += sqSize) {
			for (int y = 0; y < dispH; y += sqSize) {
				gBackLight.fillRect(x, y, sqSize, sqSize);
				gBackDark.fillRect(x + halfSqSize, y, halfSqSize, halfSqSize);
				gBackDark.fillRect(x, y + halfSqSize, halfSqSize, halfSqSize);
			}
		}
		//Draw the tileset
		g2d.drawImage(tileImg, 0, 0, dispW, dispH, null);
		//draw the tile types if applicable
		if (parent.parent.getOtherDrawOptions()[0]) {
			BufferedImage legend = iMan.getImg(ResourceManager.rsrcTiles); //$NON-NLS-1$
			for (int x = 0; x < dispW / sc; x++) {
				for (int y = 0; y < dispH / sc; y++) {
					int destX = x * sc;
					int destY = y * sc;
					int srcX = (dataHolder.calcPxa(y * 0x10 + x) % 0x10) * 16;
					int srcY = (dataHolder.calcPxa(y * 0x10 + x) / 0x10) * 16;
					g2d.drawImage(legend,
							destX,
							destY,
							destX + sc,
							destY + sc,
							srcX,
							srcY,
							srcX + 16,
							srcY + 16,
							this);

				}
			}
		}
		//draw the cursor
		g2d.setXORMode(Color.white);
		int circleX, circleY;
		if (selectionW < 0) {
			circleX = selectionX + selectionW + 1;
		} else {
			circleX = selectionX;
		}
		if (selectionH < 0) {
			circleY = selectionY + selectionH + 1;
		} else {
			circleY = selectionY;
		}
		g2d.drawRoundRect(circleX * sc, circleY * sc, Math.abs(selectionW) * sc, Math.abs(selectionH) * sc, 8, 8);
		if (dataHolder.getConfig().getTileSize() == 32) {
			g2d.drawRoundRect(circleX * sc + 1, circleY * sc + 1, Math.abs(selectionW) * sc - 2,
					Math.abs(selectionH) * sc - 2, 8, 8);
		}
		this.revalidate();
	}

	protected MapPane.TileBuffer createPen() {
		int baseX, baseY;
		MapPane.TileBuffer retVal = parent.new TileBuffer();
		if (selectionW < 0) {
			baseX = selectionX + selectionW + 1;
		} else {
			baseX = selectionX;
		}
		if (selectionH < 0) {
			baseY = selectionY + selectionH + 1;
		} else {
			baseY = selectionY;
		}
		int w = Math.abs(selectionW);
		int h = Math.abs(selectionH);
		int[][] tileDat = new int[w][h];
		int width = dataHolder.getConfig().getTilesetWidth();
		if (width <= 0) {
			//get width as actual fittable tiles
			width = iMan.getImg(dataHolder.getTileset()).getWidth() /
					dataHolder.getConfig().getTileSize();
		}
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				tileDat[x][y] = (baseY + y) * width + (baseX + x);
			}
		}
		retVal.data = tileDat;
		retVal.dx = selectionX - baseX;
		retVal.dy = selectionY - baseY;
		return retVal;
	}

	public void save() {
		iMan.savePxa(dataHolder.getPxa());
	}

	class TileMouseAdapter extends MouseAdapter {
		int lastX = -1;
		int lastY = -1;

		@Override
		public void mouseClicked(MouseEvent eve) {
			if (eve.getClickCount() == 2) {
				parent.parent.dockOrUndockTileset();
				eve.consume();
			}
		}

		@Override
		public void mousePressed(MouseEvent eve) {
			int scale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.tilesetScale);
			selectionX = eve.getX() / scale;
			selectionY = eve.getY() / scale;
			if (eve.isPopupTrigger()) {
				popTileX = selectionX;
				popTileY = selectionY;
				tileidLabel.setText("Tile #" + (popTileY * (maxTilesX + 1) + popTileX));
				popup.show(eve.getComponent(), eve.getX(), eve.getY());
				return;
			}
			if (selectionX > maxTilesX) {
				selectionX = maxTilesX;
			}
			if (selectionY > maxTilesY) {
				selectionY = maxTilesY;
			}
			selectionW = 1;
			selectionH = 1;
			lastX = selectionX;
			lastY = selectionY;
			TilesetPane tp = (TilesetPane) eve.getSource();
			tp.repaint();
			parent.updatePen(createPen());
		}

		@Override
		public void mouseReleased(MouseEvent eve) {
			int scale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.tilesetScale);
			int currentX = eve.getX() / scale;
			int currentY = eve.getY() / scale;
			if (eve.isPopupTrigger()) {
				popTileX = currentX;
				popTileY = currentY;
				tileidLabel.setText("Tile #" + (popTileY * (maxTilesX + 1) + popTileX));
				popup.show(eve.getComponent(), eve.getX(), eve.getY());
			}

		}

		@Override
		public void mouseDragged(MouseEvent eve) {
			int scale = (int) (dataHolder.getConfig().getTileSize() * EditorApp.tilesetScale);
			int currentX = eve.getX() / scale;
			int currentY = eve.getY() / scale;
			if (currentX > maxTilesX) {
				currentX = maxTilesX;
			}
			if (currentX < 0) {
				currentX = 0;
			}
			if (currentY > maxTilesY) {
				currentY = maxTilesY;
			}
			if (currentY < 0) {
				currentY = 0;
			}
			//check to see if our thing has moved
			if (currentX != lastX || currentY != lastY) {
				if (currentX < selectionX) {
					selectionW = currentX - selectionX - 1;
				} else {
					selectionW = currentX - selectionX + 1;
				}
				if (currentY < selectionY) {
					selectionH = currentY - selectionY - 1;
				} else {
					selectionH = currentY - selectionY + 1;
				}
				((TilesetPane) eve.getSource()).repaint();
				parent.updatePen(createPen());
				lastX = currentX;
				lastY = currentY;
			}
		}
	}

	private class TileTypePane extends JPanel {
		/**
		 *
		 */
		private static final long serialVersionUID = -1485121541731376313L;
		JLabel typeLabel;

		TileTypePane() {
			typeLabel = new JLabel(Messages.getString("TilesetPane.273")); //$NON-NLS-1$
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent eve) {
					TilesetPane parent = (TilesetPane) popup.getInvoker();
					int sc = 16;
					int selX = eve.getX() / sc;
					if (selX < 0 || selX > 0x10) {
						popup.setVisible(false);
						return;
					}
					int selY = eve.getY() / sc;
					if (selY < 0 || selY > 0x10) {
						popup.setVisible(false);
						return;
					}
					byte[] pxaData = iMan.getPxa(dataHolder.getPxa());
					pxaData[popTileY * 0x10 + popTileX] = (byte) (selY * 0x10 + selX);
					parent.parent.dataHolder.markChanged();
					parent.repaint();
					//parent.parent.redraw();
					parent.parent.repaint();
					popup.setVisible(false);
				}
			});
			this.addMouseMotionListener(new MouseAdapter() {
				int prvX = 0;
				int prvY = 0;

				@Override
				public void mouseMoved(MouseEvent eve) {
					int currentX = eve.getX() / 16;
					int currentY = eve.getY() / 16;
					if (currentX != prvX || currentY != prvY) {
						typeLabel.setText(Messages.getString("TilesetPane.274") + String.format("%X",
								(currentY * 0x10 + currentX)) + " - " + tileNames[currentY * 0x10 + currentX]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						typeLabel.repaint();
						prvX = currentX;
						prvY = currentY;
					}
				}
			});
			BufferedImage tilesImg = iMan.getImg(ResourceManager.rsrcTiles); //$NON-NLS-1$
			this.setPreferredSize(new Dimension(tilesImg.getWidth(), tilesImg.getHeight()));
		}

		public Component getLabel() {
			return typeLabel;
		}

		public void paint(Graphics g) {
			this.setBackground(Color.black);
			super.paint(g);
			g.drawImage(iMan.getImg(ResourceManager.rsrcTiles), //$NON-NLS-1$
					0,
					0,
					this);
			//twice b/c the image is pre-transparented
			g.drawImage(iMan.getImg(ResourceManager.rsrcTiles), //$NON-NLS-1$
					0,
					0,
					this);
		}
	}
}
