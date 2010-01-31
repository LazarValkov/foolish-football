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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.lwjgl.input.Keyboard;

/**
 * Keyboard listening is synchronous, not asynchronous, with LWJGL - this class provides the poll.
 *
 * @author Samuel Halliday
 */
class LwjglKeyboardController {

	private static final Map<Integer, Player.Action> actionLookup = Maps.newHashMap();

	private static final Map<Integer, Ball.Aftertouch> aftertouchLookup = Maps.newHashMap();

	static {
		actionLookup.put(Keyboard.KEY_UP, Player.Action.UP);
		actionLookup.put(Keyboard.KEY_DOWN, Player.Action.DOWN);
		actionLookup.put(Keyboard.KEY_LEFT, Player.Action.LEFT);
		actionLookup.put(Keyboard.KEY_RIGHT, Player.Action.RIGHT);
		actionLookup.put(Keyboard.KEY_SPACE, Player.Action.KICK);
		actionLookup.put(Keyboard.KEY_RETURN, Player.Action.TACKLE);
		actionLookup.put(Keyboard.KEY_A, Player.Action.HEAD);

		aftertouchLookup.put(Keyboard.KEY_UP, Ball.Aftertouch.UP);
		aftertouchLookup.put(Keyboard.KEY_DOWN, Ball.Aftertouch.DOWN);
		aftertouchLookup.put(Keyboard.KEY_LEFT, Ball.Aftertouch.LEFT);
		aftertouchLookup.put(Keyboard.KEY_RIGHT, Ball.Aftertouch.RIGHT);
	}

	private final Collection<Integer> lastKeys = Sets.newHashSet();

	private final GamePhysics game;

	LwjglKeyboardController(GamePhysics game) {
		this.game = game;
	}

	void poll() {
		boolean change = false;
		Collection<Player.Action> actions = Sets.newHashSet();
		Collection<Ball.Aftertouch> aftertouches = Sets.newHashSet();
		for (Entry<Integer, Player.Action> e : actionLookup.entrySet()) {
			int key = e.getKey();
			Player.Action action = e.getValue();
			Ball.Aftertouch aftertouch = aftertouchLookup.get(key);
			boolean lastActive = lastKeys.contains(key);
			boolean active = Keyboard.isKeyDown(key);
			if (lastActive && !active) {
				actions.remove(action);
				aftertouches.remove(aftertouch);
				change = true;
				lastKeys.remove(key);
			} else if (!lastActive && active) {
				actions.add(action);
				aftertouches.add(aftertouch);
				change = true;
				lastKeys.add(key);
			} else if (active) {
				actions.add(action);
				aftertouches.add(aftertouch);
			}
		}
		if (change) {
			game.setUserActions(actions, aftertouches);
		}
	}
}
