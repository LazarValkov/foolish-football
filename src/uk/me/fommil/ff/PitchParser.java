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
import com.google.common.collect.Sets;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.SortedSet;
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
 * <p>
 * Notes, cobbled together from the SWOS Picture Editor (SWPE) documentation and
 * inspection.
 * <p>
 * Colour 11 will be converted into the primary colour of the team. Colour 10 will be
 * converted to the secondary colour or shorts. Only 42 patterns have this colour
 * converting property, and those are mostly crowd patterns.
 * <p>
 * Pitch types are achieved by palette modification of entries
 * 0, 7, 9, 78, 79, 80, 81, 106 and 107
 * denoting visual changes according to conditions.
 * Patterns 1 to 24 are animated. Patterns 1..12 are upper crowd, and patterns 13..24
 * are lower crowd. Odd indices are pictures when not moving, and even indices are
 * pictures when animated (jumping, cheering).
 * <p>
 * There are as many as 59 unique colours in the BLK files, 11 of which are defined as
 * above, maximum value 108. The {@code PALA.DAT} file, which looks like a palette file,
 * has 11 blocks of 48 ASCII hex codes.
 * <p>
 * Pattern 0 is usually empty pattern (except in training pitch).
 * <p>
 * SWOS can only read a maximum of 296 unique patterns and pitches are
 * comprised of 42 x 53 patterns.
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

		for (int i = 1 ; i <= 6; i++) {
			File blkFile = new File(Main.SWOS.getPath() + "/PITCH" + i + ".BLK");
			File datFile = new File(Main.SWOS.getPath() + "/PITCH" + i + ".DAT");
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
			assert dat.read() == -1;

			// FIXME: colour index
			Color[] index = new Color[256];
			for (int i = 0; i < 256; i++) {
				index[255 - i] = new Color(i, i, i);
			}
			// team colours
			index[10] = Color.RED;
			index[11] = Color.BLUE;

			// weather-based indices
			int[] weather = new int[]{0, 7, 9, 78, 79, 80, 81, 106, 107};
			for (int i : weather) {
				index[i] = Color.GREEN;
			}

			SortedSet<Integer> uniqueColours = Sets.newTreeSet();
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
						uniqueColours.add(read);
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

			log.info(uniqueColours.size() + " " + uniqueColours);

			return image;
		} finally {
			blk.close();
			dat.close();
		}
	}
}
