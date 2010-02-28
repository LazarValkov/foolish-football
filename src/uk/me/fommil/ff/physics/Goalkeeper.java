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
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.internal.Rotation;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.PlayerStats;

/**
 * The model (M) for a goalkeeper.
 *
 * @author Samuel Halliday
 */
public class Goalkeeper extends Player {

	public enum GoalkeeperState {

		DIVE_START, DIVE_MID, DIVE_PEAK, FALL_START, FALL_MID, FALL_END
		// TODO FALL_END_BALL HOLDING

	}

	private Direction opponent;

	/**
	 * @param i
	 * @param stats
	 * @param world
	 * @param space
	 */
	public Goalkeeper(int i, PlayerStats stats, DWorld world, DSpace space) {
		// TODO: consider rolling Goalkeeper functionality into Player
		super(i, stats, world, space);
	}

	@Override
	void setActions(Collection<Action> actions) {
		if (getGkState() != null)
			return;

		Collection<Action> sanitised = Lists.newArrayList(actions);
		sanitised.remove(Action.TACKLE);
		sanitised.remove(Action.HEAD);
		if (actions.contains(Action.DIVE))
			sanitised.remove(Action.KICK);

		if (!sanitised.contains(Action.DIVE)) {
			super.setActions(sanitised);
			return;
		}
		sanitised.remove(Action.UP);
		sanitised.remove(Action.DOWN);
		DVector3 move = Action.asVector(sanitised);

		DMatrix3 rotation = new DMatrix3();
		DMatrix3 tilt = new DMatrix3();
		double direction = 0; // TODO: opponent direction
		Rotation.dRFromAxisAndAngle(rotation, 0, 0, -1, direction);
		double tiltAngle = 0;
		if (sanitised.contains(Action.RIGHT))
			tiltAngle = Math.PI / 4;
		else if (sanitised.contains(Action.LEFT))
			tiltAngle = -Math.PI / 4;
		Rotation.dRFromAxisAndAngle(tilt, 0, 1, 0, tiltAngle);
		rotation.eqMul(rotation.clone(), tilt);

		move.scale(5); // TODO: goalkeeper stats
		move.set2(3);

		body.setLinearVel(move);
		body.setRotation(rotation);
	}

	public GoalkeeperState getGkState() {
		DVector3C position = body.getPosition();
		DVector3C velocity = body.getLinearVel();
		double z = position.get2() - HEIGHT / 2;
		double vz = velocity.get2();
		double tilt = getTilt();
		if (tilt < Math.PI / 8 && z < 0.1 && Math.abs(vz) < 0.1)
			return null;

		if (vz > 0.5) {
			if (z < 0.1)
				return GoalkeeperState.DIVE_START;
			if (z < 0.25)
				return GoalkeeperState.DIVE_MID;
			return GoalkeeperState.DIVE_PEAK;
		}
		if (z < 0.1)
			return GoalkeeperState.FALL_END;
		if (z < 0.25)
			return GoalkeeperState.FALL_MID;
		return GoalkeeperState.FALL_START;
	}

	public void dive(@Nullable Direction direction) {
		Preconditions.checkArgument(direction == null || direction == Direction.EAST || direction == Direction.WEST, direction);

		List<Action> auto = Lists.newArrayList();
		if (direction != null)
			switch (direction) {
				case EAST:
					auto.add(opponent == Direction.NORTH ? Action.RIGHT : Action.LEFT);
					break;
				case WEST:
					auto.add(opponent == Direction.NORTH ? Action.LEFT : Action.RIGHT);
					break;
			}
		auto.add(Action.DIVE);
		setActions(auto);
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public Direction getOpponent() {
		return opponent;
	}

	public void setOpponent(Direction opponent) {
		this.opponent = opponent;
	}
	// </editor-fold>
}
