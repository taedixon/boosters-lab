package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SplashTabPane extends JTabbedPane {
	private static final long serialVersionUID = -7794512987355595924L;
	BufferedImage background;
	BufferedImage cornerLeft;
	BufferedImage cornerRight;

	public SplashTabPane(BufferedImage c1, BufferedImage c2, BufferedImage mid) {
		background = mid;
		cornerLeft = c1;
		cornerRight = c2;

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (this.getTabCount() == 0) {
			g.setColor(Color.decode("0x30203D"));
			Rectangle clip = g.getClipBounds();
			g.fillRect(clip.x, clip.y, clip.width, clip.height);
			Dimension d = this.getSize();
			int x = (d.width - background.getWidth()) / 2;
			int y = (d.height - background.getHeight()) / 2;
			g.drawImage(background, x, y, this);

			g.drawImage(cornerLeft, 0, 0, this);
			g.drawImage(cornerRight, d.width - cornerRight.getWidth(),
					d.height - cornerRight.getHeight(), this);
		}
	}
}
