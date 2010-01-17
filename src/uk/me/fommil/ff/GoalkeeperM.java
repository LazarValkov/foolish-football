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
package uk.me.fommil.ff;

import javax.vecmath.Vector3d;

/**
 * The model (M) for a goalkeeper.
 *
 * @author Samuel Halliday
 */
public class GoalkeeperM extends PlayerMC {

	public enum GoalkeeperState {

		DIVE_START, DIVE_MID, DIVE_PEAK, FALL_START, FALL_MID, FALL_END
		// TODO FALL_END_BALL

	}

	private volatile GoalkeeperState gkState;

	public GoalkeeperM(int i, Player player) {
		super(i, player);
	}

	/**
	 * @param t with units of seconds
	 */
	public void tick(double t) {
		time += t;
		assert mode != null;
		switch (mode) {
			case GROUND:
			case THROW:
			case INJURED:
				break;
			default:
				Vector3d dv = (Vector3d) v.clone();
				dv.scale(t);
				s.add(dv);
		}

		switch (mode) {
			case KICK:
				changeModeIfTimeExpired(0.1, PlayerState.RUN);
				break;
			case GROUND:
				if (random.nextBoolean())
					changeModeIfTimeExpired(2, PlayerState.RUN);
				else
					changeModeIfTimeExpired(2, PlayerState.INJURED);
				break;
			case INJURED:
				changeModeIfTimeExpired(5, PlayerState.RUN);
		}

		switch (gkState) {
			case DIVE_START:
				changeGkModeIfTimeExpired(0.1, GoalkeeperState.DIVE_MID);
				break;
			case DIVE_MID:
				changeGkModeIfTimeExpired(0.1, GoalkeeperState.DIVE_PEAK);
				break;
			case DIVE_PEAK:
				changeGkModeIfTimeExpired(0.1, GoalkeeperState.FALL_START);
				break;
			case FALL_START:
				changeGkModeIfTimeExpired(0.1, GoalkeeperState.FALL_MID);
				break;
			case FALL_MID:
				changeGkModeIfTimeExpired(0.1, GoalkeeperState.FALL_END);
				break;
			case FALL_END:
				changeGkModeIfTimeExpired(0.1, null);
				break;
			default:
				if (random.nextInt(100) < 10) {
					v.set(0, random.nextBoolean() ? -10 : 10, 0);
					changeGkModeIfTimeExpired(0, GoalkeeperState.DIVE_START);
				}
				break;
		}
	}


	// TODO: no code duplication
	private volatile double timestamp = Double.NaN; // of last mode switch
	@Deprecated
	protected void changeGkModeIfTimeExpired(double t, GoalkeeperState gkState) {
		if (time - timestamp > t) {
			this.gkState = gkState;
			if (gkState == null)
				timestamp = Double.NaN;
			else
				timestamp = time;
		}
	}
}
