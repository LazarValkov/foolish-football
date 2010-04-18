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

import org.ode4j.ode.DGeom.DNearCallback;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.PlayerStats;

/**
 *
 * @author Samuel Halliday
 */
public class DummyPhysics extends Physics {

	private final Pitch pitch = new Pitch();

	public DummyPhysics() {
		super(9.81);
	}

	@Override
	protected DNearCallback getCollisionCallback() {
		GameCollisionHandler handler = new GameCollisionHandler();
		return new CollisionCallback(world, joints, handler);
	}

	public Ball createBall() {
		Ball ball = new Ball(world, space);
		return ball;
	}

	public Player createPlayer(int shirt, PlayerStats stats) {
		Player player = new Player(shirt, stats, world, space);
		return player;
	}

	public Goalpost createGoalpost(Direction direction) {
		Goalpost goal = new Goalpost(world, space, pitch, direction);
		return goal;
	}
}
