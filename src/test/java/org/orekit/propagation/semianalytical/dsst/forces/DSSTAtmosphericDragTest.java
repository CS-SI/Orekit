/* Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst.forces;


import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.HarrisPriester;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class DSSTAtmosphericDragTest {

    @Test
    public void testMeanElementRate() throws OrekitException {
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, FramesFactory.getITRF2005(true));
        final Atmosphere atm = new HarrisPriester(sun, earth);
        final DSSTForceModel force = new DSSTAtmosphericDrag(atm, 2., 5.);

        // Equinoxe 21 mars 2003 à 1h00m
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final double mu   = 3.9860047e14;
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);

        force.initialize(new AuxiliaryElements(orbit, 1));
        final double[] daidt = force.getMeanElementRate(new SpacecraftState(orbit));

        final double[] daidtRef = new double[6];

        Assert.assertEquals(daidtRef[0], daidt[0], 4.1e-5);
        Assert.assertEquals(daidtRef[1], daidt[1], 1.5e-12);
        Assert.assertEquals(daidtRef[2], daidt[2], 1.4e-12);
        Assert.assertEquals(daidtRef[3], daidt[3], 1.1e-13);
        Assert.assertEquals(daidtRef[4], daidt[4], 6.5e-14);
        Assert.assertEquals(daidtRef[5], daidt[5], 2.5e-15);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
