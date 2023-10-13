/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.propagation.conversion;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

public class KeplerianConverterTest {

    private Orbit orbit;

    private final static Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    private final static Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    private final static double mu = 3.9860047e14;

    @Test
    public void testConversionPositionVelocity() {
        checkFit(orbit, 86400, 300, 1.0e-3, false, 1.901e-8);
    }

    @Test
    public void testConversionPositionOnly() {
        checkFit(orbit, 86400, 300, 1.0e-3, true, 2.691e-8);
    }

    @Test
    public void testConversionWithFreeParameter() {
        Assertions.assertThrows(OrekitException.class, () -> {
            checkFit(orbit, 86400, 300, 1.0e-3, true, 2.65e-8, "toto");
        });
    }

    protected void checkFit(final Orbit orbit,
                            final double duration,
                            final double stepSize,
                            final double threshold,
                            final boolean positionOnly,
                            final double expectedRMS,
                            final String... freeParameters)
        {

        Propagator p = new KeplerianPropagator(orbit);
        List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt < duration; dt += stepSize) {
            sample.add(p.propagate(orbit.getDate().shiftedBy(dt)));
        }

        PropagatorBuilder builder = new KeplerianPropagatorBuilder(OrbitType.KEPLERIAN.convertType(orbit),
                                                                   PositionAngleType.MEAN,
                                                                   1.0);

        FiniteDifferencePropagatorConverter fitter = new FiniteDifferencePropagatorConverter(builder, threshold, 1000);

        fitter.convert(sample, positionOnly, freeParameters);

        Assertions.assertEquals(expectedRMS, fitter.getRMS(), 0.01 * expectedRMS);

        KeplerianPropagator prop = (KeplerianPropagator)fitter.getAdaptedPropagator();
        Orbit fitted = prop.getInitialState().getOrbit();

        final double eps = 1.0e-12;
        Assertions.assertEquals(orbit.getPosition().getX(),
                            fitted.getPosition().getX(),
                            eps * orbit.getPosition().getX());
        Assertions.assertEquals(orbit.getPosition().getY(),
                            fitted.getPosition().getY(),
                            eps * orbit.getPosition().getY());
        Assertions.assertEquals(orbit.getPosition().getZ(),
                            fitted.getPosition().getZ(),
                            eps * orbit.getPosition().getZ());

        Assertions.assertEquals(orbit.getPVCoordinates().getVelocity().getX(),
                            fitted.getPVCoordinates().getVelocity().getX(),
                            -eps * orbit.getPVCoordinates().getVelocity().getX());
        Assertions.assertEquals(orbit.getPVCoordinates().getVelocity().getY(),
                            fitted.getPVCoordinates().getVelocity().getY(),
                            eps * orbit.getPVCoordinates().getVelocity().getY());
        Assertions.assertEquals(orbit.getPVCoordinates().getVelocity().getZ(),
                            fitted.getPVCoordinates().getVelocity().getZ(),
                            eps * orbit.getPVCoordinates().getVelocity().getZ());

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                     FramesFactory.getEME2000(), initDate, mu);
    }

}

