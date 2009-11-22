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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * The SWOS 96/97 directory contains {@code PITCH} files containing the graphics of the
 * game pitches, this file parses them into {@code Image} files. Each pitch is stored
 * across two files:
 * <ul>
 * <li>BLK - contains blocks of 16x16 tiles, 256 bit indexed.</li>
 * <li>DAT - Unknown, but probably the index and the ordering.</li>
 * </ul>
 *
 * @author Samuel Halliday
 */
public class PitchParser {

	private static final Logger log = Logger.getLogger(PitchParser.class.getName());

	/**
	 * @param args
	 * @throws Exception
	 */
	public static final void main(String[] args) throws Exception {
//		List<File> files = Arrays.asList(Main.SWOS.listFiles(new FilenameFilter() {
//
//			@Override
//			public boolean accept(File dir, String name) {
//				return name.toLowerCase().startsWith("pitch");
//			}
//		}));
//		System.out.println(files);

		File blkFile = new File(Main.SWOS.getPath() + "/PITCH1.BLK");
		File datFile = new File(Main.SWOS.getPath() + "/PITCH1.DAT");
		FileInputStream blk = new FileInputStream(blkFile);
		FileInputStream dat = new FileInputStream(datFile);
		PitchParser parser = new PitchParser();
		Image image = parser.extractPitch(blk, dat);

		JFrame jf = new JFrame();
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.add(new JLabel(new ImageIcon(image)));
		jf.pack();
		jf.setVisible(true);

	}

	public Image extractPitch(InputStream blk, InputStream dat) throws IOException {
		Preconditions.checkNotNull(blk);
		Preconditions.checkNotNull(dat);
		try {

			int WIDTH = 55;
			int HEIGHT = 42;
			// pattern index
			int[][] pattern = new int[WIDTH][HEIGHT];
			for (int i = 0; i < WIDTH; i++) {
				for (int j = 0; j < HEIGHT; j++) {
					dat.read(); // ??
					pattern[i][j] = dat.read();
					dat.read(); // ??
					dat.read(); // ??
				}
			}

			// FIXME: colour index
			Color[] index = new Color[256];
			for (int i = 0; i < 256; i++) {
				int r = dat.read();
				int g = dat.read();
				int b = dat.read();
				// index[i] = new Color(r, g, b);
				index[i] = new Color(i, i, i);
			}


			/**
			 * Notes from the SWOS Picture Editor (SWPE) documentation.
			 * 
			 * Colour 11 will be converted into the primary colour of the team. Colour 10 will be
			 * converted to the secondary colour or shorts. Only 42 patterns have this colour
			 * converting property, and those are mostly crowd patterns.
			 *
			 * Pitch types are achieved by palette modification of entries
			 * 0, 7, 9, 78, 79, 80, 81, 106 and 107
			 * denoting visual changes according to conditions.
			 * Patterns 1 to 24 are animated. Patterns 1..12 are upper crowd, and patterns 13..24
			 * are lower crowd. Odd indices are pictures when not moving, and even indices are
			 * pictures when animated (jumping, cheering).
			 *
			 * Pattern 0 is usually empty pattern (except in training pitch).
			 * 
			 * SWOS can only read a maximum of 296 unique patterns and pitches are
			 * comprised of 42 x 53 patterns
			 */
			// red/blue have bit values of 252 when shown using SWPE
			BufferedImage[] patterns = new BufferedImage[296];
			outer:
			for (int i = 0; i < 296; i++) {
				patterns[i] = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
				for (int x = 0; x < 16; x++) {
					for (int y = 0; y < 16; y++) {
						int read = blk.read();
						if (read == -1)
							break outer;

						Color c = index[read];
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
