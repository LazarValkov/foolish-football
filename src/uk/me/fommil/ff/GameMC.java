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

	private final GoalMC goalTop, goalBottom;

	private final GoalkeeperM goalkeeper;

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
			PlayerMC pma = new PlayerMC(i, aPlayers.get(i - 1));
			pma.setPosition(p);
			as.add(pma);
		}
		selected = as.get(9);
		goalTop = new GoalMC(pitch.getGoalNetTop(), 2, Direction.DOWN);
		goalBottom = new GoalMC(pitch.getGoalNetBottom(), 2, Direction.UP);
		goalkeeper = new GoalkeeperM(1, aPlayers.get(0));

		BoundingBox net = pitch.getGoalNetBottom();
		Point3d netLower = Utils.getLower(net);
		goalkeeper.setPosition(new Point3d(netLower.x + 25, netLower.y, 0));
	}

	/**
	 * @param team
	 * @param actions
	 * @param aftertouches
	 */
	public void setUserActions(Team team, Collection<PlayerMC.Action> actions, Collection<BallMC.Aftertouch> aftertouches) {
		this.actions = Lists.newArrayList(actions);
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
		for (PlayerMC pm : as) {
			Bounds pmb = pm.getBounds();
			if (pm.getPosition().distance(bp) < 100 && pmb.intersect(bp)) {
				candidate.add(pm);
			}
		}
		// TODO: better resolution of contended owner (e.g. by skill, tackling state)
		// TODO: physics rebounding off players
		// TODO: physics restrictions to stop players walking through walls
		// TODO: getForce for kick/heading/dribbling strength
		if (!candidate.isEmpty()) {
			PlayerMC owner = candidate.get(random.nextInt(candidate.size()));
			updateSelected(owner);
			Vector3d kick = owner.getVelocity();
			kick.z += ball.getVelocity().z;
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
		goalkeeper.tick(dt);
		ball.tick(dt);

		BoundingBox p = pitch.getPitch();
		Point3d lower = Utils.getLower(p);
		Point3d upper = Utils.getUpper(p);

		Point3d bNewP = ball.getPosition();
		if (bNewP.x < lower.x || bNewP.x > upper.x) {
			ball.setVelocity(new Vector3d());
			bNewP.z = 0;
			ball.setPosition(bNewP);
			selected.setPosition(bNewP);
			selected.setThrowIn();
		}

		if (bNewP.y >= upper.y || bNewP.y <= lower.y) {
			Point3d bouncePos = ball.getPosition();
			Vector3d bounceVel = ball.getVelocity();
			goalTop.bounce(bouncePos, bounceVel, bp);
			goalBottom.bounce(bouncePos, bounceVel, bp);
			if (!bouncePos.equals(ball.getPosition())) {
				ball.setPosition(bouncePos);
				ball.setVelocity(bounceVel);
			}

			if (goalTop.inside(ball.getPosition()))
				log.fine("GOAL!!!!");
			else if (goalBottom.inside(ball.getPosition()))
				log.fine("OWN GOAL!!!!");
			else
				log.fine("CORNER/GOAL KICK");
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

	public Iterable<GoalkeeperM> getGoalkeepers() {
		return Collections.singleton(goalkeeper);
	}
}
