package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class BgList<E> extends JList<E> {
	private static final long serialVersionUID = 200328847993395736L;
	BufferedImage background;

	public BgList(BufferedImage bg) {
		super();
		background = bg;
		this.setCellRenderer(new BgListRender(bg));
		this.setOpaque(false);
	}

	public BgList(Vector<E> listData, BufferedImage bg) {
		super(listData);
		background = bg;
		this.setCellRenderer(new BgListRender(bg));
		this.setOpaque(false);
	}

	public void paintComponent(Graphics g) {
		Dimension d = this.getSize();
		for (int x = 0; x < d.width; x += background.getWidth()) {
			for (int y = 0; y < d.height; y += background.getHeight()) {
				g.drawImage(background, x, y, this);
			}
		}
		super.paintComponent(g);
	}
}
