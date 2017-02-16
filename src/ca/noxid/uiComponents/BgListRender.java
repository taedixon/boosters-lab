package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BgListRender extends DefaultListCellRenderer {
	///TODO shit
	private static final long serialVersionUID = 6831932103635388073L;
	BufferedImage background;

	public BgListRender(BufferedImage img) {
		super();
		background = img;
		this.setOpaque(false);
	}

	@Override
	public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list,
			Object value, int index, boolean isSelected, boolean hasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
		boolean opq = false;
		for (int i : list.getSelectedIndices()) {
			if (i == index) {
				opq = true;
				break;
			}
		}
		setOpaque(opq);
		return this;
	}

	@Override
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
