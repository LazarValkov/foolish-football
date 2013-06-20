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
package com.github.fommil.ff;

import com.google.common.base.Preconditions;

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

	/**
	 * @param min
	 * @param value
	 * @param max
	 * @return
	 */
	public static double bounded(double min, double value, double max) {
		Preconditions.checkArgument(max >= min);
		return Math.max(min, Math.min(value, max));
	}

	/**
	 * @param d
	 * @return
	 */
	public static int round(double d) {
		return (int) Math.round(d);
	}

}
