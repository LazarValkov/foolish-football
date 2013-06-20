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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;
import com.github.fommil.ff.Pitch;
import static org.junit.Assert.*;

/**
 * @author Samuel Halliday
 */
public class PlayerTest {

	private final Pitch pitch = new Pitch();

	private static final double dt = 0.01;

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
	public void testKick() throws Exception {
		fail("test not written");
	}

	@Test
	public void testRun() throws Exception {
		Position centre = pitch.getCentre();
		List<Collection<Action>> actions = Lists.newArrayList();
		actions.add(Sets.immutableEnumSet(Action.RIGHT)); // 0
		actions.add(Sets.immutableEnumSet(Action.DOWN));  // 1
		actions.add(Sets.immutableEnumSet(Action.LEFT));  // 2
		actions.add(Sets.immutableEnumSet(Action.UP));    // 3
		actions.add(Sets.immutableEnumSet(Action.DOWN, Action.RIGHT)); // 4
		actions.add(Sets.immutableEnumSet(Action.UP, Action.LEFT));    // 5
		actions.add(Sets.immutableEnumSet(Action.UP, Action.RIGHT));   // 6
		actions.add(Sets.immutableEnumSet(Action.DOWN, Action.LEFT));  // 7
		actions.add(EnumSet.noneOf(Action.class));  // 7

		fail("test not written");

	}

	@Test
	public void testHead() throws Exception {
		fail("test not written");
	}

	@Test
	public void testTackle() throws Exception {
		fail("test not written");
	}
}
