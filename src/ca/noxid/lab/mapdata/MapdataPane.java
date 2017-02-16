package ca.noxid.lab.mapdata;
import ca.noxid.lab.BlConfig;
import ca.noxid.lab.Changeable;
import ca.noxid.lab.Messages;
import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.uiComponents.BgPanel;
import ca.noxid.uiComponents.LenLimitDoc;
import ca.noxid.uiComponents.UpdateTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

public class MapdataPane extends BgPanel implements ActionListener, Changeable {
	private static final long serialVersionUID = -1064501567760708361L;
	private Mapdata dat;
	//private GameInfo exeDat; 
	
	private UpdateTextField nameField;
	private UpdateTextField fileField;
	//private JTextField xField;
	//private JTextField yField;
	private JComboBox<String> tilesetList;
	private JComboBox<String> npcList1;
	private JComboBox<String> npcList2;
	private JComboBox<String> background;
	private JComboBox<String> bgType;
	private JComboBox<String> bossType;
	
	private boolean shouldWarn;
	private boolean changed;
	
	private static final String[] bossList = {
		Messages.getString("MapdataPane.0"), //$NON-NLS-1$
		Messages.getString("MapdataPane.1"), //$NON-NLS-1$
		Messages.getString("MapdataPane.2"), //$NON-NLS-1$
		Messages.getString("MapdataPane.3"), //$NON-NLS-1$
		Messages.getString("MapdataPane.4"), //$NON-NLS-1$
		Messages.getString("MapdataPane.5"), //$NON-NLS-1$
		Messages.getString("MapdataPane.6"), //$NON-NLS-1$
		Messages.getString("MapdataPane.7"), //$NON-NLS-1$
		Messages.getString("MapdataPane.8"), //$NON-NLS-1$
		Messages.getString("MapdataPane.9") //$NON-NLS-1$
	};
	
	private static final String[] bgTypes = {
		Messages.getString("MapdataPane.10"), //$NON-NLS-1$
		Messages.getString("MapdataPane.11"), //$NON-NLS-1$
		Messages.getString("MapdataPane.12"), //$NON-NLS-1$
		Messages.getString("MapdataPane.13"), //$NON-NLS-1$
		Messages.getString("MapdataPane.14"), //$NON-NLS-1$
		Messages.getString("MapdataPane.15"), //$NON-NLS-1$
		Messages.getString("MapdataPane.16"), //$NON-NLS-1$
		Messages.getString("MapdataPane.17"), //$NON-NLS-1$
		Messages.getString("MapdataPane.18"), //$NON-NLS-1$
	};
	
	public Mapdata getMapdata() {return dat;}
	
	public MapdataPane(GameInfo i, int mapNum, BufferedImage img, boolean warn) {
		super(img);
		dat = i.getMapdata(mapNum);
		//exeDat = i;
		shouldWarn = warn;
		addComponentsToPane(this, i);
	}
	
	MapdataPane(GameInfo i, File mapFile, BufferedImage img, boolean warn) {
		super(img);
		dat = i.addTempMap();
		String filename = mapFile.getName().substring(0, mapFile.getName().lastIndexOf('.'));
		dat.setFile(filename);
		shouldWarn = warn;
		addComponentsToPane(this, i);
		fileField.setEnabled(false);
		nameField.setEnabled(false);
		bossType.setEnabled(false);
		bgType.setEnabled(false);
	}

	public MapdataPane(GameInfo i, Mapdata mapdat, BufferedImage img,
			boolean warn) {
		super(img);
		dat = mapdat;
		//exeDat = i;
		shouldWarn = warn;
		addComponentsToPane(this, i);
		fileField.setEnabled(false);
		nameField.setEnabled(false);
		bossType.setEnabled(false);
		bgType.setEnabled(false);
	}

