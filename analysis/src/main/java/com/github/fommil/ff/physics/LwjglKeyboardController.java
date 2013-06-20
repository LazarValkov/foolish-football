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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import org.lwjgl.input.Keyboard;

/**
 * Keyboard listening is synchronous, not asynchronous, with LWJGL - this class provides the poll.
 *
 * @author Samuel Halliday
 */
class LwjglKeyboardController {

	private static final Multimap<Integer, Action> actionLookup = HashMultimap.create();

	private static final Map<Integer, Aftertouch> aftertouchLookup = Maps.newHashMap();

	static {
		actionLookup.put(Keyboard.KEY_UP, Action.UP);
		actionLookup.put(Keyboard.KEY_DOWN, Action.DOWN);
		actionLookup.put(Keyboard.KEY_LEFT, Action.LEFT);
		actionLookup.put(Keyboard.KEY_RIGHT, Action.RIGHT);
		actionLookup.put(Keyboard.KEY_SPACE, Action.KICK);
		actionLookup.put(Keyboard.KEY_SPACE, Action.CHANGE);
		actionLookup.put(Keyboard.KEY_RETURN, Action.TACKLE);
		actionLookup.put(Keyboard.KEY_A, Action.HEAD);

		aftertouchLookup.put(Keyboard.KEY_UP, Aftertouch.UP);
		aftertouchLookup.put(Keyboard.KEY_DOWN, Aftertouch.DOWN);
		aftertouchLookup.put(Keyboard.KEY_LEFT, Aftertouch.LEFT);
		aftertouchLookup.put(Keyboard.KEY_RIGHT, Aftertouch.RIGHT);
	}

	private final Collection<Integer> lastKeys = Sets.newHashSet();

	private final GamePhysics game;

	LwjglKeyboardController(GamePhysics game) {
		this.game = game;
	}

	synchronized void poll() {
		boolean change = false;
		Collection<Action> allActions = Sets.newHashSet();
		Collection<Aftertouch> aftertouches = Sets.newHashSet();
		for (Integer key : actionLookup.keySet()) {
			Collection<Action> theseActions = actionLookup.get(key);
			Aftertouch aftertouch = aftertouchLookup.get(key);
			boolean lastActive = lastKeys.contains(key);
			boolean active = Keyboard.isKeyDown(key);
			if (lastActive && !active) {
				allActions.removeAll(theseActions);
				aftertouches.remove(aftertouch);
				change = true;
				lastKeys.remove(key);
			} else if (!lastActive && active) {
				allActions.addAll(theseActions);
				if (aftertouch != null)
					aftertouches.add(aftertouch);
				change = true;
				lastKeys.add(key);
			} else if (active) {
				allActions.addAll(theseActions);
				if (aftertouch != null)
					aftertouches.add(aftertouch);
			}
		}
		if (change) {
			game.setUserActions(allActions, aftertouches);
		}
	}
}
