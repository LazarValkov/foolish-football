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

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * The SWOS 96/97 directory contains {@code PITCH} files containing the graphics of the
 * game pitches, this file parses them into {@code Image} files. Each pitch is stored
 * across two files:
 * <ul>
 * <li>BLK - contains pattern blocks: 16x16 pixels, 256 bit indexed.</li>
 * <li>DAT - contains the ordering of the patterns for a pitch.</li>
 * </ul>
 * <p>
 * The palette was extracted by Zlatko Karakas (author of SWOS Picture Editor).
 * Every colour has its darkened counterpart (colour + 128). Note that although
 * the {@code PALA.DAT} file contains a palette that is apparently 11 blocks of 48
 * ASCII hex codes, it is not clear if it encodes the palette used for the pitch.
 * <p>
 * Colour 11 is converted into the primary colour of the team. Colour 10 is
 * converted to the secondary colour or shorts. Only 42 patterns have this colour
 * converting property, and those are mostly crowd patterns.
 * <p>
 * Pitch types are achieved by palette modification of entries
 * 0, 7, 9, 78, 79, 80, 81, 106 and 107
 * denoting visual changes according to conditions.
 * TODO: the exact process of weather-modification is unclear.
 * <p>
 * Patterns 1 to 24 are animated. Patterns 1..12 are upper crowd, and patterns 13..24
 * are lower crowd. Odd indices are pictures when not moving, and even indices are
 * pictures when animated (jumping, cheering).
 * <p>
 * Pattern 0 is usually empty pattern (except in training pitch).
 * <p>
 * SWOS can only read a maximum of 296 unique patterns and pitches are
 * comprised of 42 x 53 patterns.
 * <p>
 * The location of the corner flags are pixels (81, 129), (590, 129), (590, 769),
 * (81, 769).
 * <p>
 * The location of the goal posts are (300, 769), (300, 751), (372, 751),
 * (372, 769) and (372, 129), (372, 111), (300, 111), (300, 129). (Note that the posts
 * are wider than single pixels).
 * <p>
 * The locations of the penalty boxes are (193, 129), (478, 129), (193, 216),
 * (478, 216) and (193, 769), (193, 682), (478, 682), (478, 769).
 * <p>
 * The locations of the goal boxes are (273, 769), (273, 740), (398, 740), (398, 769)
 * and (273, 129), (273, 158), (398, 129), (398, 158).
 * <p>
 * The penalty spots are (336, 711) and (336, 187).
 *
 * @author Samuel Halliday
 */
public class PitchParser {

