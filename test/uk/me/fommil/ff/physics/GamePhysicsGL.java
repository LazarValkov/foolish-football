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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.lwjgl.input.Keyboard;
import org.ode4j.drawstuff.DrawStuff;
import org.ode4j.drawstuff.DrawStuff.dsFunctions;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DSphere;
import uk.me.fommil.ff.KeyboardController;
import uk.me.fommil.ff.Main;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.Team;
import uk.me.fommil.ff.swos.TacticsParser;

/**
 * A debugging version of the game using a 3D rendered view.
 *
 * @author Samuel Halliday
 */
public class GamePhysicsGL extends dsFunctions {

	private static final Logger log = Logger.getLogger(GamePhysicsGL.class.getName());

	/** @param args */
	public static final void main(String[] args) throws Exception {
		final int width = 800;
		final int height = 600;
		Team a = new Team();
		a.setCurrentTactics(TacticsParser.getSwosTactics(Main.SWOS).get("442"));
		Pitch pitch = new Pitch();
		GamePhysics game = new GamePhysics(a, pitch);

		GamePhysicsGL demo = new GamePhysicsGL(game);
		DrawStuff.dsSimulationLoop(args, width, height, demo);
	}

	private final GamePhysics game;

	private final KeyboardController controller;

	private GamePhysicsGL(GamePhysics game) {
		this.game = game;
		this.controller = new KeyboardController(game);
	}

	@Override
	public void start() {
	}

	@Override
	public void step(boolean pause) {
		gatherActions();

		game.tick(0.01);

		Position c = game.getBall().getPosition();
		float[] xyz = {(float) c.x, (float) c.y + 5, 20f};
		float[] hpr = {-90, -70, 0};
		DrawStuff.dsSetViewpoint(xyz, hpr);

		for (Player player : game.getPlayers()) {
			DrawStuff.dsSetColor(1, 0, 0);
			DBox box = (DBox) player.getGeometry();
			DrawStuff.dsDrawBox(box.getPosition(), box.getRotation(), box.getLengths());
		}

		DrawStuff.dsSetColor(1, 1, 1);
		DSphere sphere = (DSphere) game.getBall().getGeometry();
		DrawStuff.dsDrawSphere(sphere.getPosition(), sphere.getRotation(), (float) sphere.getRadius());
//		DBox sphere = (DBox) game.getBall().getGeometry();
//		DrawStuff.dsDrawBox(sphere.getPosition(), sphere.getRotation(), sphere.getLengths());
	}

	@Override
	public void command(char cmd) {
	}

	@Override
	public void stop() {
	}

	private static final Map<Integer, Player.Action> inputs = Maps.newHashMap();

	static {
		inputs.put(Keyboard.KEY_UP, Player.Action.UP);
		inputs.put(Keyboard.KEY_DOWN, Player.Action.DOWN);
		inputs.put(Keyboard.KEY_LEFT, Player.Action.LEFT);
		inputs.put(Keyboard.KEY_RIGHT, Player.Action.RIGHT);
		inputs.put(Keyboard.KEY_SPACE, Player.Action.KICK);
		inputs.put(Keyboard.KEY_RETURN, Player.Action.TACKLE);
		inputs.put(Keyboard.KEY_A, Player.Action.HEAD);
	}

	private final Collection<Integer> lastKeys = Sets.newHashSet();

	private void gatherActions() {
		boolean change = false;
		Collection<Player.Action> actions = Sets.newHashSet();
		for (Entry<Integer, Player.Action> e : inputs.entrySet()) {
			int key = e.getKey();
			Player.Action action = e.getValue();
			boolean lastActive = lastKeys.contains(key);
			boolean active = Keyboard.isKeyDown(key);
			if (lastActive && !active) {
				actions.remove(action);
				change = true;
				lastKeys.remove(key);
			} else if (!lastActive && active) {
				actions.add(action);
				change = true;
				lastKeys.add(key);
			} else if (active) {
				actions.add(action);
			}
		}
		if (change) {
			// TODO: aftertouches
			game.setUserActions(actions, null);
		}
	}
}
