package ca.noxid.lab.tile;

import javax.swing.*;
import java.awt.*;

/**
 * Created by noxid on 11/08/17.
 */
public class TileListRender extends DefaultListCellRenderer {

	TileLayer layer;

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (!(value instanceof TileLayer)) {
			return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		} else {
			layer = (TileLayer)value;
			this.setToolTipText(layer.getName());
			this.setText(layer.getName());
			if (isSelected) {
				this.setBorder(BorderFactory.createLineBorder(Color.red));
				this.setBackground(Color.gray.darker());
			} else {
				this.setBorder(BorderFactory.createLineBorder(Color.gray));
				this.setBackground(Color.white.darker());
			}
		}
		this.setPreferredSize(new Dimension(140, 60));
		return this;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g.create();
		g2d.setBackground(this.getBackground());
		g2d.setColor(this.getForeground());
		g2d.clearRect(0, 0, this.getWidth(), this.getHeight());

		//draw a small version of the layer's tiles on each map
		g2d.drawImage(layer.getIcon(), 40, 0, 100, 60, null);
		g2d.drawString(this.getText(), 4, this.getHeight()/2);

	}
}
