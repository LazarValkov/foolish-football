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

import org.ode4j.ode.DContact.DSurfaceParameters;
import org.ode4j.ode.OdeConstants;
import uk.me.fommil.ff.physics.CollisionCallback.CollisionHandler;

/**
 * Handles collisions using objects specific to this application.
 *
 * @author Samuel Halliday
 */
class GameCollisionHandler implements CollisionHandler {

	@Override
	public void collide(Ball ball, Player player, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		ball.setDamping(0.1); // ?? can be overridden
		surface.bounce = player.getBounce();
	}

	@Override
	public void collide(Player player1, Player player2, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		surface.bounce = 0.75; // affects tackling
	}

	@Override
	public void collide(Ball ball, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		surface.bounce = 0.5;
		ball.setDamping(0.1); // ?? can be overridden
		ball.setAftertouchEnabled(false);
	}

	@Override
	public void collide(Player player, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		if (player.getTilt() > Math.PI / 8) // ?? exposing more than is needed?
			surface.mu = 1000;
	}

	private void enableSoftBounce(DSurfaceParameters surface) {
		surface.mode = OdeConstants.dContactBounce | OdeConstants.dContactSoftERP;
		surface.bounce_vel = 0.1;
	}
}
