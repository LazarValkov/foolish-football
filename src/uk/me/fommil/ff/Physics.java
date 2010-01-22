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
package uk.me.fommil.ff;

import com.google.common.base.Preconditions;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DPlane;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeConstants;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.OdeInit;

/**
 *
 * @author Samuel Halliday
 */
public class Physics {

	/** @param args */
	public static final void main(String[] args) {
		// http://www.alsprogrammingresource.com/basic_ode.html
		// http://opende.sourceforge.net/wiki/index.php/HOWTO_simple_bouncing_sphere

		OdeInit.dInitODE();

		DWorld world = OdeHelper.createWorld();
		world.setGravity(0, 0, -9.81);

		DSpace space = OdeHelper.createSimpleSpace();
		DJointGroup joints = OdeHelper.createJointGroup();
		NearCallback near = new NearCallback(world, joints);

		DBody body = OdeHelper.createBody(world);
		DBox box = OdeHelper.createBox(space, 1, 1, 1);
		box.setBody(body);
		body.setPosition(0, 0, 100);
		DMass mass = OdeHelper.createMass();
		mass.setBox(1, 1, 1, 1);
		body.setMass(mass);
		body.setLinearVel(1, -1, 0);

		DPlane ground = OdeHelper.createPlane(space, 0, 0, 1, 0);

		for (int i = 0; i < 1000; i++) {
			space.collide(null, near);
			world.step(0.01);
			joints.empty();
			System.out.println(body.getPosition());
		}

		joints.destroy();
		world.destroy();
		space.destroy();

		OdeInit.dCloseODE();
	}

	private static class NearCallback implements DNearCallback {

		private final DWorld world;

		private final DJointGroup joints;

		private NearCallback(DWorld world, DJointGroup joints) {
			this.world = world;
			this.joints = joints;
		}

		@Override
		public void call(Object data, DGeom o1, DGeom o2) {
			Preconditions.checkNotNull(o1, "o1");
			Preconditions.checkNotNull(o2, "o2");

			DBody b1 = o1.getBody();
			DBody b2 = o2.getBody();

			final int MAX_CONTACTS = 8;
			DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);
			int numc = OdeHelper.collide(o1, o2, MAX_CONTACTS, contacts.getGeomBuffer());

			for (int i = 0; i < numc; i++) {
				contacts.get(i).surface.mode = OdeConstants.dContactBounce;
				contacts.get(i).surface.bounce = 0.5;
				DJoint c = OdeHelper.createContactJoint(world, joints, contacts.get(i));
				c.attach(b1, b2);
			}
		}
	};
}
