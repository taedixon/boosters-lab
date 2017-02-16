package ca.noxid.lab.script;

import ca.noxid.lab.Messages;

public class TscToken {
	private int ID;
	private String desc;
	private String token;
	private boolean comment;
	private boolean whitespace;
	private boolean error;
	private int lineNum;
	private int charBegin;
	private int charEnd;
	private String strErr;
	private int state;

	TscToken(String description, String tokenStr, int lineNum_in, int charBegin_in, int charEnd_in) {
		ID = 0;
		desc = description;
		token = tokenStr;
		lineNum = lineNum_in;
		charBegin = charBegin_in;
		charEnd = charEnd_in;
		strErr = Messages.getString("TscLexer.15"); //$NON-NLS-1$
		comment = false;
		whitespace = false;
		error = false;
		state = 0;
	}

	public int getID() {
		return ID;
	}

	public String getDescription() {
		return desc;
	}

	public String getContents() {
		return token;
	}

	public boolean isComment() {
		return comment;
	}

	public boolean isWhiteSpace() {
		return whitespace;
	}

	public boolean isError() {
		return error;
	}

	public int getLineNumber() {
		return lineNum;
	}

	public int getCharBegin() {
		return charBegin;
	}

	public int getCharEnd() {
		return charEnd;
	}

	public String errorString() {
		return strErr;
	}

	public int getState() {
		return state;
	}
}