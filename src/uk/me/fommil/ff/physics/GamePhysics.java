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
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DPlane;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeConstants;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.OdeInit;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.PlayerStats;
import uk.me.fommil.ff.Tactics;
import uk.me.fommil.ff.Tactics.BallZone;
import uk.me.fommil.ff.Tactics.PlayerZone;
import uk.me.fommil.ff.physics.Player.Action;
import uk.me.fommil.ff.Team;

/**
 * The model (M) and controller (C) for the game play.
 *
 * @author Samuel Halliday
 */
public class GamePhysics {

	private static final Logger log = Logger.getLogger(GamePhysics.class.getName());

	private final Team a;

	private final Ball ball;

	private final List<Player> as = Lists.newArrayListWithCapacity(11);

	private final Pitch pitch;

	private Player selected;

	private double time;

	private final Random random = new Random();

	private Collection<Action> actions = Collections.emptyList();

//	private final GoalMC goalTop, goalBottom;
	private final List<Goalkeeper> goalkeepers = Lists.newArrayList();

	private final DWorld world;

	private final DSpace space;

	private final DJointGroup joints;

	private final DPlane ground;

	private final NearCallback collision = new NearCallback();

	/**
	 * @param a
	 * @param pitch
	 */
	public GamePhysics(Team a, Pitch pitch) {
		this.a = a;
		this.pitch = pitch;
//		goalTop = new GoalMC(pitch.getGoalNetTop(), 2, Direction.DOWN);
//		goalBottom = new GoalMC(pitch.getGoalNetBottom(), 2, Direction.UP);

		// goalkeepers.add(new GoalkeeperM(1, aPlayers.get(0)));

		OdeInit.dInitODE();

		world = OdeHelper.createWorld();
		world.setGravity(0, 0, -9.81);

		space = OdeHelper.createSimpleSpace();
		joints = OdeHelper.createJointGroup();

		ground = OdeHelper.createPlane(space, 0, 0, 1, 0);

		ball = new Ball(OdeHelper.createBody(world));
		Position centre = pitch.getCentre();
		centre = new Position(centre.x, centre.y, 5);
		ball.setPosition(centre);
		space.add(ball.getGeometry());

		BallZone bz = ball.getZone(pitch);
		List<PlayerStats> aPlayers = a.getPlayers();
		Tactics tactics = a.getCurrentTactics();
		for (int i = 2; i <= 11; i++) {
			Position p = tactics.getZone(bz, i).getCentre(pitch, Pitch.Facing.UP);
			Player pma = new Player(i, aPlayers.get(i - 1), OdeHelper.createBody(world));
			pma.setPosition(p);
			for (DGeom geom : pma.getGeometries()) {
				space.add(geom);
			}
			as.add(pma);
		}
		selected = as.get(9);
	}

	/**
	 * @param actions
	 * @param aftertouches
	 */
	public void setUserActions(Collection<Player.Action> actions, Collection<Ball.Aftertouch> aftertouches) {
		this.actions = Lists.newArrayList(actions);
		if (actions.contains(Player.Action.KICK))
			updateSelected(null);
		else
			selected.setActions(actions);
		ball.setAftertouch(aftertouches);
	}

