package ca.noxid.lab;

import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.uiComponents.BgPanel;
import ca.noxid.uiComponents.FormattedUpdateTextField;
import ca.noxid.uiComponents.UpdateTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class BlIniDialog extends JDialog {

	private static final long serialVersionUID = 7586989452025851027L;
	private static final java.text.NumberFormat nf =
			FormattedUpdateTextField.getNumberOnlyFormat(1, 12);
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

	BlConfig config;

	BlIniDialog(Frame aFrame, BlConfig conf, java.awt.image.BufferedImage bg) {
		super(aFrame, true);
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		Point ep = aFrame.getLocationOnScreen();
		ep.x += aFrame.getWidth() / 2;
		ep.y += aFrame.getHeight() / 2;
		config = conf;

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

		JPanel pane = new BgPanel(bg);
		pane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		pane.setLayout(new GridLayout(0, 2));

		pane.add(new JLabel("Line Resolution"));
		pane.add(lineResField);
		lineResField.setToolTipText("Used for the MR/RIP engine. Disregard if you are not GIR.");
		lineResField.setColumns(12);

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
		checkLabel.setToolTipText(
				"Preserves comments, macros, etc in TSC files. Uncheck if you often use other TSC editors.");
		scriptSourceField.setToolTipText(
				"Preserves comments, macros, etc in TSC files. Uncheck if you often use other TSC editors.");
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
		bgPrefixField.setToolTipText("May need to be changed for foreign language mods.");

		JButton b;
		b = new JButton(new AbstractAction() {
			private static final long serialVersionUID = 7943594021298535346L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
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
				};
				config.set(vals);
				config.save();
				BlIniDialog.this.dispose();
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
		ep.x -= this.getWidth() / 2;
		ep.y -= this.getHeight() / 2;
		this.setLocation(ep);
		this.setVisible(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
}
