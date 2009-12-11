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
package uk.me.fommil.ff.swos;

import com.google.common.collect.Lists;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import uk.me.fommil.ff.Main;

/**
 * There are 1,334 sprites in SWOS contained across
 * CHARSET.DAT, SCORE.DAT, TEAM1.DAT, TEAM2.DAT, TEAM3.DAT, GOAL1.DAT, BENCH.DAT.
 * <p>
 * Special thanks to Zlatko Karakas for providing the source code to the SWOS Picture Editor
 * which cracked the file format and a high level description of the NASM code, and to
 * {@code Jester01} on {@code ##asm@irc.freenode.net} for helping with initial interpretation of
 * the NASM code.
 * <p>
 * The sprite files consist of a sequence of individual sprites, each one has the following
 * binary structure (in Little Endian, all bytes are unsigned)
 * <ul>
 * <li>{@code int} pointer to start of graphics in file</li>
 * <li>{@code short} - unused</li>
 * <li>{@code short} - unused</li>
 * <li>{@code byte} - unused</li>
 * <li>{@code byte} - unknown</li>
 * <li>{@code short} - width, for visual cropping</li>
 * <li>{@code short} - nlines (height)</li>
 * <li>{@code short} - wquads (number of bytes / 8) in one line</li>
 * <li>{@code short} - the x centre</li>
 * <li>{@code short} - the x centre</li>
 * <li>{@code byte} - unknown</li>
 * <li>{@code byte} - nlines/4</li>
 * <li>{@code short} - sprite id</li>
 * <li>{@code byte[]} - encoded pixel data, {@code 8 * nlines * wquads} length.</li>
 * </ul>
 * The {@code byte[]} part is encoded into interleaved blocks of 8 bytes.
 * <ul>
 * <li>line is divided in 4 "planes", 0, 1, 2 and 3 (wquads * 8 / 4)</li>
 * <li>every pixel consists of four bits</li>
 * <li>every pixel consists of 1 bit out of each plane</li>
 * <li>first pixel consists of highest bits from plane 3, 2, 1, 0 respectively</li>
 * <li>second pixel consists of following bits, and so on</li>
 * <li>algorithm is going through the line, assembling pixels bit by bit to the right, till the end of the line</li>
 * </ul>
 * This process is repeated until all lines are processed.
 *
 * @author Samuel Halliday
 */
public class SpriteParser {

	private static final Logger log = Logger.getLogger(SpriteParser.class.getName());
	// order to read the sprite files
	private static final List<String> ORDER = Lists.newArrayList("CHARSET.DAT", "SCORE.DAT", "TEAM1.DAT", "TEAM3.DAT", "GOAL1.DAT", "GOAL1.DAT", "BENCH.DAT");

