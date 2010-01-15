/*
 * Copyright Samuel Halliday 2010
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

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import javax.vecmath.Vector3d;

/**
 *
 * @author Samuel Halliday
 */
public class Utils {

	/**
	 * @param min
	 * @param value
	 * @param max
	 * @return
	 */
	public static int bounded(int min, int value, int max) {
		Preconditions.checkArgument(max >= min);
		return Math.max(min, Math.min(value, max));
	}

	private static final Logger log = Logger.getLogger(GameMC.class.getName());

	// this is mutable, so be careful not to edit it
	private static final Vector3d NORTH = new Vector3d(0, -1, 0);

	/**
	 * @param vector
	 * @return the angle relate to NORTH {@code (-PI, + PI]}, ignoring {@code z} component.
	 */
	public static double getBearing(Vector3d vector) {
		Vector3d v = (Vector3d) vector.clone();
		v.z = 0;
		if (v.x < 0)
			return -v.angle(NORTH);
		else
			return v.angle(NORTH);
	}
}
