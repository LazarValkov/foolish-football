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

import java.util.Collection;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.PlayerStats;
import uk.me.fommil.ff.physics.Action;

/**
 * The model (M) for a goalkeeper.
 *
 * @author Samuel Halliday
 */
public class Goalkeeper {

	public enum GoalkeeperState {

		DIVE_START, DIVE_MID, DIVE_PEAK, FALL_START, FALL_MID, FALL_END, RUN
		// TODO FALL_END_BALL

	}

	private volatile GoalkeeperState gkState = GoalkeeperState.RUN;

	private Direction opponent;

	public Direction getOpponent() {
		return opponent;
	}

	public void setOpponent(Direction opponent) {
		this.opponent = opponent;
	}

	public Goalkeeper(int i, PlayerStats player) {
	}

	public GoalkeeperState getGkState() {
		return gkState;
	}

	public void setActions(Collection<Action> actions) {
		// TODO: allow player to direct and kick
		throw new UnsupportedOperationException("actions not available for goalkeeper");
	}

	public Velocity getVelocity() {
		// TODO: implement method
		throw new UnsupportedOperationException("not implemented yet");
	}

	public Position getPosition() {
		// TODO: implement method
		throw new UnsupportedOperationException("not implemented yet");
	}

	public double getDirection() {
		// TODO: implement method
		throw new UnsupportedOperationException("not implemented yet");
	}
}