	public void tick(double dt) {
		time += dt;

		Position bp = ball.getPosition();
		BallZone bz = ball.getZone(pitch);
		Tactics tactics = a.getCurrentTactics();
		for (Player p : as) {
			if (p != selected) {
				Position target;
				if (bp.distance(p.getPosition()) < Math.min(100, bp.distance(selected.getPosition()))) {
					target = bp;
				} else {
					PlayerZone pz = tactics.getZone(bz, p.getShirt());
					target = pz.getCentre(pitch, Pitch.Facing.UP);
				}
				p.autoPilot(target);
			}
		}
		selected.setActions(actions);

		space.collide(null, collision);
		drag(ball.getGeometry(), 0.1); // air drag on ball

		world.step(dt);
		joints.empty();


//		List<PlayerMC> candidate = Lists.newArrayList();
//		for (PlayerMC pm : as) {
//			Bounds pmb = pm.getBounds();
//			if (pm.getPosition().distance(bp) < 100 && pmb.intersect(bp)) {
//				candidate.add(pm);
//			}
//		}
//		// TODO: better resolution of contended owner (e.g. by skill, tackling state)
//		// TODO: physics rebounding off players
//		// TODO: physics restrictions to stop players walking through walls
//		// TODO: getForce for kick/heading/dribbling strength
//		if (!candidate.isEmpty()) {
//			PlayerMC owner = candidate.get(random.nextInt(candidate.size()));
//			updateSelected(owner);
//			Vector3d kick = owner.getVelocity();
//			kick.z += ball.getVelocity().z;
//			switch (owner.getMode()) {
//				case KICK:
//				case HEAD_START:
//				case HEAD_MID:
//				case HEAD_END:
//					kick.scale(2.5);
//					kick.z = 4;
//					ball.setVelocity(kick);
//					break;
//				case RUN:
//				case TACKLE:
//					ball.setVelocity(kick);
//			}
//		}

//		for (PlayerMC pm : as) {
//			pm.tick(dt);
//		}
//		for (GoalkeeperM gk : goalkeepers) {
//			gk.tick(dt);
//		}
//		ball.tick(dt);

//		BoundingBox p = pitch.getPitch();
//		Point3d lower = Utils.getLower(p);
//		Point3d upper = Utils.getUpper(p);
//
//		Point3d bNewP = ball.getPosition();
//		if (bNewP.x < lower.x || bNewP.x > upper.x) {
//			ball.setVelocity(new Vector3d());
//			bNewP.z = 0;
//			ball.setPosition(bNewP);
//			selected.setPosition(bNewP);
//			selected.setThrowIn();
//		}

//		if (bNewP.y >= upper.y || bNewP.y <= lower.y) {
//			Point3d bouncePos = ball.getPosition();
//			Vector3d bounceVel = ball.getVelocity();
//			goalTop.bounce(bouncePos, bounceVel, bp);
//			goalBottom.bounce(bouncePos, bounceVel, bp);
//			if (!bouncePos.equals(ball.getPosition())) {
//				ball.setPosition(bouncePos);
//				ball.setVelocity(bounceVel);
//			}
//
//			if (goalTop.inside(ball.getPosition()))
//				log.fine("GOAL!!!!");
//			else if (goalBottom.inside(ball.getPosition()))
//				log.fine("OWN GOAL!!!!");
//			else
//				log.fine("CORNER/GOAL KICK");
//		}
	}

	private void updateSelected(Player closest) {
		assert selected != null;

		if (closest == null) {
			closest = selected;
			double distance = Double.MAX_VALUE;
			for (Player model : as) {
				switch (model.getState()) {
					case GROUND:
					case INJURED:
					case HEAD_START:
					case HEAD_MID:
					case HEAD_END:
						continue;
				}
				double ds2 = model.getPosition().distance(ball.getPosition());
				if (ds2 < distance) {
					distance = ds2;
					closest = model;
				}
			}
		}
		selected = closest;
		selected.setActions(actions);
	}

	private void drag(DGeom geom, double d) {
		DBody body = geom.getBody();
		DVector3C v = body.getLinearVel();
		v = v.reScale(-d);
		body.addForce(v);
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

	public Iterable<Goalkeeper> getGoalkeepers() {
		return goalkeepers;
	}
	// </editor-fold>

	private class NearCallback implements DNearCallback {

		@Override
		public void call(Object data, DGeom o1, DGeom o2) {
			Preconditions.checkNotNull(o1, "o1");
			Preconditions.checkNotNull(o2, "o2");

			DBody b1 = o1.getBody();
			DBody b2 = o2.getBody();

			final int MAX_CONTACTS = 8;
			DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);
			int numc = OdeHelper.collide(o1, o2, MAX_CONTACTS, contacts.getGeomBuffer());

			for (int i = 0; i < numc; i++) {
				DContact contact = contacts.get(i);
				contact.surface.mode = OdeConstants.dContactBounce;
				if ((o1 instanceof DSphere || o2 instanceof DSphere)) {
					contact.surface.mu = OdeConstants.dInfinity;
				}
				if ((o1 instanceof DBox || o2 instanceof DBox) && (o1 instanceof DSphere || o2 instanceof DSphere)) {
					// ball bouncing off player
					contact.surface.bounce = 0.1;
				} else {
					contact.surface.bounce = 0.5;
					contact.surface.mu = 10;
				}
				contact.surface.bounce_vel = 0.1;

				// ODE doesn't have rolling friction, so we manually apply it here for the ball
				if (o1 instanceof DPlane || o2 instanceof DPlane) {
					DGeom geom = (o1 instanceof DPlane) ? o2 : o1;
					if (geom instanceof DSphere) {
						drag(geom, 2.0);
					}
				}

				DJoint c = OdeHelper.createContactJoint(world, joints, contacts.get(i));
				c.attach(b1, b2);
			}
		}
	};
}
