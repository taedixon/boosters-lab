package ca.noxid.lab.tile;

import ca.noxid.lab.EditorApp;

import java.awt.*;

public enum LINECLASS {
	FLOOR, ROOF, LEFTWALL, RIGHTWALL, NONE;

	public static LINECLASS classify(Point p1, Point p2) {
		//tells us how to treat the line during collisions;
		if (p2 == null) {
			return LINECLASS.NONE;
		}
		double x1 = p1.getX();
		double x2 = p2.getX();
		double y1 = p1.getY();
		double y2 = p2.getY();
		if ((x1 - x2) == 0) {//avoid divide by 0
			if ((y2 - y1) > 0)//top to bottom
			{
				return LINECLASS.LEFTWALL;
			} else {
				return LINECLASS.RIGHTWALL;
			}
		} else {//otherwise we examine the slope of the line
			double slope = ((y2 - y1) / (x2 - x1));
			if ((slope <= 1) && (slope >= -1)) {
				if ((x2 - x1) > 0)//left to right
				{
					return LINECLASS.FLOOR;
				} else {
					return LINECLASS.ROOF;
				}
			} else {
				if ((y2 - y1) > 0)//top to bottom
				{
					return LINECLASS.LEFTWALL;
				} else {
					return LINECLASS.RIGHTWALL;
				}
			}
		}
	}

	public void drawLine(Point p1, Point p2, Graphics2D g2d, Color bottom, Color top) {
		int xPos = (int) (p1.x * EditorApp.mapScale);
		int yPos = (int) (p1.y * EditorApp.mapScale);
		int yPos2 = (int) (p2.y * EditorApp.mapScale);
		int xPos2 = (int) (p2.x * EditorApp.mapScale);
		switch (this) {
		case FLOOR:
			yPos++;
			yPos2++;
			break;
		case ROOF:
			yPos--;
			yPos2--;
			break;
		case LEFTWALL:
			xPos--;
			xPos2--;
			break;
		case RIGHTWALL:
			xPos++;
			xPos2++;
			break;
		case NONE:
			break;
		default:
			break;
		}
		g2d.setColor(bottom);
		g2d.drawLine(xPos, yPos, xPos2, yPos2); //draw the basic line

		switch (this) {
		case ROOF:
			yPos += 2;
			yPos2 += 2;
			break;
		case FLOOR:
			yPos -= 2;
			yPos2 -= 2;
			break;
		case RIGHTWALL:
			xPos -= 2;
			xPos2 -= 2;
			break;
		case LEFTWALL:
			xPos += 2;
			xPos2 += 2;
			break;
		case NONE:
			break;
		default:
			break;
		}
		g2d.setColor(top);
		g2d.drawLine(xPos, yPos, xPos2, yPos2); //draw the other line

	}
}