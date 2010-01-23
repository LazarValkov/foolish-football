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

import uk.me.fommil.ff.physics.Ball;
import uk.me.fommil.ff.physics.GamePhysics;
import uk.me.fommil.ff.physics.Player;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import static java.lang.Math.*;
import javax.swing.JPanel;
import uk.me.fommil.ff.physics.Goalkeeper;
import uk.me.fommil.ff.physics.Goalkeeper.GoalkeeperState;
import uk.me.fommil.ff.physics.Position;
import uk.me.fommil.ff.physics.Velocity;

/**
 * The view (V) for the game play.
 * <p>
 * There are various different coordinate systems in place when painting.
 * We use Hungarian Notation to keep us right, because there is no type safety for
 * {@code int} values.
 * <ul>
 * <li>{@code gVar}: The {@link Point} locations of the {@link Graphics2D} object.</li>
 * <li>{@code sVar}: The set of pixels on the screen of those {@link Point}s,
 *     the same as 'g' if there is no {@link AffineTransform} applied.</li>
 * <li>{@code pVar}: The {@link Point3d} position of the objects using our physics model.</li>
 * <li>{@code vVar}: The {@link Point} view of the physics model (essentially {@code Point}
 *     versions of the {@code Point3d} instances).</li>
 * </ul>

 * @author Samuel Halliday
 */
@SuppressWarnings("serial")
public class ClassicView extends JPanel {

	private static final Logger log = Logger.getLogger(ClassicView.class.getName());

	private final boolean debugging = false;

	private final int zoom = 4;

	private final Team a;

	private final BufferedImage pitch;

	private final Map<Integer, Sprite> teamSprites = Maps.newHashMap();

	private final Map<Integer, Sprite> ballSprites = Maps.newHashMap();

	private final Map<Integer, Sprite> teamNumberSprites = Maps.newHashMap();

	private final Map<Integer, Sprite> goalkeeperSprites = Maps.newHashMap();

	private final Map<Position, Sprite> objectSprites = Maps.newHashMap();

