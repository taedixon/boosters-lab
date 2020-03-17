package ca.noxid.lab;

import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.uiComponents.BgPanel;
import ca.noxid.uiComponents.FormattedUpdateTextField;
import ca.noxid.uiComponents.UpdateTextField;

import javax.swing.*;

import com.carrotlord.string.StrTools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class BlIniDialog extends JDialog {

	private static final long serialVersionUID = 7586989452025851027L;
	private static final java.text.NumberFormat nf =
			FormattedUpdateTextField.getNumberOnlyFormat(1, 12);
	private static final byte[] TEST_STRING = new byte[] { (byte) 'T', (byte) 'e', (byte) 's', (byte) 't' };
	private JTextField lineResField = new FormattedUpdateTextField(nf);
	private JTextField entityResField = new FormattedUpdateTextField(nf);
	private JTextField tileSizeField = new FormattedUpdateTextField(nf);
	private JTextField tileWidthField = new FormattedUpdateTextField(nf);
	private JTextField mapMinXField = new FormattedUpdateTextField(nf);
	private JTextField mapMinYField = new FormattedUpdateTextField(nf);
	private JCheckBox scriptSourceField = new JCheckBox();
	private JTextField tilesetPrefixField = new UpdateTextField();
	private JTextField npcPrefixField = new UpdateTextField();
	private JTextField bgPrefixField = new UpdateTextField();
	private JTextField encodingField = new UpdateTextField();
	private JTextField imageExtensionField = new UpdateTextField();

	BlConfig config;

	BlIniDialog(final EditorApp app, java.awt.image.BufferedImage bg) {
		super(app, true);
		final GameInfo info = app.getGameInfo();
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		config = info.getConfig();

		lineResField.setText(Integer.toString(config.getLineRes()));
		entityResField.setText(Integer.toString(config.getEntityRes()));
		tileSizeField.setText(Integer.toString(config.getTileSize()));
		tileWidthField.setText(Integer.toString(config.getTilesetWidth()));
		mapMinXField.setText(Integer.toString(config.getMapMinX()));
		mapMinYField.setText(Integer.toString(config.getMapMinY()));
		scriptSourceField.setSelected(config.getUseScriptSource());
		tilesetPrefixField.setText(config.getTilesetPrefix());
		npcPrefixField.setText(config.getNpcPrefix());
		bgPrefixField.setText(config.getBackgroundPrefix());
		encodingField.setText(config.getEncoding());
		imageExtensionField.setText(config.getImageExtension());

		JPanel pane = new BgPanel(bg);
		pane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		pane.setLayout(new GridLayout(0, 2));

		pane.add(new JLabel("Line Resolution"));
		pane.add(lineResField);
		lineResField.setToolTipText("Used for the MR/RIP engine. Disregard if you are not GIR.");
		lineResField.setColumns(13);

		pane.add(new JLabel("Entity Resolution"));
		pane.add(entityResField);
		entityResField
				.setToolTipText("Size, in map pixels, that each unit of an entity's location represents. Default 16.");

		pane.add(new JLabel("Tile size"));
		pane.add(tileSizeField);
		tileSizeField.setToolTipText("Size in pixels of each map tile. Default 16.");

		pane.add(new JLabel("Tileset Width"));
		pane.add(tileWidthField);
		tileWidthField.setToolTipText(
				"The number of tiles per row in your tileset. Set to 0 and BL will calculate it for each tileset.");

		pane.add(new JLabel("Map minimum width"));
		pane.add(mapMinXField);
		mapMinXField.setToolTipText("The smallest map width (in tiles) BL will allow you to make.");

		pane.add(new JLabel("Map minimum height"));
		pane.add(mapMinYField);
		mapMinYField.setToolTipText("The smallest map height (in tiles) BL will allow you to make.");

		JLabel checkLabel = new JLabel("Use ScriptSource files");
		scriptSourceField.setToolTipText(
				"Preserve comments, macros, etc in TSC files. Uncheck if you often use other TSC editors.");
		pane.add(checkLabel);
		pane.add(scriptSourceField);

		pane.add(new JLabel("Tileset Prefix"));
		pane.add(tilesetPrefixField);
		tilesetPrefixField.setToolTipText("Used to find map tilesets.");

		pane.add(new JLabel("NPC Spritesheet Prefix"));
		pane.add(npcPrefixField);
		npcPrefixField.setToolTipText("Used to find NPC spritesheets.");

		pane.add(new JLabel("Background Prefix"));
		pane.add(bgPrefixField);
		bgPrefixField.setToolTipText("Used to find map background images.");

		pane.add(new JLabel("Character Encoding"));
		pane.add(encodingField);
		encodingField.setToolTipText("May need to be changed for foreign language mods.");

		pane.add(new JLabel("Image Format"));
		pane.add(imageExtensionField);
		imageExtensionField.setToolTipText("Used to load images. Does not affect the game itself.");

		JButton b = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 7943594021298535346L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean reload = false;
				final String origEncoding = config.getEncoding();
				String encoding = encodingField.getText();
				try {
					new String(TEST_STRING, encoding);
				} catch (UnsupportedEncodingException e1) {
					JOptionPane.showMessageDialog(BlIniDialog.this, String.format(Messages.getString("BlIniDialog.0"), encoding), //$NON-NLS-1$
							Messages.getString("BlIniDialog.1"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}
				if (!origEncoding.equals(encoding))
					reload = true;
				String[] vals = {
						lineResField.getText(),
						entityResField.getText(),
						tileSizeField.getText(),
						tileWidthField.getText(),
						mapMinXField.getText(),
						mapMinYField.getText(),
						scriptSourceField.isSelected() + "",
						tilesetPrefixField.getText(),
						npcPrefixField.getText(),
						bgPrefixField.getText(),
						"50",
						encodingField.getText(),
						imageExtensionField.getText(),
				};
				config.set(vals);
				config.save();
				BlIniDialog.this.dispose();
				if (reload) {
					StrTools.msgBox(Messages.getString("BlIniDialog.2")); //$NON-NLS-1$
					// prompt for unsaved executable chanes
					if (!app.saveAll(true)) {
						StrTools.msgBox(Messages.getString("BlIniDialog.3")); //$NON-NLS-1$
						return;
					}
					File base = info.getBase();
					try {
						app.loadFile(base);
					} catch (IOException e1) {
						System.err.println(Messages.getString("EditorApp.147") + base); //$NON-NLS-1$
					}
				}
			}

		});
		b.setText("Save");
		pane.add(b);


		b = new JButton(new AbstractAction() {
			private static final long serialVersionUID = -4049226449867601453L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				BlIniDialog.this.dispose();
			}

		});
		b.setText("Cancel");
		pane.add(b);

		//pane.setPreferredSize(new Dimension(400, 400));
		this.setContentPane(pane);
		this.pack();
		this.setLocationRelativeTo(app);
		this.setVisible(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
}
