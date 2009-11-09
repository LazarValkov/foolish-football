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

import java.awt.Point;

/**
 * Represents the team tactics, which dictates where each {@link Player} should be
 * based on the location of the ball.
 * <p>
 * Future releases may wish to import a SWOS TAC file, containing legacy tactics.
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
 * the tactic, then bytes 9 to 166 (hex) are used to define where the 10 players
 * should run to when the ball is in one of the areas (goalkeeper is not included).
 * The last few bytes in the file from 167 to 171 are unknown and always
 * {@code 00 FF FF 00 01 FF FF 01 FF FF 00}.
 * <p>
 * Within the file, player positions are held in sequences. Use the following formula to
 * find the byte defining where the player will run to:
 * {@code (Player No * 35) - 26 + (Ball Location - 1)}.
 * <p>
 * For example, if we have byte 7F in position 120 of the file, this means that when the
 * ball is in quadrant 35 (top left corner), player 8, normally the left winger will be
 * at the goal-mouth of the opponents.
 *
 * @author Samuel Halliday
 * @see <a href="http://bigcalm.tripod.com/swos/tactics-analysis.htm">Tactics File Hex Analysis</a>
 */
public class Tactics {

	/**
	 * @return the tactics associated to the kick off position.
	 */
	public static Tactics getKickOff() {
		return new Tactics();
	}

	/**
	 * @param i
	 * @return
	 */
	public Point getLocation(int i) {
		assert i >= 0 && i < 11;

		return new Point(250, 250);
	}

	// TODO TAC parser
	// TODO read the SWOS basic tactics from the data files
	// it appears a
	// for A in `find . -type f` ; do grep "4-3-3" $A ; done
	// finds that the tactics are stored in ENGLISH.EXE and SWS!!!_!.EXE
}
