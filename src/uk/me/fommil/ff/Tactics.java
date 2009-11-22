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
import com.google.common.collect.Maps;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.vecmath.Point3d;

/**
 * Represents the team tactics, which dictates where each {@link Player} should be
 * based on the location of the ball.
 * 
 * @author Samuel Halliday
 */
public class Tactics {

	/**
	 * Represents one of the zones that the ball may be in, relative to the team.
	 * The pitch is broken into 7 rows and 5 columns, indexed (0, 0) for the team's
	 * right defending corner and indexed (4, 6) for the team's left attacking
	 * corner.
	 */
	public static class BallZone {

		private final int x;
		private final int y;

		/**
		 * @param x
		 * @param y
		 */
		public BallZone(int x, int y) {
			Preconditions.checkArgument(x >= 0 && x < 5, x);
			Preconditions.checkArgument(y >= 0 && y < 7, y);
			this.x = x;
			this.y = y;
		}

		/**
		 * @param width
		 * @param height
		 * @return the central point represented by this for an image of the given width and height
		 */
		@Deprecated
		public Point getLocation(int width, int height) {
			int xx = (width * x) / 5 - 1;
			int yy = (height * y) / 7 - 1;
			return new Point(xx, yy);
		}

		@Override
		public boolean equals(Object obj) {
			// <editor-fold defaultstate="collapsed" desc="boilerplate identity, instanceof and cast">
			if (this == obj)
				return true;
			if (!(obj instanceof BallZone))
				return false;
			final BallZone other = (BallZone) obj;// </editor-fold>
			return x == other.x && y == other.y;
		}

		@Override
		public int hashCode() {
			return x + 17 * y;
		}

		@Override
		public String toString() {
			return "(" + x + ", " + y + ")";
		}
	}

	/**
	 * Represents one of the zones that the player may be in, relative to the team.
	 * The pitch is broken into 16 rows and 15 columns, indexed (0, 0) for the team's
	 * right defending corner and indexed (14, 15) for the team's left attacking
	 * corner.
	 */
	public static class PlayerZone {

		private final int x;
		private final int y;

		/**
		 * @param x
		 * @param y
		 */
		public PlayerZone(int x, int y) {
			Preconditions.checkArgument(x >= 0 && x < 15);
			Preconditions.checkArgument(y >= 0 && y < 16);
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj) {
			// <editor-fold defaultstate="collapsed" desc="boilerplate identity, instanceof and cast">
			if (this == obj)
				return true;
			if (!(obj instanceof PlayerZone))
				return false;
			final PlayerZone other = (PlayerZone) obj;// </editor-fold>
			return x == other.x && y == other.y;
		}

		@Override
		public int hashCode() {
			return x + 17 * y;
		}

		@Override
		public String toString() {
			return "(" + x + ", " + y + ")";
		}

		/**
		 * @param upwards true if the team is playing upwards
		 * @param width
		 * @param height
		 * @return the central point represented by this for an image of the given width and height
		 */
		@Deprecated
		public Point getLocation(boolean upwards, int width, int height) {
			int xx = (width * x) / 15 - 1;
			int yy = (height * y) / 16 - 1;
			if (upwards) {
				yy = height - yy;
				xx = width - xx;
			}
			return new Point(xx, yy);
		}

		/**
		 * @param upwards true if the team is playing upwards
		 * @param width
		 * @param height
		 * @return the central point represented by this for a pitch of the given width and height
		 */
		public Point3d getCentre(boolean upwards, double width, double height) {
			double xx = (width * x) / 15 - 1;
			double yy = (height * y) / 16 - 1;
			if (upwards) {
				yy = height - yy;
				xx = width - xx;
			}
			return new Point3d(xx, yy, 0);
		}
	}
	private String name;
	private final Map<BallZone, Map<Integer, PlayerZone>> zones = Maps.newHashMapWithExpectedSize(35);

	/**
	 * @param name
	 */
	public Tactics(String name) {
		setName(name);
	}

	/**
	 * @param ballZone
	 * @param shirt
	 * @return the zone the player is in for the given ball location, or {@code null}
	 * if no zone defined.
	 */
	public PlayerZone getZone(BallZone ballZone, Integer shirt) {
		Preconditions.checkNotNull(shirt);
		Preconditions.checkArgument(shirt > 1 && shirt < 12);
		Map<Integer, PlayerZone> shirt2player = getZones(ballZone);
		if (shirt2player == null)
			return null;
		return shirt2player.get(shirt);
	}

	/**
	 * @param ballZone
	 * @return the zone the player is in for the given ball location, or {@code null}
	 * if no zone defined.
	 */
	public Map<Integer, PlayerZone> getZones(BallZone ballZone) {
		Preconditions.checkNotNull(ballZone);
		return zones.get(ballZone);
	}

	/**
	 * @param ballZone
	 * @param shirt
	 * @param playerZone
	 */
	public void set(BallZone ballZone, Integer shirt, PlayerZone playerZone) {
		Preconditions.checkNotNull(ballZone);
		Preconditions.checkNotNull(playerZone);
		Preconditions.checkNotNull(shirt);

		if (!zones.containsKey(ballZone))
			zones.put(ballZone, new HashMap<Integer, PlayerZone>(10));
		Map<Integer, PlayerZone> shirt2player = zones.get(ballZone);
		shirt2player.put(shirt, playerZone);
	}

	@Override
	public String toString() {
		return name;
	}

	String debugView() {
		StringBuilder builder = new StringBuilder();
		builder.append("==========");
		builder.append(getName());
		builder.append("==========\n");
		for (int x = 4; x >= 0; x--) {
			for (int y = 6; y >= 0; y--) {
				BallZone bz = new BallZone(x, y);
				builder.append("=====");
				builder.append(bz);
				builder.append("=====\n");
				Map<Integer, PlayerZone> z = getZones(bz);
				Collection<PlayerZone> zs = z.values();
				if (z == null)
					continue;
				for (int yy = 15; yy >= 0; yy--) {
					for (int xx = 14; xx >= 0; xx--) {
						PlayerZone pz = new PlayerZone(xx, yy);
						if (zs.contains(pz)) {
							for (Entry<Integer, PlayerZone> entry : z.entrySet()) {
								if (entry.getValue().equals(pz)) {
									builder.append(Integer.toHexString(entry.getKey()));
									// can't overwrite anyway
									continue;
								}
							}
						} else {
							builder.append(' ');
						}
					}
					builder.append("|\n");
				}
			}
		}
		return builder.toString();
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public String getName() {
		return name;
	}

	public void setName(String name) {
		Preconditions.checkNotNull(name);
		Preconditions.checkArgument(!name.isEmpty());
		this.name = name;
	}
	// </editor-fold>
}
