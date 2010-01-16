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
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;
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

	/**
	 * @param bulk
	 * @param s
	 * @param v
	 * @param ds
	 * @return
	 * @deprecated TODO create a simple object oriented physics engine
	 */
	@Deprecated
	public static Point3d entryPoint(Bounds bulk, Point3d s, Vector3d v, double ds) {
		Preconditions.checkNotNull(bulk);
		Preconditions.checkNotNull(s);
		Preconditions.checkNotNull(v);
		Preconditions.checkArgument(bulk.intersect(s));
		Preconditions.checkArgument(v.length() > 0);

		// really dumb maths, take the velocity and work back to the point where
		// the object entered the intersection. Numerical, brute force approach
		// TODO: analytical solution for BoundingBox
		Vector3d r = (Vector3d) v.clone();
		Utils.reverse(r);
		r.normalize();
		r.scale(ds);
		Point3d s2 = (Point3d) s.clone();

		do {
			s2.add(r);
		} while (bulk.intersect(s2));
		return s2;
	}

	/**
	 * @param box
	 * @param s
	 * @return
	 * @deprecated TODO create a simple object oriented physics engine
	 */
	@Deprecated
	public static Vector3d entrySurface(BoundingBox box, Point3d s) {
		Preconditions.checkNotNull(box);
		Preconditions.checkNotNull(s);
		Preconditions.checkArgument(!box.intersect(s));
		Point3d lower = getLower(box);
		Point3d upper = getUpper(box);

		if (s.x < lower.x || s.x > upper.x)
			return new Vector3d(0, 1, 1);
		else if (s.y < lower.y || s.y > upper.y)
			return new Vector3d(1, 0, 1);
		else if (s.z < lower.z || s.z > upper.z)
			return new Vector3d(1, 1, 0);
		throw new UnsupportedOperationException("extreme case failure");
	}

	public static Point3d getLower(BoundingBox box) {
		Preconditions.checkNotNull(box);
		Point3d lower = new Point3d();
		box.getLower(lower);
		return lower;
	}

	public static Point3d getUpper(BoundingBox box) {
		Preconditions.checkNotNull(box);
		Point3d upper = new Point3d();
		box.getUpper(upper);
		return upper;
	}

	private static void reverse(Vector3d vector) {
		Preconditions.checkNotNull(vector);
		vector.x = 0 - vector.x;
		vector.y = 0 - vector.y;
		vector.z = 0 - vector.z;
	}

	/**
	 * @param box
	 * @param s
	 * @param v
	 * @return
	 * @deprecated TODO create a simple object oriented physics engine
	 */
	@Deprecated
	public static Vector3d rebound(BoundingBox box, Point3d s, Vector3d v) {
		Preconditions.checkNotNull(box);
		Preconditions.checkNotNull(s);
		Preconditions.checkNotNull(v);
		Preconditions.checkArgument(!box.intersect(s));

		Point3d lower = getLower(box);
		Point3d upper = getUpper(box);

		Vector3d vec = new Vector3d(v);
		if (s.x < lower.x || s.x > upper.x)
			vec.x = 0 - vec.x;
		else if (s.y < lower.y || s.y > upper.y)
			vec.y = 0 - vec.y;
		else if (s.z < lower.z || s.z > upper.z)
			vec.z = 0 - vec.z;
		else
			throw new UnsupportedOperationException("extreme case failure");
		return vec;
	}
}
