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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.ode4j.math.DVector3;
import uk.me.fommil.ff.Pitch;
import static org.junit.Assert.*;

/**
 * @author Samuel Halliday
 */
public class BallMCTest {

	private final Pitch pitch = new Pitch();

	@Before
	public void setup() {
		OdeInit.dInitODE();

		world = OdeHelper.createWorld();
		world.setGravity(0, 0, -9.81);

		space = OdeHelper.createSimpleSpace();
		joints = OdeHelper.createJointGroup();

		OdeHelper.createPlane(space, 0, 0, 1, 0);

		ball = new Ball(world, space);
		Position centre = pitch.getCentre();
		ball.setPosition(centre);

	}

	private List<Ball> createBalls(int number, Position position) {
		List<Ball> balls = Lists.newArrayList();
		for (int i = 0; i < number; i++) {
			Ball ball = new Ball();
			ball.setPosition(position);
			balls.add(ball);
		}
		return balls;
	}

	@Test
	public void testGravity() throws Exception {
		fail("test not written");
	}

	@Test
	public void testNoKick() {
		Position centre = pitch.getCentre();
		Ball ball = Iterables.getOnlyElement(createBalls(1, centre));
		ball.setPosition(centre);
		ball.setVelocity(new DVector3(0, 0, 0)); // no kick
		for (int i = 0; i < 100; i++) {
			ball.tick(dt);
			assertEquals(centre, ball.getPosition());
		}
	}

	@Test
	public void testGroundKicks() {
		Position centre = pitch.getCentre();
		List<DVector3> velocities = Lists.newArrayList();
		velocities.add(new DVector3(10, 0, 0)); // 0 right
		velocities.add(new DVector3(0, 10, 0)); // 1 down
		velocities.add(new DVector3(-10, 0, 0)); // 2 left
		velocities.add(new DVector3(0, -10, 0)); // 3 up
		velocities.add(new DVector3(10, 10, 0)); // 4 down right
		velocities.add(new DVector3(-10, -10, 0)); // 5 up left
		velocities.add(new DVector3(10, -10, 0)); // 6 up right
		velocities.add(new DVector3(-10, 10, 0)); // 7 down left
		velocities.add(new DVector3(500, 0, 0)); // 8 right, fast
		velocities.add(new DVector3(0, 500, 0)); // 9 down, fast
		List<Ball> balls = createBalls(velocities.size(), centre);
		for (int i = 0; i < balls.size(); i++) {
			balls.get(i).setVelocity(velocities.get(i));
		}

		for (int i = 0; i < 1000; i++) {
			List<Position> positions = Lists.newArrayList();
			for (Ball ball : balls) {
				ball.tick(dt);
				assertEquals(0.0, ball.getPosition().z);
				positions.add(ball.getPosition());
			}

			assertTrue(centre.x < positions.get(0).x);
			assertTrue(centre.y < positions.get(1).y);
			assertTrue(centre.x > positions.get(2).x);
			assertTrue(centre.y > positions.get(3).y);
			assertTrue(centre.x < positions.get(4).x);
			assertTrue(centre.y < positions.get(4).y);
			assertTrue(centre.x > positions.get(5).x);
			assertTrue(centre.y > positions.get(5).y);
			assertTrue(centre.x < positions.get(6).x);
			assertTrue(centre.y > positions.get(6).y);
			assertTrue(centre.x > positions.get(7).x);
			assertTrue(centre.y < positions.get(7).y);

			assertTrue(positions.get(0).x < positions.get(8).x);
			assertTrue(positions.get(1).y < positions.get(9).y);

			assertEquals(positions.get(0).x, positions.get(4).x);
			assertEquals(positions.get(0).x, positions.get(6).x);
			assertEquals(positions.get(1).y, positions.get(4).y);
			assertEquals(positions.get(1).y, positions.get(7).y);
			assertEquals(positions.get(2).x, positions.get(5).x);
			assertEquals(positions.get(2).x, positions.get(7).x);
			assertEquals(positions.get(3).y, positions.get(5).y);
			assertEquals(positions.get(3).y, positions.get(6).y);
		}

		for (Ball ball : balls) {
			assertEquals(0.0, ball.getVelocity().speed());
		}
	}

	@Test
	public void testAirKicks() {
		Position centre = pitch.getCentre();
		List<DVector3> velocities = Lists.newArrayList();
		velocities.add(new DVector3(10, 0, 10)); // 0 right
		velocities.add(new DVector3(0, 10, 10)); // 1 down
		velocities.add(new DVector3(-10, 0, 10)); // 2 left
		velocities.add(new DVector3(0, -10, 10)); // 3 up
		velocities.add(new DVector3(10, 10, 10)); // 4 down right
		velocities.add(new DVector3(-10, -10, 10)); // 5 up left
		velocities.add(new DVector3(10, -10, 10)); // 6 up right
		velocities.add(new DVector3(-10, 10, 10)); // 7 down left
		velocities.add(new DVector3(500, 0, 100)); // 8 right, fast
		velocities.add(new DVector3(0, 500, 100)); // 9 down, fast
		List<Ball> balls = createBalls(velocities.size(), centre);
		for (int i = 0; i < balls.size(); i++) {
			balls.get(i).setVelocity(velocities.get(i));
		}

		for (int i = 0; i < 1000; i++) {
			List<Position> positions = Lists.newArrayList();
			for (Ball ball : balls) {
				ball.tick(dt);
				assertTrue(0.0 <= ball.getPosition().z);
				positions.add(ball.getPosition());
			}

			assertTrue(centre.x < positions.get(0).x);
			assertTrue(centre.y < positions.get(1).y);
			assertTrue(centre.x > positions.get(2).x);
			assertTrue(centre.y > positions.get(3).y);
			assertTrue(centre.x < positions.get(4).x);
			assertTrue(centre.y < positions.get(4).y);
			assertTrue(centre.x > positions.get(5).x);
			assertTrue(centre.y > positions.get(5).y);
			assertTrue(centre.x < positions.get(6).x);
			assertTrue(centre.y > positions.get(6).y);
			assertTrue(centre.x > positions.get(7).x);
			assertTrue(centre.y < positions.get(7).y);

			assertTrue(positions.get(0).x < positions.get(8).x);
			assertTrue(positions.get(1).y < positions.get(9).y);

			assertEquals(positions.get(0).x, positions.get(4).x);
			assertEquals(positions.get(0).x, positions.get(6).x);
			assertEquals(positions.get(1).y, positions.get(4).y);
			assertEquals(positions.get(1).y, positions.get(7).y);
			assertEquals(positions.get(2).x, positions.get(5).x);
			assertEquals(positions.get(2).x, positions.get(7).x);
			assertEquals(positions.get(3).y, positions.get(5).y);
			assertEquals(positions.get(3).y, positions.get(6).y);
		}

		for (Ball ball : balls) {
			assertEquals(0.0, ball.getVelocity().speed());
			assertEquals(0.0, ball.getPosition().z);
		}
	}

	@Test
	public void testBendyAftertouch() {
		fail("test not written");
	}

	@Test
	public void testPowerAftertouch() {
		fail("test not written");
	}

	@Test
	public void testLiftAftertouch() {
		fail("test not written");
	}

	@Test
	public void testZone() {
		fail("test not written");
	}
}