	private final KeyListener keyboardInput = new KeyAdapter() {

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
			game.setUserActions(ClassicView.this.a, actions, aftertouches);
		}
	};

	private final GamePhysics game;

	/**
	 * @param game
	 * @param pitch
	 * @param sprites
	 */
	public ClassicView(GamePhysics game, BufferedImage pitch, Map<Integer, Sprite> sprites) {
		this.pitch = pitch;
		this.a = game.getTeamA();
		this.game = game;

		// TODO: eventually take in input controls
		setFocusable(true);
		addKeyListener(keyboardInput);

		Map<Color, Color> teamColours = a.getTeamColors();
		for (int i = 0; i < 101; i++) {
			teamSprites.put(i, sprites.get(i + 341).copyWithReplace(teamColours));
		}
		for (int i = 0; i < 5; i++) {
			ballSprites.put(i, sprites.get(i + 1179));
		}
		for (int i = 0; i < 16; i++) {
			teamNumberSprites.put(i + 1, sprites.get(i + 162));
		}
		for (int i = 0; i < 57; i++) {
			goalkeeperSprites.put(i, sprites.get(i + 947));
		}

		// TODO: deal with nets/flags better
		objectSprites.put(new Position(300, 117, 0), sprites.get(1205));
		objectSprites.put(new Position(300, 764, 0), sprites.get(1206));
		log.info("1205 " + sprites.get(1205));
		log.info("1206 " + sprites.get(1206));

		// 1184, 1185, 1186, 1187
	}

	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;

		// TODO: intermediatery ScreenShot object to allow highlights

		// apply the zoom and calculate the portion of the screen we can see
		AffineTransform affine = new AffineTransform();
		affine.scale(zoom, zoom);
		g.setTransform(affine);
		Dimension gSize = new Dimension(getWidth() / zoom, getHeight() / zoom);

		// we are centred over the ball
		// g never goes outside the pitch image
		// TODO: consider making this variable a field
		Rectangle vBounds = calculateWindow(gSize);

		// draw the pitch
		BufferedImage sub = pitch.getSubimage(vBounds.x, vBounds.y,
				min(gSize.width, pitch.getWidth() + 1),
				min(gSize.height, pitch.getHeight() + 1));
		// extra padding is for when a partial pixel is shown
		g.drawImage(sub, 0, 0, null);

		// FIXME: draw all sprites top to bottom to get layering correct

		// draw the ball
		drawBall(g, vBounds);

		// draw the zones
		if (debugging) {
			g.setColor(Color.GREEN);
			for (int i = 0; i <= 5; i++) {
				int x = 81 + i * (590 - 81) / 5;
				Point start = pToG(vBounds, new Position(x, 129, 0));
				Point end = pToG(vBounds, new Position(x, 769, 0));
				g.drawLine(start.x, start.y, end.x, end.y);
			}
			for (int i = 0; i <= 7; i++) {
				int y = 129 + i * (769 - 129) / 7;
				Point start = pToG(vBounds, new Position(81, y, 0));
				Point end = pToG(vBounds, new Position(590, y, 0));
				g.drawLine(start.x, start.y, end.x, end.y);
			}
		}

		// draw the players that are in view
		for (Player pm : game.getPlayers()) {
			drawPlayer(g, vBounds, pm);
		}

		for (Goalkeeper gm : game.getGoalkeepers()) {
			drawGoalkeeper(g, vBounds, gm);
		}

		for (Entry<Position, Sprite> e : objectSprites.entrySet()) {
			Point p = pToG(vBounds, e.getKey());
			Sprite sprite = e.getValue();
			Point s = sprite.getCentre();
			g.drawImage(sprite.getImage(), p.x, p.y - s.y / 2, null);
		}
	}

	private void drawPlayer(Graphics2D g, Rectangle vBounds, Player pm) {
		Position pPos = pm.getPosition();
		Point gPos = pToG(vBounds, pPos);

		// assumes sprite size
		Rectangle vPmSprite = new Rectangle(vBounds.x + gPos.x - 20, vBounds.y + gPos.y - 20, 40, 40);
		if (!vBounds.intersects(vPmSprite)) {
			return;
		}

//		if (debugging && pm == game.getSelected()) {
//			// draw the bounds, scattergun and not efficient
//			g.setColor(Color.WHITE);
//			BoundingPolytope pBounds = pm.getBounds();
//			for (int x = (int) (pPos.x - 50); x < pPos.x + 50; x++) {
//				for (int y = (int) (pPos.y - 50); y < pPos.y + 50; y++) {
//					Position pTest = new Position(x, y, 0); // TODO: use ball height
//					if (pBounds.intersect(pTest)) {
//						Point gP = pToG(vBounds, pTest);
//						g.fillRect(gP.x, gP.y, 1, 1);
//					}
//				}
//			}
//		}

		int spriteIndex = 0;
		Direction direction = Direction.valueOf(pm.getAngle());
		switch (direction) {
			case DOWN:
				spriteIndex = 1;
				break;
			case RIGHT:
				spriteIndex = 2;
				break;
			case LEFT:
				spriteIndex = 3;
				break;
			case DOWN_LEFT:
				spriteIndex = 4;
				break;
			case DOWN_RIGHT:
				spriteIndex = 5;
				break;
			case UP_LEFT:
				spriteIndex = 6;
				break;
			case UP_RIGHT:
				spriteIndex = 7;
				break;
		}

		long ts = (long) (1000L * game.getTimestamp());
		long t = (ts + pm.getShirt() * 17) % 800L;
		switch (pm.getMode()) {
			case TACKLE:			// left and right are swapped
				switch (direction) {
					case LEFT:
						spriteIndex--;
						break;
					case RIGHT:
						spriteIndex++;
						break;
				}
				spriteIndex += 54;
				break;
			case HEAD_START:
				spriteIndex += 76;
				break;
			case HEAD_MID:
				spriteIndex += 84;
				break;
			case HEAD_END:
				spriteIndex += 92;
				break;
			case GROUND:
				switch (direction) {
					case LEFT:
						spriteIndex--;
						break;
					case RIGHT:
						spriteIndex++;
						break;
				}
				spriteIndex += 62;
				break;
			case INJURED:
				spriteIndex = 70;
				if (direction == Direction.RIGHT)
					spriteIndex += 2;
				spriteIndex += t < 400 ? 0 : 1;
				break;
			case THROW:
				spriteIndex *= 3;
				spriteIndex += 30;
				if (t > 400) {
					spriteIndex += 1;
				}
				break;
			default:
				spriteIndex *= 3;
				if (pm.getVelocity().speed() > 0) {
					if (t < 200) {
					} else if (t < 400) {
						spriteIndex += 1;
					} else if (t > 600) {
						spriteIndex += 2;
					}
				}
		}
		Sprite sprite = teamSprites.get(spriteIndex);
		Point s = sprite.getCentre();
		g.drawImage(sprite.getImage(), gPos.x - s.x / 2 - 1, gPos.y - s.y / 2, null);

		if (pm == game.getSelected()) {
			sprite = teamNumberSprites.get(pm.getShirt());
			s = sprite.getCentre();
			g.drawImage(sprite.getImage(), gPos.x - s.x / 2 - 1, gPos.y - s.y / 2 - 13, null);
		}
	}

	private void drawBall(Graphics2D g, Rectangle vBounds) {
		Ball ball = game.getBall();
		int spriteIndex = 0;
		Velocity v = ball.getVelocity();
		if (v.speed() > 0) {
			long t = (long) ((1000L * game.getTimestamp()) % 800L);
			if (t < 200) {
				spriteIndex += 1;
			} else if (t < 400) {
				spriteIndex += 2;
			} else if (t < 600) {
				spriteIndex += 3;
			}
		}
		// TODO: calibrate the Z drawing with the sprites
		double z = ball.getPosition().z;
		int diff = (int) (3 * z);
		{	// the drop shadow
			Sprite sprite = ballSprites.get(4);
			Point s = sprite.getCentre();
			Point gPos = pToG(vBounds, ball.getPosition());
			g.drawImage(sprite.getImage(), gPos.x - s.x / 2 + diff / 2, gPos.y - s.y / 2 + diff, null);
		}
		{	// the moving ball
			Sprite sprite = ballSprites.get(spriteIndex);
			Point s = sprite.getCentre();
			Point gPos = pToG(vBounds, ball.getPosition());
			g.drawImage(sprite.getImage(), gPos.x - s.x / 2 - 1 - diff / 2, gPos.y - s.y / 2 - 1 - diff, null);
		}
	}

	private Point pToG(Rectangle vBounds, Position p) {
		return new Point((int) round(p.x - vBounds.getX()), (int) round(p.y - vBounds.getY()));
	}

	// TODO: remove the need for 'v' coordinates by returning a java 3d object in 'p' coords
	// gSize is the drawable graphics, top left of the view being (0, 0) in graphics 'g' coordinates
	private Rectangle calculateWindow(Dimension gSize) {
		// centre over the ball
		Position pBall = game.getBall().getPosition();
		int gMinX = (int) round(pBall.x - gSize.width / 2.0);
		int gMinY = (int) round(pBall.y - gSize.height / 2.0);
		// account for falling off, where screen could be bigger than the pitch image
		int unseenWidth = max(0, pitch.getWidth() - gSize.width);
		gMinX = Utils.bounded(0, gMinX, unseenWidth);
		int unseenHeight = max(0, pitch.getHeight() - gSize.height);
		gMinY = Utils.bounded(0, gMinY, unseenHeight);
		return new Rectangle(gMinX, gMinY, gSize.width, gSize.height);
	}

	// TODO: remove code duplication
	@Deprecated
	private void drawGoalkeeper(Graphics2D g, Rectangle vBounds, Goalkeeper gm) {
		Position pPos = gm.getPosition();
		Point gPos = pToG(vBounds, pPos);

		// assumes sprite size
		Rectangle vPmSprite = new Rectangle(vBounds.x + gPos.x - 20, vBounds.y + gPos.y - 20, 40, 40);
		if (!vBounds.intersects(vPmSprite)) {
			return;
		}

		int spriteIndex = 0;
		Direction direction = Direction.valueOf(gm.getAngle());

		long ts = (long) (1000L * game.getTimestamp());
		long t = ts % 800L;

		if (gm.getGkState() == GoalkeeperState.RUN) {
			switch (direction) {
				case DOWN:
					spriteIndex = 1;
					break;
				case RIGHT:
					spriteIndex = 2;
					break;
				case LEFT:
					spriteIndex = 3;
					break;
				case DOWN_LEFT:
					spriteIndex = 4;
					break;
				case DOWN_RIGHT:
					spriteIndex = 5;
					break;
				case UP_LEFT:
					spriteIndex = 6;
					break;
				case UP_RIGHT:
					spriteIndex = 7;
					break;
			}
			spriteIndex *= 3;
			if (gm.getVelocity().speed() > 0) {
				if (t < 200) {
				} else if (t < 400) {
					spriteIndex += 1;
				} else if (t > 600) {
					spriteIndex += 2;
				}
			}
		} else {
			switch (gm.getOpponent()) {
				case UP:
					spriteIndex = 38;
					break;
				case DOWN:
					spriteIndex = 24;
					break;
			}
			int stage = gm.getGkState().ordinal();
			assert stage < 6;
			switch (direction) {
				case RIGHT:
					spriteIndex += stage;
					break;
				case LEFT:
					spriteIndex += (12 - stage);
					break;
			}
		}

		Sprite sprite = goalkeeperSprites.get(spriteIndex);
		Point s = sprite.getCentre();
		g.drawImage(sprite.getImage(), gPos.x - s.x / 2 - 1, gPos.y - s.y / 2, null);


//# sprite number, description
//947, goalie up
//950, goalie down
//953, goalie right
//956, goalie left
//959, goalie down left
//962, goalie down right
//965, goalie up left
//968, goalie up right
//971, goalie down dive right stage 1
//...
//976, goalie down dive right stage 6
//977, goalie ground down right with ball
//978, goalie down dive left stage 6
//...
//983, goalie down dive left stage 1
//984, goalie ground down left with ball
//985, goalie up dive right stage 1
//...
//990, goalie up dive right stage 6
//991, goalie ground up right with ball
//992, goalie up dive left stage 6
//...
//997, goalie down dive left stage 1
//998, goalie ground down left with ball
//999, goalie up dive stage 1
//...
//1001, goalie up dive stage 3
//1002, goalie down dive stage 1
//...
//1004, goalie down dive stage 3


	}
}