	private static final Logger log = Logger.getLogger(PitchParser.class.getName());
	private static volatile List<Color> PAL;
	private static final int[] PAL_GAME_RAW = {
		112, 80, 0, 152, 152, 152, 252, 252, 252, 0, 0, 0, 100, 32, 0, 184, 68, 0,
		252, 100, 0, 96, 80, 0, 0, 32, 0, 80, 80, 0, 252, 0, 0, 0, 0, 252, 100, 0,
		32, 152, 152, 252, 0, 220, 0, 252, 252, 0, 156, 200, 0, 172, 172, 172, 252,
		252, 252, 24, 24, 24, 124, 36, 0, 208, 72, 0, 252, 128, 52, 132, 200, 0, 0,
		56, 0, 200, 196, 0, 252, 108, 108, 120, 124, 252, 124, 0, 40, 188, 188,
		252, 0, 240, 0, 252, 252, 188, 108, 136, 0, 116, 116, 116, 208, 208, 208,
		0, 0, 0, 72, 20, 0, 120, 36, 0, 184, 64, 0, 88, 132, 0, 0, 4, 0, 132, 128,
		0, 204, 0, 0, 0, 0, 204, 60, 0, 20, 120, 124, 204, 0, 172, 0, 204, 200, 0,
		0, 0, 0, 20, 20, 20, 44, 44, 44, 68, 68, 68, 92, 92, 92, 116, 116, 116,
		136, 136, 136, 160, 160, 160, 184, 184, 184, 208, 208, 208, 228, 228, 228,
		252, 252, 252, 252, 216, 188, 252, 176, 128, 252, 140, 64, 252, 100, 0,
		228, 88, 0, 200, 76, 0, 176, 64, 0, 152, 52, 0, 128, 44, 0, 100, 32, 0, 0,
		0, 0, 0, 0, 112, 0, 0, 128, 0, 0, 148, 0, 0, 168, 44, 44, 196, 96, 104,
		224, 148, 152, 228, 200, 184, 164, 176, 120, 0, 68, 44, 0, 92, 56, 0, 232,
		232, 232, 224, 212, 212, 220, 196, 196, 216, 184, 184, 212, 168, 168, 208,
		156, 156, 204, 140, 140, 176, 120, 120, 144, 100, 100, 116, 80, 80, 88, 60,
		60, 56, 40, 40, 28, 20, 20, 0, 0, 0, 252, 232, 208, 232, 208, 176, 212,
		188, 152, 196, 168, 128, 176, 148, 108, 148, 124, 92, 124, 100, 76, 96, 80,
		56, 84, 68, 52, 36, 28, 20, 84, 60, 8, 184, 156, 120, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 152, 252, 80, 80, 80, 112, 112, 112, 32, 32, 32, 240, 0,
		0, 240, 112, 112, 128, 128, 128, 144, 144, 144, 160, 160, 160, 176, 176,
		176, 240, 176, 176, 192, 192, 192, 208, 208, 208, 224, 224, 224, 240, 240,
		240, 252, 76, 76, 52, 36, 0, 92, 92, 92, 192, 192, 192, 0, 0, 0, 40, 12, 0,
		124, 44, 0, 192, 72, 0, 36, 28, 0, 0, 0, 0, 20, 20, 0, 192, 0, 0, 0, 0,
		192, 0, 0, 0, 116, 116, 192, 0, 160, 0, 192, 192, 0, 108, 140, 0, 112, 112,
		112, 192, 192, 192, 0, 0, 0, 64, 16, 0, 148, 48, 0, 192, 96, 40, 92, 140,
		0, 0, 0, 0, 140, 136, 0, 192, 80, 80, 92, 92, 192, 0, 0, 0, 144, 144, 192,
		0, 180, 0, 192, 192, 144, 60, 76, 0, 56, 56, 56, 148, 148, 148, 0, 0, 0,
		12, 0, 0, 60, 16, 0, 124, 40, 0, 44, 72, 0, 0, 0, 0, 72, 68, 0, 144, 0, 0,
		0, 0, 144, 0, 0, 0, 84, 84, 144, 0, 112, 0, 144, 140, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 8, 8, 8, 32, 32, 32, 56, 56, 56, 76, 76, 76, 100, 100, 100, 124,
		124, 124, 148, 148, 148, 168, 168, 168, 192, 192, 192, 192, 164, 144, 192,
		132, 96, 192, 104, 48, 192, 72, 0, 168, 60, 0, 140, 52, 0, 116, 40, 0, 92,
		28, 0, 68, 20, 0, 40, 12, 0, 0, 0, 0, 0, 0, 52, 0, 0, 68, 0, 0, 88, 0, 0,
		108, 28, 28, 136, 68, 76, 164, 108, 112, 168, 140, 128, 112, 116, 76, 0, 8,
		4, 0, 32, 16, 0, 172, 172, 172, 164, 152, 152, 160, 144, 144, 156, 132,
		132, 152, 120, 120, 148, 108, 108, 144, 96, 96, 116, 80, 80, 84, 56, 56,
		56, 36, 36, 28, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 192, 176, 156, 172, 152,
		128, 152, 132, 108, 136, 116, 88, 116, 96, 68, 88, 72, 52, 64, 48, 36, 36,
		28, 20, 24, 16, 12, 0, 0, 0, 24, 16, 0, 124, 104, 80, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 112, 192, 20, 20, 20, 52, 52, 52, 0, 0, 0, 180, 0, 0,
		180, 84, 84, 68, 68, 68, 84, 84, 84, 100, 100, 100, 116, 116, 116, 180,
		132, 132, 132, 132, 132, 148, 148, 148, 164, 164, 164, 180, 180, 180, 192,
		56, 56
	};

