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

import java.util.logging.Logger;
import org.ode4j.ode.DContact.DSurfaceParameters;
import org.ode4j.ode.OdeConstants;
import uk.me.fommil.ff.physics.CollisionCallback.CollisionHandler;
import uk.me.fommil.ff.swos.SoundParser;

/**
 * Handles collisions using objects specific to this package.
 *
 * @author Samuel Halliday
 * @see CollisionCallback
 */
class GameCollisionHandler implements CollisionHandler {

	private static final Logger log = Logger.getLogger(GameCollisionHandler.class.getName());

	@Override
	public boolean collide(Ball ball, Player player, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		ball.setDamping(0.1); // ?? can be overridden
		surface.bounce = player.getBounce();
		return true;
	}

	@Override
	public boolean collide(Player player1, Player player2, DSurfaceParameters surface) {
		if (player1 instanceof Goalkeeper || player2 instanceof Goalkeeper)
			return false; // classic graphics can't handle goalkeepers on the ground
		enableSoftBounce(surface);
		surface.bounce = 0.75; // affects tackling
		return true;
	}

	@Override
	public boolean collide(Ball ball, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		surface.bounce = 0.5;
		ball.setDamping(0.1); // ?? can be overridden
		ball.setAftertouchEnabled(false);

		if (ball.getVelocity().speed() > 10) {
			try {
				SoundParser.play(SoundParser.Fx.BALL_BOUNCE);
			} catch (Exception ex) {
				log.warning(ex.getMessage());
			}
		}

		return true;
	}

	@Override
	public boolean collide(Player player, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		if (player.getTilt() > Math.PI / 8) // ?? exposing more than is needed?
			surface.mu = 1000;
		return true;
	}

	@Override
	public boolean collide(Goalpost post, DSurfaceParameters surface) {
		enableSoftBounce(surface);
		surface.bounce = 0;
		surface.mu = Double.POSITIVE_INFINITY;
		return true;
	}

	private void enableSoftBounce(DSurfaceParameters surface) {
		surface.mode = OdeConstants.dContactBounce | OdeConstants.dContactSoftERP;
		surface.bounce_vel = 0.1;
	}
}
