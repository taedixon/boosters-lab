package ca.noxid.lab.tile;

import ca.noxid.lab.EditorApp;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class LineSeg extends Line2D {
	public static final int NONE = 0;
	public static final int FIRST = 1;
	public static final int SECOND = 2;
	public static final int LINE = 3;
	private static final int clickRange = 5;
	private Point p1, p2;
	//private LineSeg linkedSeg;
	private int lineType;
	private int selection;
	private LINECLASS lineSlope = LINECLASS.NONE;
	public LineSeg(Point p1, Point p2, int type) {
		this.p1 = p1;
		this.p2 = p2;
		lineType = type;
		lineSlope = LINECLASS.classify(p1, p2);
	}

	public LineSeg(Point p, int type) {
		p1 = p;
		p2 = null;
		lineType = type;
		lineSlope = LINECLASS.classify(p1, null);
	}

	public int getType() {
		return lineType;
	}

	public void setType(int currentType) {
		lineType = currentType;
	}

	private TypeConfig getCurrentTypeConfig() {
		TypeConfig rv = TypeConfig.getType(lineType);
		if (rv == null) {
			lineType = 1;
			rv = TypeConfig.getType(1);
		}
		return rv;
	}

	public boolean isSelected() {
		return selection != 0;
	}

	public void draw(Graphics2D g2d) {
		int xPos = (int) (p1.x * EditorApp.mapScale);
		int yPos = (int) (p1.y * EditorApp.mapScale);
		//draw the point
		g2d.setColor(Color.RED);
		g2d.fillRect(xPos - 3,
				yPos - 3,
				6,
				6);
		if ((selection & FIRST) != 0) {
			int[] starX = {
					xPos - 5, xPos, xPos + 5, xPos
			};
			int[] starY = {
					yPos, yPos - 5, yPos, yPos + 5
			};
			Shape star = new Polygon(starX, starY, 4);
			g2d.setColor(Color.cyan);
			g2d.draw(star);
		}
		if (p2 != null) {
			int xPos2 = (int) (p2.x * EditorApp.mapScale);
			int yPos2 = (int) (p2.y * EditorApp.mapScale);

			g2d.setColor(Color.yellow);
			g2d.fillRect(xPos2 - 3,
					yPos2 - 3,
					6,
					6);
			if ((selection & SECOND) != 0) {
				int[] starX = {
						xPos2 - 5, xPos2, xPos2 + 5, xPos2
				};
				int[] starY = {
						yPos2, yPos2 - 5, yPos2, yPos2 + 5
				};
				Shape star = new Polygon(starX, starY, 4);
				g2d.setColor(Color.cyan);
				g2d.draw(star);
			}

			Graphics2D lineGfx = (Graphics2D) g2d.create();
			lineGfx.setStroke(new BasicStroke(2F));
			TypeConfig type = getCurrentTypeConfig();
			Color bottomCol;
			if (selection == LINE) {
				bottomCol = Color.CYAN;
			} else {
				bottomCol = type.bottomColour;
			}

			lineSlope.drawLine(p1, p2, lineGfx, bottomCol, type.topColour);
		}
	}

	public void drag(int dx, int dy) {
		if ((selection & FIRST) != 0) {
			p1.x += dx;
			p1.y += dy;
		}
		if ((selection & SECOND) != 0) {
			p2.x += dx;
			p2.y += dy;
		}
		lineSlope = LINECLASS.classify(p1, p2);
	}

	public void setSelection(int select, boolean additive) {
		if (!additive) {
			selection = select;
		} else {
			selection ^= select;
		}
	}

	/**
	 * Checks for collision with point P, and returns a number
	 * detailing which "thing" of the line is closest to being clicked.
	 *
	 * @param p the point to be compared
	 * @return one of values LineSeg.NONE, LineSeg.FIRST, LineSeg.SECOND, LineSeg.LINE
	 */
	public int inRange(Point p) {
		if (p.x > this.p1.x - clickRange &&
				p.x < this.p1.x + clickRange &&
				p.y > this.p1.y - clickRange &&
				p.y < this.p1.y + clickRange) {
			return FIRST;
		} else if (this.p2 != null &&
				p.x > this.p2.x - clickRange &&
				p.x < this.p2.x + clickRange &&
				p.y > this.p2.y - clickRange &&
				p.y < this.p2.y + clickRange) {
			return SECOND;
		} else if (this.p2 != null && this.ptSegDist(p) < clickRange) {
			return LINE;
		}
		return NONE;
	}

	@Override
	public Rectangle2D getBounds2D() {
		return new Rectangle2D.Double(
				getX1(), getY1(), getX2(), getY2());
	}

	@Override
	public Point2D getP1() {
		return p1;
	}

	@Override
	public Point2D getP2() {
		return p2;
	}

	public void setP2(Point newP) {
		p2 = newP;
		lineSlope = LINECLASS.classify(p1, p2);
	}

	@Override
	public double getX1() {
		return (double) p1.x;
	}

	@Override
	public double getX2() {
		return (double) p2.x;
	}

	@Override
	public double getY1() {
		return (double) p1.y;
	}

	@Override
	public double getY2() {
		return (double) p2.y;
	}

	@Override
	public void setLine(double x1, double y1, double x2, double y2) {
		p1.x = (int) x1;
		p2.x = (int) x2;
		p1.y = (int) y1;
		p2.y = (int) y2;
		lineSlope = LINECLASS.classify(p1, p2);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof LineSeg) {
			LineSeg lOther = (LineSeg) other;
			if (this.p1.equals(lOther.p1)
					&& this.p2.equals(lOther.p2)
					&& this.lineType == lOther.lineType) {
				return true;
			}
		}
		return false;
	}

}