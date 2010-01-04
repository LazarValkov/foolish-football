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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import uk.me.fommil.ff.PlayerMC.Action;
import uk.me.fommil.ff.Tactics.BallZone;
import uk.me.fommil.ff.Tactics.PlayerZone;

/**
 * The model (M) and controller (C) for the game play.
 *
 * @author Samuel Halliday
 */
public class GameMC {

	private static final Logger log = Logger.getLogger(GameMC.class.getName());

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

	public enum Direction {

		UP, DOWN, LEFT, RIGHT, UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT;

		/**
		 * @param angle
		 * @return
		 */
		public static Direction valueOf(double angle) {
			if (Double.isNaN(angle))
				return null;
			Preconditions.checkArgument(angle <= Math.PI && angle > -Math.PI, angle);

			// TODO: rotate by a few degrees counter clockwise
			if (angle <= - 3 * Math.PI / 4 + Math.PI / 8) {
				return DOWN_LEFT;
			} else if (angle <= -Math.PI / 2 + Math.PI / 8) {
				return LEFT;
			} else if (angle <= -Math.PI / 4 + Math.PI / 8) {
				return UP_LEFT;
			} else if (angle <= 0 + Math.PI / 8) {
				return UP;
			} else if (angle <= Math.PI / 4 + Math.PI / 8) {
				return UP_RIGHT;
			} else if (angle <= Math.PI / 2 + Math.PI / 8) {
				return RIGHT;
			} else if (angle <= 3 * Math.PI / 4 + Math.PI / 8) {
				return DOWN_RIGHT;
			} else {
				return DOWN;
			}
		}
	}

	private final long PERIOD = 50L;

	private final Team a;

	private final BallMC ball = new BallMC();

	private final List<PlayerMC> as = Lists.newArrayListWithCapacity(11);

	private final AtomicLong ticks = new AtomicLong();

	private PlayerMC selectedA;

	private volatile GameV view = null;

	private final TimerTask ticker = new TimerTask() {

		@Override
		public synchronized void run() {
			ticks.incrementAndGet();
			updatePhysics();
			if (view != null)
				view.repaint();
		}
	};

	private final Pitch pitch;

	/**
	 * @param a
	 * @param b
	 * @param pitch
	 */
	public GameMC(Team a, Pitch pitch) {
		this.a = a;
		this.pitch = pitch;
		ball.setPosition(pitch.getCentre());
		BallZone bz = ball.getZone(pitch);
		List<Player> aPlayers = a.getPlayers();
		Tactics tactics = a.getCurrentTactics();
		for (int i = 2; i <= 11; i++) {
			Point3d p = tactics.getZone(bz, i).getCentre(pitch, Pitch.Facing.UP);
			PlayerMC pma = new PlayerMC(i, aPlayers.get(i - 2));
			pma.setPosition(p);
			as.add(pma);
		}
		selectedA = as.get(9);

		new Timer().schedule(ticker, 0L, PERIOD);
	}

	/**
	 * @param team
	 * @param actions
	 */
	public void setUserActions(Team team, Collection<PlayerMC.Action> actions) {
		updateSelected(team, actions);
		selectedA.setActions(actions);

		Set<BallMC.Aftertouch> aftertouches = Sets.newHashSet();
		for (Action action : actions) {
			switch (action) {
				case UP:
					aftertouches.add(BallMC.Aftertouch.UP);
					break;
				case DOWN:
					aftertouches.add(BallMC.Aftertouch.DOWN);
					break;
				case LEFT:
					aftertouches.add(BallMC.Aftertouch.LEFT);
					break;
				case RIGHT:
					aftertouches.add(BallMC.Aftertouch.RIGHT);
					break;
			}
		}
		ball.setAftertouch(aftertouches);
	}

	private void updatePhysics() {
		// autopilot
		BallZone bz = ball.getZone(pitch);
//		log.info(bz.toString());
		// log.info("BALL " + bz);
		Tactics tactics = a.getCurrentTactics();
		for (PlayerMC p : as) {
			if (p != selectedA) {
				PlayerZone pz = tactics.getZone(bz, p.getShirt());
				Point3d target = pz.getCentre(pitch, Pitch.Facing.UP);
				// TODO: "magnetic" behaviour when the ball is nearby
				p.autoPilot(target);
			}
		}

		// sprite collision detection for ball movement and player states
		// detect who has rights to the ball
		List<PlayerMC> candidate = Lists.newArrayList();
		Point3d b = ball.getPosition();
		for (PlayerMC pm : as) {
			Bounds pmb = pm.getBounds();
			if (pm.getPosition().distance(b) < 100 && pmb.intersect(b)) {
				//log.info("POTENTIAL OWNER " + pm);
				candidate.add(pm);
			}
		}
		// TODO: better resolution of contended owner (e.g. by skill, tackling state)
		if (!candidate.isEmpty()) {
			PlayerMC owner = candidate.get(new Random().nextInt(candidate.size()));
			// always give control to the operator
			// TODO: fix delay when handing over control
			selectedA = owner;
			if (owner.isKicking()) {
				// kick the ball
				Vector3d kick = owner.getVelocity();
				kick.scale(3);
				kick.z = 4;
				ball.setVelocity(kick);
			} else {
				// dribble the ball
				Vector3d kick = owner.getVelocity();
				ball.setVelocity(kick);
			}
		}

		for (PlayerMC pm : as) {
			pm.tick(PERIOD / 1000.0);
		}
		ball.tick(PERIOD / 1000.0);

		// log.info(ball.getPosition().toString());
		// detectors for various states of the game
		if (!pitch.getPitch().intersect(ball.getPosition())) {
			log.info("OUT OF BOUNDS " + pitch.getPitch() + " " + ball.getPosition());
			ball.setPosition(pitch.getCentre());
			ball.setVelocity(new Vector3d());
		}
	}

	// get the selected player for the given team
	private void updateSelected(Team team, Collection<PlayerMC.Action> actions) {
		assert team == a;
		assert selectedA != null;

		if (!actions.contains(PlayerMC.Action.KICK))
			return;

		// set the closed player
		PlayerMC closest = selectedA;
		double distance = selectedA.getPosition().distanceSquared(ball.getPosition());
		for (PlayerMC model : as) {
			double ds2 = model.getPosition().distanceSquared(ball.getPosition());
			if (ds2 < distance) {
				distance = ds2;
				closest = model;
			}
		}
		selectedA = closest;
	}

	public void setView(GameV view) {
		this.view = view;
	}

	public BallMC getBall() {
		return ball;
	}

	public Iterable<PlayerMC> getPlayers() {
		return as;
	}

	public PlayerMC getSelected() {
		return selectedA;
	}

	public long getTimestamp() {
		return (ticks.get() * PERIOD);
	}

	public Team getTeamA() {
		return a;
	}
}
