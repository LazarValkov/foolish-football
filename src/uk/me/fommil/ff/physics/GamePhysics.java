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
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContact.dSurfaceParameters;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DPlane;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeConstants;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.OdeInit;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.PlayerStats;
import uk.me.fommil.ff.Tactics;
import uk.me.fommil.ff.Tactics.BallZone;
import uk.me.fommil.ff.Tactics.PlayerZone;
import uk.me.fommil.ff.physics.Ball.Aftertouch;
import uk.me.fommil.ff.physics.Player.Action;
import uk.me.fommil.ff.Team;

/**
 * The model (M) and controller (C) for game play.
 * The coordinate system is a left-handed system with X = East, Y = North, Z = Sky.
 *
 * @author Samuel Halliday
 */
public class GamePhysics {

	private static final Logger log = Logger.getLogger(GamePhysics.class.getName());

	private static final int MAX_CONTACTS = 8; // for collision detection

	private final Team a;

	private final Ball ball;

	private final List<Player> as = Lists.newArrayListWithCapacity(11);

	private final Pitch pitch;

	private Player selected;

	private double time;

	private final DWorld world;

	private final DSpace space;

	private final DJointGroup joints;

	private final NearCallback collision = new NearCallback();

	private Collection<Action> actions = Collections.emptyList();

	private Collection<Aftertouch> aftertouches = Collections.emptyList();

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
		centre = new Position(centre.x, centre.y, 5);
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
	}

	/**
	 * Controller.
	 *
	 * @param actions
	 * @param aftertouches
	 */
	public void setUserActions(Collection<Player.Action> actions, Collection<Ball.Aftertouch> aftertouches) {
		Preconditions.checkNotNull(actions);
		Preconditions.checkNotNull(aftertouches);
		this.actions = Sets.newHashSet(actions);
		this.aftertouches = Sets.newHashSet(aftertouches);
	}

	public void tick(double dt) {
		time += dt;

		if (actions.contains(Player.Action.KICK))
			updateSelected();

		// TODO: be consistent with position/velocity implementations
		Position bp = ball.getPosition();
		BallZone bz = ball.getZone(pitch);
		Tactics tactics = a.getCurrentTactics();
		for (Player p : as) {
			if (p != selected) {
				Position target;
				if (bp.distance(p.getPosition()) < Math.min(10, bp.distance(selected.getPosition()))) {
					target = bp;
				} else {
					PlayerZone pz = tactics.getZone(bz, p.getShirt());
					target = pz.getCentre(pitch, Pitch.Facing.NORTH);
				}
				p.autoPilot(target);
			}
		}
		selected.setActions(actions);
		ball.setAftertouch(aftertouches);

		ball.setFriction(0);
		space.collide(null, collision);
		ball.applyFriction();
		ball.applyAftertouch();

		world.step(dt);
		joints.empty();

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

	private class NearCallback implements DNearCallback {

		@Override
		public void call(Object data, DGeom o1, DGeom o2) {
			Preconditions.checkNotNull(o1, "o1");
			Preconditions.checkNotNull(o2, "o2");

			DBody b1 = o1.getBody();
			DBody b2 = o2.getBody();

			Object obj1 = b1 != null ? b1.getData() : null;
			Object obj2 = b2 != null ? b2.getData() : null;

			boolean ballInvolved = obj1 instanceof Ball || obj2 instanceof Ball;
			boolean playerInvolved = obj1 instanceof Player || obj2 instanceof Player;
			boolean selectedInvolved = selected == obj1 || selected == obj2;
			boolean groundInvolved = o1 instanceof DPlane || o2 instanceof DPlane;

			DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);
			int numc = OdeHelper.collide(o1, o2, MAX_CONTACTS, contacts.getGeomBuffer());

			for (int i = 0; i < numc; i++) {
				DContact contact = contacts.get(i);
				dSurfaceParameters surface = contact.surface;
				surface.mode = OdeConstants.dContactBounce;// | OdeConstants.dContactSoftERP;
				surface.bounce_vel = 0.1;

				if (ballInvolved) {
					ball.setFriction(0.5);
					surface.mu = OdeConstants.dInfinity; // ball never slips
					if (selectedInvolved) {
						selected.control(ball);
						if (selected.kick(ball))
							break;
						// and do not create a joint
					}
					if (playerInvolved) {
						surface.bounce = 0.1;
					} else if (groundInvolved) {
						surface.bounce = 0.5;
					}
				}

				DJoint c = OdeHelper.createContactJoint(world, joints, contact);
				c.attach(b1, b2);
			}
		}
	};

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
		return Collections.emptyList(); // TODO: goalkeepers
	}

	Collection<DGeom> getGeoms() {
		Collection<DGeom> geoms = Lists.newArrayList();
		int num = space.getNumGeoms();
		for (int i = 0; i < num; i++) {
			geoms.add(space.getGeom(i));
		}
		return geoms;
	}
	// </editor-fold>
}
