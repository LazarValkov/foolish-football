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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
 * The last 10 bytes in the stream (359 to 368) always seem to be
 * {@code 00 FF FF 00 01 FF FF 01 FF FF}.
 * 
 * @author Samuel Halliday
 * @see <a href="http://bigcalm.tripod.com/swos/tactics-analysis.htm">Tactics File Hex Analysis</a>
 */
public class TacticsParser {

	private static final Logger log = Logger.getLogger(TacticsParser.class.getName());
	// the magic binaries that end a tactics instance
	private static final int[] TAC = new int[]{0x00, 0xFF, 0xFF, 0x00, 0x01, 0xFF, 0xFF, 0x01, 0xFF, 0xFF};

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
		for (Tactics t : parser.extractTactics(in)) {
			tactics.put(t.getName(), t);
		}
		return tactics;
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
		TacticsParser parser = new TacticsParser();
		Collection<Tactics> tactics = Sets.newLinkedHashSet();
		for (File file : dir.listFiles()) {
			if (!file.isFile())
				continue;
			FileInputStream in = new FileInputStream(file);
			tactics.addAll(parser.extractTactics(in));
		}
		log.info(tactics.toString());
		for (Tactics tactic : tactics) {
			System.out.println(tactic.debugView());
		}
	}

	/**
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public Collection<Tactics> extractTactics(InputStream in) throws IOException {
		Preconditions.checkNotNull(in);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		int read = -1;
		byte[] buf = new byte[1024];
		try {
			while ((read = in.read(buf)) != -1) {
				baos.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
		byte[] bytes = baos.toByteArray();
		List<byte[]> tacs = extractTacs(bytes);
		Collection<Tactics> tactics = Sets.newLinkedHashSet();
		for (byte[] tac : tacs) {
			tactics.add(parseTacs(tac));
		}
		return tactics;
	}

	private List<byte[]> extractTacs(byte[] bytes) {
		List<byte[]> tacs = Lists.newArrayList();
		byte[] tac;

		for (int i = 359; i < bytes.length - TAC.length; i++) {
			for (int j = 0; j < TAC.length; j++) {
				if (bytes[i + j] != (byte) TAC[j])
					break;
				if (j == TAC.length - 1) {
					tac = Arrays.copyOfRange(bytes, i - 359, i + 10);
					tacs.add(tac);
				}
			}
		}
		return tacs;
	}

	private Tactics parseTacs(byte[] tac) {
		assert tac.length == 369;
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
