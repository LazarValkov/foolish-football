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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.SortedSet;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
 * <p>
 * There are as many as 59 unique colours in the BLK files, 11 of which are defined as
 * above, maximum value 108. The {@code PALA.DAT} file contains the palette as
 * 11 blocks of 48 ASCII hex codes.
 * <p>
 * Patterns 1 to 24 are animated. Patterns 1..12 are upper crowd, and patterns 13..24
 * are lower crowd. Odd indices are pictures when not moving, and even indices are
 * pictures when animated (jumping, cheering).
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

		File palFile = new File(Main.SWOS.getPath() + "/PALA.DAT");
		FileInputStream pal = new FileInputStream(palFile);
		Color[] palette = getPalette(pal);
//		palette[10] = Color.RED;
//		palette[11] = Color.BLUE;
//		int[] weather = new int[]{0, 7, 9, 78, 79, 80, 81, 106, 107};
//		for (int i : weather) {
//			palette[i] = Color.GREEN;
//		}

		BufferedImage image = new BufferedImage(16, 11, BufferedImage.TYPE_INT_RGB);
		for (int j = 0; j < 11; j++) {
			for (int i = 0; i < 16; i++) {
				int ij = 16 * j + i;
				Color c;
				if (ij >= palette.length)
					c = Color.BLACK;
				else
					c = palette[ij];

				image.setRGB(i, j, c.getRGB());
			}
		}
//		Image img = image.getScaledInstance(160, 11, Image.SCALE_FAST);
//		image = new BufferedImage(160, 110, BufferedImage.TYPE_INT_RGB);
//		image.getGraphics().drawImage(img, 0, 0, null);
		File palOut = new File("pala.png");
		ImageIO.write(image, "png", palOut);

//		JFrame jf = new JFrame();
//		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		jf.add(new JLabel(new ImageIcon(img)));
//		jf.pack();
//		jf.setVisible(true);

		for (int i = 1; i <= 6; i++) {
			File blkFile = new File(Main.SWOS.getPath() + "/PITCH" + i + ".BLK");
			File datFile = new File(Main.SWOS.getPath() + "/PITCH" + i + ".DAT");
			File out = new File("pitch" + i + ".png");
			FileInputStream blk = new FileInputStream(blkFile);
			FileInputStream dat = new FileInputStream(datFile);
			PitchParser parser = new PitchParser();
			BufferedImage pImage = parser.extractPitch(blk, dat, palette);
			ImageIO.write(pImage, "png", out);

//			JFrame jf = new JFrame();
//			jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			jf.add(new JLabel(new ImageIcon(pImage)));
//			jf.pack();
//			jf.setVisible(true);
		}
	}

	public static Color[] getPalette(InputStream pal) throws IOException {
		InputStreamReader isr = new InputStreamReader(pal);
		BufferedReader reader = new BufferedReader(isr);
		Color[] color = new Color[176];

		int i = 0;
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.trim().isEmpty())
				continue;
			String[] cs = line.toLowerCase().split(",");
			int r = Integer.valueOf(new StringBuilder(cs[0].substring(1, 3)).reverse().toString(), 16);
			int g = Integer.valueOf(new StringBuilder(cs[1].substring(1, 3)).reverse().toString(), 16);
			int b = Integer.valueOf(new StringBuilder(cs[2].substring(1, 3)).reverse().toString(), 16);
			color[i] = new Color(r, g, b );
			i++;
			r = Integer.valueOf(new StringBuilder(cs[3].substring(1, 3)).reverse().toString(), 16);
			g = Integer.valueOf(new StringBuilder(cs[4].substring(1, 3)).reverse().toString(), 16);
			b = Integer.valueOf(new StringBuilder(cs[5].substring(1, 3)).reverse().toString(), 16);
			color[i] = new Color(r, g, b);
			i++;
		}
		return color;
	}

	public BufferedImage extractPitch(InputStream blk, InputStream dat, Color[] palette) throws IOException {
		Preconditions.checkNotNull(blk);
		Preconditions.checkNotNull(dat);
		Preconditions.checkNotNull(palette);
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

						Color c = palette[read];
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
