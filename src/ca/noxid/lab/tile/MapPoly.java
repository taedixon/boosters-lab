package ca.noxid.lab.tile;

import ca.noxid.lab.EditorApp;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

public class MapPoly {

	private static final int clickRange = 5;
	TypeConfig type;
	private LinkedList<xPoint> points;
	private Set<xPoint> selected = new HashSet<>();
	private int eventNum;
	private boolean flipped = false;
	private boolean active = false;
	public MapPoly(Point firstPoint, TypeConfig t) {
		type = t;
		points = new LinkedList<>();
		points.add(new xPoint(firstPoint));
	}

	public void draw(Graphics2D g2d) {
		xPoint prev = null;
		Graphics2D linefx = (Graphics2D) g2d.create();
		linefx.setStroke(new BasicStroke(2f));
		g2d.setColor(Color.ORANGE);
		for (xPoint p : points) {
			if (prev != null) {
				LINECLASS lc = LINECLASS.classify(prev, p);
				Color top = type.topColour;
				Color bottom = type.bottomColour;
				if (active) {
					top = top.brighter();
					bottom = bottom.brighter();
				}
				if (selected.contains(p) && selected.contains(prev)) {
					bottom = Color.CYAN;
				}
				if (flipped) {
					lc.drawLine(prev, p, linefx, top, bottom);
				} else {
					lc.drawLine(prev, p, linefx, bottom, top);
				}
			}
			if (prev == null) {
				g2d.setColor(Color.red);
			} else {
				if (active) {
					g2d.setColor(Color.green);
				} else {
					g2d.setColor(Color.ORANGE);
				}
			}
			int xPos = (int) (p.x * EditorApp.mapScale);
			int yPos = (int) (p.y * EditorApp.mapScale);
			g2d.fillRect(xPos - 3, yPos - 3, 6, 6);
			if (selected.contains(p)) {
				int[] starX = {
						xPos - 5, xPos, xPos + 5, xPos
				};
				int[] starY = {
						yPos, yPos - 5, yPos, yPos + 5
				};
				Graphics2D starGFX = (Graphics2D) g2d.create();
				starGFX.setColor(Color.CYAN);
				starGFX.draw(new Polygon(starX, starY, 4));
			}
			prev = p;
		}

		if (prev != null) {
			Point start = points.getFirst();
			//close the polygon
			linefx.drawLine((int) (start.x * EditorApp.mapScale),
					(int) (start.y * EditorApp.mapScale),
					(int) (prev.x * EditorApp.mapScale),
					(int) (prev.y * EditorApp.mapScale));
		}
	}

	public void extend(Point p) {
		xPoint xp = new xPoint(p);
		points.add(xp);
	}

	public void insert(xPoint after) {
		int index = points.indexOf(after);
		if (index < 0) return;
		if (points.size() == index + 1) return;
		xPoint next = points.get(index + 1);
		xPoint mid = new xPoint((after.x + next.x) / 2, (after.y + next.y) / 2);
		points.add(index + 1, mid);
	}

	public void remove(Collection<xPoint> pts) {
		points.removeAll(pts);
	}

	public void remove(xPoint p) {
		points.remove(p);
	}

	public void clearSelection() {
		selected.clear();
	}

	public boolean trySelect(Point click, boolean additive, boolean expansive) {

		if (!additive) {
			selected.clear();
		}
		boolean succ = false;
		for (xPoint p : points) {
			if (click.x > p.x - clickRange &&
					click.x < p.x + clickRange &&
					click.y > p.y - clickRange &&
					click.y < p.y + clickRange) {
				selected.add(p);
				succ = true;
				if (!additive) break;
			}
		}
		if (!succ) {
			xPoint prev = null;
			for (xPoint p : points) {
				if (prev != null) {
					Line2D line = new Line2D.Double(p, prev);
					if (line.ptSegDist(click) < clickRange) {
						selected.add(prev);
						selected.add(p);
						succ = true;
						if (!additive) break;
					}
				}
				prev = p;
			}
		}
		if (succ && expansive) {
			selected.addAll(points);
		}
		return succ;
	}

	public void drag(int x, int y) {
		for (Point p : selected) {
			p.translate(x, y);
		}
	}

	public Set<xPoint> getSelected() {
		return selected;
	}

	public void flip() {
		flipped = !flipped;
	}

	public boolean isFlipped() {
		return flipped;
	}

	public List<xPoint> getPoints() {
		return points;
	}

	public void setActive(boolean b) {
		active = b;
	}

	public short getType() {
		// TODO Auto-generated method stub
		return (short) type.id;
	}

	public short getEvent() {
		return (short) eventNum;
	}

	public void setEvent(int e) {
		eventNum = e;
	}

	/**
	 * This class is to subvert a funny behaviour in Java that really
	 * messes up the way Points behave in HashSets, because their HashCode
	 * is based on the x and y values which can lead to both being able to add
	 * the same point *object* to a set twice and not being able to add
	 * a *different* point object that happens to have the same location.
	 */
	@SuppressWarnings("serial")
	public class xPoint extends Point {

		int specialUniqueNumber = new Random().nextInt();

		public xPoint(Point firstPoint) {
			super(firstPoint);
		}

		public xPoint(int i, int j) {
			super(i, j);
		}

		@Override
		public boolean equals(Object p2) {
			return p2 instanceof xPoint && p2.hashCode() == this.hashCode();
		}

		@Override
		public int hashCode() {
			return specialUniqueNumber;
		}
	}
}
