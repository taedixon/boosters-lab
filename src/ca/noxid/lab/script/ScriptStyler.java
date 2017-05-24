package ca.noxid.lab.script;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by noxid on 23/05/17.
 */
public class ScriptStyler {

	Lexer lexer;

	ScriptStyler(Lexer l) {
		lexer = l;
	}

	public Lexer getLexer() {
		return lexer;
	}

	void highlightDoc(StyledDocument doc, int first, int last) {
		if (last < first) {
			last = Integer.MAX_VALUE;
		}
		try {
			lexer.reset(new StringReader(doc.getText(0, doc.getLength())), first, -1, 0);
			TscToken t;
			while ((t = lexer.getNextToken()) != null) {
				doc.setCharacterAttributes(t.getCharBegin(),
						t.getCharEnd() - t.getCharBegin(),
						lexer.getStyles().get(t.getDescription()), true);
				if (t.getLineNumber() > last) break;
			}
		} catch (BadLocationException | IOException e) {
			e.printStackTrace();
		}
	}
}
