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

import static java.lang.Math.min;
import static java.lang.Math.round;
import com.google.common.base.Preconditions;
import java.awt.Point;
import static java.lang.Math.max;
import static java.lang.Math.signum;
import static java.lang.Math.abs;
import java.util.Collection;
import java.util.logging.Logger;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import uk.me.fommil.ff.Tactics.BallZone;

/**
 * The model (M) and controller (C) for the ball during game play.
 * 
 * @author Samuel Halliday
 */
public class BallMC {

	// View
	@Deprecated
	public void setLocation(Point p) {
		setPosition(new Point3d(p.x, p.y, 0));
	}

	@Deprecated
	public Point getLocation() {
		return new Point((int) round(s.x), (int) round(s.y));
	}

	/**
	 * The aftertouch that a ball may exhibit. Directional aftertouch is always relate to
	 * the direction of motion.
	 */
	public enum Aftertouch {

		LIFT, POWER, LEFT, RIGHT
	}
	private static final Logger log = Logger.getLogger(BallMC.class.getName());
	private Point3d s = new Point3d(0, 0, 0);
	private Vector3d v = new Vector3d();
	private static final double FRICTION = 10;
	private static final double GRAVITY = -10;
	// aftertouch direction
	private Vector3d aftertouch = new Vector3d();

//	/**
//	 * Return the volume in which this player can control the ball.
//	 *
//	 * @return
//	 */
//	public Bounds getBounds() {
//		return new BoundingSphere(s, 2);
//	}
	/**
	 * @param width
	 * @param height
	 * @return
	 */
	public BallZone getZone(double width, double height) {
		Preconditions.checkArgument(width > 0);
		Preconditions.checkArgument(height > 0);
		int x, y;

		x = (int) (5 * (width - s.x) / width);
		y = (int) (7 * (height - s.y) / height);
		x = max(0, min(x, 4));
		y = max(0, min(y, 6));

		return new BallZone(x, y);
	}

	/**
	 * Update the model.
	 *
	 * @param t with units of seconds
	 */
	public void tick(double t) {
		// apply gravity
		v.z += t * GRAVITY;

		// update position
		s.x += v.x * t;
		s.y += v.y * t;
		// TODO: bounces
		s.z = max(0, s.z + v.z * t);
		if (s.z == 0)
			v.z = 0;

		// apply friction
		v.x = signum(v.x) * max(0, abs(v.x) - FRICTION);
		v.y = signum(v.y) * max(0, abs(v.y) - FRICTION);
	}

	/**
	 * Controller.
	 *
	 * @param aftertouches
	 */
	public void setAftertouches(Collection<Aftertouch> aftertouches) {
		if (aftertouches.isEmpty() || s.z < 10)
			return;

		// apply aftertouch
		Vector3d ats = new Vector3d();
		for (Aftertouch at : aftertouches) {
			switch (at) {
				case LIFT:
					break;
				case POWER:
					break;
				case LEFT:
					break;
				case RIGHT:
					break;
			}
		}
		aftertouch = ats;
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public Point3d getPosition() {
		return (Point3d) s.clone();
	}

	public void setPosition(Point3d s) {
		Preconditions.checkNotNull(s);
		this.s = s;
	}

	public Vector3d getVelocity() {
		return (Vector3d) v.clone();
	}

	public void setVelocity(Vector3d v) {
		Preconditions.checkNotNull(v);
		this.v = v;
	}
	// </editor-fold>
}