	private void addComponentsToPane(Container c, GameInfo i) {
		BlConfig conf = i.getConfig();
		nameField = new UpdateTextField();
		nameField.addActionListener(this);
		nameField.setDocument(new LenLimitDoc(31));
		nameField.setText(dat.getMapname());
		fileField = new UpdateTextField();
		fileField.addActionListener(this);
		fileField.setDocument(new LenLimitDoc(31));
		fileField.setText(dat.getFile());
		tilesetList = new JComboBox<>(i.getTilesets());
		tilesetList.setSelectedItem(conf.getTilesetPrefix() + dat.getTileset()); //$NON-NLS-1$
		tilesetList.addActionListener(this);
		npcList1 = new JComboBox<>(i.getNpcSheets());
		npcList1.setSelectedItem(conf.getNpcPrefix() + dat.getNPC1()); //$NON-NLS-1$
		npcList1.addActionListener(this);
		npcList2 = new JComboBox<>(i.getNpcSheets());
		npcList2.setSelectedItem(conf.getNpcPrefix() + dat.getNPC2()); //$NON-NLS-1$
		npcList2.addActionListener(this);
		background = new JComboBox<>(i.getBackgrounds());
		background.setSelectedItem(dat.getBG());
		background.addActionListener(this);
		bgType = new JComboBox<>(bgTypes);
		bgType.setSelectedIndex(dat.getScroll());
		bgType.addActionListener(this);
		bossType = new JComboBox<>(bossList);
		bossType.setSelectedIndex(dat.getBoss());
		bossType.addActionListener(this);
		
		c.setLayout(new GridLayout(0, 2));
		c.add(new JLabel(Messages.getString("MapdataPane.21"))); //$NON-NLS-1$
		c.add(new JLabel("")); //$NON-NLS-1$
		c.add(new JLabel(Messages.getString("MapdataPane.23"))); //$NON-NLS-1$
		c.add(fileField);
		c.add(new JLabel(Messages.getString("MapdataPane.24"))); //$NON-NLS-1$
		c.add(nameField);
		c.add(new JLabel(Messages.getString("MapdataPane.25"))); //$NON-NLS-1$
		c.add(tilesetList);
		c.add(new JLabel(Messages.getString("MapdataPane.26"))); //$NON-NLS-1$
		c.add(npcList1);
		c.add(new JLabel(Messages.getString("MapdataPane.27"))); //$NON-NLS-1$
		c.add(npcList2);
		c.add(new JLabel(Messages.getString("MapdataPane.28"))); //$NON-NLS-1$
		c.add(background);
		c.add(new JLabel(Messages.getString("MapdataPane.29"))); //$NON-NLS-1$
		c.add(bgType);
		c.add(new JLabel(Messages.getString("MapdataPane.30"))); //$NON-NLS-1$
		c.add(bossType);
	}
	
	public boolean allFieldsCommitted() {
		return nameField.isCommited() && fileField.isCommited();
	}
	
	@Override
	public void actionPerformed(ActionEvent eve) {
		// TODO Auto-generated method stub
		Object src = eve.getSource();
		//System.out.println(eve.getSource());
		if (src == nameField || src == fileField) {
			UpdateTextField txt = (UpdateTextField) src;
			//txt.setBackground(Color.white);
			if (src == nameField) {
				dat.setMapname(txt.getText());
				//this.firePropertyChange(P_NAME, null, txt.getText());
			} else {
				if (shouldWarn) {
					txt.refresh();
					int r = JOptionPane.showConfirmDialog(this, Messages.getString("MapdataPane.31") + //$NON-NLS-1$
							Messages.getString("MapdataPane.32"), Messages.getString("MapdataPane.33"), JOptionPane.YES_NO_CANCEL_OPTION); //$NON-NLS-1$ //$NON-NLS-2$
					if (r == JOptionPane.YES_OPTION) {
						dat.setMapname(nameField.getText());
						//this.firePropertyChange(P_FILE, dat.fileName, txt.getText());
						dat.setFile(txt.getText());
					} else if (r == JOptionPane.NO_OPTION) {
						dat.setFile(txt.getText());
						//this.firePropertyChange(P_FILE, null, txt.getText());
					} else {
						txt.setText(dat.getFile());
					}
				} else {
					dat.setFile(txt.getText());
				}
			}
		} else {
			@SuppressWarnings("unchecked")
			JComboBox<String> combo = (JComboBox<String>)src;
			if (combo == tilesetList) {
				dat.setTileset(combo.getSelectedItem().toString().substring(3));
				//this.firePropertyChange(P_TILE, null, combo.getSelectedItem());
			} else if (combo == npcList1) {
				dat.setNPC1(combo.getSelectedItem().toString().substring(3));
				//this.firePropertyChange(P_NPC1, null, combo.getSelectedItem());				
			} else if (combo == npcList2) {
				dat.setNPC2(combo.getSelectedItem().toString().substring(3));
				//this.firePropertyChange(P_NPC2, null, combo.getSelectedItem());
			} else if (combo == background) {
				dat.setBG(combo.getSelectedItem().toString());
				//this.firePropertyChange(P_BGIMG, null, combo.getSelectedItem());
			} else if (combo == bgType) {
				dat.setScroll(combo.getSelectedIndex());
				//this.firePropertyChange(P_SCROLL, null, combo.getSelectedIndex());	
			} else if (combo == bossType) {
				dat.setBoss(combo.getSelectedIndex());
				//this.firePropertyChange(P_BOSS, null, combo.getSelectedIndex());				
			}
		}
	}

	public void commitAll() {
		dat.setFile(fileField.getText());
		dat.setMapname(nameField.getText());
		dat.setNPC1(npcList1.getSelectedItem().toString().substring(3));
		dat.setNPC2(npcList2.getSelectedItem().toString().substring(3));
		dat.setBoss(bossType.getSelectedIndex());
		dat.setTileset(tilesetList.getSelectedItem().toString().substring(3));
		dat.setBG(background.getSelectedItem().toString());
		dat.setScroll(bgType.getSelectedIndex());
		markUnchanged();
	}

	@Override
	public boolean isModified() {
		return changed;
	}

	@Override
	public void markUnchanged() {
		if (changed) {
			changed = false;
			this.firePropertyChange(PROPERTY_EDITED, true, false);
		}
	}

	@Override
	public void markChanged() {
		if (!changed) {
			changed = true;
			//this.firePropertyChange(PROPERTY_EDITED, false, true);
		}
	}
}
