package ca.noxid.lab;

import javax.swing.undo.AbstractUndoableEdit;

/**
 * Created by Noxid on 11-Aug-17.
 */
public class SignifUndoableEdit extends AbstractUndoableEdit {
	boolean isSignificant = true;

	public void setSignificant(boolean signif) {
		isSignificant = signif;
	}

	@Override
	public boolean isSignificant() {
		return isSignificant;
	}
}
