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

import static java.lang.Math.round;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.media.j3d.BoundingPolytope;
import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * The model (M) and controller (C) for a {@link Player} during game play.
 *
 * @author Samuel Halliday
 */
public class PlayerMC {

	// this is mutable, so be careful not to edit it
	private static final Vector3d NORTH = new Vector3d(0, -1, 0);
	private static final int AUTO = 10;

	// View
	@Deprecated
	public Point getStep() {
		return new Point((int) round(v.x), (int) round(v.y));
	}

	@Deprecated
	public Shape getViewBounds() {
		return new Rectangle.Double(s.x - 4, s.y - 4, 9, 9);
	}

	/**
	 * @return the angle relate to NORTH {@code (-PI, + PI]}.
	 */
	public double getAngle() {
		if (facing.x < 0)
			return -facing.angle(NORTH);
		else
			return facing.angle(NORTH);
	}

	/**
	 * The actions that a player can perform.
	 */
	public enum Action {

		UP, DOWN, LEFT, RIGHT, KICK, TACKLE, HEAD
	};
	private final Player player;
	private final int shirt;
	private Collection<Action> actions;
	private boolean kicking, tackling, heading;
	private Point3d s = new Point3d();
	private Vector3d v = new Vector3d();
	private Vector3d facing = new Vector3d(0, -1, 0);

	/**
	 * @param i
	 * @param player
	 */
	public PlayerMC(int i, Player player) {
		Preconditions.checkArgument(i > 1 && i <= 11, i);
		Preconditions.checkNotNull(player);
		this.shirt = i;
		this.player = player;
	}

	/**
	 * Return the volume in which this player can control the ball.
	 *
	 * @return
	 */
	public BoundingPolytope getBounds() {
		BoundingPolytope b = new BoundingPolytope();

		// TODO: heading and tackling bounds (should follow same pattern)

		Transform3D affine = new Transform3D();
		Vector3d t = new Vector3d(facing);
		// centre of bounding box is biased in front of the player
		t.scale(1);
		t.add(s);
		affine.setTranslation(t);
		// defines the scale of the bounding box in x, y, z
		// TODO: control could determine the x scale
		affine.setScale(new Vector3d(8, 5, 2));
		// rotate
		affine.setRotation(new AxisAngle4d(0, 0, 1, getAngle()));

		b.transform(affine);
		return b;
	}
	private static final Logger log = Logger.getLogger(PlayerMC.class.getName());

	/**
	 * @param t with units of seconds
	 */
	public void tick(double t) {
		Vector3d dv = (Vector3d) v.clone();
		dv.scale(t);
		s.add(dv);
	}

	/**
	 * Controller.
	 * 
	 * @param actions
	 */
	public void setActions(Collection<Action> actions) {
		if (kicking || tackling || heading)
			return;
		double x = 0;
		double y = 0;
		for (Action action : actions) {
			switch (action) {
				case UP:
					y -= 50;
					break;
				case DOWN:
					y += 50;
					break;
				case LEFT:
					x -= 50;
					break;
				case RIGHT:
					x += 50;
					break;
				case KICK:
					kick();
					break;
				case TACKLE:
					tackle();
					break;
				case HEAD:
					head();
					break;
			}
		}
		v = new Vector3d(x, y, 0);
		if (v.lengthSquared() > 0) {
			facing.normalize(v);
		}
	}

	/**
	 * Controller. Clear the action list.
	 */
	public void clearActions() {
		v = new Vector3d();
	}

	/**
	 * Controller. Ignore user input and go to the zone indicated.
	 *
	 * @param attractor
	 */
	public void autoPilot(Point3d attractor) {
		Preconditions.checkNotNull(attractor);
		List<PlayerMC.Action> auto = Lists.newArrayList();
		double dx = s.x - attractor.x;
		if (dx < -AUTO) {
			auto.add(PlayerMC.Action.RIGHT);
		} else if (dx > AUTO) {
			auto.add(PlayerMC.Action.LEFT);
		}
		double dy = s.y - attractor.y;
		if (dy < -AUTO) {
			auto.add(PlayerMC.Action.DOWN);
		} else if (dy > AUTO) {
			auto.add(PlayerMC.Action.UP);
		}
		setActions(auto);
	}

	private void kick() {
		if (kicking || tackling || heading)
			return;
		TimerTask kick = new TimerTask() {

			@Override
			public void run() {
				kicking = false;
			}
		};
		kicking = true;
		new Timer().schedule(kick, 100L);
	}

	private void tackle() {
		if (kicking || tackling || heading)
			return;
		TimerTask tackle = new TimerTask() {

			@Override
			public void run() {
				tackling = false;
			}
		};
		tackling = true;
		new Timer().schedule(tackle, 2000L);
	}

	private void head() {
		if (kicking || tackling || heading)
			return;
		TimerTask head = new TimerTask() {

			@Override
			public void run() {
				heading = false;
			}
		};
		heading = true;
		new Timer().schedule(head, 1000L);
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public int getShirt() {
		return shirt;
	}

	public boolean isKicking() {
		return kicking;
	}

	public Point3d getPosition() {
		return (Point3d) s.clone();
	}

	public void setPosition(Point3d s) {
		Preconditions.checkNotNull(s);
		this.s = s;
	}

	public Vector3d getVelocity() {
		return (Vector3d) v.clone();
	}
	// </editor-fold>
}
