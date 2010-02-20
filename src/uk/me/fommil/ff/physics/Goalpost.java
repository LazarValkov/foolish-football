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
package uk.me.fommil.ff.physics;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.logging.Logger;
import javax.media.j3d.BoundingBox;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DFixedJoint;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.Pitch;

/**
 * The model (M) and controller (C) for a goal during game play.
 *
 * @author Samuel Halliday
 */
public class Goalpost {

	private static final Logger log = Logger.getLogger(Goalpost.class.getName());

	private final DBody body;

	/**
	 * @param bbox
	 * @param posts the width of the posts, inset from the bounding box
	 * @param facing
	 */
	Goalpost(DWorld world, DSpace space, Pitch pitch, Direction facing) {
		Preconditions.checkNotNull(world);
		Preconditions.checkNotNull(space);
		Preconditions.checkArgument(facing == Direction.NORTH || facing == Direction.SOUTH);

		this.body = OdeHelper.createBody(world);

		double width = pitch.getGoalWidth();
		double height = pitch.getGoalHeight();
		double depth = pitch.getGoalDepth();

		DBox box = OdeHelper.createBox(space, width, depth, height);
		box.setBody(body);
		body.setData(this);

		DVector3 centre;
		if (facing == Direction.NORTH) {
			centre = pitch.getGoalBottom().toDVector();
		} else {
			centre = pitch.getGoalTop().toDVector();
		}
		centre.add(2, height / 2);
		log.info(centre.toString());
		body.setPosition(centre);

		DFixedJoint fixed = OdeHelper.createFixedJoint(world, null);
		fixed.attach(null, body);
		fixed.setFixed();
	}
}
