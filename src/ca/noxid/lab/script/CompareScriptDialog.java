package ca.noxid.lab.script;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Created by noxid on 23/05/17.
 */
class CompareScriptDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private File selection;

	public File getSelection() {
		return selection;
	}

	CompareScriptDialog(final File srcFile, final File scriptFile, String encoding, ScriptStyler styler) {
		super();
		selection = scriptFile;
		JSplitPane disp = new JSplitPane();

		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		String opt1 = TscPane.parseScript(srcFile, encoding);
		JTextPane sourcePane = new JTextPane();
		sourcePane.setText(opt1);
		sourcePane.setEditable(false);
		styler.highlightDoc(sourcePane.getStyledDocument(), 0, -1);
		JScrollPane jsp = new JScrollPane(sourcePane);
		jsp.setPreferredSize(new Dimension(320, 480));
		left.add(jsp);
		JButton button = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				selection = srcFile;
				dispose();
			}

		});
		button.setText("Use ScriptSource version");
		left.add(button);

		String opt2 = TscPane.parseScript(scriptFile, encoding);
		JPanel right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		sourcePane = new JTextPane();
		sourcePane.setText(opt2);
		sourcePane.setEditable(false);
		styler.highlightDoc(sourcePane.getStyledDocument(), 0, -1);
		jsp = new JScrollPane(sourcePane);
		right.add(jsp);
		jsp.setPreferredSize(new Dimension(320, 480));
		button = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				selection = scriptFile;
				dispose();
			}

		});
		button.setText("Use TSC version");
		right.add(button);

		disp.setLeftComponent(left);
		disp.setRightComponent(right);

		this.setContentPane(disp);
		this.pack();

		this.setModal(true);
		this.setVisible(true);
	}
}
