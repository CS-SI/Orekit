/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;


import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.SimpleExponentialAtmosphere;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class DSSTAtmosphericDragTest {

    @Test
    public void testMeanElementRate() throws OrekitException {
        final Frame itrf = FramesFactory.getITRF2005();
        final Atmosphere atm = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Utils.ae, 1.0 / 298.257222101, itrf),
                                                               itrf, 0.0004, 42000.0, 7500.0);
        final DragSensitive sc = new SphericalSpacecraft(5., 2., 0., 0.);
        final DSSTForceModel force = new DSSTAtmosphericDrag(atm, sc);

        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double mu  = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);

        double[] daidt = force.getMeanElementRate(new SpacecraftState(orbit));

        for (int i = 0; i < daidt.length; i++) {
            System.out.println(daidt[i]);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
