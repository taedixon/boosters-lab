package ca.noxid.lab.script;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.uiComponents.BgPanel;
import com.carrotlord.string.StrTools;

import javax.script.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

public class TscBuilder extends JPanel {
	private static final long serialVersionUID = -6958912249152141539L;
	JTextPane outputArea;
	JTextPane inputArea;

	public TscBuilder(BufferedImage i)	{
		this.buildComponents(i);
	}

	private void buildComponents(BufferedImage i) {
		JPanel leftPanel = new BgPanel(new GridBagLayout(), i);
		leftPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		this.setPreferredSize(new Dimension(520, 240));
		JPanel rightPanel = new BgPanel(new GridBagLayout(), i);
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.7;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		//c.anchor = GridBagConstraints.NORTHWEST;
		this.add(leftPanel, c);
		c.weightx = 0.3;
		c.gridx = 1;
		this.add(rightPanel, c);

		inputArea = new JTextPane();
		inputArea.setBorder(BorderFactory.createLineBorder(inputArea.getForeground()));
		inputArea.setMinimumSize(new Dimension(240, 148));
		JScrollPane jsp = new JScrollPane(inputArea);
		//jsp.setMinimumSize(new Dimension(145, 256));
		//jsp.setPreferredSize(new Dimension(240, 120));

		//gridbag
		c.gridx = 0;
		c.weightx = 0.9;
		c.weighty = 0.7;
		leftPanel.add(jsp, c);
		JButton aButton = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent eve) {
				String contents = inputArea.getText();
				ScriptEngineManager mgr = new ScriptEngineManager();
				ScriptEngine e = mgr.getEngineByName(Messages.getString("TscBuilder.4")); //$NON-NLS-1$
				Bindings bind = e.getBindings(ScriptContext.ENGINE_SCOPE);
				bind.put(Messages.getString("TscBuilder.3"), outputArea); //$NON-NLS-1$

				try {
					e.eval(contents);
					//outputArea.setText(result.toString());
				} catch (ScriptException e1) {
					StrTools.msgBox(Messages.getString("TscBuilder.2")); //$NON-NLS-1$
				}
			}
		});
		aButton.setText(Messages.getString("TscBuilder.0")); //$NON-NLS-1$
		aButton.setOpaque(false);
		//gridbag
		c.gridx++;
		c.weightx = 0.1;
		c.fill = GridBagConstraints.NONE;
		leftPanel.add(aButton, c);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.5;
		c.weighty = 0.5;
		URL kittenURL = EditorApp.class.getResource("rsrc/RainbowCat.gif"); //$NON-NLS-1$
		ImageIcon catImg = new ImageIcon(kittenURL, "pic"); //$NON-NLS-1$
		JLabel kittyLabel = new JLabel(catImg);
		leftPanel.add(kittyLabel, c); //$NON-NLS-1$


		outputArea = new JTextPane();
		outputArea.setBorder(BorderFactory.createLineBorder(inputArea.getForeground()));
		jsp = new JScrollPane(outputArea);
		//jsp.setMinimumSize(new Dimension(145, 256));
		//gridbag
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		rightPanel.add(jsp, c);
	}

}
