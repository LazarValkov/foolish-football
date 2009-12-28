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
import com.google.common.collect.Maps;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import uk.me.fommil.ff.swos.SwosUtils;

/**
 * Represents a football team, including the list of available {@link Player}s and
 * {@link Tactics}.
 *
 * @author Samuel Halliday
 */
public class Team {

	private Tactics current;
	private List<Player> players = Lists.newArrayList();

	/** */
	public Team() {
		for (int i = 0; i < 20; i++) {
			players.add(new Player());
		}
	}

	/**
	 * @return the currently chosen tactics.
	 */
	public Tactics getCurrentTactics() {
		return current;
	}

	public void setCurrentTactics(Tactics tactics) {
		this.current = tactics;
	}

	/**
	 * @return
	 */
	public List<Player> getPlayers() {
		return Collections.unmodifiableList(players);
	}

	public Map<Color, Color> getTeamColors() {
		Map<Color, Color> colours = Maps.newHashMap();
		List<Color> pal = SwosUtils.getPalette();
		colours.put(pal.get(10), Color.RED);
		colours.put(pal.get(11), Color.RED);
		colours.put(pal.get(14), Color.WHITE);
		colours.put(pal.get(15), Color.RED);
		return colours;
	}
}
