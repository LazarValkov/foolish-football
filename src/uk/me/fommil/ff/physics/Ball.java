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
package uk.me.fommil.ff.physics;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.logging.Logger;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.Tactics.BallZone;

/**
 * The model (M) and controller (C) for the ball during game play.
 * 
 * @author Samuel Halliday
 * @see <a href="http://en.wikipedia.org/wiki/Football_(ball)#Dimensions">Dimension information</a>
 */
public class Ball {

	private static final double MASS_KG = 0.45;

	private final DSphere sphere;

	private final double g;

	/** The aftertouch that a ball may exhibit. Aftertouch depends on the direction of motion. */
	public enum Aftertouch {

		UP, DOWN, LEFT, RIGHT

	}

	private static final Logger log = Logger.getLogger(Ball.class.getName());

	// aftertouch
//	private final Vector3d after = new Vector3d();
	// no aftertouch after a bounce
//	private volatile boolean bounced = false;
	// creates Ball and assign it to the body
	Ball(DWorld world, DSpace space) {
		Preconditions.checkNotNull(world);
		Preconditions.checkNotNull(space);

		DVector3 gravity = new DVector3();
		world.getGravity(gravity);
		g = gravity.length();

		DBody body = OdeHelper.createBody(world);
		double radius = 0.7 / (2 * Math.PI);
		sphere = OdeHelper.createSphere(radius);
		sphere.setBody(body);
		DMass mass = OdeHelper.createMass();
		mass.setSphereTotal(MASS_KG, radius);
		body.setMass(mass);
		space.add(sphere);
		body.setData(this);
	}

	private volatile double mu;

	@Deprecated // HACK to workaround ODE lack of friction on spheres
	void applyFriction() {
		if (mu == 0)
			return;
		DVector3 friction = new DVector3(sphere.getBody().getLinearVel());
		friction.scale(-1);
		friction.scale(mu * MASS_KG * g);
		sphere.getBody().addForce(friction);
	}

	@Deprecated // HACK to workaround ODE lack of friction on spheres
	void setFriction(double mu) {
		this.mu = mu;
	}

	void addForce(DVector3C force) {
		assert force != null;
		DBody body = sphere.getBody();
		body.addForce(force);
	}

	void setVelocity(DVector3 v) {
		sphere.getBody().setLinearVel(v);
	}

	/**
	 * @param pitch
	 * @return
	 */
	public BallZone getZone(Pitch pitch) {
		Preconditions.checkNotNull(pitch);
		return new BallZone(new Position(sphere.getPosition()), pitch);
	}

	/**
	 * Controller.
	 *
	 * @param aftertouches
	 */
	public void setAftertouch(Collection<Aftertouch> aftertouches) {
//		// TODO: consider the player who applies the aftertouch
//		Vector3d aftertouch = new Vector3d();
//		for (Aftertouch at : aftertouches) {
//			switch (at) {
//				case UP:
//					aftertouch.y -= 1;
//					break;
//				case DOWN:
//					aftertouch.y += 1;
//					break;
//				case LEFT:
//					aftertouch.x -= 1;
//					break;
//				case RIGHT:
//					aftertouch.x += 1;
//					break;
//			}
//		}
////		log.info(aftertouches + " " + aftertouch);
//		after.scale(0);
//		Direction direction = Direction.valueOf(Utils.getBearing(v));
//		if (v.lengthSquared() == 0 || direction == null) {
//			return;
//		}
//		// TODO: clean up horrible code duplication
//		double power = 200;
//		double power_gravity = GRAVITY / 3;
//		double power_bendy = 200;
//		double lift = 20;
//		double lift_gravity = 3 * GRAVITY;
//		double lift_bendy = 50;
//
//		// log.info("BALL FACING " + direction + " AFTERTOUCH " + aftertouch);
//		switch (direction) {
//			case UP_RIGHT:
//			case UP_LEFT:
//			case UP:
//				if (aftertouch.y < 0) {
//					// power shot
//					after.x = power_bendy * aftertouch.x;
//					after.y = -power;
//					after.z = power_gravity;
//				} else if (aftertouch.y > 0) {
//					// lift
//					after.x = lift_bendy * aftertouch.x;
//					after.y = -lift;
//					after.z = lift_gravity;
//				}
//				break;
//			case DOWN_LEFT:
//			case DOWN_RIGHT:
//			case DOWN:
//				if (aftertouch.y > 0) {
//					after.x = power_bendy * aftertouch.x;
//					after.y = power;
//					after.z = power_gravity;
//				} else if (aftertouch.y < 0) {
//					after.x = lift_bendy * aftertouch.x;
//					after.y = lift;
//					after.z = lift_gravity;
//				}
//				break;
//			case RIGHT:
//				if (aftertouch.x > 0) {
//					after.x = power;
//					after.y = power_bendy * aftertouch.y;
//					after.z = power_gravity;
//				} else if (aftertouch.x < 0) {
//					after.x = lift;
//					after.y = lift_bendy * aftertouch.y;
//					after.z = lift_gravity;
//				}
//				break;
//			case LEFT:
//				if (aftertouch.x < 0) {
//					after.x = -power;
//					after.y = power_bendy * aftertouch.y;
//					after.z = power_gravity;
//				} else if (aftertouch.x > 0) {
//					after.x = -lift;
//					after.y = lift_bendy * aftertouch.y;
//					after.z = lift_gravity;
//				}
//				break;
//		}
	}

	public Velocity getVelocity() {
		return new Velocity(sphere.getBody().getLinearVel());
	}

	public Position getPosition() {
		return new Position(sphere.getPosition());
	}

	public void setPosition(Position p) {
		DVector3 vector = p.toDVector();
		vector.add(0, 0, sphere.getRadius());
//		vector.add(0, 0, sphere.getLengths().get2() / 2);
		sphere.setPosition(vector);
	}
}
