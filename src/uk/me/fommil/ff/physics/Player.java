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
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.Rotation;
import uk.me.fommil.ff.PlayerStats;

/**
 * The model (M) and controller (C) for a {@link Player} during game play.
 *
 * @author Samuel Halliday
 */
public class Player {

	private final Collection<DGeom> geometries = Lists.newArrayList();
//	private final DBox box;
	private final DBody body;
	private static final double height = 2;

	private volatile double direction;

	/**
	 * The actions that a player can perform.
	 */
	public enum Action {

		UP, DOWN, LEFT, RIGHT, KICK, TACKLE, HEAD

	};

	public enum PlayerState {

		RUN, KICK, TACKLE, HEAD_START, HEAD_MID, HEAD_END, GROUND, INJURED,
		THROW,
		// TODO CELEBRATE, PUNISH
	}

	private static final Logger log = Logger.getLogger(Player.class.getName());

	protected static final int AUTO = 10;

	protected final PlayerStats stats;

	protected final int shirt;

	protected volatile PlayerState mode = PlayerState.RUN;

	protected final Random random = new Random();

	Player(int i, PlayerStats stats, DWorld world, DSpace space) {
		Preconditions.checkArgument(i >= 1 && i <= 11, i);
		Preconditions.checkNotNull(stats);
		this.shirt = i;
		this.stats = stats;
		this.body = OdeHelper.createBody(world);
		DBox box1 = OdeHelper.createBox(space, 1, 0.5, height);
		box1.setBody(body);
		geometries.add(box1);

//		DBody leftFoot = OdeHelper.createBody(world);
//		DBox box2 = OdeHelper.createBox(space, 0.5, 1.0, height);
//		box2.setBody(leftFoot);
//		box2.setOffsetPosition(0.5, 0.25, height/2);
//		DFixedJoint j = OdeHelper.createFixedJoint(world);
//		j.attach(body, leftFoot);
//		j.setFixed();
//		geometries.add(box2);

//		DBox box2 = OdeHelper.createBox(0.5, 1, height);
//		box2.setBody(body);
//		box2.setOffsetPosition(0.75, 0.5, height / 2);
//		geometries.add(box2);

		DMass mass = OdeHelper.createMass();
		mass.setBoxTotal(80, 1, 0.5, height); // ?? code dupe
		body.setMass(mass);
	}

	/**
	 * Controller.
	 * 
	 * @param actions
	 */
	public void setActions(Collection<Action> actions) {
		Preconditions.checkNotNull(actions);
		switch (mode) {
			case RUN:
			case THROW:
				break;
			default:
				return;
		}
		// stabilise
		setPosition(body.getPosition());

		DVector3 vector = actionsToVector(actions);
		vector.scale(5);
		body.setLinearVel(vector);
		direction = computeDirection(vector);

		DMatrix3 rotation = new DMatrix3();
		Rotation.dRFromAxisAndAngle(rotation, 0, 0, 1, direction);
		for (DGeom geom : geometries) {
			geom.setRotation(rotation);
		}
	}

	private DVector3 actionsToVector(Collection<Action> actions) {
		DVector3 impulse = new DVector3();
		for (Action action : actions) {
			switch (action) {
				case UP:
					impulse.sub(0, 1, 0);
					break;
				case DOWN:
					impulse.add(0, 1, 0);
					break;
				case LEFT:
					impulse.sub(1, 0, 0);
					break;
				case RIGHT:
					impulse.add(1, 0, 0);
					break;
			}
		}
		if (impulse.lengthSquared() > 0) {
			impulse.normalize();
		}
		return impulse;
	}

	private double computeDirection(DVector3 vector) {
		if (vector.length() == 0)
			return direction;
		return dePhase(Math.atan2(vector.get1(), vector.get0()) + Math.PI / 2);
	}

	private double dePhase(double d) {
		if (d > Math.PI)
			return dePhase(d - 2 * Math.PI);
		if (d <= -Math.PI)
			return dePhase(d + 2 * Math.PI);
		return d;
	}

	/**
	 * Controller. Ignore user input and go to the zone indicated.
	 *
	 * @param attractor
	 */
	public void autoPilot(Position attractor) {
		Preconditions.checkNotNull(attractor);
		List<Player.Action> auto = Lists.newArrayList();
		double dx = body.getPosition().get0() - attractor.x;
		if (dx < -AUTO) {
			auto.add(Player.Action.RIGHT);
		} else if (dx > AUTO) {
			auto.add(Player.Action.LEFT);
		}
		double dy = body.getPosition().get1() - attractor.y;
		if (dy < -AUTO) {
			auto.add(Player.Action.DOWN);
		} else if (dy > AUTO) {
			auto.add(Player.Action.UP);
		}
		setActions(auto);
	}

	/**
	 * @return the angle relate to NORTH {@code (- PI, + PI]}.
	 */
	public double getDirection() {
		return direction;
	}

	public int getShirt() {
		return shirt;
	}

	public PlayerState getState() {
		return PlayerState.RUN; // TODO: calculate state
	}

	public Velocity getVelocity() {
		return new Velocity(body.getLinearVel());
	}

	public Position getPosition() {
		return new Position(body.getPosition());
	}

	public void setPosition(Position p) {
		setPosition(p.toDVector());
	}

	void setPosition(DVector3C p) {
		DVector3 position = new DVector3(p);
		body.setAngularVel(0, 0, 0);
		position.set(2, height / 2);
		body.setPosition(position);
	}

	Collection<DGeom> getGeometries() {
//		Collection<DGeom> geometries = Lists.newArrayList();
//		geometries.add(box);
//		return geometries;

		 return Collections.unmodifiableCollection(geometries);
	}
}
