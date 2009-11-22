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

import static java.lang.Math.abs;
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
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Bounds;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import uk.me.fommil.ff.Tactics.BallZone;

/**
 * The model (M), view (V) and controller (C) for the game play.
 * 
 * @author Samuel Halliday
 */
@SuppressWarnings("serial")
public class GameMVC extends JPanel {

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
//		this.b = b;
		this.ball = new BallMC();
		// create models for each of the team's players
		List<Player> aPlayers = a.getPlayers();
//		List<Player> bPlayers = b.getPlayers();
		BallZone centre = new Tactics.BallZone(2, 3);
		for (int i = 2; i <= 11; i++) {
			Point p = a.getCurrentTactics().getZone(centre, i).getLocation(true, 400, 600);

			PlayerMC pma = new PlayerMC(i, aPlayers.get(i - 2));
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
		Point ballLoc = ball.getLocation();

		Rectangle2D view = new Rectangle.Double(ballLoc.getX() - w / 2, ballLoc.getY() - h / 2, w, h);

		// TODO: draw the pitch

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
		PlayerMC pm = updateSelected(team, actions);

		// TODO: consider context of when action is to change the selected player
		pm.setActions(actions);
		// ball.setAftertouches(actions);

		// clear actions for all non-interactive models
		// ?? this is where the AI for each model would be called
		for (PlayerMC p : as) {
			if (p != pm)
				p.clearActions();
		}
	}
	// get the selected player for the given team
	private PlayerMC selectedA = null;

	private PlayerMC updateSelected(Team team, Collection<PlayerMC.Action> actions) {
		assert team == a;
		if (selectedA == null)
			selectedA = as.get(9);

		if (!actions.contains(PlayerMC.Action.KICK))
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

	// HACK: should really ask view for the sprite
	private void draw(Graphics2D g, Rectangle2D view, PlayerMC pm) {
		int xoff = (int) view.getX();
		int yoff = (int) view.getY();
		Point p = pm.getLocation();
		g.drawOval(p.x - 4 - xoff, p.y - 4 - yoff, 9, 9);

		Point kv = pm.getStep();
		g.drawLine(p.x - xoff, p.y - yoff, kv.x + p.x - xoff, kv.y + p.y - yoff);

		BoundingBox bounds = (BoundingBox) pm.getBounds();
		Point3d upper = new Point3d();
		Point3d lower = new Point3d();
		bounds.getUpper(upper);
		bounds.getLower(lower);
		

		int x1 = (int) Math.round(lower.x - xoff);
		int y1 = (int) Math.round(lower.y - yoff);

		int x4 = (int) Math.round(upper.x - xoff);
		int y4 = (int) Math.round(upper.y - yoff);
		g.drawPolygon(new int[]{x1, x4}, new int[]{y1, y4}, 2);
	}

	private void updatePhysics() {
		ball.tick(PERIOD / 1000.0);

		// sprite collision detection for ball movement and player states
		// detect who has rights to the ball
		List<PlayerMC> candidate = Lists.newArrayList();
		for (PlayerMC pm : as) {
			Bounds pmb = pm.getBounds();
			if (pm.getPosition().distance(ball.getPosition()) < 20 && !pmb.intersect(ball.getBounds())) {
				log.warning(pmb + " " + ball.getPosition());
			}
			if (pmb.intersect(ball.getBounds())) {
				log.info("POTENTIAL OWNER " + pm);
				candidate.add(pm);
			}
		}
		// TODO: better resolution of contended owner (e.g. by skill, tackling state)
		if (!candidate.isEmpty()) {
			PlayerMC owner = candidate.get(new Random().nextInt(candidate.size()));
			// always give control to the operator
			// FIXME: delay when handing over control
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
	}
}
