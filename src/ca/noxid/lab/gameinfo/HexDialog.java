package ca.noxid.lab.gameinfo;

import ca.noxid.lab.Messages;
import com.carrotlord.string.StrTools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.Vector;

public class HexDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -553912401130449080L;
	 
   	private JButton okButton;
   	private JButton cancelButton;
   	
   	private JTextPane textArea;
   	
   	private GameInfo exe;
   	
	public HexDialog(Frame aFrame, GameInfo inf) {
		super(aFrame, true);
		exe = inf;
		this.setLocation(aFrame.getLocation());
		this.setTitle(Messages.getString("HexDialog.0"));  //$NON-NLS-1$
        okButton = new JButton(Messages.getString("HexDialog.1")); //$NON-NLS-1$
        //okButton.setFont(new Font("HGGothicMedium", Font.PLAIN, 12));
        okButton.addActionListener(this);
        cancelButton = new JButton(Messages.getString("HexDialog.2")); //$NON-NLS-1$
        cancelButton.addActionListener(this);
        textArea = new JTextPane();
        textArea.setText(Messages.getString("HexDialog.3") + //$NON-NLS-1$
        		Messages.getString("HexDialog.4") + //$NON-NLS-1$
        		Messages.getString("HexDialog.5") + //$NON-NLS-1$
        		Messages.getString("HexDialog.6") + //$NON-NLS-1$
        		Messages.getString("HexDialog.7") + //$NON-NLS-1$
        		Messages.getString("HexDialog.8")); //$NON-NLS-1$
 
        //Create an array specifying the number of dialog buttons
        //and their text.
 
        //Create the JOptionPane.
        JPanel dialogPane = new JPanel();
        dialogPane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(okButton);
        
        buttonPanel.add(cancelButton);
        dialogPane.add(buttonPanel, c);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        JScrollPane textScroll = new JScrollPane(textArea);
        dialogPane.add(textScroll, c);
 
        //Make this dialog display it.
        setContentPane(dialogPane);
 
        //Handle window closing correctly.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); 
       
        this.setSize(400, 400);
        this.setResizable(true);
        this.setModal(true);
        this.setVisible(true);
	}
	    
	/** This method handles events for the text field. */
    public void actionPerformed(ActionEvent eve) {
    	if (eve.getSource().equals(cancelButton)) {
    		this.dispose();
    	} else if (eve.getSource().equals(okButton)) {
			//validate input
    		parseHex(textArea.getText().replaceAll("\\s+", ""));
    		System.out.println(Messages.getString("HexDialog.9")); //$NON-NLS-1$
    	} // if OK button
    }
    
    private void parseHex(String input) {
    	int parseloc = 0;
    	Vector<Byte> patchData = new Vector<>();
    	int cPointer = -1;
    	int nPatches = 0;
    	while (parseloc < input.length()-1) {
    		
    		String token = input.substring(parseloc, parseloc+2);
    		parseloc += 2;
    		if (token.startsWith("0x")) { //$NON-NLS-1$
    			//pointer
    			int newPointer;
    			try {
    				newPointer = Integer.parseInt(input.substring(parseloc, parseloc+6 ), 16);
    			} catch (NumberFormatException err) {
    				StrTools.msgBox(Messages.getString("HexDialog.11") + //$NON-NLS-1$
    		    			token + Messages.getString("HexDialog.12")); //$NON-NLS-1$
	    			return;
    			}
    			parseloc +=6;
    			if (newPointer > 0x400000) newPointer -= 0x400000;
    			if (cPointer != -1) {
    				//we have (maybe) data to patch
    				ByteBuffer buf = ByteBuffer.wrap(bVec2Array(patchData));
    				exe.patch(buf, cPointer);
    				nPatches++;
    			}
    			patchData = new Vector<>();
    			cPointer = newPointer;
    		} else if (token.length() == 2) {
    			//possibly a byte
    			
    			int pByte;
    			try {
    				pByte = Integer.parseInt(token, 16);
    			} catch (NumberFormatException err) {
    				StrTools.msgBox(Messages.getString("HexDialog.11") + //$NON-NLS-1$
    		    			token + Messages.getString("HexDialog.14")); //$NON-NLS-1$
    		    			return;
    			}
    			patchData.add((byte) pByte);
    		} else {
    			//error
    			StrTools.msgBox(Messages.getString("HexDialog.11") + //$NON-NLS-1$
    			token + Messages.getString("HexDialog.16")); //$NON-NLS-1$
    			return;
    		}
    	}//while tokens
    	if (cPointer != -1) {
			//we have (maybe) data to patch
			ByteBuffer buf = ByteBuffer.wrap(bVec2Array(patchData));
			exe.patch(buf, cPointer);
			nPatches++;
		}
    	if (nPatches > 0) {
    		StrTools.msgBox(Messages.getString("HexDialog.17") + nPatches + Messages.getString("HexDialog.18")); //$NON-NLS-1$ //$NON-NLS-2$
    	} else {
    		StrTools.msgBox(Messages.getString("HexDialog.19")); //$NON-NLS-1$
    	}
    }
    
    private byte[] bVec2Array(Vector<Byte> vIn) {
    	byte[] vDat = new byte[vIn.size()];
		for (int i = 0; i < vDat.length; i++) {
			vDat[i] = vIn.get(i); //I'm sorry but Java made me do it, you can't fix this
		}
		return vDat;
    }
}
