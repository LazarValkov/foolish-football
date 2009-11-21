/*
 * Copyright Samuel Halliday 2009
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

import static java.lang.Math.round;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import uk.me.fommil.ff.Tactics.BallZone;

/**
 * The model (M), View (V) and controller (C) for the game play.
 * 
 * @author Samuel Halliday
 */
@SuppressWarnings("serial")
public class GameMVC extends JPanel {

	private static final Logger log = Logger.getLogger(GameMVC.class.getName());
	private final long PERIOD = 100L;

	/**
	 * the full list of actions we can expect from each team
	 */
	public enum Action {

		UP, DOWN, LEFT, RIGHT, BUTTON_A, BUTTON_B
	};
	private final Team a;
//	private final Team b;
	private final BallMC ball;
	private final List<PlayerMC> as = Lists.newArrayListWithCapacity(11);
//	private final List<PlayerModel> bs = Lists.newArrayListWithCapacity(11);
//	private final Tactics bTactics;
	private final TimerTask ticker = new TimerTask() {

		@Override
		public synchronized void run() {
			updatePhysics();
			repaint();
		}
	};
	private final KeyListener keyboardInput = new KeyAdapter() {

		private final Collection<Action> actions = Sets.newHashSet();

		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					actions.add(Action.LEFT);
					break;
				case KeyEvent.VK_RIGHT:
					actions.add(Action.RIGHT);
					break;
				case KeyEvent.VK_UP:
					actions.add(Action.UP);
					break;
				case KeyEvent.VK_DOWN:
					actions.add(Action.DOWN);
					break;
				case KeyEvent.VK_SPACE:
					actions.add(Action.BUTTON_A);
					break;
				case KeyEvent.VK_ENTER:
					actions.add(Action.BUTTON_B);
					break;
				default:
					return;
			}
			setActions(GameMVC.this.a, actions);
		}

		@Override
		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					actions.remove(Action.LEFT);
					break;
				case KeyEvent.VK_RIGHT:
					actions.remove(Action.RIGHT);
					break;
				case KeyEvent.VK_UP:
					actions.remove(Action.UP);
					break;
				case KeyEvent.VK_DOWN:
					actions.remove(Action.DOWN);
					break;
				case KeyEvent.VK_SPACE:
					actions.remove(Action.BUTTON_A);
					break;
				case KeyEvent.VK_ENTER:
					actions.remove(Action.BUTTON_B);
					break;
				default:
					return;
			}
			setActions(GameMVC.this.a, actions);
		}
	};

	/**
	 * @param a
	 * @param b
	 */
	public GameMVC(Team a, Team b) {
		this.a = a;
//		this.b = b;
		this.ball = new BallMC();
		// create models for each of the team's players
		List<Player> aPlayers = a.getPlayers();
//		List<Player> bPlayers = b.getPlayers();
		BallZone centre = new Tactics.BallZone(2, 3);
		for (int i = 1; i < 11; i++) {
			Point p = a.getCurrentTactics().getZone(centre, i + 1).getLocation(true, 400, 600);

			PlayerMC pma = new PlayerMC(i, aPlayers.get(i));
			pma.setLocation(p);
			as.add(pma);
		}

		// HACK: eventually take in input controls
		// needs to get focus for keys
		// TODO: calculate who pressed the key and the associated action
		setFocusable(true);
		addKeyListener(keyboardInput);
		new Timer().schedule(ticker, 0L, PERIOD);
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.BLACK);

		Dimension size = getSize();
		double w = size.getWidth();
		double h = size.getHeight();
		g2.clearRect(0, 0, (int) w, (int) h);
		g2.setColor(Color.GREEN);

		// we are always centred over the ball
		Point2D ballLoc = ball.getLocation();

		Rectangle2D view = new Rectangle.Double(ballLoc.getX() - w / 2, ballLoc.getY() - h / 2, w, h);

		// TODO: draw the pitch

		// draw the players that are in view
		for (PlayerMC pm : as) {
			Rectangle2D b = pm.getBounds();
			if (view.intersects(b))
				draw(g2, view, pm);
		}

		// draw the ball
		int cx = (int) Math.round(w / 2);
		int cy = (int) Math.round(h / 2);
		int d = ((int)Math.round(ball.getHeight() * 2) + 5);
		g2.fillOval(cx - d/2, cy - d/2, d, d);
	}

	private void setActions(Team team, Collection<Action> actions) {
		PlayerMC pm = updateSelected(team, actions);

		// TODO: consider context of when action is to change the selected player
		pm.setActions(actions);
		ball.setActions(actions);

		// clear actions for all non-interactive models
		// ?? this is where the AI for each model would be called
		for (PlayerMC p : as) {
			if (p != pm)
				p.setActions(Collections.<Action>emptySet());
		}
	}

	// get the selected player for the given team
	private PlayerMC selectedA = null;
	private PlayerMC updateSelected(Team team, Collection<Action> actions) {
		assert team == a;
		if (selectedA == null)
			selectedA = as.get(9);

		if (!actions.contains(Action.BUTTON_A))
			return selectedA;

		// set the closed player
		PlayerMC closest = selectedA;
		double distance = selectedA.getLocation().distanceSq(ball.getLocation());
		for (PlayerMC model : as) {
			double ds2 = model.getLocation().distanceSq(ball.getLocation());
			if (ds2 < distance) {
				distance = ds2;
				closest = model;
			}
		}
		selectedA = closest;
		return selectedA;
	}

	// HACK: should really ask model for the sprite
	private void draw(Graphics2D g, Rectangle2D view, PlayerMC pm) {
		int xoff = (int) view.getX();
		int yoff = (int) view.getY();
		Point2D p2d = pm.getLocation();
		Point p = new Point((int)round(p2d.getX()), (int)round(p2d.getY()));
		g.drawOval(p.x - 4 - xoff, p.y - 4 - yoff, 9, 9);
//		Point2D kv2d = pm.getVelocity();
//		if (kv2d == null || (kv2d.getX() == 0 && kv2d.getY() == 0))
//			return;
//		Point kv = new Point((int)round(kv2d.getX()), (int)round(kv2d.getY()));
//		g.drawLine(p.x - xoff, p.y - yoff, kv.x + p.x - xoff, kv.y + p.y - yoff);
	}

	private void updatePhysics() {
		// do sprite collision detection for ball movement and player states

		// detect who has rights to the ball
		List<PlayerMC> candidate = Lists.newArrayList();
		for (PlayerMC pm : as) {
			Shape b = pm.getControlBounds();
			if (ball.getHeight() < 2 && b.contains(ball.getLocation())) {
				log.info("POTENTIAL OWNER " + pm);
				candidate.add(pm);
			}
		}
		// TODO: better resolution of contended owner (e.g. by skill)
		if (!candidate.isEmpty()) {
			PlayerMC owner = candidate.get(new Random().nextInt(candidate.size()));
			if (owner.isKicking()) {
				// log.info("KICK");
				Point3d kick = owner.getVelocity();
				kick.scale(3);
				kick.z = 1;
				ball.setVelocity(kick);
			} else {
				// dribble the ball
				//log.info("DRIBBLE");
				Point3d kick = owner.getVelocity();
				ball.setVelocity(kick);
			}
		}

		// TODO: tackling

		// TODO: headers

		// TODO: reset tackling states for others

		for (PlayerMC pm : as) {
			pm.setKicking(false);
			pm.tick(PERIOD / 1000.0);
		}
		ball.tick(PERIOD / 1000.0);
	}
}
