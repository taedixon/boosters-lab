package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;

public class LinkLabel extends JLabel {
	private static final long serialVersionUID = 447996885658329852L;

	public LinkLabel(String arg, MouseListener clickListener) {
		super(arg);
		this.addMouseListener(clickListener);
		this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		this.setForeground(Color.BLUE);
	}

}
