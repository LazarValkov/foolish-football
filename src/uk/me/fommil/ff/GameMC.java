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

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javax.media.j3d.BoundingBox;
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

	private final Team a;

	private final BallMC ball = new BallMC();

	private final List<PlayerMC> as = Lists.newArrayListWithCapacity(11);

	private final Pitch pitch;

	private PlayerMC selected;

	private double time;

	private final Random random = new Random();

	private Collection<Action> actions = Collections.emptyList();

	/**
	 * @param a
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
		selected = as.get(9);
	}

	/**
	 * @param team
	 * @param actions
	 * @param aftertouches
	 */
	public void setUserActions(Team team, Collection<PlayerMC.Action> actions, Collection<BallMC.Aftertouch> aftertouches) {
		this.actions = actions;
		if (actions.contains(PlayerMC.Action.KICK))
			updateSelected(null);
		else
			selected.setActions(actions);
		ball.setAftertouch(aftertouches);
	}

	public void tick(double dt) {
		time += dt;
		Point3d bp = ball.getPosition();
		BallZone bz = ball.getZone(pitch);
		Tactics tactics = a.getCurrentTactics();
		for (PlayerMC p : as) {
			if (p != selected) {
				Point3d target;
				if (bp.distance(p.getPosition()) < Math.min(100, bp.distance(selected.getPosition()))) {
					target = bp;
				} else {
					PlayerZone pz = tactics.getZone(bz, p.getShirt());
					target = pz.getCentre(pitch, Pitch.Facing.UP);
				}
				p.autoPilot(target);
			}
		}

		List<PlayerMC> candidate = Lists.newArrayList();
		Point3d b = ball.getPosition();
		for (PlayerMC pm : as) {
			Bounds pmb = pm.getBounds();
			if (pm.getPosition().distance(b) < 100 && pmb.intersect(b)) {
				candidate.add(pm);
			}
		}
		// TODO: better resolution of contended owner (e.g. by skill, tackling state)
		// TODO: physics rebounding off players
		if (!candidate.isEmpty()) {
			PlayerMC owner = candidate.get(random.nextInt(candidate.size()));
			updateSelected(owner);
			Vector3d kick = owner.getVelocity(); // FIXME: this should be getForce or similar
			switch (owner.getMode()) {
				case KICK:
				case HEAD_START:
				case HEAD_MID:
				case HEAD_END:
					kick.scale(2.5);
					kick.z = 4;
					ball.setVelocity(kick);
					break;
				case RUN:
				case TACKLE:
					ball.setVelocity(kick);
			}
		}

		for (PlayerMC pm : as) {
			pm.tick(dt);
		}
		ball.tick(dt);

		BoundingBox p = pitch.getPitch();
		Point3d lower = Utils.getLower(p);
		Point3d upper = Utils.getUpper(p);

		if (bp.x < lower.x || bp.x > upper.x) {
			ball.setVelocity(new Vector3d());
			Point3d position = ball.getPosition();
			position.z = 0;
			ball.setPosition(position);
			selected.setPosition(ball.getPosition());
			selected.setThrowIn();
		}

		if (bp.y <= upper.y || bp.y >= lower.y) {
			BoundingBox outer = pitch.getGoalNetTopOutside();
			BoundingBox inner = pitch.getGoalNetTopInside();
			if (outer.intersect(bp) && !inner.intersect(bp)) {
				Vector3d v = ball.getVelocity();
				if (v.length() > 0) {
					Point3d entry = Utils.entryPoint(outer, bp, v, 0.01);
					ball.setPosition(entry); // ?? losing energy
					Vector3d rebound = Utils.rebound(outer, entry, v);
					rebound.scale(0.5);
					ball.setVelocity(rebound);
				}
			}
		}
	}

	private void updateSelected(PlayerMC closest) {
		assert selected != null;

		if (closest == null) {
			closest = selected;
			double distance = Double.MAX_VALUE;
			for (PlayerMC model : as) {
				switch (model.getMode()) {
					case GROUND:
					case INJURED:
					case HEAD_START:
					case HEAD_MID:
					case HEAD_END:
						continue;
				}
				double ds2 = model.getPosition().distanceSquared(ball.getPosition());
				if (ds2 < distance) {
					distance = ds2;
					closest = model;
				}
			}
		}
		selected = closest;
		selected.setActions(actions);
	}

	public BallMC getBall() {
		return ball;
	}

	public Iterable<PlayerMC> getPlayers() {
		return as;
	}

	public PlayerMC getSelected() {
		return selected;
	}

	public double getTimestamp() {
		return time;
	}

	public Team getTeamA() {
		return a;
	}
}
