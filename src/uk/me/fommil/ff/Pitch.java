/*
 * Copyright Samuel Halliday 2009
 * 
 * This file is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this file.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package uk.me.fommil.ff;

import java.awt.Point;
import java.awt.Rectangle;
import javax.media.j3d.BoundingBox;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

/**
 * Contains the details of the lines on the pitch in 'v' coordinates.
 * TODO: should be in 'p' coordinates.
 * <p>
 * This is a solid implementation that is tied to the values from the SWOS
 * pitch graphics.
 * <p>
 * The location of the corner flags are pixels (81, 129), (590, 129), (590, 769),
 * (81, 769).
 * <p>
 * The location of the goal nets are (300, 769), (300, 751), (371, 751),
 * (371, 769) and (371, 129), (371, 111), (300, 111), (300, 129).
 * (Note that the posts are two pixels wide, and these correspond to the leftmost pixels).
 * <p>
 * The locations of the penalty boxes are (193, 129), (478, 129), (193, 216),
 * (478, 216) and (193, 769), (193, 682), (478, 682), (478, 769).
 * <p>
 * The locations of the goal boxes are (273, 769), (273, 740), (398, 740), (398, 769)
 * and (273, 129), (273, 158), (398, 129), (398, 158).
 * <p>
 * The penalty spots are (336, 711) and (336, 187).
 * <p>
 * The centre spot is (336, 449).

 * @author Samuel Halliday
 */
public class Pitch {

	public enum Facing {
		UP, DOWN
	}

	private final Rectangle pitch = new Rectangle(81, 129, 509, 640);

//	private final Rectangle penaltyBoxTop = new Rectangle(193, 129, 285, 87);
//
//	private final Rectangle penaltyBoxBottom = new Rectangle(193, 682, 285, 87);
//
//	private final Rectangle goalBoxTop = new Rectangle(273, 129, 125, 29);
//
//	private final Rectangle goalBoxBottom = new Rectangle(273, 740, 125, 29);
//
//	private final Rectangle goalLineTop = new Rectangle(300, 129, 71, 1);
//
//	private final Rectangle goalLineBottom = new Rectangle(300, 769, 71, 1);

	private final Rectangle goalNetTop = new Rectangle(300, 117, 71, 12);

	private final Rectangle goalNetBottom = new Rectangle(300, 769, 71, 12);

//	private final Point penaltySpotTop = new Point(336, 187);
//
//	private final Point penaltySpotBottom = new Point(336, 711);

	private final Point centreSpot = new Point(336, 449);

	public BoundingBox getPitch() {
		return rectangleTo3d(pitch, new Point2d(0, Double.MAX_VALUE));
	}

	public BoundingBox getGoalNetTop() {
		return rectangleTo3d(goalNetTop, new Point2d(0, 30)); // FIXME: z of goal post
	}

	public BoundingBox getGoalNetBottom() {
		return rectangleTo3d(goalNetBottom, new Point2d(0, 30)); // FIXME: z of goal post
	}

	public Rectangle getPitchAsRectangle() {
		return pitch;
	}

	public Point3d getCentre() {
		return new Point3d(centreSpot.x, centreSpot.y, 0);
	}

	// provides infinite z bounds to a rectangle
	private BoundingBox rectangleTo3d(Rectangle r, Point2d h) {
		return new BoundingBox(
				new Point3d(r.x, r.y, h.x),
				new Point3d(r.x + r.width, r.y + r.height, h.y));
	}
}
