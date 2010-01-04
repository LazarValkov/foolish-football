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
import java.awt.Rectangle;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import java.util.Collection;
import java.util.logging.Logger;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import uk.me.fommil.ff.Tactics.BallZone;

/**
 * The model (M) and controller (C) for the ball during game play.
 * 
 * @author Samuel Halliday
 */
public class BallMC {

	/**
	 * The aftertouch that a ball may exhibit. Directional aftertouch is always relate to
	 * the direction of motion.
	 */
	public enum Aftertouch {

		UP, DOWN, LEFT, RIGHT

	}

	private static final Logger log = Logger.getLogger(BallMC.class.getName());

	private Point3d s = new Point3d(0, 0, 0);

	private Vector3d v = new Vector3d();

	// TODO: friction in air
	private static final double FRICTION = 100;

	private static final double GRAVITY = -10;

	// aftertouch direction
	private Vector3d aftertouch = new Vector3d();

	/**
	 * @param pitch
	 * @return
	 */
	public BallZone getZone(Pitch pitch) {
		Preconditions.checkNotNull(pitch);
		Rectangle p = pitch.getPitchAsRectangle();

//		double bx = min(max(s.x, p.x), p.x + p.width);
//		double by = min(max(s.y, p.y), p.y + p.height);

		int x = (int) (5 * (p.width + p.x - s.x) / p.width);
		int y = (int) (7 * (p.height + p.y - s.y) / p.height);

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

		// FIXME: aftertouch
		// apply aftertouch
		if (s.z > 0.5) {
			Vector3d a = (Vector3d) aftertouch.clone();
			if (v.z <= 0 || s.z > 3) {
				// no more lift or velocity allowed when the ball is coming down or too high
				a.z = 0;
				a.y = 0;
			}
			//a.scale(t);
			v.add(a);
		}

		// update position
		s.x += v.x * t;
		s.y += v.y * t;
		s.z += v.z * t;
		// TODO: more efficient "bounce" logic
		if (s.z < 0) {
			s.z = Math.abs(s.z) / 2;
			v.z = Math.abs(v.z) / 2;
			if (s.z < 0.5) {
				s.z = 0;
				v.z = 0;
			}
		}

		// apply friction
		v.x = signum(v.x) * max(0, abs(v.x) - t * FRICTION);
		v.y = signum(v.y) * max(0, abs(v.y) - t * FRICTION);
	}

	/**
	 * Controller.
	 *
	 * @param aftertouches
	 */
	public void setAftertouches(Collection<Aftertouch> aftertouches) {
		// apply aftertouch
		aftertouch.scale(0);
		for (Aftertouch at : aftertouches) {
			switch (at) {
				// TODO: consider direction of motion
				case DOWN:
					aftertouch.z = 2;
					aftertouch.y -= 10;
					break;
				case UP:
					aftertouch.y -= 5;
					break;
				case LEFT:
					aftertouch.x -= 5;
					break;
				case RIGHT:
					aftertouch.x += 5;
					break;
			}
		}
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
