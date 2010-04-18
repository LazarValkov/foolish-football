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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import uk.me.fommil.ff.Main;

/**
 * @author Samuel Halliday
 * @see <a href="http://swos.hajas.org/sounds.htm">Hajas SWOS Brasil</a>
 */
public class SoundParser {

	private static final AudioFormat SWOS_RAW_FORMAT = new AudioFormat(22000, 8, 1, false, true);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static final void main(String[] args) throws Exception {
		File file = new File(Main.SWOS + "/SFX/PIERCE/M196_K_.RAW"); // "penalty" SFX
//		File file = new File(Main.SWOS + "/SFX/ENGLISH/ECA01.RAW");

		InputStream in = new FileInputStream(file);

		AudioInputStream audio = new AudioInputStream(in, SWOS_RAW_FORMAT, file.length());

		Clip clip = AudioSystem.getClip();
		clip.open(audio);
		clip.start();

		audio.close();
	}
}
