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
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.media.j3d.BoundingPolytope;
import javax.media.j3d.Bounds;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import uk.me.fommil.ff.Tactics.BallZone;
import uk.me.fommil.ff.Tactics.PlayerZone;

/**
 * The model (M), view (V) and controller (C) for the game play.
 * 
 * @author Samuel Halliday
 */
@SuppressWarnings("serial")
public class GameMVC extends JPanel {

	private static final int PITCH_WIDTH = 400;
	private static final int PITCH_HEIGHT = 600;
	private static final Logger log = Logger.getLogger(GameMVC.class.getName());
	private final long PERIOD = 100L;
	private final Team a;
	private final BallMC ball;
	private final List<PlayerMC> as = Lists.newArrayListWithCapacity(11);
	private final TimerTask ticker = new TimerTask() {

		@Override
		public synchronized void run() {
			updatePhysics();
			repaint();
		}
	};
	private final KeyListener keyboardInput = new KeyAdapter() {

		private final Collection<PlayerMC.Action> actions = Sets.newHashSet();

		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					actions.add(PlayerMC.Action.LEFT);
					break;
				case KeyEvent.VK_RIGHT:
					actions.add(PlayerMC.Action.RIGHT);
					break;
				case KeyEvent.VK_UP:
					actions.add(PlayerMC.Action.UP);
					break;
				case KeyEvent.VK_DOWN:
					actions.add(PlayerMC.Action.DOWN);
					break;
				case KeyEvent.VK_SPACE:
					actions.add(PlayerMC.Action.KICK);
					break;
				case KeyEvent.VK_ENTER:
					actions.add(PlayerMC.Action.TACKLE);
					break;
				default:
					return;
			}
			setPlayerActions(GameMVC.this.a, actions);
		}

		@Override
		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					actions.remove(PlayerMC.Action.LEFT);
					break;
				case KeyEvent.VK_RIGHT:
					actions.remove(PlayerMC.Action.RIGHT);
					break;
				case KeyEvent.VK_UP:
					actions.remove(PlayerMC.Action.UP);
					break;
				case KeyEvent.VK_DOWN:
					actions.remove(PlayerMC.Action.DOWN);
					break;
				case KeyEvent.VK_SPACE:
					actions.remove(PlayerMC.Action.KICK);
					break;
				case KeyEvent.VK_ENTER:
					actions.remove(PlayerMC.Action.TACKLE);
					break;
				default:
					return;
			}
			setPlayerActions(GameMVC.this.a, actions);
		}
	};

	/**
	 * @param a
	 * @param b
	 */
	public GameMVC(Team a, Team b) {
		this.a = a;
		this.ball = new BallMC();
		ball.setPosition(new Point3d(PITCH_WIDTH / 2, PITCH_HEIGHT / 2, 0));
		BallZone bz = ball.getZone(PITCH_WIDTH, PITCH_HEIGHT);
		List<Player> aPlayers = a.getPlayers();
		Tactics tactics = a.getCurrentTactics();
		for (int i = 2; i <= 11; i++) {
			Point3d p = tactics.getZone(bz, i).getCentre(true, PITCH_WIDTH, PITCH_HEIGHT);
			PlayerMC pma = new PlayerMC(i, aPlayers.get(i - 2));
			pma.setPosition(p);
			as.add(pma);
		}
		selectedA = as.get(9);

		// HACK: eventually take in input controls
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
		Point3d ballLoc = ball.getPosition();

		Rectangle2D view = new Rectangle.Double(ballLoc.x - w / 2, ballLoc.y - h / 2, w, h);
		int xoff = (int) view.getX();
		int yoff = (int) view.getY();

		// TODO: draw the pitch
		for (int i = 0; i <= 5; i++) {
			int x = i * PITCH_WIDTH / 5;
			g2.drawLine(x - xoff, 0 - yoff, x - xoff, PITCH_HEIGHT - yoff);
		}
		for (int i = 0; i <= 7; i++) {
			int y = i * PITCH_HEIGHT / 7;
			g2.drawLine(0 - xoff, y - yoff, PITCH_WIDTH - xoff, y - yoff);
		}

		// draw the players that are in view
		for (PlayerMC pm : as) {
			Shape b = pm.getViewBounds();
			if (b.intersects(view))
				draw(g2, view, pm);
		}

		// draw the ball
		int cx = (int) Math.round(w / 2);
		int cy = (int) Math.round(h / 2);
		int d = ((int) Math.round(ball.getPosition().z * 2) + 5);
		g2.fillOval(cx - d / 2, cy - d / 2, d, d);
	}

	private void setPlayerActions(Team team, Collection<PlayerMC.Action> actions) {
		updateSelected(team, actions);
		selectedA.setActions(actions);
		// ball.setAftertouches(actions);
	}
	// get the selected player for the given team
	private PlayerMC selectedA = null;

	private void updateSelected(Team team, Collection<PlayerMC.Action> actions) {
		assert team == a;
		assert selectedA != null;

		if (!actions.contains(PlayerMC.Action.KICK))
			return;

		// set the closed player
		PlayerMC closest = selectedA;
		double distance = selectedA.getPosition().distanceSquared(ball.getPosition());
		for (PlayerMC model : as) {
			double ds2 = model.getPosition().distanceSquared(ball.getPosition());
			if (ds2 < distance) {
				distance = ds2;
				closest = model;
			}
		}
		selectedA = closest;
	}

	// HACK: should really ask view for the sprite
	private void draw(Graphics2D g, Rectangle2D view, PlayerMC pm) {
		if (pm == selectedA)
			g.setColor(Color.WHITE);
		else
			g.setColor(Color.GREEN);

		int xoff = (int) view.getX();
		int yoff = (int) view.getY();
		Point p = pm.getLocation();
		g.drawOval(p.x - 4 - xoff, p.y - 4 - yoff, 9, 9);

		Point kv = pm.getStep();
		g.drawLine(p.x - xoff, p.y - yoff, kv.x + p.x - xoff, kv.y + p.y - yoff);

		// draw the bounds, scattergun and not efficient
		BoundingPolytope bounds = pm.getBounds();
		for (int x = p.x - 50; x < p.x + 50; x++) {
			for (int y = p.y - 50; y < p.y + 50; y++) {
				Point3d test = new Point3d(x, y, 0);
				if (bounds.intersect(test))
					g.drawLine(x - xoff, y - yoff, x - xoff, y - yoff);
			}
		}

		g.drawString(Integer.toString(pm.getShirt()), p.x - xoff - 5, p.y - yoff - 10);
	}

	private void updatePhysics() {
		// autopilot
		BallZone bz = ball.getZone(PITCH_WIDTH, PITCH_HEIGHT);
		log.info("BALL " + bz);
		Tactics tactics = a.getCurrentTactics();
		for (PlayerMC p : as) {
			if (p != selectedA) {
				PlayerZone pz = tactics.getZone(bz, p.getShirt());
				Point3d target = pz.getCentre(true, PITCH_WIDTH, PITCH_HEIGHT);
				p.autoPilot(target);
			}
		}

		// sprite collision detection for ball movement and player states
		// detect who has rights to the ball
		List<PlayerMC> candidate = Lists.newArrayList();
		Point3d b = ball.getPosition();
		for (PlayerMC pm : as) {
			Bounds pmb = pm.getBounds();
			if (pm.getPosition().distance(b) < 100 && pmb.intersect(b)) {
				//log.info("POTENTIAL OWNER " + pm);
				candidate.add(pm);
			}
		}
		// TODO: better resolution of contended owner (e.g. by skill, tackling state)
		if (!candidate.isEmpty()) {
			PlayerMC owner = candidate.get(new Random().nextInt(candidate.size()));
			// always give control to the operator
			// TODO: fix delay when handing over control
			selectedA = owner;
			if (owner.isKicking()) {
				// kick the ball
				Vector3d kick = owner.getVelocity();
				kick.scale(3);
				kick.z = 1;
				ball.setVelocity(kick);
			} else {
				// dribble the ball
				Vector3d kick = owner.getVelocity();
				ball.setVelocity(kick);
			}
		}

		for (PlayerMC pm : as) {
			pm.tick(PERIOD / 1000.0);
		}
		ball.tick(PERIOD / 1000.0);
	}
}
