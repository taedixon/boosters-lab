package ca.noxid.uiComponents;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BgSlider extends JSlider {
	private static final long serialVersionUID = -5870773689039290025L;
	BufferedImage background;

	public BgSlider(int orientation, int min, int max, int initial, BufferedImage bg) {
		super(orientation, min, max, initial);
		background = bg;
		this.setOpaque(false);
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,
				Color.decode("0xCFE5A0"),
				Color.decode("0x6B8258")));
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
