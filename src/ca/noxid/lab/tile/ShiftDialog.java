package ca.noxid.lab.tile;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import com.carrotlord.string.StrTools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/* 1.4 example used by DialogDemo.java. */
public class ShiftDialog extends JDialog implements ActionListener {

	public static final int OPTION_ENTITY = 1;
	public static final int OPTION_WRAP = 2;
	public static final int OPTION_TILE = 4;
	public static final int OPTION_LINE = 8;
	private static final long serialVersionUID = -3327313528301701743L;
	protected JCheckBox wrapCheck;
	protected JCheckBox tileCheck;
	protected JCheckBox entCheck;
	protected JCheckBox lineCheck;
	boolean acceptYes = false;
	private JTextField xField;
	private JTextField yField;
	private JButton okButton;
	private JButton cancelButton;

	private int xVal;
	private int yVal;

	/**
	 * Creates the reusable dialog.
	 */
	public ShiftDialog(Frame aFrame) {
		super(aFrame, true);
		this.setLocation(aFrame.getLocation());
		setTitle(Messages.getString("ShiftDialog.0")); //$NON-NLS-1$

		xField = new JTextField(4);
		xField.setText(""); //$NON-NLS-1$
		xField.setColumns(4);
		yField = new JTextField(4);
		yField.setText(""); //$NON-NLS-1$
		yField.setColumns(4);
		wrapCheck = new JCheckBox(Messages.getString("ShiftDialog.3")); //$NON-NLS-1$
		tileCheck = new JCheckBox("Shift Tiles");
		tileCheck.setSelected(true);
		entCheck = new JCheckBox("Shift NPCs");
		entCheck.setSelected(true);
		okButton = new JButton(Messages.getString("ShiftDialog.4")); //$NON-NLS-1$
		okButton.addActionListener(this);
		cancelButton = new JButton(Messages.getString("ShiftDialog.5")); //$NON-NLS-1$
		cancelButton.addActionListener(this);
		lineCheck = new JCheckBox("Shift Lines");
		lineCheck.setSelected(false);

		//Create an array specifying the number of dialog buttons
		//and their text.

		//Create the JOptionPane.
		JPanel dialogPane = new JPanel();
		dialogPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 8;
		c.ipady = 8;
		dialogPane.add(new JLabel(Messages.getString("ShiftDialog.6")), c); //$NON-NLS-1$
		c.gridx = 1;
		dialogPane.add(xField, c);
		c.gridy = 1;
		c.gridx = 0;
		dialogPane.add(new JLabel(Messages.getString("ShiftDialog.7")), c); //$NON-NLS-1$
		c.gridx = 1;
		dialogPane.add(yField, c);
		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		dialogPane.add(wrapCheck, c);
		c.gridy++;
		dialogPane.add(tileCheck, c);
		c.gridy++;
		dialogPane.add(entCheck, c);
		if (EditorApp.EDITOR_MODE == 2) {
			c.gridy++;
			dialogPane.add(lineCheck);
		}
		c.gridwidth = 1;
		c.gridy++;
		dialogPane.add(okButton, c);
		c.gridx++;
		dialogPane.add(cancelButton, c);

		//Make this dialog display it.
		setContentPane(dialogPane);

		//Handle window closing correctly.
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		//Ensure the text field always gets the first focus.
		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent ce) {
				xField.requestFocusInWindow();
			}
		});

		//Register an event handler that puts the text into the option pane.


		this.pack();
		this.setResizable(false);
		//this.setModal(true);
		this.setVisible(false);
	}

	public int getOptions() {
		int rv = 0;
		if (entCheck.isSelected()) rv |= OPTION_ENTITY;
		if (wrapCheck.isSelected()) rv |= OPTION_WRAP;
		if (tileCheck.isSelected()) rv |= OPTION_TILE;
		if (lineCheck.isSelected()) rv |= OPTION_LINE;
		return rv;
	}

	public int getShiftX() {
		return xVal;
	}

	public int getShiftY() {
		return yVal;
	}

	public void init() {
		xField.setText(""); //$NON-NLS-1$
		yField.setText(""); //$NON-NLS-1$
	}

	/**
	 * This method handles events for the text field.
	 */
	public void actionPerformed(ActionEvent eve) {
		if (eve.getSource().equals(cancelButton)) {
			acceptYes = false;
			this.setVisible(false);
		} else if (eve.getSource().equals(okButton)) {
			try {
				if (xField.getText().equals("")) //$NON-NLS-1$
				{
					xVal = 0;
				} else {
					xVal = Integer.parseInt(xField.getText());
				}
				if (yField.getText().equals("")) //$NON-NLS-1$
				{
					yVal = 0;
				} else {
					yVal = Integer.parseInt(yField.getText());
				}
				acceptYes = true;
			} catch (NumberFormatException e) {
				StrTools.msgBox(Messages.getString("ShiftDialog.12")); //$NON-NLS-1$
			}
			this.setVisible(false);
		}
	}

	public boolean accepted() {
		return acceptYes;
	}
}