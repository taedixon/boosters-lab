package ca.noxid.lab;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.carrotlord.string.StrTools;

import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.uiComponents.BgPanel;

public class OOBFlagDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JTextField addrField, valField;
	private JComboBox<String> sizeList;
	private JTextArea outArea;
	private JButton genButton, copyButton;

	private static final String[] sizes = {
		"BYTE (8 bits)",
		"WORD (16 bits)",
		"DWORD (32 bits)"
	};

	private static final long[] maxValues = {
		0xFF,
		0xFFFF,
		0xFFFFFFFF & 0xFFFFFFFFL
	};

	private static final int[] bitCounts = {
		8,
		16,
		32
	};

	public OOBFlagDialog(Frame parent, ResourceManager iMan) {
		super(parent, true);
		if (EditorApp.blazed)
			setCursor(ResourceManager.cursor);
		setLocation(parent.getLocation());
		setTitle("OOB Flag Generator");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addComponentsToPane(iMan.getImg(ResourceManager.rsrcBgBlue));
		pack();
		setResizable(false);
		setVisible(true);
	}

	private void addComponentsToPane(java.awt.image.BufferedImage bgImage) {
		addrField = new JTextField("49DDA0");
		valField = new JTextField("0");
		sizeList = new JComboBox<String>(sizes);
		outArea = new JTextArea();
		outArea.setEditable(false);
		genButton = new JButton("Generate");
		genButton.addActionListener(this);
		genButton.setOpaque(false);
		copyButton = new JButton("Copy to Clipboard");
		copyButton.addActionListener(this);
		copyButton.setOpaque(false);

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
		BgPanel fieldPanel = new BgPanel(bgImage);
		fieldPanel.setLayout(new GridLayout(0, 2));
		fieldPanel.add(new JLabel("OOB Flag Generator"));
		fieldPanel.add(new JLabel(""));
		fieldPanel.add(new JLabel("Address (hex):"));
		fieldPanel.add(addrField);
		fieldPanel.add(new JLabel("Value (hex):"));
		fieldPanel.add(valField);
		fieldPanel.add(new JLabel("Value Size:"));
		fieldPanel.add(sizeList);
		c.add(fieldPanel);
		BgPanel outPanel = new BgPanel(bgImage);
		JScrollPane outScroll = new JScrollPane(outArea);
		outScroll.setPreferredSize(new Dimension(400, 200));
		outPanel.add(outScroll);
		c.add(outPanel);
		BgPanel btnPanel = new BgPanel(bgImage);
		btnPanel.add(genButton);
		btnPanel.add(copyButton);
		c.add(btnPanel);
		setContentPane(c);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(genButton)) {
			long flag = 0;
			try {
				flag = Long.parseLong(addrField.getText(), 16);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, "Whatever's in the address field, it's not a hexadecimal number!",
						"Bad number format", JOptionPane.ERROR_MESSAGE);
				return;
			}
			flag &= 0xFFFFFFFFL;
			flag -= 0x49DDA0;
			if (flag > 0x288E || flag < -0x8AE) {
				JOptionPane.showMessageDialog(this, "Address is too far away from the flag array!",
						"Address too far away", JOptionPane.ERROR_MESSAGE);
				return;
			}
			flag *= 8; // multiply by 8 to get actual flag ID
			long val = 0;
			try {
				val = Long.parseLong(valField.getText(), 16);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, "Whatever's in the value field, it's not a hexadecimal number!",
						"Bad number format", JOptionPane.ERROR_MESSAGE);
				return;
			}
			val &= 0xFFFFFFFFL;
			// 0 - byte (8 bits, max 0xFF)
			// 1 - word (16 bits, max 0xFFFF)
			// 2 - dword (32 bits, max 0xFFFFFFFF)
			final int size = sizeList.getSelectedIndex();
			if (val > maxValues[size]) {
				JOptionPane.showMessageDialog(this, "The value is too large for the specified size!", "Value too large",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			// encode the value as a boolean array
			boolean[] valBin = new boolean[bitCounts[size]];
			for (int i = 0; i < valBin.length; i++)
				valBin[i] = (val & (1 << i)) != 0;
			// make the commands
			String out = "";
			for (int i = 0; i < valBin.length; i++) {
				out += "<FL" + (valBin[i] ? "+" : "-");
				out += num2TSCParam(flag++);
				if ((i + 1) % 8 == 0)
					out += System.lineSeparator();
			}
			outArea.setText(out);
			StrTools.msgBox("Successfully generated TSC.");
		} else if (e.getSource().equals(copyButton)) {
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			cb.setContents(new StringSelection(outArea.getText()), null);
		}
	}

	private String num2TSCParam(long num) {
		String ret = "";
		for (int i = 3; i > -1; i--) {
			boolean addedChar = false;
			for (char c = 0xFF; c > 0x1F; c--) {
				if (num == 0)
					break;
				long val = (long) ((c - 0x30) * Math.pow(10, i));
				if (val > num)
					continue;
				num -= val;
				ret += c;
				addedChar = true;
				break;
			}
			if (!addedChar)
				ret += "0";
		}
		if (num != 0)
			return null;
		return ret;
	}

}
