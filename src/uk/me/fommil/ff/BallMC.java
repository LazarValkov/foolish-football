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

import static java.lang.Math.max;
import static java.lang.Math.signum;
import static java.lang.Math.abs;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import uk.me.fommil.ff.GameMVC.Action;

/**
 * The model (M) and controller (C) for the ball during game play.
 * 
 * @author Samuel Halliday
 */
public class BallMC {

	private static final Logger log = Logger.getLogger(BallMC.class.getName());
	private final Point3d s = new Point3d(200, 400, 0);
	private Point3d v = new Point3d();
	private static final double FRICTION = 10;
	private static final double GRAVITY = -10;

	// aftertouch direction
	Point2d at = new Point2d();

	/**
	 * @return
	 */
	public Point2D getLocation() {
		return new Point2D.Double(s.x, s.y);
	}

	/**
	 * @param v
	 */
	public void setVelocity(Point3d v) {
		this.v = v;
	}

	/**
	 * @return
	 */
	public double getHeight() {
		return s.z;
	}

	/**
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
	 * @param actions
	 */
	public void setActions(Collection<Action> actions) {
		if (actions.isEmpty() || s.z < 10)
			return;

		// apply aftertouch
		Point2d at = new Point2d();
		for (Action action : actions) {
			switch (action) {
				case UP:
					at.y += 1;
					break;
				case DOWN:
					at.y -= 1;
					break;
				case LEFT:
					at.x -= 1;
					break;
				case RIGHT:
					at.x += 1;
					break;
			}
		}
		this.at = at;
	}
}