	/**
	 * @param swos
	 * @param i
	 * @return
	 * @throws IOException
	 */
	public static final BufferedImage getPitch(File swos, int i) throws IOException {
		File blkFile = new File(swos.getPath() + "/PITCH" + i + ".BLK");
		File datFile = new File(swos.getPath() + "/PITCH" + i + ".DAT");
		FileInputStream blk = new FileInputStream(blkFile);
		FileInputStream dat = new FileInputStream(datFile);
		PitchParser parser = new PitchParser();
		return parser.extractPitch(blk, dat);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static final void main(String[] args) throws Exception {
		for (int i = 1; i <= 6; i++) {
			ImageIO.write(getPitch(Main.SWOS, i), "png", new File("pitch" + i + ".png"));
		}
	}

	private static List<Color> getPalette() {
		if (PAL != null)
			return PAL;

		Color[] palette = new Color[256];
		for (int i = 0; i < palette.length; i++) {
			palette[i] = new Color(PAL_GAME_RAW[i * 3], PAL_GAME_RAW[i * 3 + 1], PAL_GAME_RAW[i * 3 + 2]);
		}
		PAL = Collections.unmodifiableList(Arrays.asList(palette));
		return PAL;
	}

	/**
	 * @param blk
	 * @param dat
	 * @return
	 * @throws IOException
	 */
	public BufferedImage extractPitch(InputStream blk, InputStream dat) throws IOException {
		Preconditions.checkNotNull(blk);
		Preconditions.checkNotNull(dat);
		List<Color> palette = getPalette();

		// TODO: perhaps we should use an indexed BufferedImage instead of RGB
		try {
			int HEIGHT = 55;
			int WIDTH = 42;
			// pattern index, [0][0] is top left.
			int[][] pattern = new int[WIDTH][HEIGHT];
			for (int j = HEIGHT - 1; j >= 0; j--) {
				for (int i = WIDTH - 1; i >= 0; i--) {
					int a = dat.read(); // ??
					pattern[WIDTH - i - 1][HEIGHT - j - 1] = dat.read();
					int b = dat.read(); // ??
					int c = dat.read(); // ??
					Preconditions.checkArgument(a != -1 && b != -1 && c != -1 && pattern[i][j] != -1, "bad DAT file");
					if (a != 0 || b != 0 || c != 0)
						log.info("Strange bytes in pitch DAT file: " + a + " " + b + " " + c);
				}
			}
			Preconditions.checkArgument(dat.read() == -1, "bad DAT file");
			BufferedImage[] patterns = new BufferedImage[296];
			outer:
			for (int i = 0; i < 296; i++) {
				patterns[i] = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
				for (int y = 0; y < 16; y++) {
					for (int x = 0; x < 16; x++) {
						int read = blk.read();
						if (read == -1) {
							Preconditions.checkArgument(x == 0 && y == 0, "bad BLK file");
							break outer;
						}
						Color c = palette.get(read);
						patterns[i].setRGB(x, y, c.getRGB());
					}
				}
			}

			BufferedImage image = new BufferedImage(16 * WIDTH, 16 * HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics g = image.getGraphics();
			for (int i = 0; i < WIDTH; i++) {
				for (int j = 0; j < HEIGHT; j++) {
					int p = pattern[i][j];
					g.drawImage(patterns[p], i * 16, j * 16, null);
				}
			}
			return image;
		} finally {
			blk.close();
			dat.close();
		}
	}
}
