/*
 * Copyright Samuel Halliday 2010
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
import org.ode4j.math.DVector3;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.Pitch;

/**
 * The logic for automatic controlling a {@link Goalkeeper} - users do not get to directly
 * control the goalkeeper.
 *
 * @author Samuel Halliday
 */
class GoalkeeperController {

	private final Pitch pitch;

	public GoalkeeperController(Pitch pitch) {
		this.pitch = pitch;
	}

	public void autoPilot(Goalkeeper p, Ball ball) {
		Preconditions.checkNotNull(p);
		Preconditions.checkNotNull(ball);
		Position position = p.getPosition();
		Position ballPosition = ball.getPosition();
		if (ballPosition.distance(position) > 5) {
			Position target;
			if (p.getOpponent() == Direction.SOUTH) {
 				target = pitch.getGoalTop();
				target = new Position(target.x, target.y - 5, target.z);
			} else {
				target = pitch.getGoalBottom();
				target = new Position(target.x, target.y + 5, target.z);
			}
			p.autoPilot(target);
			return;
		}

		DVector3 ballP = ballPosition.toDVector();
		DVector3 s = position.toDVector();
		DVector3 diff = ballP.sub(s);

		if (diff.get0() > 1)
			p.dive(Direction.EAST);
		else if (diff.get0() < -1)
			p.dive(Direction.WEST);
		else
			p.dive(null);
	}
}
