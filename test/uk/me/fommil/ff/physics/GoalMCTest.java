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

import uk.me.fommil.ff.physics.Goalpost;
import uk.me.fommil.ff.physics.Ball;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Logger;
import javax.media.j3d.BoundingBox;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import org.junit.Test;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.Utils;
import static org.junit.Assert.*;

/**
 * @author Samuel Halliday
 */
public class GoalMCTest {

	private static final Logger log = Logger.getLogger(GoalMCTest.class.getName());

	private final Pitch pitch = new Pitch();

	private static final double dt = 50L / 1000.0;

	@Test
	public void testBounce() throws Exception {
		testBounceDelegate(pitch.getGoalNetTop(), Direction.DOWN);
		testBounceDelegate(pitch.getGoalNetBottom(), Direction.UP);
	}

	public void testBounceDelegate(BoundingBox bbox, Direction direction) throws Exception {
		Goalpost goal = new Goalpost(bbox, 2, direction);
		Point3d lower = Utils.getLower(bbox);
		Point3d upper = Utils.getUpper(bbox);

		Point3d cog = new Point3d((upper.x - lower.x) / 2, (upper.y - lower.y) / 2, (upper.z - lower.z) / 2);
		cog.add(lower);

		List<Vector3d> velocities = Lists.newArrayList();
		velocities.add(new Vector3d(100, 0, 0)); // 0 right
		velocities.add(new Vector3d(0, 100, 0)); // 1 down
		velocities.add(new Vector3d(-100, 0, 0)); // 2 left
		velocities.add(new Vector3d(0, -100, 0)); // 3 up
		velocities.add(new Vector3d(100, 100, 0)); // 4 down right
		velocities.add(new Vector3d(-100, -100, 0)); // 5 up left
		velocities.add(new Vector3d(100, -100, 0)); // 6 up right
		velocities.add(new Vector3d(-100, 100, 0)); // 7 down left
		velocities.add(new Vector3d(500, 0, 0)); // 8 right, fast
		velocities.add(new Vector3d(0, -500, 0)); // 9 up, fast
		List<Point3d> positions = Lists.newArrayList();
		for (Vector3d v : velocities) {
			Vector3d vu = (Vector3d) v.clone();
			vu.normalize();
			vu.scale(50);
			Point3d p = new Point3d(vu);
			p.negate();
			p.add(cog);
			p.z = 2;
			positions.add(p);
		}
		List<Ball> balls = Lists.newArrayList();
		for (int i = 0; i < velocities.size(); i++) {
			Ball ball = new Ball();
			ball.setPosition(positions.get(i));
			ball.setVelocity(velocities.get(i));
			balls.add(ball);
		}

		for (int i = 0; i < 1000; i++) {
			for (Ball ball : balls) {
				Point3d pOld = ball.getPosition();
				ball.tick(dt);
				Point3d p = ball.getPosition();
				Vector3d v = ball.getVelocity();
				goal.bounce(p, v, pOld);
				if (!p.equals(pOld)) {
					ball.setPosition(p);
					ball.setVelocity(v);
				}
			}
		}
		for (Ball ball : balls) {
			assertTrue(ball.getVelocity().length() == 0);
		}

		assertTrue(balls.get(0).getPosition().x < lower.x);
		assertTrue(balls.get(1).getPosition().y < lower.y);
		assertTrue(balls.get(2).getPosition().x > upper.x);
		assertTrue(balls.get(3).getPosition().y > lower.y);

		assertTrue(balls.get(4).getPosition().y < lower.y);
		assertTrue(balls.get(5).getPosition().y > upper.y);
		assertTrue(balls.get(6).getPosition().y > upper.y);
		assertTrue(balls.get(7).getPosition().y < lower.y);

		assertTrue(balls.get(8).getPosition().x < lower.x);
		assertTrue(balls.get(9).getPosition().y > upper.y);
	}
}
