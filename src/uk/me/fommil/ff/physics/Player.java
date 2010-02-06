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
import java.util.logging.Logger;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.Rotation;
import uk.me.fommil.ff.PlayerStats;
import uk.me.fommil.ff.physics.GamePhysics.Action;

/**
 * The model (M) and controller (C) for a {@link Player} during game play.
 *
 * @author Samuel Halliday
 */
public class Player {

	private static final double HEIGHT = 2;

	private static final double SPEED = 6.5; // about 15 MPH

	private static final double MASS = 60;

	private static final Logger log = Logger.getLogger(Player.class.getName());

	private static final double AUTOPILOT_TOLERANCE = 1;

	public enum PlayerState {

		RUN, KICK, TACKLE, HEAD_START, HEAD_MID, HEAD_END, GROUND, INJURED,
		THROW,
		// TODO CELEBRATE, PUNISH
	}

	protected final PlayerStats stats;

	protected final int shirt;

	private final DBox box;

	private final DBody body;

	private volatile double direction;

	private volatile Collection<Action> actions = Collections.emptySet();

	Player(int i, PlayerStats stats, DWorld world, DSpace space) {
		Preconditions.checkArgument(i >= 1 && i <= 11, i);
		Preconditions.checkNotNull(stats);
		this.shirt = i;
		this.stats = stats;
		this.body = OdeHelper.createBody(world);
		box = OdeHelper.createBox(space, 1, 0.5, HEIGHT);
		box.setBody(body);

		DMass mass = OdeHelper.createMass();
		mass.setBoxTotal(MASS, 1, 0.5, HEIGHT); // ?? code dupe
		body.setMass(mass);
		body.setData(this);
		body.setAngularDamping(1.0);
	}

	void control(Ball ball) {
		Preconditions.checkNotNull(ball);
		// TODO: come up with a solution that avoids the oscillation
		DVector3 control = getPosition().toDVector().sub(ball.getPosition().toDVector());
		control.set(2, 0);
		control.scale(25);
		ball.addForce(control);
	}

	void kick(Ball ball) {
		assert actions.contains(Action.KICK);
		if (getPosition().distance(ball.getPosition()) > 1)
			return;
		log.info("KICK");

		DVector3 kick = new DVector3(body.getLinearVel());
		kick.safeNormalize();
		kick.scale(20);
		kick.set(2, 2);
		ball.setVelocity(kick);
	}

	/**
	 * Controller, must be called for each time step.
	 * 
	 * @param actions
	 */
	public void setActions(Collection<Action> actions) {
		Preconditions.checkNotNull(actions);
		this.actions = actions;
		switch (getState()) {
			case RUN:
			case THROW:
				break;
			default:
				return;
		}
		DVector3 move = moveActionsToDirection(actions);
		direction = computeDirection(move);

		move.add(2, body.getLinearVel().get(2));
		if (actions.contains(Action.HEAD)) {
			log.info("HEAD");
			move.add(0, 0, 3);
		}

		body.setLinearVel(move);

		DMatrix3 rotation = new DMatrix3();
		Rotation.dRFromAxisAndAngle(rotation, 0, 0, -1, direction);
		box.setRotation(rotation);
	}

	private DVector3 moveActionsToDirection(Collection<Action> actions) {
		DVector3 move = new DVector3();
		for (Action action : actions) {
			switch (action) {
				case UP:
					move.add(0, 1, 0);
					break;
				case DOWN:
					move.sub(0, 1, 0);
					break;
				case LEFT:
					move.sub(1, 0, 0);
					break;
				case RIGHT:
					move.add(1, 0, 0);
					break;
			}
		}
		if (move.length() > 0) {
			move.normalize();
			move.scale(SPEED);
		}
		return move;
	}

	private double computeDirection(DVector3 vector) {
		if (vector.length() == 0)
			return direction;
		return dePhase(Math.PI / 2 - Math.atan2(vector.get1(), vector.get0()));
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
		List<Action> auto = Lists.newArrayList();
		double dx = body.getPosition().get0() - attractor.x;
		if (dx < -AUTOPILOT_TOLERANCE) {
			auto.add(Action.RIGHT);
		} else if (dx > AUTOPILOT_TOLERANCE) {
			auto.add(Action.LEFT);
		}
		double dy = body.getPosition().get1() - attractor.y;
		if (dy < -AUTOPILOT_TOLERANCE) {
			auto.add(Action.UP);
		} else if (dy > AUTOPILOT_TOLERANCE) {
			auto.add(Action.DOWN);
		}
		setActions(auto);
	}

	/**
	 * @return the angle relative to NORTH {@code (- PI, + PI]}.
	 */
	public double getDirection() {
		return direction;
	}

	public int getShirt() {
		return shirt;
	}

	public PlayerState getState() {
		DVector3C position = body.getPosition();
		DVector3C velocity = body.getLinearVel();
		DVector3C angular = body.getAngularVel();
		double z = position.get2() - HEIGHT / 2 + 0.01;
		double vz = velocity.get2();

		if (vz > 0) {
			if (z < 0.2)
				return PlayerState.HEAD_START;
			if (z < 0.4)
				return PlayerState.HEAD_MID;
			return PlayerState.HEAD_END;
		}
		if (z > 0.1)
			return PlayerState.HEAD_END;
		if (actions.contains(Action.KICK))
			return PlayerState.KICK;
		if (z < 0)
			return PlayerState.RUN; // ?? should be GROUND but numerical errors
		return PlayerState.RUN;
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
		position.set(2, HEIGHT / 2);
		body.setPosition(position);
	}
}
