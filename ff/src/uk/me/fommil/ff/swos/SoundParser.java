/*
 * Copyright Samuel Halliday 2010
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import uk.me.fommil.ff.Main;

/**
 * Reads the SWOS commentary and SFX files into a Java compatible format.
 *
 * @author Samuel Halliday
 * @see <a href="http://swos.hajas.org/sounds.htm">Hajas SWOS Brasil</a>
 */
public class SoundParser {

	private static final AudioFormat SWOS_RAW_FORMAT = new AudioFormat(22000, 8, 1, false, true);

	enum Commentary {

		GOALS, ON_THE_POST, NEAR_MISSES, OWN_GOALS, CLAIMED_BY_THE_KEEPER,
		SAVES_BY_THE_KEEPER, PENALTIES, SCORED_PENALTIES, MISSED_PENALTIES, SAVED_PENALTIES,
		GOOD_PASSES, GOOD_PLAY, FOULS, GOOD_JUMPING_HEADERS, DISPUTED_REFEREE

	}

	private static final Multimap<Commentary, String> commentary = ArrayListMultimap.create();

	static {
		commentary.put(Commentary.GOALS, "M158_1_.raw");
		commentary.put(Commentary.GOALS, "M158_2_.raw");
		commentary.put(Commentary.GOALS, "M158_3_.raw");
		commentary.put(Commentary.GOALS, "M158_4_.raw");
		commentary.put(Commentary.GOALS, "M158_5_.raw");
		commentary.put(Commentary.GOALS, "M158_7_.raw");

		commentary.put(Commentary.ON_THE_POST, "M10_f_.raw");
		commentary.put(Commentary.ON_THE_POST, "M10_g_.raw");
		commentary.put(Commentary.ON_THE_POST, "M10_i_.raw");
		commentary.put(Commentary.ON_THE_POST, "M10_s_.raw");
		commentary.put(Commentary.ON_THE_POST, "M10_t_.raw");
		commentary.put(Commentary.ON_THE_POST, "M10_u_.raw");

		commentary.put(Commentary.NEAR_MISSES, "M10_d_.raw");
		commentary.put(Commentary.NEAR_MISSES, "M10_e_.raw");
		commentary.put(Commentary.NEAR_MISSES, "M10_j_.raw");
		commentary.put(Commentary.NEAR_MISSES, "M10_k_.raw");
		commentary.put(Commentary.NEAR_MISSES, "M10_r_.raw");

		commentary.put(Commentary.OWN_GOALS, "M158_p_.raw");
		commentary.put(Commentary.OWN_GOALS, "M158_q_.raw");
		commentary.put(Commentary.OWN_GOALS, "M158_s_.raw");
		commentary.put(Commentary.OWN_GOALS, "M158_t_.raw");
		commentary.put(Commentary.OWN_GOALS, "M158_x_.raw");
		commentary.put(Commentary.OWN_GOALS, "M158_y_.raw");

		commentary.put(Commentary.CLAIMED_BY_THE_KEEPER, "M313_6_.raw");
		commentary.put(Commentary.CLAIMED_BY_THE_KEEPER, "M313_8_.raw");
		commentary.put(Commentary.CLAIMED_BY_THE_KEEPER, "M313_9_.raw");
		commentary.put(Commentary.CLAIMED_BY_THE_KEEPER, "M313_c_.raw");
		commentary.put(Commentary.CLAIMED_BY_THE_KEEPER, "M313_d_.raw");
		commentary.put(Commentary.CLAIMED_BY_THE_KEEPER, "M313_g_.raw");

		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M196_w_.raw");
		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M196_x_.raw");
		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M196_z_.raw");
		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M233_1_.raw");
		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M313_7_.raw");
		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M313_a_.raw");
		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M313_b_.raw");
		commentary.put(Commentary.SAVES_BY_THE_KEEPER, "M313_h_.raw");
		// files.put(Commentary.SAVES_BY_THE_KEEPER, "M313_e_.raw"); // bad sampling in original

		commentary.put(Commentary.PENALTIES, "M196_k_.raw");
		commentary.put(Commentary.PENALTIES, "M196_l_.raw");
		commentary.put(Commentary.PENALTIES, "M196_m_.raw");
		commentary.put(Commentary.PENALTIES, "M196_n_.raw");

		commentary.put(Commentary.SCORED_PENALTIES, "M233_7_.raw");
		commentary.put(Commentary.SCORED_PENALTIES, "M233_8_.raw");
		commentary.put(Commentary.SCORED_PENALTIES, "M233_9_.raw");
		commentary.put(Commentary.SCORED_PENALTIES, "M233_c_.raw");

		commentary.put(Commentary.MISSED_PENALTIES, "M233_2_.raw");
		commentary.put(Commentary.MISSED_PENALTIES, "M233_4_.raw");
		commentary.put(Commentary.MISSED_PENALTIES, "M233_5_.raw");
		commentary.put(Commentary.MISSED_PENALTIES, "M233_6_.raw");

		commentary.put(Commentary.SAVED_PENALTIES, "M196_t_.raw");
		commentary.put(Commentary.SAVED_PENALTIES, "M196_u_.raw");
		commentary.put(Commentary.SAVED_PENALTIES, "M196_v_.raw");

		commentary.put(Commentary.GOOD_PASSES, "M313_i_.raw");
		commentary.put(Commentary.GOOD_PASSES, "M313_j_.raw");
		commentary.put(Commentary.GOOD_PASSES, "M313_o_.raw");

		commentary.put(Commentary.GOOD_PLAY, "M349_4_.raw");
		commentary.put(Commentary.GOOD_PLAY, "M349_7_.raw");
		commentary.put(Commentary.GOOD_PLAY, "M349_8_.raw");
		commentary.put(Commentary.GOOD_PLAY, "M349_e_.raw");
		commentary.put(Commentary.GOOD_PLAY, "M365_1_.raw");

		commentary.put(Commentary.FOULS, "M158_z_.raw");
		commentary.put(Commentary.FOULS, "M195.raw");
		commentary.put(Commentary.FOULS, "M196_1_.raw");
		commentary.put(Commentary.FOULS, "M196_2_.raw");
		commentary.put(Commentary.FOULS, "M196_4_.raw");
		commentary.put(Commentary.FOULS, "M196_6_.raw");
		commentary.put(Commentary.FOULS, "M196_7_.raw");

		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_2_.raw");
		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_3_.raw");
		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_4_.raw");
		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_5_.raw");
		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_6_.raw");
		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_8_.raw");
		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_a_.raw");
		commentary.put(Commentary.GOOD_JUMPING_HEADERS, "M33_b_.raw");

		commentary.put(Commentary.DISPUTED_REFEREE, "M158__.raw");
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static final void main(String[] args) throws Exception {
//		File file = new File(Main.SWOS + "/SFX/PIERCE/M196_K_.RAW"); // "penalty" SFX
		//File file = new File(Main.SWOS + "/HARD/CHEER.RAW");

		for (String filename : commentary.get(Commentary.GOOD_JUMPING_HEADERS)) {
			File file = new File(Main.SWOS + "/SFX/PIERCE/" + filename);
			InputStream in = new FileInputStream(file);

			AudioInputStream audio = new AudioInputStream(in, SWOS_RAW_FORMAT, file.length());

			Clip clip = AudioSystem.getClip();
			clip.open(audio);
			clip.start();

			clip.drain();
			audio.close();
		}
	}
}
