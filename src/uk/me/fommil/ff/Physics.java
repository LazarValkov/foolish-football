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

import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DHashSpace;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DPlane;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

/**
 *
 * @author Samuel Halliday
 */
public class Physics {

	/** @param args */
	public static final void main(String[] args) {
		// http://www.alsprogrammingresource.com/basic_ode.html
		// http://opende.sourceforge.net/wiki/index.php/HOWTO_simple_bouncing_sphere
		DWorld world = OdeHelper.createWorld();
		world.setGravity(0, 0, -9.81);

		DHashSpace space = OdeHelper.createHashSpace();

		final DBody body = OdeHelper.createBody(world);
		body.setPosition(0, 0, 100);
		DMass mass = OdeHelper.createMass();
		mass.setBox(1, 1, 1, 1);
		body.setMass(mass);

		DBox box = OdeHelper.createBox(space, 1, 1, 1);
		box.setBody(body);

		DPlane plane = OdeHelper.createPlane(space, 0, 0, 1, 0);

		for (int i = 0; i < 1000; i++) {
			space.collide(null, nearCallback);

			world.step(0.01);
			System.out.println(body.getPosition());
		}

		world.destroy();
	}

	private static final DNearCallback nearCallback = new DNearCallback() {

		@Override
		public void call(Object data, DGeom o1, DGeom o2) {
			DBody b1 = o1.getBody();
			DBody b2 = o2.getBody();
//			DContact contact = new DContact();
//			contact.surface.mode = dContactBounce | dContactSoftCFM;
//			// friction parameter
//			contact.surface.mu = dInfinity;
//			// bounce is the amount of "bouncyness".
//			contact.surface.bounce = 0.9;
//			// bounce_vel is the minimum incoming velocity to cause a bounce
//			contact.surface.bounce_vel = 0.1;
//			// constraint force mixing parameter
//			contact.surface.soft_cfm = 0.001;
//			if (int
//				numc = dCollide(o1, o2, 1, &  contact.geom, sizeof(dContact))
//
//				     ) {
//        dJointID c = dJointCreateContact (world,contactgroup, & contact);
//				dJointAttach(c, b1, b2);
//		}
		}
	};

}
