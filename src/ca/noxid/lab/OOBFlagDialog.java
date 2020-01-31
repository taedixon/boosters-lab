package ca.noxid.lab;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

	private static final ByteBuffer BB = ByteBuffer.allocate(Long.BYTES);

	private static final String[] sizes = {
		Messages.getString("OOBFlagDialog.0"),
		Messages.getString("OOBFlagDialog.1"),
		Messages.getString("OOBFlagDialog.2"),
		Messages.getString("OOBFlagDialog.3")
	};

	private static final long[] maxValues = {
		0xFF,
		0xFFFF,
		0xFFFFFFFF,
		0xFFFFFFFFFFFFFFFFL
	};

	private static final int[] bitCounts = {
		Byte.SIZE,
		Short.SIZE,
		Integer.SIZE,
		Long.SIZE
	};

	public OOBFlagDialog(Frame parent, ResourceManager iMan) {
		super(parent, true);
		if (EditorApp.blazed)
			setCursor(ResourceManager.cursor);
		setLocation(parent.getLocation());
		setTitle(Messages.getString("EditorApp.168"));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addComponentsToPane(iMan.getImg(ResourceManager.rsrcBgBlue));
		pack();
		setResizable(false);
		setVisible(true);
	}

	private void addComponentsToPane(java.awt.image.BufferedImage bgImage) {
		addrField = new JTextField("49DDA0"); //$NON-NLS-1$
		valField = new JTextField("0");
		sizeList = new JComboBox<>(sizes);
		outArea = new JTextArea();
		outArea.setEditable(false);
		genButton = new JButton(Messages.getString("OOBFlagDialog.10"));
		genButton.addActionListener(this);
		genButton.setOpaque(false);
		copyButton = new JButton(Messages.getString("OOBFlagDialog.11"));
		copyButton.addActionListener(this);
		copyButton.setOpaque(false);

		JPanel c = new JPanel();
		c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
		BgPanel fieldPanel = new BgPanel(bgImage);
		fieldPanel.setLayout(new GridLayout(0, 2));
		fieldPanel.add(new JLabel(Messages.getString("EditorApp.168")));
		fieldPanel.add(new JLabel(""));
		fieldPanel.add(new JLabel(Messages.getString("OOBFlagDialog.20")));
		fieldPanel.add(addrField);
		fieldPanel.add(new JLabel(Messages.getString("OOBFlagDialog.21")));
		fieldPanel.add(valField);
		fieldPanel.add(new JLabel(Messages.getString("OOBFlagDialog.22")));
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
			int flag;
			try {
				flag = Integer.parseInt(addrField.getText(), 16);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, Messages.getString("OOBFlagDialog.30"),
						Messages.getString("OOBFlagDialog.31"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			flag &= 0xFFFFFFFFL;
			flag -= 0x49DDA0;
			if (flag > 0x288E || flag < -0x8AE) {
				JOptionPane.showMessageDialog(this, Messages.getString("OOBFlagDialog.40"),
						Messages.getString("OOBFlagDialog.41"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			flag *= 8; // multiply by 8 to get actual flag ID
			long val;
			try {
				val = Long.parseLong(valField.getText(), 16);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, Messages.getString("OOBFlagDialog.50"),
						Messages.getString("OOBFlagDialog.51"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			// 0 - byte (8 bits, max 0xFF)
			// 1 - word (16 bits, max 0xFFFF)
			// 2 - dword (32 bits, max 0xFFFFFFFF)
			// 3 - qword (64 bits, max 0xFFFFFFFFFFFFFFFF)
			final int size = sizeList.getSelectedIndex();
			if (Long.compareUnsigned(val, maxValues[size]) > 0) {
				JOptionPane.showMessageDialog(this, Messages.getString("OOBFlagDialog.60"),
						Messages.getString("OOBFlagDialog.61"),
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			// transform our value into little endian...
			BB.clear();
			BB.order(ByteOrder.nativeOrder());
			BB.putLong(val);
			BB.order(ByteOrder.LITTLE_ENDIAN);
			BB.flip();
			val = BB.getLong();
			// ...and encode it as a boolean array
			boolean[] valBin = new boolean[bitCounts[size]];
			for (int i = 0; i < valBin.length; i++)
				valBin[i] = (val & (1 << i)) != 0;
			// make the commands
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < valBin.length; i++) {
				out.append("<FL").append(valBin[i] ? '+' : '-'); //$NON-NLS-1$
				out.append(num2TSCParam(flag++));
				if ((i + 1) % 8 == 0)
					out.append('\n');
			}
			outArea.setText(out.toString());
			StrTools.msgBox(Messages.getString("OOBFlagDialog.70"));
		} else if (e.getSource().equals(copyButton)) {
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			cb.setContents(new StringSelection(outArea.getText()), null);
		}
	}

	/**
	 * Converts a number into its equivalent TSC parameter, with support for negative and huge positive numbers.
	 * @param num number
	 * @return number as TSC parameter
	 * @author txin
	 */
	private String num2TSCParam(int num) {
		int offset = (int) Math.floor(Math.max(0, -num + 999.9 / 1000));
		num += offset * 1000;
		StringBuilder output = new StringBuilder();
		int a = 1;
		for (int i = 0; i < 3; i++) {
			int b = num / a;
			char c = (char) (b % 10);
			c += 48;
			addCharToSB(c, output);
			a *= 10;
		}
		char c = (char) (num / 1000);
		c += 48;
		c -= offset;
		addCharToSB(c, output);
		output.reverse();
		return output.toString();
	}

	private void addCharToSB(char c, StringBuilder sb) {
		if (isPrintableChar(c))
			sb.append(c);
		else
			sb.append(String.format("\\u%04X", (short) c)); //$NON-NLS-1$
	}

	// https://stackoverflow.com/a/418560
	private boolean isPrintableChar(char c) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
		return (!Character.isISOControl(c)) &&
				c != java.awt.event.KeyEvent.CHAR_UNDEFINED &&
				block != null &&
				block != Character.UnicodeBlock.SPECIALS;
	}

}
