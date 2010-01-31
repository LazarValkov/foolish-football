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
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
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

	private static final double RADIUS = 0.7 / (2 * Math.PI);

	private final DSphere sphere;

	private volatile Collection<Aftertouch> aftertouches = Collections.emptyList();

	/** The aftertouch that a ball may exhibit. Aftertouch depends on the direction of motion. */
	public enum Aftertouch {

		UP, DOWN, LEFT, RIGHT

	}

	private static final Logger log = Logger.getLogger(Ball.class.getName());

	Ball(DWorld world, DSpace space) {
		Preconditions.checkNotNull(world);
		Preconditions.checkNotNull(space);

		DVector3 gravity = new DVector3();
		world.getGravity(gravity);
		g = gravity.length();

		DBody body = OdeHelper.createBody(world);
		sphere = OdeHelper.createSphere(RADIUS);
		sphere.setBody(body);
		DMass mass = OdeHelper.createMass();
		mass.setSphereTotal(MASS_KG, RADIUS);
		body.setMass(mass);
		space.add(sphere);
		body.setData(this);
	}

	/**
	 * Controller.
	 *
	 * @param aftertouches
	 */
	public void setAftertouch(Collection<Aftertouch> aftertouches) {
		Preconditions.checkNotNull(aftertouches);
		this.aftertouches = aftertouches;
	}

	void applyAftertouch() {
		Collection<Aftertouch> touches = Lists.newArrayList(aftertouches); // concurrency
		if (touches.isEmpty())
			return;

//		if (touches.contains(Aftertouch.RIGHT))
//			addForce(new DVector3(10, 0, 0)); // ?? testing

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

	/**
	 * @param pitch
	 * @return
	 */
	public BallZone getZone(Pitch pitch) {
		Preconditions.checkNotNull(pitch);
		return new BallZone(new Position(sphere.getPosition()), pitch);
	}

	public Velocity getVelocity() {
		return new Velocity(sphere.getBody().getLinearVel());
	}

	public Position getPosition() {
		return new Position(sphere.getPosition());
	}

	/**
	 * Places the ball, at rest, at the given position.
	 *
	 * @param p
	 */
	public void setPosition(Position p) {
		DVector3 vector = p.toDVector();
		vector.add(0, 0, sphere.getRadius());
		sphere.setPosition(vector);
		setVelocity(new DVector3());
	}

	void setVelocity(DVector3 v) {
		sphere.getBody().setLinearVel(v);
	}

	void addForce(DVector3C force) {
		assert force != null;
		DBody body = sphere.getBody();
		body.addForce(force);
	}

	// <editor-fold defaultstate="collapsed" desc="HACK TO DEAL WITH FRICTION">
	@Deprecated // HACK to workaround ODE lack of friction on spheres
	private final double g;

	@Deprecated // HACK to workaround ODE lack of friction on spheres
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
	// </editor-fold>
}
