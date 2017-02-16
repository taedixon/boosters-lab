package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@SuppressWarnings("UnusedDeclaration")
public class BgTextArea extends JTextArea {
	private static final long serialVersionUID = -3197395604648619462L;
	BufferedImage background;

	protected BgTextArea(BufferedImage i) {
		background = i;
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
