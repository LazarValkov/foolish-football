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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.swing.JPanel;
import uk.me.fommil.ff.Tactics.BallZone;

/**
 * The overlord class for visualising game play.
 * 
Regarding sprites and reading the MAP files for the pitch.
from http://eab.abime.net/showthread.php?p=561488
if you are ripping from SWOS , its a lot easier to just use the datafiles in the game and convert them to iff

they are all dimensions; 352,272,16 (colours) or 320,256,16 , and are stored as raw interleaved files which need fist decrunching (RNC packed with propack)

once converted you need only add a palette (which you can rip with action replay or similar)

the only exceptions being the .MAP files, which you will have to calcuate the dimension size for. they are all 16 pixels wide and 3000+ pixels tall.

tiles:
off: 16188
x 16
y 16
Bit 4
Pal E 660
Mode ST

sprites:
Off 224284
x 16
y 132 (whatever you want but bigger then 16)
Bit 4
Pal E 660
Mode ST

those are tricky, you don't get a nice clear sheet but you can rip single sprites and need to change for other "teamsprites" use left and right cursor to swap around until
you see the actual sprites:

eg:
same settings as above but off: 245174

fonts: 190776
 * @author Samuel Halliday
 */
@SuppressWarnings("serial")
public class GameView extends JPanel {

	private static final Logger log = Logger.getLogger(GameView.class.getName());
	private final long PERIOD = 100L;

	/**
	 * the full list of actions we can expect from each team
	 */
	public enum Action {

		UP, DOWN, LEFT, RIGHT, BUTTON_A, BUTTON_B
	};
	private final Team a;
//	private final Team b;
	private final BallModel ball;
	private final List<PlayerModel> as = Lists.newArrayListWithCapacity(11);
//	private final List<PlayerModel> bs = Lists.newArrayListWithCapacity(11);
	private final Tactics aTactics;
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
			setActions(GameView.this.a, actions);
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
			setActions(GameView.this.a, actions);
		}
	};

	/**
	 * @param a
	 * @param b
	 */
	public GameView(Team a, Team b) {
		this.a = a;
//		this.b = b;
		this.ball = new BallModel();
		aTactics = a.getCurrentTactics();
//		bTactics = b.getCurrentTactics();
		// create models for each of the team's players
		List<Player> aPlayers = a.getPlayers();
//		List<Player> bPlayers = b.getPlayers();
		BallZone centre = new Tactics.BallZone(2, 3);
		for (int i = 1; i < 11; i++) {
			Point p = a.getCurrentTactics().getZone(centre, i + 1).getLocation(true, 400, 600);

			PlayerModel pma = new PlayerModel(i, aPlayers.get(i));
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
		for (PlayerModel pm : as) {
			Rectangle2D b = pm.getBounds();
			if (view.intersects(b))
				draw(g2, view, pm);
		}

		// draw the ball
		int cx = (int) Math.round(w / 2);
		int cy = (int) Math.round(h / 2);
		g2.fillOval(cx - 2, cy - 2, 5, 5);
	}

	private void setActions(Team team, Collection<Action> actions) {
		PlayerModel pm = getSelected(team);

		// TODO: consider context of when action is to change the selected player

		pm.setActions(actions);
	}

	// get the selected player for the given team
	private PlayerModel getSelected(Team team) {
		if (team == a)
			return as.get(1);
		// assert team == b;
		// return bs.get(1);
		throw new AssertionError();
	}

	// HACK: should really ask model for the sprite
	private void draw(Graphics2D g, Rectangle2D view, PlayerModel pm) {
		int xoff = (int) view.getX();
		int yoff = (int) view.getY();
		Point p = new Point(pm.getLocation());
		g.drawOval(p.x - 4 - xoff, p.y - 4 - yoff, 9, 9);
		Point kv = pm.getLastStep();
		if (kv == null)
			return;
		g.drawLine(p.x - xoff, p.y - yoff, kv.x + p.x - xoff, kv.y + p.y - yoff);
	}

	private void updatePhysics() {
		// do sprite collision detection for ball movement and player states

		// detect who has rights to the ball
		List<PlayerModel> candidate = Lists.newArrayList();
		for (PlayerModel pm : as) {
			Shape b = pm.getControlBounds();
			if (b.contains(ball.getLocation())) {
				log.info("POTENTIAL OWNER");
				candidate.add(pm);
			}
		}
		// FIXME: resolution of contended owner
		if (!candidate.isEmpty()) {
			PlayerModel owner = candidate.get(new Random().nextInt(candidate.size()));
			if (owner.isKicking()) {
				log.info("KICK");
				Point direction = owner.getLastStep();
				ball.setVelocity(direction);
			}
		}

		// TODO: reset tackling states for others

		for (PlayerModel pm : as) {
			pm.setKicking(false);
			pm.tick(PERIOD);
		}
		ball.tick(PERIOD);
	}
}
