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
package com.github.fommil.ff.physics;

import org.junit.Test;
import com.github.fommil.ff.Pitch;
import static org.junit.Assert.*;

/**
 * @author Samuel Halliday
 */
public class BallTest {

	private static final double dt = 0.01;

	private static final double EPSILON = 0.0001;

	private final Pitch pitch = new Pitch();

	private final Position centre = pitch.getCentre();

	interface Tester {

		void test(Position s, Velocity v);
	}

	void testHelper(Position position, Velocity velocity, Tester stepTest) {
		DummyPhysics physics = new DummyPhysics();
		Ball ball = physics.createBall();
		ball.setPosition(position);
		ball.setVelocity(velocity);
		for (int i = 0; i < 1000; i++) {
			physics.step(dt);
			stepTest.test(ball.getPosition(), ball.getVelocity());
		}
		physics.clean();
	}

	@Test
	public void testGravity() throws Exception {
		fail("test not written");
	}

	@Test
	public void testNoKick() {
		final Velocity velocity = new Velocity(0, 0, 0);
		testHelper(centre, velocity, new Tester() {

			@Override
			public void test(Position s, Velocity v) {
				assertEquals(centre.x, s.x, EPSILON);
				assertEquals(centre.y, s.y, EPSILON);
				assertEquals(centre.z, s.z, EPSILON);
			}
		});
	}

	@Test
	public void testGroundKicks() {
		{
			final Velocity velocity = new Velocity(10, 0, 0); // right
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.x < s.x);
					assertEquals(centre.y, s.y);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(0, -10, 0); // down
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.y > s.y);
					assertEquals(centre.x, s.x);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(-10, 0, 0); // left
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.x > s.x);
					assertEquals(centre.y, s.y);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(0, 10, 0); // up
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.y < s.y);
					assertEquals(centre.x, s.x);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(10, -10, 0); // down right
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.x < s.x);
					assertTrue(centre.y > s.y);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(-10, 10, 0); // up left
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.x > s.x);
					assertTrue(centre.y < s.y);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(10, 10, 0); // up right
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.x < s.x);
					assertTrue(centre.y < s.y);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(-10, -10, 0); // down left
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.x > s.x);
					assertTrue(centre.y > s.y);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(50, 0, 0); // right, fast
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.x < s.x);
					assertEquals(centre.y, s.y);
				}
			});
		}
		{
			final Velocity velocity = new Velocity(0, -50, 0); // down, fast
			testHelper(centre, velocity, new Tester() {

				@Override
				public void test(Position s, Velocity v) {
					assertTrue(centre.y > s.y);
					assertEquals(centre.x, s.x);
				}
			});
		}
	}

	@Test
	public void testAirKicks() {
		fail("test not written");
//		Position centre = pitch.getCentre();
//		List<DVector3> velocities = Lists.newArrayList();
//		velocities.add(new DVector3(10, 0, 10)); // 0 right
//		velocities.add(new DVector3(0, 10, 10)); // 1 down
//		velocities.add(new DVector3(-10, 0, 10)); // 2 left
//		velocities.add(new DVector3(0, -10, 10)); // 3 up
//		velocities.add(new DVector3(10, 10, 10)); // 4 down right
//		velocities.add(new DVector3(-10, -10, 10)); // 5 up left
//		velocities.add(new DVector3(10, -10, 10)); // 6 up right
//		velocities.add(new DVector3(-10, 10, 10)); // 7 down left
//		velocities.add(new DVector3(500, 0, 100)); // 8 right, fast
//		velocities.add(new DVector3(0, 500, 100)); // 9 down, fast
//		List<Ball> balls = createBalls(velocities.size(), centre);
//		for (int i = 0; i < balls.size(); i++) {
//			balls.get(i).setVelocity(velocities.get(i));
//		}
//
//		for (int i = 0; i < 1000; i++) {
//			List<Position> positions = Lists.newArrayList();
//			for (Ball ball : balls) {
//				ball.tick(dt);
//				assertTrue(0.0 <= ball.getPosition().z);
//				positions.add(ball.getPosition());
//			}
//
//			assertTrue(centre.x < positions.get(0).x);
//			assertTrue(centre.y < positions.get(1).y);
//			assertTrue(centre.x > positions.get(2).x);
//			assertTrue(centre.y > positions.get(3).y);
//			assertTrue(centre.x < positions.get(4).x);
//			assertTrue(centre.y < positions.get(4).y);
//			assertTrue(centre.x > positions.get(5).x);
//			assertTrue(centre.y > positions.get(5).y);
//			assertTrue(centre.x < positions.get(6).x);
//			assertTrue(centre.y > positions.get(6).y);
//			assertTrue(centre.x > positions.get(7).x);
//			assertTrue(centre.y < positions.get(7).y);
//
//			assertTrue(positions.get(0).x < positions.get(8).x);
//			assertTrue(positions.get(1).y < positions.get(9).y);
//
//			assertEquals(positions.get(0).x, positions.get(4).x);
//			assertEquals(positions.get(0).x, positions.get(6).x);
//			assertEquals(positions.get(1).y, positions.get(4).y);
//			assertEquals(positions.get(1).y, positions.get(7).y);
//			assertEquals(positions.get(2).x, positions.get(5).x);
//			assertEquals(positions.get(2).x, positions.get(7).x);
//			assertEquals(positions.get(3).y, positions.get(5).y);
//			assertEquals(positions.get(3).y, positions.get(6).y);
//		}
//
//		for (Ball ball : balls) {
//			assertEquals(0.0, ball.getVelocity().speed());
//			assertEquals(0.0, ball.getPosition().z);
//		}
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
