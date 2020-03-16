package ca.noxid.lab;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.*;

import com.carrotlord.string.StrTools;

import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.uiComponents.BgPanel;

public class OOBFlagDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JTextField inField;
	private JButton convertBtn;
	private JTextField outField;

	private JTextField flagAddrField, flagValField;
	private JCheckBox flagValSignedCheck;
	private JComboBox<String> flagSizeList;
	private JTextArea flagOutArea;
	private JButton flagGenBtn, flagCopyBtn;

	private static final ByteBuffer BB = ByteBuffer.allocate(Long.BYTES);

	private static final String[] sizes = {
			Messages.getString("OOBFlagDialog.0"),
			Messages.getString("OOBFlagDialog.1"),
			Messages.getString("OOBFlagDialog.2"),
			Messages.getString("OOBFlagDialog.3")
	};

	private static final long[][] limitsUnsigned = {
			{ 0, 0xFF },
			{ 0, 0xFFFF },
			{ 0, 0xFFFFFFFF },
			{ 0, 0xFFFFFFFFFFFFFFFFL }
	};

	private static final long[][] limitsSigned = {
			{ Byte.MIN_VALUE, Byte.MAX_VALUE },
			{ Short.MIN_VALUE, Short.MAX_VALUE },
			{ Integer.MIN_VALUE, Integer.MAX_VALUE },
			{ Long.MIN_VALUE, Long.MAX_VALUE }
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
		inField = new JTextField();
		convertBtn = new JButton("->"); //$NON-NLS-1$
		convertBtn.addActionListener(this);
		convertBtn.setOpaque(false);
		outField = new JTextField();
		outField.setEditable(false);

		flagAddrField = new JTextField("49DDA0"); //$NON-NLS-1$
		flagValField = new JTextField("0");
		flagValSignedCheck = new JCheckBox(Messages.getString("OOBFlagDialog.23"));
		flagValSignedCheck.setOpaque(false);
		flagSizeList = new JComboBox<>(sizes);
		flagOutArea = new JTextArea();
		flagOutArea.setEditable(false);
		flagGenBtn = new JButton(Messages.getString("OOBFlagDialog.10"));
		flagGenBtn.addActionListener(this);
		flagGenBtn.setOpaque(false);
		flagCopyBtn = new JButton(Messages.getString("OOBFlagDialog.11"));
		flagCopyBtn.addActionListener(this);
		flagCopyBtn.setOpaque(false);

		BgPanel c = new BgPanel(bgImage);
		c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));

		JPanel convertPanel = new JPanel();
		convertPanel.setLayout(new BoxLayout(convertPanel, BoxLayout.X_AXIS));
		convertPanel.setBorder(BorderFactory.createTitledBorder(Messages.getString("OOBFlagDialog.81")));
		convertPanel.setOpaque(false);
		convertPanel.add(inField);
		convertPanel.add(convertBtn);
		convertPanel.add(outField);
		c.add(convertPanel);

		JPanel flagPanel = new JPanel();
		flagPanel.setLayout(new BoxLayout(flagPanel, BoxLayout.Y_AXIS));
		flagPanel.setBorder(BorderFactory.createTitledBorder(Messages.getString("OOBFlagDialog.82")));
		flagPanel.setOpaque(false);

		JPanel flagInPanel = new JPanel();
		flagInPanel.setLayout(new GridBagLayout());
		flagInPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
		flagInPanel.setOpaque(false);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		flagInPanel.add(new JLabel(Messages.getString("OOBFlagDialog.20")), gbc);
		gbc.gridx++;
		flagInPanel.add(flagAddrField, gbc);
		gbc.gridy++;
		gbc.gridx = 0;
		flagInPanel.add(new JLabel(Messages.getString("OOBFlagDialog.21")), gbc);
		gbc.gridx++;
		flagInPanel.add(flagValField, gbc);
		gbc.gridy++;
		flagInPanel.add(flagValSignedCheck, gbc);
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		flagInPanel.add(new JLabel(Messages.getString("OOBFlagDialog.22")), gbc);
		gbc.gridx++;
		flagInPanel.add(flagSizeList, gbc);
		flagPanel.add(flagInPanel);

		JPanel flagOutPanel = new JPanel();
		flagOutPanel.setOpaque(false);
		JScrollPane outScroll = new JScrollPane(flagOutArea);
		outScroll.setPreferredSize(new Dimension(400, 200));
		flagOutPanel.add(outScroll);
		flagPanel.add(flagOutPanel);

		JPanel flagBtnPanel = new JPanel();
		flagBtnPanel.setOpaque(false);
		flagBtnPanel.add(flagGenBtn);
		flagBtnPanel.add(flagCopyBtn);
		flagPanel.add(flagBtnPanel);

		c.add(flagPanel);

		setContentPane(c);
	}

	@FunctionalInterface
	public interface LongCompareFunction {
		int compare(long x, long y);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (convertBtn.equals(src)) {
			int in;
			try {
				in = Integer.parseInt(inField.getText());
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, Messages.getString("OOBFlagDialog.80"),
						Messages.getString("OOBFlagDialog.31"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			outField.setText(num2TSCParam(in));
		} else if (flagGenBtn.equals(src)) {
			int flag;
			try {
				flag = Integer.parseUnsignedInt(flagAddrField.getText(), 16);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, Messages.getString("OOBFlagDialog.30"),
						Messages.getString("OOBFlagDialog.31"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			flag -= 0x49DDA0;
			if (flag > 0x288E || flag < -0x8AE) {
				JOptionPane.showMessageDialog(this, Messages.getString("OOBFlagDialog.40"),
						Messages.getString("OOBFlagDialog.41"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			flag *= 8; // multiply by 8 to get actual flag ID
			long[][] limits;
			LongCompareFunction cmp;
			long val;
			try {
				if (flagValSignedCheck.isSelected()) {
					limits = limitsSigned;
					cmp = Long::compare;
					val = Long.parseLong(flagValField.getText(), 16);
				} else {
					limits = limitsUnsigned;
					cmp = Long::compareUnsigned;
					val = Long.parseUnsignedLong(flagValField.getText(), 16);
				}
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, Messages.getString("OOBFlagDialog.50"),
						Messages.getString("OOBFlagDialog.51"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			final int size = flagSizeList.getSelectedIndex();
			if (cmp.compare(val, limits[size][0]) < 0 || cmp.compare(val, limits[size][1]) > 0) {
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
			flagOutArea.setText(out.toString());
			StrTools.msgBox(Messages.getString("OOBFlagDialog.70"));
		} else if (flagCopyBtn.equals(src)) {
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			cb.setContents(new StringSelection(flagOutArea.getText()), null);
		}
	}

	/**
	 * Converts a number into its equivalent TSC parameter, with support for negative and huge positive numbers.
	 *
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
