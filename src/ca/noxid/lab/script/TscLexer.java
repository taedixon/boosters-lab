package ca.noxid.lab.script;

import ca.noxid.lab.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

@SuppressWarnings("unused")
public class TscLexer {

	private Scanner sc = new Scanner(System.in);

	private int charCount;
	private boolean isFace = false;
	private boolean overLimit = false;
	private boolean wasEnded = false;
	private static final int EVENT_NORMAL = 0;
	private static final int EVENT_SPEECHBUBBLE = 1;
	private int lineNum;
	private int col;
	private int character;

	private String line;
	private int strPos;
	private String cmd;
	private int argsRemaining;
	private int paramNum;

	private TscToken lastToken;

	private static Map<String, TscCommand> cmdMap = new Hashtable<>();

	public TscLexer() {

	}

	TscLexer(InputStream in) {
		sc.close();
		sc = new Scanner(in);
		line = sc.nextLine();
		lineNum = 0;
		col = 0;
		character = 0;
		strPos = 0;
	}

	TscLexer(Reader in) {
		sc.close();
		sc = new Scanner(in);
		line = sc.nextLine();
		character = 0;
		lineNum = 0;
		col = 0;
		strPos = 0;
	}

	static void initMap(Vector<TscCommand> commandInf) {
		for (int i = 0; i < commandInf.size(); i++) {
			cmdMap.put(commandInf.elementAt(i).commandCode, commandInf.elementAt(i));
		}
	}

