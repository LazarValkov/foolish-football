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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import com.google.common.base.Preconditions;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * The model (M) and controller (C) for a {@link Player} during game play.
 *
 * @author Samuel Halliday
 */
public class PlayerMC {

	// this is mutable, so be careful not to edit it
	private static final Vector3d UP = new Vector3d(0, 0, 1);

	// View
	@Deprecated
	public void setLocation(Point p) {
		setPosition(new Point3d(p.x, p.y, 0));
	}

	@Deprecated
	public Point getLocation() {
		return new Point((int) round(s.x), (int) round(s.y));
	}

	@Deprecated
	public Point getStep() {
		return new Point((int) round(v.x), (int) round(v.y));
	}

	@Deprecated
	public Shape getViewBounds() {
		return new Rectangle.Double(s.x - 4, s.y - 4, 9, 9);
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
	private Vector3d facing = new Vector3d(0, 1, 0);

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
	public Bounds getBounds() {
		


		// perhaps the best approach is to construct a BoundingPolytope and rotate/translate/scale

		// TODO: implement method
		throw new UnsupportedOperationException("not implemented yet");


//		Vector3d bottom = new Vector3d();
//		bottom.cross(facing, UP);
//		Vector3d top = (Vector3d) facing.clone();
//		top.add(UP);
//		top.sub(bottom);

		// FIXME: diagonal
//		bottom.scale(10);
//		bottom.add(s);
//		top.scale(10);
//		top.add(s);

//		Point3d min = new Point3d(min(bottom.x, top.x), min(bottom.y, top.y), min(bottom.z, top.z));
//		Point3d max = new Point3d(max(bottom.x, top.x), max(bottom.y, top.y), max(bottom.z, top.z));
//		BoundingBox bbox = new BoundingBox(min, max);
//		return bbox;
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
		if (v.lengthSquared() > 0)
			facing.normalize(v);
	}

	/**
	 * Controller. Clear the action list.
	 */
	public void clearActions() {
		v = new Vector3d();
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

	public void setfacing(Vector3d facing) {
		Preconditions.checkNotNull(facing);
		this.facing.normalize(facing);
	}

	public Vector3d getFacing() {
		return (Vector3d) facing.clone();
	}
	// </editor-fold>
}
