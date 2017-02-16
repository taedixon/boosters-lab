package ca.noxid.uiComponents;

import ca.noxid.lab.EditorApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Timer;
import java.util.TimerTask;


public class ScrollZoomAdapter implements MouseWheelListener {
	private EditorApp parent;
	private JPanel target;

	public ScrollZoomAdapter(EditorApp p, JPanel pane) {
		parent = p;
		target = pane;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent eve) {
		if (eve.isControlDown()) {
			Point p = eve.getPoint();
			final Rectangle r = target.getVisibleRect();
			//System.out.println(r);
			r.x = r.x + p.x;
			r.y = r.y + p.y;
			//System.out.println(dx + " " + dy);
			if (eve.getWheelRotation() < 0) {
				parent.mapZoomIn();
				r.x *= 2;
				r.y *= 2;
			} else {
				parent.mapZoomOut();
				r.x /= 2;
				r.y /= 2;
			}
			r.x -= r.width / 2;
			r.y -= r.height / 2;
			TimerTask t = new TimerTask() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					target.scrollRectToVisible(r);
					//System.out.println(target.getVisibleRect());
				}

			};
			Timer timer = new Timer();
			timer.schedule(t, 30);
			//target.scrollRectToVisible(r);
		}
	}
}
