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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.swing.JPanel;
import uk.me.fommil.ff.physics.Goalkeeper;
import uk.me.fommil.ff.physics.Goalkeeper.GoalkeeperState;
import uk.me.fommil.ff.physics.Position;
import uk.me.fommil.ff.physics.Velocity;
import static uk.me.fommil.ff.Utils.*;

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
 * <li>{@code pVar}: The {@link Position} position of the objects using our physics model.</li>
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

	private final KeyListener keyboardInput;

	private final GamePhysics game;

	private Dimension gSize;

	/**
	 * @param game
	 * @param pitch
	 * @param sprites
	 */
	public ClassicView(GamePhysics game, BufferedImage pitch, Map<Integer, Sprite> sprites) {
		this.pitch = pitch;
		this.a = game.getTeamA();
		this.game = game;
		keyboardInput = new KeyboardController(game);

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
		objectSprites.put(new Position(30, 88 - 11.7, 0), sprites.get(1205));
		objectSprites.put(new Position(30, 88 - 76.4, 0), sprites.get(1206));
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
		gSize = new Dimension(getWidth() / zoom, getHeight() / zoom);

		// we are centered over the ball
		// g never goes outside the pitch image
		calculateWindow(gSize);

		drawPitch(g);

		// FIXME: draw all sprites north to south to get layering correct

		// draw the ball
		drawBall(g);

//		// draw the zones
//		if (debugging) {
//			g.setColor(Color.GREEN);
//			for (int i = 0; i <= 5; i++) {
//				int x = 81 + i * (590 - 81) / 5;
//				Point start = pToG(new Position(x, 129, 0));
//				Point end = pToG(new Position(x, 769, 0));
//				g.drawLine(start.x, start.y, end.x, end.y);
//			}
//			for (int i = 0; i <= 7; i++) {
//				int y = 129 + i * (769 - 129) / 7;
//				Point start = pToG(new Position(81, y, 0));
//				Point end = pToG(new Position(590, y, 0));
//				g.drawLine(start.x, start.y, end.x, end.y);
//			}
//		}

		// draw the players that are in view
		for (Player pm : game.getPlayers()) {
			drawPlayer(g, pm);
		}

		for (Goalkeeper gm : game.getGoalkeepers()) {
			drawGoalkeeper(g, gm);
		}

		for (Entry<Position, Sprite> e : objectSprites.entrySet()) {
			Point p = pToG(e.getKey());
			Sprite sprite = e.getValue();
			Point s = sprite.getCentre();
			g.drawImage(sprite.getImage(), p.x, p.y - s.y / 2, null);
		}
	}

	private void drawPlayer(Graphics2D g, Player pm) {
		Position pPos = pm.getPosition();
		Point gPos = pToG(pPos);

		// TODO: draw only when in the region

		int spriteIndex = 0;
		Direction direction = Direction.valueOf(pm.getDirection());
		switch (direction) {
			case SOUTH:
				spriteIndex = 1;
				break;
			case EAST:
				spriteIndex = 2;
				break;
			case WEST:
				spriteIndex = 3;
				break;
			case SOUTH_WEST:
				spriteIndex = 4;
				break;
			case SOUTH_EAST:
				spriteIndex = 5;
				break;
			case NORTH_WEST:
				spriteIndex = 6;
				break;
			case NORTH_EAST:
				spriteIndex = 7;
				break;
		}

		long ts = (long) (1000L * game.getTimestamp());
		long t = (ts + pm.getShirt() * 17) % 800L;
		switch (pm.getState()) {
			case TACKLE:			// left and right are swapped
				switch (direction) {
					case WEST:
						spriteIndex--;
						break;
					case EAST:
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
					case WEST:
						spriteIndex--;
						break;
					case EAST:
						spriteIndex++;
						break;
				}
				spriteIndex += 62;
				break;
			case INJURED:
				spriteIndex = 70;
				if (direction == Direction.EAST)
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
				if (pm.getVelocity().speed() > 0.1) {
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
		g.drawImage(sprite.getImage(), gPos.x - s.x, gPos.y - s.y, null);

		if (pm == game.getSelected()) {
			sprite = teamNumberSprites.get(pm.getShirt());
			s = sprite.getCentre();
			g.drawImage(sprite.getImage(), gPos.x - s.x, gPos.y - s.y - 13, null);
		}
	}

	private void drawBall(Graphics2D g) {
		Ball ball = game.getBall();
		int spriteIndex = 0;
		Velocity v = ball.getVelocity();
		if (v.speed() > 0.1) {
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
			Point gPos = pToG(ball.getPosition());
			g.drawImage(sprite.getImage(), gPos.x - s.x + diff / 2 + 1, gPos.y - s.y + diff + 1, null);
		}
		{	// the moving ball
			Sprite sprite = ballSprites.get(spriteIndex);
			Point s = sprite.getCentre();
			Point gPos = pToG(ball.getPosition());
			g.drawImage(sprite.getImage(), gPos.x - s.x - diff / 2, gPos.y - s.y - diff, null);
		}
	}

	private Point pToG(Position p) {
		double scale = 1.0 / game.getPitch().getScale();
		// FIXME: reverse Y to get graphics
		return new Point(
				round(scale * (p.x - pBottomLeft.x)),
				gSize.height - round(scale * (p.y - pBottomLeft.y)));
	}

	private Position pBottomLeft;

	private Position pTopRight;

	// gSize is the drawable graphics, top left of the view being (0, 0) in graphics 'g' coordinates
	private void calculateWindow(Dimension gSize) {
		double scale = game.getPitch().getScale();
		// centre over the ball
		Position pBall = game.getBall().getPosition();
		double pMinX = pBall.x - scale * gSize.width / 2.0;
		double pMinY = pBall.y - scale * gSize.height / 2.0;
		// account for falling off, where screen could be bigger than the pitch image
		double pUnseenWidth = Math.max(0, (pitch.getWidth() - gSize.width) * scale);
		pMinX = Utils.bounded(0, pMinX, pUnseenWidth);
		double pUnseenHeight = Math.max(0, (pitch.getHeight() - gSize.height) * scale);
		pMinY = Utils.bounded(0, pMinY, pUnseenHeight);
		pBottomLeft = new Position(pMinX, pMinY, 0);
		pTopRight = new Position(pMinX + gSize.width * scale, pMinY + gSize.height * scale, 0);
	}

	// TODO: remove code duplication
	@Deprecated
	private void drawGoalkeeper(Graphics2D g, Goalkeeper gm) {
		Position pPos = gm.getPosition();
		Point gPos = pToG(pPos);

		// assumes sprite size
//		Rectangle vPmSprite = new Rectangle(vBounds.x + gPos.x - 20, vBounds.y + gPos.y - 20, 40, 40);
//		if (!vBounds.intersects(vPmSprite)) {
//			return;
//		}

		int spriteIndex = 0;
		Direction direction = Direction.valueOf(gm.getDirection());

		long ts = (long) (1000L * game.getTimestamp());
		long t = ts % 800L;

		if (gm.getGkState() == GoalkeeperState.RUN) {
			switch (direction) {
				case SOUTH:
					spriteIndex = 1;
					break;
				case EAST:
					spriteIndex = 2;
					break;
				case WEST:
					spriteIndex = 3;
					break;
				case SOUTH_WEST:
					spriteIndex = 4;
					break;
				case SOUTH_EAST:
					spriteIndex = 5;
					break;
				case NORTH_WEST:
					spriteIndex = 6;
					break;
				case NORTH_EAST:
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
				case NORTH:
					spriteIndex = 38;
					break;
				case SOUTH:
					spriteIndex = 24;
					break;
			}
			int stage = gm.getGkState().ordinal();
			assert stage < 6;
			switch (direction) {
				case EAST:
					spriteIndex += stage;
					break;
				case WEST:
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

	private void drawPitch(Graphics2D g) {
		double scale = 1.0 / game.getPitch().getScale();

		int gTopLeftX = Math.max(0, round(pBottomLeft.x * scale));
		int gTopLeftY = Math.max(0, pitch.getHeight() - round(pTopRight.y * scale));
		int gWidth = Math.min(round((pTopRight.x - pBottomLeft.x) * scale) + 1, pitch.getWidth() - gTopLeftX);
		int gHeight = Math.min(round((pTopRight.y - pBottomLeft.y) * scale) + 1, pitch.getHeight() - gTopLeftY);

//		log.info(pBottomLeft.x + " " + gTopLeftX + ", " + gTopLeftX);

		// extra padding is for when a partial pixel is shown
		BufferedImage sub = pitch.getSubimage(gTopLeftX, gTopLeftY, gWidth, gHeight);
		g.drawImage(sub, 0, 0, null);
	}
}
