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

import com.google.common.collect.Lists;
import java.util.Collection;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
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

	private volatile GoalkeeperState gkState;

	private Direction opponent;

	/**
	 * @param i
	 * @param stats
	 * @param world
	 * @param space
	 */
	public Goalkeeper(int i, PlayerStats stats, DWorld world, DSpace space) {
		super(i, stats, world, space);
	}

	@Override
	public void setActions(Collection<Action> actions) {
		Collection<Action> sanitised = Lists.newArrayList(actions);
		sanitised.remove(Action.TACKLE);
		sanitised.remove(Action.HEAD);
		// TODO: remove KICK when diving
		super.setActions(sanitised);
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public Direction getOpponent() {
		return opponent;
	}

	public void setOpponent(Direction opponent) {
		this.opponent = opponent;
	}

	public GoalkeeperState getGkState() {
		return gkState;
	}
	// </editor-fold>
}
