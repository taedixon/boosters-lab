package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class UpdateTextField extends JTextField implements KeyListener, ActionListener {
	private static final long serialVersionUID = 5123472341660584854L;
	private static final Color bgCol = Color.decode("0xC9E4FF");
	boolean updated = false;
	private boolean focusSwitchAction = true;

	public UpdateTextField() {
		super();
		this.addKeyListener(this);
		this.addActionListener(this);
	}

	public UpdateTextField(String s) {
		super(s);
		this.addKeyListener(this);
		this.addActionListener(this);
	}

	public UpdateTextField(int i) {
		super(i);
		this.addKeyListener(this);
		this.addActionListener(this);
	}

	public void setFireActionOnFocusSwitch(boolean tf) {
		focusSwitchAction = tf;
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		//ignore
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		//ignore
	}

	@Override
	public void keyTyped(KeyEvent eve) {
		char k = eve.getKeyChar();
		if (k >= ' ' || k == 8 || k == 22) { //character or backspace or paste
			this.setBackground(bgCol);
			updated = true;
		}
	}

	@Override
	public void actionPerformed(ActionEvent eve) {
		refresh();
	}

	@Override
	public void processFocusEvent(FocusEvent eve) {
		super.processFocusEvent(eve);
		if (updated && focusSwitchAction) {
			this.fireActionPerformed();
		}
	}

	/**
	 * This sets updated to false and clears the bg
	 * as an override maybe to work around sequencing
	 */
	public void refresh() {
		this.setBackground(Color.white);
		updated = false;
	}

	public boolean isCommited() {
		//if the bg color isn't the special color it has been committed
		return !updated;
	}
}
