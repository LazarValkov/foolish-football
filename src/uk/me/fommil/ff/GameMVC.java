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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
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

	@Deprecated
	private static final int PITCH_WIDTH = 672;
	@Deprecated
	private static final int PITCH_HEIGHT = 880;
	private static final Logger log = Logger.getLogger(GameMVC.class.getName());
	private final long PERIOD = 100L;
	private final Team a;
	private final BallMC ball;
	private final List<PlayerMC> as = Lists.newArrayListWithCapacity(11);
	private final AtomicLong ticks = new AtomicLong();
	private final TimerTask ticker = new TimerTask() {

		@Override
		public synchronized void run() {
			ticks.incrementAndGet();
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
	private final BufferedImage pitch;
	private final Map<Integer, Sprite> sprites = Maps.newHashMap();

	/**
	 * @param a
	 * @param b
	 * @param pitch
	 * @param sprites
	 */
	public GameMVC(Team a, Team b, BufferedImage pitch, Map<Integer, Sprite> sprites) {
		this.pitch = pitch;
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

		Map<Color, Color> teamColours = a.getTeamColors();
		for (int i = 0; i < 76; i++) {
			this.sprites.put(i, sprites.get(i + 341).copyWithReplace(teamColours));
		}
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		Dimension size = getSize();
		double w = size.getWidth();
		double h = size.getHeight();

//		g2.setBackground(Color.BLACK);
//		g2.clearRect(0, 0, size.width, size.height);
		g2.setColor(Color.GREEN);

		// we are always centred over the ball
		Point3d ballLoc = ball.getPosition();

		// view never goes outside the pitch image
		int xoff = (int) Math.min(pitch.getWidth() - size.width, Math.max(0, ballLoc.x - w / 2.0));
		int yoff = (int) Math.min(pitch.getHeight() - size.height, Math.max(0, ballLoc.y - h / 2.0));
		assert xoff >= 0 && yoff >= 0 : xoff + " " + yoff;
		assert xoff < pitch.getWidth() && yoff < pitch.getHeight() : xoff + " " + yoff;
		Rectangle2D view = new Rectangle.Double(xoff, yoff, w, h);

		// draw the pitch
		// TODO: consider case when the window is bigger than the pitch image
		g2.drawImage(pitch.getSubimage(xoff, yoff, size.width, size.height), 0, 0, null);

		for (int i = 0; i <= 5; i++) {
			int x = 81 + i * (590 - 81) / 5;
			g2.drawLine(x - xoff, 129 - yoff, x - xoff, 769 - yoff);
		}
		for (int i = 0; i <= 7; i++) {
			int y = 129 + i * (769 - 129) / 7;
			g2.drawLine(81 - xoff, y - yoff, 590 - xoff, y - yoff);
		}

		// draw the players that are in view
		for (PlayerMC pm : as) {
			Shape b = pm.getViewBounds();
			if (b.intersects(view))
				draw(g2, view, pm);
		}

		// draw the ball; FIXME view might not be centre, consider physics of height
		// and false perspective also
		int cx = (int) (ballLoc.x - xoff);
		int cy = (int) (ballLoc.y - yoff);
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

	// draw a player
	private void draw(Graphics2D g, Rectangle2D view, PlayerMC pm) {
		Point3d pos = pm.getPosition();
		// p is the pixel location of the centre of the player
		Point p = new Point((int) Math.round(pos.x - view.getX()), (int) Math.round(pos.y - view.getY()));

//		g.setColor(Color.WHITE);
//		g.drawOval(p.x - 4, p.y - 4, 9, 9);

//		Point kv = pm.getStep();
//		g.drawLine(p.x, p.y, kv.x + p.x, kv.y + p.y);

//		if (pm == selectedA) {
//			// draw the bounds, scattergun and not efficient
//			BoundingPolytope bounds = pm.getBounds();
//			for (int x = p.x - 50; x < p.x + 50; x++) {
//				for (int y = p.y - 50; y < p.y + 50; y++) {
//					Point3d test = new Point3d(x + view.getX(), y + view.getY(), 0); // TODO: use ball height
//					if (bounds.intersect(test))
//						g.drawLine(x, y, x, y);
//				}
//			}
//		}

		int spriteIndex;
		double angle = pm.getAngle();
		if (angle <= - 3 * Math.PI / 4) {
			spriteIndex = 12;
		} else if (angle <= -Math.PI / 2) {
			spriteIndex = 9;
		} else if (angle <= -Math.PI / 4) {
			spriteIndex = 18;
		} else if (angle <= 0) {
			spriteIndex = 0;
		} else if (angle <= Math.PI / 4) {
			spriteIndex = 21;
		} else if (angle <= Math.PI / 2) {
			spriteIndex = 6;
		} else if (angle <= 3 * Math.PI / 4) {
			spriteIndex = 15;
		} else {
			spriteIndex = 3;
		}

		// 0/+1/+2 depending on timestamp and motion
		if (pm.getVelocity().length() > 0) {
			long t = (ticks.get() * PERIOD) % 500L;
			if (t < 250) {
				spriteIndex += 1;
			} else {
				spriteIndex += 2;
			}
		}

		Sprite sprite = sprites.get(spriteIndex);
		Point s = sprite.getCentre();
		g.drawImage(sprite.getImage(), p.x - s.x / 2 - 1, p.y - s.y / 2, null);

		if (pm == selectedA) {
			g.setColor(Color.WHITE);
			String shirt = Integer.toString(pm.getShirt());
			FontMetrics fm = getFontMetrics(getFont());
			Rectangle2D textsize = fm.getStringBounds(shirt, g);
			g.drawString(shirt, Math.round(p.x - textsize.getWidth() / 2), p.y - 10);
		}
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
