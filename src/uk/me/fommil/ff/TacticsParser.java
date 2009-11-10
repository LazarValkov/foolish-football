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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Import a SWOS TAC file, containing legacy tactics.
 * The referenced documentation should be of interest, which notes that the field
 * is broken into 35 sections, 5 columns and 7 rows, starting at the bottom right
 * and proceeding left along the rows then up the columns. The opponent's net is always at
 * the top in this frame.
 * <p>
 * The tactics file is divided into
 * players positions that they should run to when the ball is in one of the 35
 * positions. The player positions, of which there are {@code 15 x 16 = 240}, are stored as
 * two hex numbers (X-Y coordinates), {@code 00} being the bottom right and {@code EF} being
 * the top left.
 * <p>
 * The first 8 bytes of the file are a string containing the name of
 * the tactic, then bytes 9 to 359 are used to define where the 10 players
 * should run to when the ball is in one of the areas (goalkeeper is not included).
 * The last 10 bytes in the stream (360 to 369) always seem to be
 * {@code 00 FF FF 00 01 FF FF 01 FF FF}.
 * 
 * @author Samuel Halliday
 * @see <a href="http://bigcalm.tripod.com/swos/tactics-analysis.htm">Tactics File Hex Analysis</a>
 */
public class TacticsParser {

	private static final Logger log = Logger.getLogger(TacticsParser.class.getName());
	private static final int[] TAC = new int[]{0x00, 0xFF, 0xFF, 0x00, 0x01, 0xFF, 0xFF, 0x01, 0xFF, 0xFF};

	/**
	 * @param args
	 * @throws Exception
	 */
	public static final void main(String[] args) throws Exception {
		// for A in `find . -type f` ; do grep "4-3-3" $A ; done
		// finds that the tactics are stored in ENGLISH.EXE and SWS!!!_!.EXE
		// File file = new File("data/Sensible World of Soccer 96-97/ENGLISH.EXE");
		File dir = new File("data/Sensible World of Soccer 96-97");
		for (File file : dir.listFiles()) {
			if (!file.isFile())
				continue;
			extractTacs(file);
		}
	}

	private static void extractTacs(File file) throws Exception {
		FileInputStream is = new FileInputStream(file);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		int read = -1;
		byte[] buf = new byte[1024];
		try {
			while ((read = is.read(buf)) != -1) {
				baos.write(buf, 0, read);
			}
		} finally {
			is.close();
		}
		byte[] bytes = baos.toByteArray();
		List<byte[]> tacs = extractTac(bytes);
		if (!tacs.isEmpty())
			log.info("EXTRACTED " + tacs.size() + " from " + file);
	}

	private static List<byte[]> extractTac(byte[] bytes) {
		List<byte[]> tacs = Lists.newArrayList();
		byte[] tac;

		for (int i = 359; i < bytes.length - TAC.length; i++) {
			for (int j = 0; j < TAC.length; j++) {
				if (bytes[i + j] != (byte) TAC[j])
					break;
				if (j == TAC.length - 1) {
					tac = Arrays.copyOfRange(bytes, i - 360, i + 9);
					tacs.add(tac);
					StringBuilder name = new StringBuilder(8);
					for (int k = 0; k < 8; k++) {
						name.append((char) tac[k]);
					}
					log.info("MATCHED: " + name);
				}
			}
		}
		return tacs;
	}
}
