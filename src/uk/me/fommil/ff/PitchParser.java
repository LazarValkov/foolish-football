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

		File file = new File(Main.SWOS.getPath() + "/GRAFS/SWCPICH1.MAP");
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

//			for (int i = 0 ; i < 100 * 255 ; i++) {
//				// Palette?
//				Preconditions.checkArgument(in.read() != -1);
//			}

			outer:
			for (int j = 0; j < image.getHeight(); j++) {
				for (int i = 0; i < image.getWidth() / 2; i++) {
					int read = in.read();
					if (read == -1)
						break outer;

					int r1 = read & 0xFF;
					int r2 = (read >>> 4) & 0xFF;

					Color c1 = new Color(r1, r1, r1);
					Color c2 = new Color(r2, r2, r2);
					image.setRGB(2 * i, j, c1.getRGB());
					image.setRGB(2 * i + 1, j, c2.getRGB());
				}
			}
			return image;
		} finally {
			in.close();
		}
	}
}
