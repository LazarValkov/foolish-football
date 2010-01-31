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
import uk.me.fommil.ff.physics.Position;

/**
 * This is a container that is tied to the pixel values of features from the SWOS pitch graphics. A
 * ratio is used to convert pixels into meters.
 * <p>
 * The pitches are (672, 880).
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

		NORTH, SOUTH;

	}

	private static final double SCALE = 0.1;

	private static final Point bounds = new Point(672, 880);

	private static final Rectangle pitch = new Rectangle(81, 129, 509, 640);

	private static final Point centreSpot = new Point(336, 449);

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
//	private final Rectangle goalNetTop = new Rectangle(300, 117, 71, 12);
//
//	private final Rectangle goalNetBottom = new Rectangle(300, 769, 71, 12);
//	private final Point penaltySpotTop = new Point(336, 187);
//
//	private final Point penaltySpotBottom = new Point(336, 711);
	public Position getPitchLowerLeft() {
		return new Position(pitch.x * SCALE, (bounds.y - pitch.y - pitch.height) * SCALE, 0);
	}

	public Position getPitchUpperRight() {
		return new Position((pitch.x + pitch.width) * SCALE, (bounds.y - pitch.y) * SCALE, 0);
	}

	public Position getCentre() {
		return new Position(centreSpot.x * SCALE, (bounds.y - centreSpot.y) * SCALE, 0);
	}

	@Deprecated // implementation detail
	public double getScale() {
		return SCALE;
	}
}
