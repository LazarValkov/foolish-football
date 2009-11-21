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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import javax.vecmath.Point3d;
import uk.me.fommil.ff.GameMVC.Action;

/**
 * The model (M) and controller (C) for a {@link Player} during game play.
 *
 * @author Samuel Halliday
 */
public class PlayerMC {

	private final Player player;
	private final int i;
	private Collection<Action> actions;
	private boolean kicking;
	private Point3d s = new Point3d();
	private Point3d v = new Point3d();

	/**
	 * @param i
	 * @param player
	 */
	public PlayerMC(int i, Player player) {
		Preconditions.checkArgument(i >= 0 && i < 11, i);
		Preconditions.checkNotNull(player);

		this.i = i;
		this.player = player;
	}

	/**
	 * @param t with units of seconds
	 */
	public void tick(double t) {
		Point3d dv = (Point3d) v.clone();
		dv.scale(t);
		s.add(dv);
	}

	public Rectangle2D getBounds() {
		return new Rectangle.Double(s.x - 4, s.y - 4, 9, 9);
	}

	// return the area that this player can control the ball with
	public Shape getControlBounds() {
		// TODO: bounds of control
		return getBounds();
	}

	/**
	 * Controller.
	 * 
	 * @param actions
	 */
	public void setActions(Collection<Action> actions) {
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
				case BUTTON_A:
					setKicking(true);
					break;
			}
		}
		v = new Point3d(x, y, 0);
	}

	public void setLocation(Point p) {
		s = new Point3d(p.x, p.y, 0);
	}

	public Point2D getLocation() {
		return new Point2D.Double(s.x, s.y);
	}

	public Point3d getVelocity() {
		return (Point3d) v.clone();
	}

	public boolean isKicking() {
		return kicking;
	}

	public void setKicking(boolean kicking) {
		this.kicking = kicking;
	}
}
