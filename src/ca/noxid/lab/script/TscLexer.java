package ca.noxid.lab.script;

import ca.noxid.lab.Messages;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

public class TscLexer extends Lexer {

	private static final Hashtable<String, SimpleAttributeSet> styles = initStyles(); //maps styles for text formatting
	public static final String STYLE_EVENT = "eveNum"; //$NON-NLS-1$
	public static final String STYLE_SBEVENT = "sbEvent";
	public static final String STYLE_SBFLAGS = "sbFlag";
	public static final String STYLE_TAG = "tag"; //$NON-NLS-1$
	public static final String STYLE_NUM = "number"; //$NON-NLS-1$
	public static final String STYLE_SPACER = "spacer"; //$NON-NLS-1$
	public static final String STYLE_TXT = "text"; //$NON-NLS-1$
	public static final String STYLE_OVER = "overflow"; //$NON-NLS-1$
	public static final String STYLE_COMMENT = "comment"; //$NON-NLS-1$


	private int charCount;
	private boolean isFace = false;
	private boolean overLimit = false;
	private boolean wasEnded = false;
	private static final int EVENT_NORMAL = 0;
	private static final int EVENT_SPEECHBUBBLE = 1;

	private int argsRemaining;

	private TscToken lastToken;

	private Map<String, Integer> argMap;

	public Hashtable<String, SimpleAttributeSet> getStyles() {return styles;}

	public TscLexer() {
		argMap = new Hashtable<>();
	}

	TscLexer(Vector<TscCommand> commands) {
		argMap = new Hashtable<>();
		for (int i = 0; i < commands.size(); i++) {
			argMap.put(commands.elementAt(i).commandCode, commands.elementAt(i).numParam);
		}
	}

