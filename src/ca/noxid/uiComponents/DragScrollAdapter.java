package ca.noxid.uiComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * For use on JComponents & subclasses thereof
 * It works as a mouseAdapter to process the mouse motion events,
 * and a key listener to change the cursor to a hand
 *
 * @author Taeler Dixon
 */
public class DragScrollAdapter extends MouseAdapter {

	Point scrollPoint;
	boolean dragging = false;

	@Override
	public void mouseDragged(MouseEvent eve) {
		if (dragging) {
			//http://java-swing-tips.blogspot.com/2008/06/mouse-drag-auto-scrolling.html
			JComponent src = (JComponent) eve.getSource();
			JViewport view = (JViewport) src.getParent();
			Point converted = SwingUtilities.convertPoint(src, eve.getPoint(), view);
			int difX = scrollPoint.x - converted.x;
			int difY = scrollPoint.y - converted.y;
			Point vp = view.getViewPosition();
			vp.translate(difX, difY);
			src.scrollRectToVisible(new Rectangle(vp, view.getSize()));
			scrollPoint.setLocation(converted);
		}
	}

	public void mouseMoved(MouseEvent eve) {
		JComponent src = (JComponent) eve.getSource();
		if ((eve.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {//shift pressd
			if (!src.isCursorSet()) {
				src.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}
		} else {
			if (src.isCursorSet()) {
				src.setCursor(null);
			}
		}
	}

	public void mousePressed(MouseEvent eve) {
		if ((eve.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
			JComponent src = (JComponent) eve.getSource();
			JViewport view = (JViewport) src.getParent();
			scrollPoint = SwingUtilities.convertPoint(src, eve.getPoint(), view);
			dragging = true;
		} else {
			dragging = false;
		}
	}
}
