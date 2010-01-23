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

import com.google.common.base.Preconditions;

/**
 *
 * @author Samuel Halliday
 */
public enum Direction {

	UP, DOWN, LEFT, RIGHT, UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT;

	/**
	 * @param angle {@code (-PI, PI]}
	 * @return
	 */
	public static Direction valueOf(double angle) {
		if (Double.isNaN(angle))
			return null;
		Preconditions.checkArgument(angle <= Math.PI && angle > -Math.PI, angle);
		if (angle <= -3 * Math.PI / 4 + Math.PI / 8) {
			return DOWN_LEFT;
		} else if (angle <= -Math.PI / 2 + Math.PI / 8) {
			return LEFT;
		} else if (angle <= -Math.PI / 4 + Math.PI / 8) {
			return UP_LEFT;
		} else if (angle <= 0 + Math.PI / 8) {
			return UP;
		} else if (angle <= Math.PI / 4 + Math.PI / 8) {
			return UP_RIGHT;
		} else if (angle <= Math.PI / 2 + Math.PI / 8) {
			return RIGHT;
		} else if (angle <= 3 * Math.PI / 4 + Math.PI / 8) {
			return DOWN_RIGHT;
		} else {
			return DOWN;
		}
	}
}
