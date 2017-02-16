package ca.noxid.lab.script;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.rsrc.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TscDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	TscPane tsc;

	public TscDialog(Frame aFrame, String title, TscPane tp) {
		super(aFrame, title);
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		tsc = tp;
		addComponentsToPane();
		this.setSize(new Dimension(500, 400));

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent eve) {
				if (tsc.isModified()) {
					int r = JOptionPane.showConfirmDialog(TscDialog.this,
							Messages.getString("TscDialog.0"), //$NON-NLS-1$
							"close tsc", JOptionPane.YES_NO_CANCEL_OPTION); //$NON-NLS-1$
					switch (r) {
					case JOptionPane.YES_OPTION:
						tsc.save();
					case JOptionPane.NO_OPTION:
						dispose();
					default:
					}
				} else { //tsc not modified
					dispose();
				}
			}
		});
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		this.setVisible(true);
	}

	private void addComponentsToPane() {

		JButton closeButton = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				dispose();
			}
		});
		closeButton.setText(Messages.getString("TscDialog.1")); //$NON-NLS-1$
		JButton saveButton = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				tsc.save();
			}
		});
		saveButton.setText(Messages.getString("TscDialog.2")); //$NON-NLS-1$

		JPanel rPane = new JPanel();
		rPane.add(closeButton);
		rPane.add(saveButton);
		JPanel pp = new JPanel();
		pp.setLayout(new BoxLayout(pp, BoxLayout.Y_AXIS));
		JScrollPane jsp = new JScrollPane(tsc);
		pp.add(jsp);
		pp.add(rPane, BorderLayout.PAGE_END);
		this.add(pp);
	}
}
