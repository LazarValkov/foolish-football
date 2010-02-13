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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DContact.DSurfaceParameters;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeConstants;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.OdeInit;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.PlayerStats;
import uk.me.fommil.ff.Tactics;
import uk.me.fommil.ff.Tactics.BallZone;
import uk.me.fommil.ff.Tactics.PlayerZone;
import uk.me.fommil.ff.Team;
import uk.me.fommil.ff.physics.CollisionCallback.CollisionHandler;
import uk.me.fommil.ff.physics.Player.PlayerState;

/**
 * The model (M) and controller (C) for game play.
 * The coordinate system is a left-handed system with X = East, Y = North, Z = Sky.
 *
 * @author Samuel Halliday
 */
public class GamePhysics {

	private static final Logger log = Logger.getLogger(GamePhysics.class.getName());

	private static final double MIN_SPEED = 0.01;

	/**
	 * The actions that a user can perform.
	 */
	public enum Action {

		UP, DOWN, LEFT, RIGHT, KICK, TACKLE, HEAD, CHANGE;

		// returns a normalised vector that represents the UP/DOWN/LEFT/RIGHT user input
		static DVector3 asVector(Collection<Action> actions) {
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
			}
			return move;
		}
	};

	/** The aftertouch that a ball may exhibit. Aftertouch depends on the direction of motion. */
	public enum Aftertouch {

		UP, DOWN, LEFT, RIGHT;

		// returns a normalised vector that represents the aftertouch user input
		static DVector3 asVector(Collection<Aftertouch> touches) {
			Preconditions.checkNotNull(touches);
			DVector3 aftertouch = new DVector3();
			for (Aftertouch touch : touches) {
				assert touch != null;
				switch (touch) {
					case UP:
						aftertouch.add(0, 1, 0);
						break;
					case DOWN:
						aftertouch.sub(0, 1, 0);
						break;
					case LEFT:
						aftertouch.sub(1, 0, 0);
						break;
					case RIGHT:
						aftertouch.add(1, 0, 0);
						break;
				}
			}
			if (aftertouch.length() > 0) {
				aftertouch.normalize();
			}
			return aftertouch;
		}

		static Direction toDirection(Collection<Aftertouch> touches) {
			DVector3 vector = asVector(touches);
			double angle = toAngle(vector, 0);
			return Direction.valueOf(angle);
		}
	}

	static double toAngle(DVector3 vector, double fallback) {
		if (vector.length() == 0)
			return fallback;
		return dePhase(Math.PI / 2 - Math.atan2(vector.get1(), vector.get0()));
	}

	private static double dePhase(double d) {
		if (d > Math.PI)
			return dePhase(d - 2 * Math.PI);
		if (d <= -Math.PI)
			return dePhase(d + 2 * Math.PI);
		return d;
	}

	private final Team a;

	private final Ball ball;

	private final List<Player> as = Lists.newArrayListWithCapacity(11);

	private final Pitch pitch;

	private Player selected;

	private double time;

	private final DWorld world;

	private final DSpace space;

	private final DJointGroup joints;

	private final CollisionCallback collision;

	private final CollisionHandler collisionHandler = new CollisionHandler() {

		@Override
		public void collide(Ball ball, Player player, DSurfaceParameters surface) {
			enableSoftBounce(surface);
			ball.setDamping(0.1); // ?? can be overridden
			surface.bounce = player.getBounce();
			if (player.equals(getSelected())) {
				selected.control(ball);
			}
		}

		@Override
		public void collide(Player player1, Player player2, DSurfaceParameters surface) {
			enableSoftBounce(surface);
			surface.bounce = 0.75; // affects tackling
		}

		@Override
		public void collide(Ball ball, DSurfaceParameters surface) {
			enableSoftBounce(surface);
			surface.bounce = 0.5;
			ball.setDamping(0.1); // ?? can be overridden
			ball.setAftertouchEnabled(false);
		}

		@Override
		public void collide(Player player, DSurfaceParameters surface) {
			enableSoftBounce(surface);
			if (player.getTilt() > Math.PI / 8) // ?? exposing more than is needed?
				surface.mu = 1000;
		}

		private void enableSoftBounce(DSurfaceParameters surface) {
			surface.mode = OdeConstants.dContactBounce | OdeConstants.dContactSoftERP;
			surface.bounce_vel = 0.1;
		}
	};

	private volatile Collection<Action> actions = Collections.emptyList();

	private volatile Collection<Aftertouch> aftertouches = Collections.emptyList();

	private final Map<Player, Double> grounded = Maps.newHashMap();

	/**
	 * @param a
	 * @param pitch
	 */
	public GamePhysics(Team a, Pitch pitch) {
		this.a = a;
		this.pitch = pitch;

		OdeInit.dInitODE();

		world = OdeHelper.createWorld();
		world.setGravity(0, 0, -9.81);

		space = OdeHelper.createSimpleSpace();
		joints = OdeHelper.createJointGroup();

		OdeHelper.createPlane(space, 0, 0, 1, 0);

		ball = new Ball(world, space);
		Position centre = pitch.getCentre();
		ball.setPosition(centre);

		BallZone bz = ball.getZone(pitch);
		List<PlayerStats> aPlayers = a.getPlayers();
		Tactics tactics = a.getCurrentTactics();
		for (int i = 2; i <= 11; i++) {
			Position p = tactics.getZone(bz, i).getCentre(pitch, Pitch.Facing.NORTH);
			Player pma = new Player(i, aPlayers.get(i - 1), world, space);
			pma.setPosition(p);
			as.add(pma);
		}
		selected = as.get(9);

		collision = new CollisionCallback(world, joints, collisionHandler);
	}

	/**
	 * Controller.
	 *
	 * @param actions
	 * @param aftertouches
	 */
	public void setUserActions(Collection<Action> actions, Collection<Aftertouch> aftertouches) {
		Preconditions.checkNotNull(actions);
		Preconditions.checkNotNull(aftertouches);
		this.actions = Sets.newHashSet(actions);
		this.aftertouches = Sets.newHashSet(aftertouches);
	}

	public void tick(double dt) {
		time += dt;

		if (actions.contains(Action.CHANGE))
			updateSelected();

		Position bp = ball.getPosition();
		BallZone bz = ball.getZone(pitch);
		Tactics tactics = a.getCurrentTactics();
		for (Player p : as) {
			transition(p);
			if (p == selected)
				continue;
			Position target = bp;
			double near = Math.min(10, bp.distance(selected.getPosition()));
			if (bp.distance(p.getPosition()) > near) {
				PlayerZone pz = tactics.getZone(bz, p.getShirt());
				target = pz.getCentre(pitch, Pitch.Facing.NORTH);
			}
			p.autoPilot(target);
		}

		selected.setActions(actions);
		ball.setAftertouch(aftertouches);
		ball.setDamping(0);
		space.collide(null, collision);

		world.step(dt);
		if (ball.getVelocity().speed() < MIN_SPEED)
			ball.setVelocity(new DVector3()); // stops small movements
		switch (selected.getState()) {
			case KICK:
				selected.kick(ball);
				break;
			case THROWING:
				selected.throwIn(ball);
		}
		joints.empty();
	}

	private void updateSelected() {
		assert selected != null;
		Player closest = selected;
		double distance = Double.MAX_VALUE;
		for (Player model : as) {
			switch (model.getState()) {
				case GROUND:
				case INJURED:
				case HEAD_START:
				case HEAD_MID:
				case HEAD_END:
				case TACKLE:
					continue;
			}
			double ds2 = model.getPosition().distance(ball.getPosition());
			if (ds2 < distance) {
				distance = ds2;
				closest = model;
			}
		}
		selected = closest;
	}

	@SuppressWarnings("fallthrough")
	private void transition(Player p) {
		// TODO: should be in the Player class, not sure how
		switch (p.getState()) {
			case TACKLE:
				if (p.getVelocity().speed() > MIN_SPEED)
					break;
			case GROUND:
				if (!grounded.containsKey(p)) {
					grounded.put(p, time);
				} else if ((time - grounded.get(p)) > 2) {
					p.setState(PlayerState.RUN);
					grounded.remove(p);
				}
		}
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public Ball getBall() {
		return ball;
	}

	public Iterable<Player> getPlayers() {
		return as;
	}

	public Player getSelected() {
		return selected;
	}

	public double getTimestamp() {
		return time;
	}

	public Team getTeamA() {
		return a;
	}

	public Pitch getPitch() {
		return pitch;
	}

	// </editor-fold>
	public Iterable<Goalkeeper> getGoalkeepers() {
		return Collections.emptyList(); // TODO: goalkeepers
	}

	Collection<DGeom> getGeoms() {
		Collection<DGeom> geoms = Lists.newArrayList();
		int num = space.getNumGeoms();
		for (int i = 0; i
				< num; i++) {
			geoms.add(space.getGeom(i));
		}
		return geoms;
	}
}
