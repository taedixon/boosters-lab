package ca.noxid.lab.mapdata;

import ca.noxid.lab.Messages;
import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.uiComponents.BgPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class MapdataDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -553912401130449080L;
	 
   	private JButton okButton;
   	private JButton cancelButton;
   	
   	private MapdataPane pane;

	public MapdataDialog(Frame aFrame, int mapNum, GameInfo inf, java.awt.image.BufferedImage img) {
		super(aFrame, true);
		this.setLocation(aFrame.getLocation());
		pane = new MapdataPane(inf, mapNum, img, false);
		this.setTitle(Messages.getString("MapdataDialog.0"));  //$NON-NLS-1$
        
		addComponentsToPane(img);
 
        //Handle window closing correctly.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); 
       
        this.pack();
        this.setResizable(false);
        //this.setModal(true);
        this.setVisible(true);
	}
	
	public MapdataDialog(Frame aFrame, File mapFile, GameInfo inf, java.awt.image.BufferedImage img) {
		super(aFrame, true);
		this.setLocation(aFrame.getLocation());
		pane = new MapdataPane(inf, mapFile, img, false);
		this.setTitle(Messages.getString("MapdataDialog.0"));  //$NON-NLS-1$
        
		addComponentsToPane(img);
 
        //Handle window closing correctly.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); 
       
        this.pack();
        this.setResizable(false);
        //this.setModal(true);
        this.setVisible(true);
	}
	
	private void addComponentsToPane(java.awt.image.BufferedImage img) {
		okButton = new JButton(Messages.getString("MapdataDialog.1")); //$NON-NLS-1$
        okButton.addActionListener(this);
        okButton.setOpaque(false);
        cancelButton = new JButton(Messages.getString("MapdataDialog.2")); //$NON-NLS-1$
        cancelButton.addActionListener(this);
        cancelButton.setOpaque(false);
 
        //Create an array specifying the number of dialog buttons
        //and their text.
 
        //Create the JOptionPane.
        JPanel dialogPane = new JPanel();
        dialogPane.setLayout(new BoxLayout(dialogPane, BoxLayout.Y_AXIS));
        dialogPane.add(pane);
        JPanel buttonPanel = new BgPanel(img);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialogPane.add(buttonPanel);
 
        //Make this dialog display it.
        setContentPane(dialogPane);
	}
	
	public Mapdata getMapdata() {
		return pane.getMapdata();
	}
	    
	/** This method handles events for the text field. */
    public void actionPerformed(ActionEvent eve) {
    	if (eve.getSource().equals(cancelButton)) {
    		this.dispose();
    	} else if (eve.getSource().equals(okButton)) {
			//validate input
    		if (!pane.allFieldsCommitted()) {
	    		int response = JOptionPane.showConfirmDialog(this, Messages.getString("MapdataDialog.3") + //$NON-NLS-1$
	    				Messages.getString("MapdataDialog.4"), Messages.getString("MapdataDialog.5"), JOptionPane.YES_NO_CANCEL_OPTION); //$NON-NLS-1$ //$NON-NLS-2$
	    		switch (response) {
	    		case JOptionPane.YES_OPTION:
	    			pane.commitAll();
	    			this.dispose();
	    			break;
	    		case JOptionPane.NO_OPTION:
	        		this.dispose();
	    			break;
	    		case JOptionPane.CANCEL_OPTION:
	    			break;
	    		}
	    		//if not all fields committed
    		} else {
    			pane.commitAll();
    		} //else all fields were committed 
			this.dispose();
    	} // if OK button
    }
}
