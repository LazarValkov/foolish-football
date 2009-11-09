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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JPanel;

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
		for (int i = 0; i < 11; i++) {
			Point p = Tactics.getKickOff().getLocation(i);

			PlayerModel pma = new PlayerModel(i, aPlayers.get(i));
			pma.setLocation(p);
			as.add(pma);
		}

		// needs to get focus for keys
		setFocusable(true);
		addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();

				// TODO: calculate who pressed the key and the associated action
				final Team team = GameView.this.a;
				switch (key) {
					case KeyEvent.VK_LEFT:
						performAction(team, Action.LEFT);
						break;
					case KeyEvent.VK_RIGHT:
						performAction(team, Action.RIGHT);
						break;
					case KeyEvent.VK_UP:
						performAction(team, Action.UP);
						break;
					case KeyEvent.VK_DOWN:
						performAction(team, Action.DOWN);
						break;
					case KeyEvent.VK_ENTER:
						performAction(team, Action.BUTTON_A);
						break;
					case KeyEvent.VK_SPACE:
						performAction(team, Action.BUTTON_B);
						break;
					default:
						return;
				}
				repaint();
			}
		});
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.BLACK);

		Dimension size = getSize();
		double w = size.getWidth();
		double h = size.getHeight();
		g2.clearRect(0, 0, (int)w, (int)h);
		g2.setColor(Color.GREEN);

		// we are always centred over the ball
		Point ballLoc = ball.getLocation();

		Rectangle2D view = new Rectangle.Double(ballLoc.x - w / 2, ballLoc.y - h / 2, w, h);


		// TODO: draw the pitch

		// draw the players that are in view
		for (PlayerModel pm : as) {
			Rectangle2D b = pm.getBounds();
			// TODO: investigate why intersection of rectangles returns false sometimes
			if (view.intersects(b))
				draw(g2, pm);
		}

		// draw the ball
		int cx = (int) Math.round(w / 2);
		int cy = (int) Math.round(h / 2);
		g2.fillOval(cx - 2, cy - 2, 5, 5);
	}

	private void performAction(Team team, Action action) {
		PlayerModel pm = getSelected(team);

		// TODO: consider context of when action is to change the selected player

		pm.performAction(action);
	}

	// get the selected player for the given team
	private PlayerModel getSelected(Team team) {
		if (team == a)
			return as.get(1);
		// assert team == b;
		// return bs.get(1);
		throw new AssertionError();
	}

	private void draw(Graphics2D g, PlayerModel pm) {
		// HACK
		Point p = new Point(pm.getLocation());
		g.drawOval(p.x - 4, p.y - 4, 9, 9);
		Point ds = pm.getLastStep();
		if (ds == null)
			return;
		g.drawLine(p.x, p.y, p.x + ds.x, p.y + ds.y);
	}
}
