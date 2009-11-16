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
 * game pitches, this file parses them into {@code Image} files.
 * <p>
 * The following formats exist in the game directory:
 * <ul>
 * <li>MAP - Not supported yet</li>
 * <li>256 - Not supported yet</li>
 * <li>BLK - Not supported yet</li>
 * <li>DAT - Not supported yet</li>
 * </ul>
 * Pitch is composed of 42 x 53 patterns, dimensions 16 x 16.
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

		File file = new File(Main.SWOS.getPath() + "/GRAFS/SWCPICH2.MAP");
		FileInputStream in = new FileInputStream(file);
		PitchParser parser = new PitchParser();
		Image image = parser.extractPitch(in);


		JFrame jf = new JFrame();
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.add(new JLabel(new ImageIcon(image)));
		jf.pack();
		jf.setVisible(true);

	}

	public Image extractPitch(InputStream in) throws IOException {
		Preconditions.checkNotNull(in);
		try {
//			BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
			BufferedImage image = new BufferedImage(672, 848, BufferedImage.TYPE_INT_RGB);

			for (int i = 0 ; i < 16 * 3 ; i++) {
				// Palette?
				Preconditions.checkArgument(in.read() != -1);
			}


			/*
			 * This mode enables user to scroll through the whole pitch, and also to edit it. Pitch is composed from 42 x 53 patterns, dimensions 16 x 16. By pressing CTRL + E, edit mode is activated, and flashing cursor is shown.
			 *
			 * Process of replacing patterns is similar to replacing sprites. Here only allowed bitmap dimension is 16 x 16. Bitmap data are simply copied into corresponding place in the pitch file. Also index of the pattern can be changed in pitch mode.
			When editing pattern in graphics editor, note that colors 10 and 11 will be converted. Color no. 10 will be shorts color if team is using one color shirt, or shirt basic color if not, and color 11 will always be primary shirt color. Only 42 patterns have this color converting property, and those are mostly crowd patterns. Those colors are red and blue, and you can see in PITCH mode that fans are mostly covered with it.
			Pitch types are achieved by palette modification. Palette entries: 0, 7, 9, 78, 79, 80, 81, 106 and 107 are changed depending on pitch type accomplishing visual changes according to conditions.
			Patterns 1 to 24 have special properties - they are animated crowd. Patterns 1..12 are upper crowd, and patterns 13..24 are lower crowd. Odd indices are pictures when not moving, and even indices are pictures when animated (jumping, chearing, etc.). Pattern 0 is usually empty pattern (except in training pitch).
			7
			You can replace individual patterns without any special considerations, but inserting whole pitch from bitmaps needs some attention. Following does not hold for training pitch, which does not have crowd, and therefore animated patterns. When some pitch with crowd is inserted, pattern zero will be set to empty pattern (filled with zeros), and patterns from 1 to 24 will be set: 1 = 2, 3 = 4, etc., every odd and following even number will hold the same pattern. By default, this will turn animation off. To re-enable it, use change pattern number feature and set patterns that will be animated into odd patterns 1..24. Even patterns must be inserted manually.
			Important thing to know when inserting bitmaps is that maximum allowed number of unique patterns on pitch is 296. SWOS uses only 75776 bytes of memory for patterns, and if that limit is exceeded each pattern with index greater than 295 will be overwritten.
			 */
			outer:
			for (int j = 0; j < 53; j++) {
				for (int i = 0; i < 42; i++) {
					for (int y = 0; y < 16; y++) {
						for (int x = 0; x < 16; x += 2) {
							int read = in.read();
							if (read == -1)
								break outer;

							int r1 = (read & 0x0F) << 4;
							int r2 = read & 0xF0;

							Color c1 = new Color(r1, r1, r1);
							Color c2 = new Color(r2, r2, r2);
							image.setRGB(i * 16 + x, j * 16 + y, c1.getRGB());
							image.setRGB(i * 16 + x + 1, j * 16 + y, c2.getRGB());
						}
					}
				}
			}
			return image;
		} finally {
			in.close();
		}
	}
}