	public TscToken getNextToken() throws IOException {
		if (sc == null) {
			return null;
		}
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
		TscToken nextToken;
		String tokenStr = ""; //$NON-NLS-1$
		if (lastToken != null && lastToken.getDescription().equals(STYLE_SBEVENT)) {
			tokenStr = line;
			nextToken = new TscToken(STYLE_SBFLAGS, tokenStr, lineNum, character,
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
			nextToken = new TscToken(STYLE_SBEVENT, tokenStr, lineNum, character,
					character + tokenStr.length());
			character += tokenStr.length();
			wasEnded = false;
			isFace = false;
			strPos = tokenStr.length();
			//noinspection UnusedAssignment
			eventType = TscLexer.EVENT_SPEECHBUBBLE;
		} else if (nextChar == '#' && strPos == 0) {
			tokenStr = line;
			nextToken = new TscToken(STYLE_EVENT, tokenStr, lineNum, character, character + tokenStr.length());
			character += tokenStr.length();
			wasEnded = false;
			isFace = false;
			strPos = tokenStr.length();
			//noinspection UnusedAssignment
			eventType = TscLexer.EVENT_NORMAL;
		} else if (argsRemaining > 0) {
			if ((lastToken.getContents().charAt(0) == '<') || (lastToken.getContents().length() == 1)) {
				//number token
				//try to grab the value one character at a time
				for (int i = 0; i < 4; i++) {
					try {
						tokenStr += line.charAt(strPos + i);
					} catch (IndexOutOfBoundsException e) {
						nextToken = new TscToken(STYLE_NUM, tokenStr, lineNum, character,
								character + tokenStr.length());
						character += tokenStr.length();
						strPos += tokenStr.length();
						lastToken = nextToken;
						return nextToken;
					}
				}
				nextToken = new TscToken(STYLE_NUM, tokenStr, lineNum, character, character + 4);
				if (lastToken.getContents().equals("<FAC") && //$NON-NLS-1$
						tokenStr.equals("0000")) //$NON-NLS-1$
				{
					isFace = false;
				}
				character += 4;
				strPos += 4;
				argsRemaining--;
			} else {
				//spacer token
				tokenStr += line.charAt(strPos);
				nextToken = new TscToken(STYLE_SPACER, tokenStr, lineNum, character, character + 1);
				character++;
				strPos++;
			}
		} else if (wasEnded) {
			int tokenLen = line.substring(strPos).length();
			nextToken = new TscToken(STYLE_COMMENT, line.substring(strPos), lineNum, character,
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
						nextToken = new TscToken(STYLE_TAG, tokenStr, lineNum, character,
								character + tokenStr.length());
						character += tokenStr.length();
						strPos += tokenStr.length();
						lastToken = nextToken;
						return nextToken;
					}
				}
				if (argMap.containsKey(tokenStr)) {
					argsRemaining = argMap.get(tokenStr);
				} else {
					argsRemaining = 0;
				}
				if (tokenStr.equals("<FAC")) //$NON-NLS-1$
				{
					isFace = true;
				}
				if (tokenStr.equals("<END") ||  //$NON-NLS-1$
						tokenStr.equals("<TRA") ||  //$NON-NLS-1$
						tokenStr.equals("<EVE") ||  //$NON-NLS-1$
						tokenStr.equals("<LDP") ||  //$NON-NLS-1$
						tokenStr.equals("<INI") || //$NON-NLS-1$
						tokenStr.equals("<ESC")) //$NON-NLS-1$
				{
					wasEnded = true;
				}
				if (tokenStr.equals("<CLR")) //$NON-NLS-1$
				{
					charCount = 0;
					overLimit = false;
				}
				nextToken = new TscToken(STYLE_TAG, tokenStr, lineNum, character, character + 4);
				character += 4;
				strPos += 4;

			} else if (nextChar == '/' && line.length() > strPos + 1
					&& line.charAt(strPos + 1) == '/') { // comment
				tokenStr = line.substring(strPos);
				nextToken = new TscToken(STYLE_COMMENT,
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
					nextToken = new TscToken(STYLE_TXT, tokenStr, lineNum, character,
							character + tokenStr.length());
				} else {
					nextToken = new TscToken(STYLE_OVER, tokenStr, lineNum, character,
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

	@Override
	public String sanitize(String sourceText) {
		String strippedScript = ""; //$NON-NLS-1$
		try {
			TscToken t;
			int line = 0;
			reset(new StringReader(sourceText), 0, -1, 0);
			while ((t = getNextToken()) != null) {
				if (line != t.getLineNumber()) {
					strippedScript += "\r\n"; //$NON-NLS-1$
				}
				if (!t.getDescription().equals(STYLE_COMMENT)) {
					String content = t.getContents();
					if (t.getDescription().equals(STYLE_EVENT)) {
						strippedScript += content.substring(0,
								(content.length() >= 5) ? 5 : content.length());
					} else {
						strippedScript += content;
					}
				}
				line = t.getLineNumber();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strippedScript;
	}

	private static Hashtable<String, SimpleAttributeSet> initStyles() {
		Hashtable<String, SimpleAttributeSet> retVal = new Hashtable<>();
		SimpleAttributeSet newStyle;
		String fontFamily = "Monospaced";

		//event numbers
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.black);
		StyleConstants.setBold(newStyle, true);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_EVENT, newStyle);
		//speech bubble
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.ORANGE);
		StyleConstants.setBold(newStyle, true);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_SBEVENT, newStyle);
		//tsc tags
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.blue);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_TAG, newStyle);
		//numbers
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.decode("0xC42F63")); //$NON-NLS-1$
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_NUM, newStyle);
		//number spacer
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.GRAY);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_SPACER, newStyle);
		//text
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.black);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_TXT, newStyle);
		//speech bubble flags
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.decode("0xFF6060"));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_SBFLAGS, newStyle);
		//overlimit text
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.gray);
		StyleConstants.setForeground(newStyle, Color.red);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		retVal.put(STYLE_OVER, newStyle);
		//inaccessible commands
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, fontFamily); //$NON-NLS-1$
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.decode("0x367A2A")); //$NON-NLS-1$
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, true);
		retVal.put(STYLE_COMMENT, newStyle);

		return retVal;
	}
}

