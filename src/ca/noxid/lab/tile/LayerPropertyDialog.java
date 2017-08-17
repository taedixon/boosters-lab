package ca.noxid.lab.tile;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.rsrc.ResourceManager;

import javax.swing.*;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Noxid on 16-Aug-17.
 */
public class LayerPropertyDialog extends JDialog implements ActionListener {

	TileLayer layer;
	JTextField nameField;
	JComboBox<TileLayer.LAYER_TYPE> typeSelect;

	LayerPropertyDialog(Frame aFrame, TileLayer layer) {
		super(aFrame, true);
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		this.layer = layer;

		this.setContentPane(createContentPane());

		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setModal(true);
		this.pack();
		this.setVisible(true);
	}

	private Container createContentPane() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
		JPanel wrapper;
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		wrapper = new JPanel();
		wrapper.add(new JLabel("Layer Name:"));
		panel.add(wrapper);
		nameField = new JTextField();
		nameField.setText(layer.getName());
		panel.add(nameField);

		wrapper = new JPanel();
		wrapper.add(new JLabel("Layer Type:"));
		panel.add(wrapper);
		typeSelect = new JComboBox<>(TileLayer.LAYER_TYPE.values());
		typeSelect.setSelectedItem(layer.getType());
		panel.add(typeSelect);

		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		JButton acceptButton = new JButton("Accept");
		acceptButton.setActionCommand("accept");
		acceptButton.addActionListener(this);
		buttonPanel.add(acceptButton);
		panel.add(buttonPanel);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "cancel":
			this.dispose();
			break;
		case "accept":
			layer.setLayerType((TileLayer.LAYER_TYPE) typeSelect.getSelectedItem());
			layer.setName(nameField.getText());
			this.dispose();
			break;
		}
	}
}
