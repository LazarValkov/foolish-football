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

import java.util.Collection;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import static java.lang.Math.*;

/**
 * The model (M) for a goalkeeper.
 *
 * @author Samuel Halliday
 */
public class GoalkeeperM extends PlayerMC {


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

	public GoalkeeperM(int i, Player player) {
		super(i, player);
	}

	/**
	 * @param t with units of seconds
	 */
	public void tick(double t) {
		time += t;
		if (v.length() > 0) {
			Vector3d dv = (Vector3d) v.clone();
			dv.scale(t);
			s.add(dv);
			v.x = signum(v.x) * max(0, abs(v.x) - friction(t, s.z));
			v.y = signum(v.y) * max(0, abs(v.y) - friction(t, s.z));
			if (v.length() < 10)
				v.scale(0);
		}

		switch (mode) {
			case KICK:
				changeModeIfTimeExpired(0.1, PlayerState.RUN);
				break;
		}

		assert gkState != null;
		switch (gkState) {
			case DIVE_START:
				changeGkModeIfTimeExpired(0.05, GoalkeeperState.DIVE_MID);
				break;
			case DIVE_MID:
				changeGkModeIfTimeExpired(0.05, GoalkeeperState.DIVE_PEAK);
				break;
			case DIVE_PEAK:
				changeGkModeIfTimeExpired(0.05, GoalkeeperState.FALL_START);
				break;
			case FALL_START:
				changeGkModeIfTimeExpired(0.05, GoalkeeperState.FALL_MID);
				break;
			case FALL_MID:
				changeGkModeIfTimeExpired(0.05, GoalkeeperState.FALL_END);
				break;
			case FALL_END:
				changeGkModeIfTimeExpired(2, GoalkeeperState.RUN);
				break;
			default:
				if (random.nextInt(100) < 5) {
					if (random.nextBoolean()) {
						v.set(-75, 0, 1);
						facing.set(-1, 0, 0);
					} else {
						v.set(75, 0, 1);
						facing.set(1, 0, 0);
					}
					timestamp = time;
					gkState = GoalkeeperState.DIVE_START;
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
			if (gkState == GoalkeeperState.RUN)
				timestamp = Double.NaN;
			else
				timestamp = time;
		}
	}

	@Deprecated
	private double friction(double t, double z) {
		if (z > 0.1)
			return AIR_FRICTION * t;
		return GROUND_FRICTION * t;
	}

	private static final double GROUND_FRICTION = 500;

	private static final double AIR_FRICTION = 50;

	private static final double GRAVITY = 10;

	public GoalkeeperState getGkState() {
		return gkState;
	}

	@Override
	public void autoPilot(Point3d attractor) {
		throw new UnsupportedOperationException("no autopilot for goalkeeper");
	}

	@Override
	public void setActions(Collection<Action> actions) {
		// TODO: allow player to direct and kick
		throw new UnsupportedOperationException("actions not available for goalkeeper");
	}
}