	public TscToken getNextToken() throws IOException {
		if (strPos >= line.length()) {
			strPos = 0;
			try {
				line = sc.nextLine();
			} catch (NoSuchElementException e) {
				return null;
			}
			lineNum++;
			character++;
			charCount = 0;
			overLimit = false;
		}
		TscToken nextToken = null;
		String tokenStr = ""; //$NON-NLS-1$
		if (lastToken != null && lastToken.getDescription().equals(TscPane.STYLE_SBEVENT)) {
			tokenStr = line;
			nextToken = new TscToken(TscPane.STYLE_SBFLAGS, tokenStr, lineNum, character,
					character + tokenStr.length());
			character += tokenStr.length();
			strPos = tokenStr.length();
			lastToken = nextToken;
			return nextToken;
		}
		if (line.equals("")) //$NON-NLS-1$
		{
			return getNextToken();
		}
		char nextChar = line.charAt(strPos);
		int eventType;
		if ((nextChar == '@') && strPos == 0) {
			tokenStr = line;
			nextToken = new TscToken(TscPane.STYLE_SBEVENT, tokenStr, lineNum, character,
					character + tokenStr.length());
			character += tokenStr.length();
			wasEnded = false;
			isFace = false;
			strPos = tokenStr.length();
			//noinspection UnusedAssignment
			eventType = TscLexer.EVENT_SPEECHBUBBLE;
		} else if (nextChar == '#' && strPos == 0) {
			tokenStr = line;
			nextToken = new TscToken(TscPane.STYLE_EVENT, tokenStr, lineNum, character, character + tokenStr.length());
			character += tokenStr.length();
			wasEnded = false;
			isFace = false;
			strPos = tokenStr.length();
			//noinspection UnusedAssignment
			eventType = TscLexer.EVENT_NORMAL;
		} else if (argsRemaining > 0) {
			if ((lastToken.getContents().charAt(0) == '<') || ((lastToken.getContents().length() == 1) || cmdMap.containsKey(cmd) && !cmdMap.get(cmd).paramSep)) {
				//number token
				//try to grab the value one character at a time
				TscCommand c = cmdMap.get(cmd);
				for (int i = 0; i < c.paramLen[paramNum]; i++) {
					try {
						tokenStr += line.charAt(strPos + i);
					} catch (IndexOutOfBoundsException e) {
						nextToken = new TscToken(TscPane.STYLE_NUM, tokenStr, lineNum, character,
								character + tokenStr.length());
						character += tokenStr.length();
						strPos += tokenStr.length();
						lastToken = nextToken;
						return nextToken;
					}
				}
				nextToken = new TscToken(TscPane.STYLE_NUM, tokenStr, lineNum, character, character + c.paramLen[paramNum]);
				if (lastToken.getContents().equals("<FAC")) { //$NON-NLS-1$
					if (tokenStr.equals("0000")) //$NON-NLS-1$
						isFace = false;
					else
						isFace = true;
				}
				character += c.paramLen[paramNum];
				strPos += c.paramLen[paramNum];
				argsRemaining--;
				paramNum++;
			} else {
				//spacer token
				if (cmdMap.containsKey(cmd) && cmdMap.get(cmd).paramSep) {
					tokenStr += line.charAt(strPos);
					nextToken = new TscToken(TscPane.STYLE_SPACER, tokenStr, lineNum, character, character + 1);
					character++;
					strPos++;
				}
			}
		} else if (wasEnded) {
			int tokenLen = line.substring(strPos).length();
			nextToken = new TscToken(TscPane.STYLE_COMMENT, line.substring(strPos), lineNum, character,
					character + tokenLen);
			character += tokenLen;
			strPos += tokenLen;
		} else {
			if (nextChar == '<') {
				//try to grab the value one character at a time
				for (int i = 0; i < 4; i++) {
					try {
						tokenStr += line.charAt(strPos + i);
					} catch (IndexOutOfBoundsException e) {
						nextToken = new TscToken(TscPane.STYLE_TAG, tokenStr, lineNum, character,
								character + tokenStr.length());
						character += tokenStr.length();
						strPos += tokenStr.length();
						lastToken = nextToken;
						return nextToken;
					}
				}
				cmd = tokenStr;
				paramNum = 0;
				if (cmdMap.containsKey(cmd)) {
					TscCommand cmdDef = cmdMap.get(cmd);
					argsRemaining = cmdDef.numParam;
					if (cmdDef.endsEvent)
						wasEnded = true;
					if (cmdDef.clearsMsg) {
						charCount = 0;
						overLimit = false;
					}
				} else {
					argsRemaining = 0;
				}
				nextToken = new TscToken(TscPane.STYLE_TAG, tokenStr, lineNum, character, character + 4);
				character += 4;
				strPos += 4;
			} else if (nextChar == '/' && line.length() > strPos + 1
					&& line.charAt(strPos + 1) == '/') { // comment
				tokenStr = line.substring(strPos);
				nextToken = new TscToken(TscPane.STYLE_COMMENT,
						tokenStr, lineNum, character, character + tokenStr.length());
				character += tokenStr.length();
				strPos += tokenStr.length();
			} else { // anything else
				tokenStr = ""; //$NON-NLS-1$
				boolean setOver = false;
				while (true) {
					if (isFace) {
						if ((charCount > 27) && (!overLimit)) {
							setOver = true;
							break;
						}
					} else {
						if ((charCount > 34) && (!overLimit)) {
							setOver = true;
							break;
						}
					}
					tokenStr += nextChar;
					strPos++;
					if (strPos >= line.length()) break;
					nextChar = line.charAt(strPos);
					if (nextChar == '<') break;
					charCount++;
				}
				if (!overLimit) {
					nextToken = new TscToken(TscPane.STYLE_TXT, tokenStr, lineNum, character,
							character + tokenStr.length());
				} else {
					nextToken = new TscToken(TscPane.STYLE_OVER, tokenStr, lineNum, character,
							character + tokenStr.length());
				}
				if (setOver) {
					overLimit = true;
				}
				character += tokenStr.length();
			}
		}
		lastToken = nextToken;
		return nextToken;
	}


	public void reset(Reader reader, int yyline, int yychar, int yycolumn)
			throws IOException {
		sc.close();
		sc = new Scanner(reader);
		if (yychar < 0) {
			for (int i = 0; i < yyline; i++) {
				line = sc.nextLine();
				character += line.length() + 1;
			}
			lineNum = yyline;
			strPos = yycolumn;
			if (line != null) {
				character -= (line.length() - strPos + 1);
			} else if (sc.hasNextLine()) {
				line = sc.nextLine();
				character = 0;
			} else {
				line = Messages.getString("TscLexer.14"); //$NON-NLS-1$
				character = 0;
			}
		} else {
			character = 0;
			while (character < yychar) {
				line = sc.nextLine();
				character += line.length() + 1;
				lineNum++;
			}
			if (character > yychar) {
				strPos = line.length() - (character - yychar);
				character = yychar;
			}
		}
	}
}

