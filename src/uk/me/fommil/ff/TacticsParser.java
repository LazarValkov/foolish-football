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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import uk.me.fommil.ff.Tactics.BallZone;
import uk.me.fommil.ff.Tactics.PlayerZone;

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
 * the tactic, then bytes 9 to 358 are used to define where the 10 players
 * should run to when the ball is in one of the areas (goalkeeper is not included).
 * The next 10 bytes in the stream (360 to 369) are specific to the tactics editor (pairs)
 * and then the id of the tactics, again used by the tactics editor. There is no magic number
 * to indicate when a tactics stream begins.
 * 
 * @author Samuel Halliday
 * @see <a href="http://bigcalm.tripod.com/swos/tactics-analysis.htm">Tactics File Hex Analysis</a>
 * @see <a href="https://yodasoccer.svn.sourceforge.net/svnroot/yodasoccer/trunk/data/tactics/">Yoda Soccer Tactics</a>
 */
public class TacticsParser {

	private static final Logger log = Logger.getLogger(TacticsParser.class.getName());
	/** Offsets of the tactics in the SWOS ENGLISH.EXE file */
	private static final int[] SWOS_OFFSETS = new int[]{
		1528886, // 442
		1529262, // 541
		1529638, // 451
		1530014, // 532
		1530390, // 352
		1530766, // 433
		1531142, // 424
		1531518, // 343
		1531894, // SWEEP
		1532270, // 523
		1532646, // ATTACK
		1533022 // DEFEND
	};

	/**
	 * Extract the base tactics from the SWOS installation.
	 *
	 * @param dir top level of the SWOS installation.
	 * @return
	 * @throws IOException
	 */
	public static final Map<String, Tactics> getSwosTactics(File dir) throws IOException {
		Preconditions.checkNotNull(dir);
		Preconditions.checkArgument(dir.isDirectory());
		File file = new File(dir.getPath() + File.separator + "ENGLISH.EXE");
		Preconditions.checkArgument(file.isFile(), file);

		Map<String, Tactics> tactics = Maps.newHashMap();
		TacticsParser parser = new TacticsParser();
		FileInputStream in = new FileInputStream(file);
		BufferedInputStream bin = new BufferedInputStream(in);
		try {
			long pointer = 0;
			for (int i = 0; i < SWOS_OFFSETS.length; i++) {
				long skipped = bin.skip(SWOS_OFFSETS[i] - pointer);
				pointer += skipped;
				Preconditions.checkArgument(pointer == SWOS_OFFSETS[i]);
				byte[] b = new byte[370];
				int read = bin.read(b);
				pointer += read;
				Tactics t = parser.parseTacs(b);
				tactics.put(t.getName(), t);
			}
			Preconditions.checkArgument(tactics.size() == 12);
			return tactics;
		} finally {
			bin.close();
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static final void main(String[] args) throws Exception {
		// for A in `find . -type f` ; do grep "4-3-3" $A ; done
		// finds that the tactics are stored in ENGLISH.EXE and SWS!!!_!.EXE
		// File file = new File("data/Sensible World of Soccer 96-97/ENGLISH.EXE");
		File dir = new File("data/Sensible World of Soccer 96-97");
		log.info(getSwosTactics(dir).keySet().toString());
	}

	public Tactics parseTacs(byte[] tac) {
		Preconditions.checkNotNull(tac);
		Preconditions.checkArgument(tac.length == 370);

		StringBuilder name = new StringBuilder(8);
		for (int i = 0; i < 8; i++) {
			int c = tac[i];
			if (c == 0)
				break;
			name.append((char) c);
		}
		Tactics tactics = new Tactics(name.toString());

		for (int p = 0; p < 10; p++) {
			for (int x = 0; x < 5; x++) {
				for (int y = 0; y < 7; y++) {
					int i = 9 + (p * 35) + (y * 5) + x;
					int loc = unsignedByteToInt(tac[i]);
					Preconditions.checkArgument(loc >= 0, Joiner.on(",").join(loc, tactics, p, x, y, i));
					int px = (loc >> 4) & 0x0F;
					assert px >= 0 && px < 15 : Joiner.on(",").join(loc, tactics, p, x, y, i, px);
					int py = loc & 0x0F;
					assert py >= 0 && py < 16 : Joiner.on(",").join(loc, tactics, p, x, y, i, py);
					tactics.set(new BallZone(x, y), p + 2, new PlayerZone(px, py));
				}
			}
		}
		// ignore the final "pair" and "id" fields

		return tactics;
	}

	/**
	 * @param b
	 * @return
	 */
	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}
}
