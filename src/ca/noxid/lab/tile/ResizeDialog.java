package ca.noxid.lab.tile;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.rsrc.ResourceManager;
import com.carrotlord.string.StrTools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


class ResizeDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -5539124011370449080L;
	boolean acceptYes = false;
	private JTextField xField;
	private JTextField yField;
	private JButton okButton;
	private JButton cancelButton;
	private int xVal;
	private int yVal;

	public ResizeDialog(Frame aFrame, int defaultX, int defaultY) {
		super(aFrame, true);
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		this.setLocation(aFrame.getLocation());
		this.setTitle(Messages.getString("ResizeDialog.0"));  //$NON-NLS-1$
		xField = new JTextField(4);
		xField.setColumns(4);
		yField = new JTextField(4);
		yField.setColumns(4);
		okButton = new JButton(Messages.getString("ResizeDialog.1")); //$NON-NLS-1$
		okButton.addActionListener(this);
		cancelButton = new JButton(Messages.getString("ResizeDialog.2")); //$NON-NLS-1$
		cancelButton.addActionListener(this);

		init(defaultX, defaultY);

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
		dialogPane.add(new JLabel(Messages.getString("ResizeDialog.3")), c); //$NON-NLS-1$
		c.gridx = 1;
		dialogPane.add(xField, c);
		c.gridy = 1;
		c.gridx = 0;
		dialogPane.add(new JLabel(Messages.getString("ResizeDialog.4")), c); //$NON-NLS-1$
		c.gridx = 1;
		dialogPane.add(yField, c);
		c.gridx = 0;
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

	public int getSizeX() {
		return xVal;
	}

	public int getSizeY() {
		return yVal;
	}

	public void init(int defaultX, int defaultY) {
		xField.setText(Integer.toString(defaultX));
		yField.setText(Integer.toString(defaultY));
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
				//check valid
				if (xVal < 1 || xVal > 3000) {
					throw new IllegalArgumentException();
				}
				if (yField.getText().equals("")) //$NON-NLS-1$
				{
					yVal = 0;
				} else {
					yVal = Integer.parseInt(yField.getText());
				}
				if (yVal < 1 || yVal > 3000) {
					throw new IllegalArgumentException();
				}
				acceptYes = true;
			} catch (NumberFormatException e) {
				StrTools.msgBox(Messages.getString("ResizeDialog.7")); //$NON-NLS-1$
			} catch (IllegalArgumentException e) {
				StrTools.msgBox(Messages.getString("ResizeDialog.8") + //$NON-NLS-1$
						Messages.getString("ResizeDialog.9")); //$NON-NLS-1$
			}
			this.setVisible(false);
		}
	}

	public boolean accepted() {
		return acceptYes;
	}
}
