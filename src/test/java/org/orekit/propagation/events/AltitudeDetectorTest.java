/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.events;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class AltitudeDetectorTest {

    @Test
    public void testBackAndForth() throws OrekitException {

        final Frame EME2000 = FramesFactory.getEME2000();
        final AbsoluteDate initialDate = new AbsoluteDate(2009,1,1,TimeScalesFactory.getUTC());
        final double a = 8000000;
        final double e = 0.1;
        final double earthRadius = 6378137.0;
        final double earthF = 1.0 / 298.257223563;
        final double apogee = a*(1+e);
        final double alt = apogee - earthRadius - 500;



        // initial state is at apogee
        final Orbit initialOrbit = new KeplerianOrbit(a,e,0,0,0,FastMath.PI,PositionAngle.MEAN,EME2000,
                                                      initialDate,CelestialBodyFactory.getEarth().getGM());
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        final KeplerianPropagator kepPropagator = new KeplerianPropagator(initialOrbit);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(earthRadius, earthF, EME2000);
        final AltitudeDetector altDetector = new AltitudeDetector(alt, earth).
                                             withHandler(new StopOnEvent<AltitudeDetector>());
        Assert.assertEquals(alt, altDetector.getAltitude(), 1.0e-15);
        Assert.assertSame(earth, altDetector.getBodyShape());

        // altitudeDetector should stop propagation upon reaching required altitude
        kepPropagator.addEventDetector(altDetector);

        // propagation to the future
        SpacecraftState finalState = kepPropagator.propagate(initialDate.shiftedBy(1000));
        Assert.assertEquals(finalState.getPVCoordinates().getPosition().getNorm()-earthRadius,alt,1e-5);
        Assert.assertEquals(44.079, finalState.getDate().durationFrom(initialDate), 1.0e-3);

        // propagation to the past
        kepPropagator.resetInitialState(initialState);
        finalState = kepPropagator.propagate(initialDate.shiftedBy(-1000));
        Assert.assertEquals(finalState.getPVCoordinates().getPosition().getNorm()-earthRadius,alt,1e-5);
        Assert.assertEquals(-44.079, finalState.getDate().durationFrom(initialDate), 1.0e-3);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

