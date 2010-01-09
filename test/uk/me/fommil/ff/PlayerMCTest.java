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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import javax.vecmath.Point3d;
import org.junit.Test;
import uk.me.fommil.ff.PlayerMC.Action;
import uk.me.fommil.ff.PlayerMC.PlayerMode;
import static org.junit.Assert.*;

/**
 * @author Samuel Halliday
 */
public class PlayerMCTest {

	private final Pitch pitch = new Pitch();

	private final double dt = 50L / 1000.0;

	@Test
	public void testKick() throws Exception {
		fail("test not written");
	}

	@Test
	public void testRun() throws Exception {
		Point3d centre = pitch.getCentre();
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
		List<PlayerMC> players = createPlayers(actions.size(), centre);
		for (int i = 0; i < players.size(); i++) {
			players.get(i).setActions(actions.get(i));
		}

		for (int i = 0; i < 1000; i++) {
			List<Point3d> positions = Lists.newArrayList();
			for (PlayerMC player : players) {
				player.tick(dt);
				assertEquals(PlayerMode.RUN, player.getMode());
				assertEquals(0.0, player.getPosition().z);
				positions.add(player.getPosition());
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

			assertTrue(centre + " " + positions.get(0) + " " + positions.get(4), positions.get(0).x > positions.get(4).x);
			assertTrue(positions.get(0).x > positions.get(6).x);
			assertTrue(positions.get(1).y > positions.get(4).y);
			assertTrue(positions.get(1).y > positions.get(7).y);
			assertTrue(positions.get(2).x < positions.get(5).x);
			assertTrue(positions.get(2).x < positions.get(7).x);
			assertTrue(positions.get(3).y < positions.get(5).y);
			assertTrue(positions.get(3).y < positions.get(6).y);
		}

		for (int i = 0; i < players.size(); i++) {
			players.get(i).setActions(EnumSet.noneOf(Action.class));
		}

		for (PlayerMC player : players) {
			assertEquals("v = " + player.getVelocity(), 0.0, player.getVelocity().length());
		}
	}

	@Test
	public void testHead() throws Exception {
		fail("test not written");
	}

	@Test
	public void testTackle() throws Exception {
		fail("test not written");
	}

	private List<PlayerMC> createPlayers(int number, Point3d position) {
		List<PlayerMC> models = Lists.newArrayList();
		for (int i = 0; i < number; i++) {
			Player player = new Player();
			PlayerMC model = new PlayerMC(7, player);
			model.setPosition(position);
			models.add(model);
		}
		return models;
	}
}