	// /* SpritesGetPalette
//
//   Return sprite mode specific palette.
//*/
//static byte *SpritesGetPalette()
//{
//    return s.sprite_no >= 1209 && s.sprite_no <= 1272 ? gamepal : pal;
//}
	/**
	 * @param args
	 * @throws IOException
	 */
	public static final void main(String[] args) throws IOException {
		List<Color> pal = SwosUtils.getGamePalette();

		File pout = new File("data/sprites/pal.dat");
		FileOutputStream pfout = new FileOutputStream(pout);
		DataOutputStream pdout = new DataOutputStream(pfout);
		for (Color c : pal) {
			pdout.write(c.getRed());
			pdout.write(c.getGreen());
			pdout.write(c.getBlue());
		}
		pdout.close();

		for (String name : ORDER) {
			File datFile = new File(Main.SWOS.getPath() + File.separator + name);
			InputStream datS = new FileInputStream(datFile);
			DataInputStream dat = new DataInputStream(datS);
			try {
				while (true) {
					try {
						dat.readInt();
					} catch (EOFException e) {
						break;
					}
					dat.skipBytes(6);
					// width is display only
					int width = Short.reverseBytes(dat.readShort());
					int nlines = Short.reverseBytes(dat.readShort());
					int wquads = Short.reverseBytes(dat.readShort());
					dat.skipBytes(6);
					int id = Short.reverseBytes(dat.readShort());
					assert id >= 0 && id < 1335 : id;
					log.info("nlines = " + nlines + ", wquads = " + wquads + ", width = " + width + ", id = " + id);
					byte[] data = new byte[8 * wquads * nlines];
					dat.readFully(data);

//					File out = new File("data/sprites/" + id + ".dat");
//					FileOutputStream fout = new FileOutputStream(out);
//					DataOutputStream dout = new DataOutputStream(fout);
//					dout.writeInt(id);
//					dout.writeInt(nlines);
//					dout.writeInt(wquads);
//					dout.write(data);
//					dout.close();

					decodeSprite(data, wquads, nlines);

					BufferedImage image = new BufferedImage(8 * wquads, nlines, BufferedImage.TYPE_INT_RGB);
					// FIXME: interpretation of color values is incorrect
					for (int y = 0; y < nlines; y++) {
						for (int x = 0; x < wquads * 8; x++) {
							byte[] quads = new byte[8];
							dat.readFully(quads);

							int c = dat.read();
							Color col = pal.get(c);
							// log.info(c + " = " + col);
							int rgb = col.getRGB();
							image.setRGB(x, y, rgb);
						}
					}
					ImageIO.write(image, "png", new File("sprite" + id + ".png"));
				}
			} finally {
				dat.close();
			}
		}
	}

	static private byte[] decodeSprite(byte[] data, int wquads, int nlines) {
		return data;
//		int [] sprite = byteArrayToUnsignedIntArray(data);
//
//		for each line {
//   output_pix_index = 0;
//   for (byte_index = 0; byte_index < width / 4; byte_index++) {
//       byte1 = line[byte_index];
//       byte2 = line[byte_index + width / 4];
//       byte3 = line[byte_index + width / 2];
//       byte4 = line[byte_index + 3 * width / 4];
//
//       for (i = 0; i < 8; i++) {
//           pixel = (byte1 & 0x80) >> 7;
//           pixel |= (byte2 & 0x80) >> 6
//           pixel |= (byte3 & 0x80) >> 5;
//           pixel |= (byte4 & 0x80) >> 4;
//           output_pixels[line][output_pix_index + i] = pixel;
//           byte1 = (byte1 << 1) & 0xff;
//           byte2 = (byte2 << 1) & 0xff;
//           byte3 = (byte3 << 1) & 0xff;
//           byte4 = (byte4 << 1) & 0xff;
//       }
//       output_pix_index += 8;
//   }
//}
	}
//	// modifies input
//	static private byte getByte(byte[] chain) {
//		Preconditions.checkArgument(chain.length == 8, chain.length);
//		int b = 0;
//
//		for (int j = 1; j >= 0; j--) {
//			for (int i = 6; i >= 0; i -= 2) {
//				b = (b << 1) | (chain[i] >>> 7);
//				chain[i] = (byte) (chain[i] << 1);
//			}
//		}
//		return (byte) b;
//	}
//
//	static private void decodeSprite(byte[] data, int wquads, int nlines) {
//		int line_size = wquads * 8;
//		byte[] line_buffer = new byte[line_size];
//		byte[] chain = new byte[8];
//
//		for (int line = 0; line < nlines; line++) {
//			int line_ptr = 0;
//			int input_ptr = line * line_size;
//			for (int quad = 0; quad < wquads; quad++) {
//				for (int i = 0; i < 8; i += 2) {
//					chain[i] = data[input_ptr + wquads * i + quad * 2];
//					chain[i + 1] = data[input_ptr + wquads * i + quad * 2 + 1];
//				}
//				for (int i = 0; i < 8; i++) {
//					line_buffer[line_ptr] = getByte(chain);//getByte(Arrays.copyOfRange(chain, i >> 2, 8));
//				}
//			}
//			for (int i = 0; i < line_size; i++) {
//				int n = i + input_ptr;
//				assert n < data.length;
//				data[n] = line_buffer[i];
//			}
//		}
//	}
}
