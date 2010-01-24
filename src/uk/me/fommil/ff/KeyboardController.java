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

import com.google.common.collect.Sets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import uk.me.fommil.ff.physics.Ball;
import uk.me.fommil.ff.physics.GamePhysics;
import uk.me.fommil.ff.physics.Player;

/**
 *
 * @author Samuel Halliday
 */
public class KeyboardController extends KeyAdapter {

	private final GamePhysics game;

	public KeyboardController(GamePhysics game) {
		this.game = game;
	}

	private final Collection<Player.Action> actions = Sets.newHashSet();

	private final Collection<Ball.Aftertouch> aftertouches = Sets.newHashSet();

	@Override
	public synchronized void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				actions.add(Player.Action.LEFT);
				aftertouches.add(Ball.Aftertouch.LEFT);
				break;
			case KeyEvent.VK_RIGHT:
				actions.add(Player.Action.RIGHT);
				aftertouches.add(Ball.Aftertouch.RIGHT);
				break;
			case KeyEvent.VK_UP:
				actions.add(Player.Action.UP);
				aftertouches.add(Ball.Aftertouch.UP);
				break;
			case KeyEvent.VK_DOWN:
				actions.add(Player.Action.DOWN);
				aftertouches.add(Ball.Aftertouch.DOWN);
				break;
			case KeyEvent.VK_SPACE:
				actions.add(Player.Action.KICK);
				break;
			case KeyEvent.VK_ENTER:
				actions.add(Player.Action.TACKLE);
				break;
			case KeyEvent.VK_A:
				actions.add(Player.Action.HEAD);
				break;
			default:
				return;
		}
		updateActions();
	}

	@Override
	public synchronized void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				actions.remove(Player.Action.LEFT);
				aftertouches.remove(Ball.Aftertouch.LEFT);
				break;
			case KeyEvent.VK_RIGHT:
				actions.remove(Player.Action.RIGHT);
				aftertouches.remove(Ball.Aftertouch.RIGHT);
				break;
			case KeyEvent.VK_UP:
				actions.remove(Player.Action.UP);
				aftertouches.remove(Ball.Aftertouch.UP);
				break;
			case KeyEvent.VK_DOWN:
				actions.remove(Player.Action.DOWN);
				aftertouches.remove(Ball.Aftertouch.DOWN);
				break;
			case KeyEvent.VK_SPACE:
				actions.remove(Player.Action.KICK);
				break;
			case KeyEvent.VK_ENTER:
				actions.remove(Player.Action.TACKLE);
				break;
			case KeyEvent.VK_A:
				actions.remove(Player.Action.HEAD);
				break;
			case KeyEvent.VK_ESCAPE:
				System.exit(0);
				break;
			default:
				return;
		}
		updateActions();
	}

	private void updateActions() {
		game.setUserActions(actions, aftertouches);
	}
}
