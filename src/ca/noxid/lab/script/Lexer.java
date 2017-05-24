package ca.noxid.lab.script;

import ca.noxid.lab.Messages;

import javax.swing.text.SimpleAttributeSet;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Scanner;

/**
 * Created by noxid on 24/05/17.
 */
public abstract class Lexer {

	Scanner sc = null;
	protected int lineNum;
	protected int character;
	protected int strPos;
	protected String line;

	public abstract TscToken getNextToken() throws IOException;
	public abstract Hashtable<String, SimpleAttributeSet> getStyles();

	public void reset(Reader reader, int yyline, int yychar, int yycolumn)
			throws IOException {
		if (sc != null) {
			sc.close();
		}
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

	public abstract String sanitize(String sourceText);
}
