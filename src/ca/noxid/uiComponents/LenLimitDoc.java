package ca.noxid.uiComponents;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class LenLimitDoc extends PlainDocument {
	private static final long serialVersionUID = -8438949308739684949L;
	int lim = 31;

	public LenLimitDoc(int len) {
		super();
		lim = len;
	}

	public void insertString(int offset, String str, javax.swing.text.AttributeSet attr)
			throws BadLocationException {
		if (str == null) return;

		if ((getLength() + str.length()) <= lim) {
			super.insertString(offset, str, attr);
		}
	}
}
