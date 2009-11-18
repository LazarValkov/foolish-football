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

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Collection;
import uk.me.fommil.ff.GameView.Action;

/**
 * Contains all the physics information about the ball during game play.
 * 
 * @author Samuel Halliday
 */
public class BallModel {

	Point2D p = new Point(200, 300);
	Point2D v;
	double h = 0;
	Point2D aftertouch;

	public Point2D getLocation() {
		return p;
	}

	public void setVelocity(Point direction) {
		v = direction;
	}

	public void tick(long PERIOD) {
		if (v != null) {
			p = new Point.Double(p.getX() + v.getX(), p.getY() + v.getY());
			if (h > 10) {
				p = new Point.Double(p.getX() + aftertouch.getX(), p.getY());
			}
			v = new Point.Double(0.9 * v.getX(), 0.9 * v.getY());
			if (h > 1) {
				// FIXME
				p = new Point.Double(p.getX(), p.getY() + aftertouch.getY());
			}
			h = Math.max(0, h - 1);
		}
	}

	public double getHeight() {
		return h;
	}

	public void setActions(Collection<Action> actions) {
		aftertouch = new Point2D.Double();
		for (Action action : actions) {
			switch (action) {
				case DOWN:
					aftertouch = new Point2D.Double(aftertouch.getX(), aftertouch.getY() + 5);
					break;
				case LEFT:
					aftertouch = new Point2D.Double(aftertouch.getX() - 5, aftertouch.getY());
					break;
				case RIGHT:
					aftertouch = new Point2D.Double(aftertouch.getX() + 5, aftertouch.getY());
					break;
			}
		}
	}
}
