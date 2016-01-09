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
package org.orekit.propagation.conversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class KeplerianConverterTest {

    private Orbit orbit;

    private final static Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    private final static Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    private final static double mu = 3.9860047e14;

    @Test
    public void testWrongParametersSize() throws OrekitException {
        try {
            PropagatorBuilder builder =
                            new KeplerianPropagatorBuilder(mu, orbit.getFrame(),
                                                           OrbitType.CIRCULAR, PositionAngle.TRUE);
            final List<String> empty = Collections.emptyList();
            builder.setFreeParameters(empty);
            builder.buildPropagator(orbit.getDate(), new double[3]);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(LocalizedFormats.DIMENSIONS_MISMATCH_SIMPLE, oiae.getSpecifier());
            Assert.assertEquals(3, ((Integer) oiae.getParts()[0]).intValue());
            Assert.assertEquals(6, ((Integer) oiae.getParts()[1]).intValue());
        }
    }

    @Test
    public void testNotSupportedParameterFree() throws OrekitException {
        final String name = "not-supported-parameter";
        try {
            PropagatorBuilder builder =
                            new KeplerianPropagatorBuilder(mu, orbit.getFrame(),
                                                           OrbitType.CIRCULAR, PositionAngle.TRUE);
            builder.setFreeParameters(Arrays.asList(name));
            builder.buildPropagator(orbit.getDate(), new double[3]);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            Assert.assertEquals(name, oiae.getParts()[0]);
        }
    }

    @Test
    public void testNotSupportedParameterGet() throws OrekitException {
        final String name = "not-supported-parameter";
        try {
            PropagatorBuilder builder =
                            new KeplerianPropagatorBuilder(mu, orbit.getFrame(),
                                                           OrbitType.CIRCULAR, PositionAngle.TRUE);
            builder.getParameter(name);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            Assert.assertEquals(name, oiae.getParts()[0]);
        }
    }

    @Test
    public void testNotSupportedParameterSet() throws OrekitException {
        final String name = "not-supported-parameter";
        try {
            PropagatorBuilder builder =
                            new KeplerianPropagatorBuilder(mu, orbit.getFrame(),
                                                           OrbitType.CIRCULAR, PositionAngle.TRUE);
            builder.setParameter(name, 0.0);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            Assert.assertEquals(name, oiae.getParts()[0]);
        }
    }

    @Test
    public void testSupportedParameters() {
        PropagatorBuilder builder =
                        new KeplerianPropagatorBuilder(mu, orbit.getFrame(),
                                                       OrbitType.CIRCULAR, PositionAngle.TRUE);
        List<String> supported = builder.getSupportedParameters();
        Assert.assertEquals(1, supported.size());
        Assert.assertEquals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                            supported.get(0));
        Assert.assertEquals(mu,
                            builder.getParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT),
                            1.0e-5);
        builder.setParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                             Constants.JPL_SSD_MARS_SYSTEM_GM);
        Assert.assertEquals(Constants.JPL_SSD_MARS_SYSTEM_GM,
                            builder.getParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT),
                            1.0e-5);
    }

    @Test
    public void testConversionPositionVelocity() throws OrekitException {
        checkFit(orbit, 86400, 300, 1.0e-3, false, 1.89e-8);
    }

    @Test
    public void testConversionPositionOnly() throws OrekitException {
        checkFit(orbit, 86400, 300, 1.0e-3, true, 2.90e-8);
    }

    @Test(expected = OrekitException.class)
    public void testConversionWithFreeParameter() throws OrekitException {
        checkFit(orbit, 86400, 300, 1.0e-3, true, 2.65e-8, "toto");
    }

    protected void checkFit(final Orbit orbit,
                            final double duration,
                            final double stepSize,
                            final double threshold,
                            final boolean positionOnly,
                            final double expectedRMS,
                            final String ... freeParameters)
        throws OrekitException {

        Propagator p = new KeplerianPropagator(orbit);
        List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt < duration; dt += stepSize) {
            sample.add(p.propagate(orbit.getDate().shiftedBy(dt)));
        }

        PropagatorBuilder builder = new KeplerianPropagatorBuilder(mu, p.getFrame(),
                                                                   OrbitType.KEPLERIAN,
                                                                   PositionAngle.MEAN);

        FiniteDifferencePropagatorConverter fitter = new FiniteDifferencePropagatorConverter(builder, threshold, 1000);

        fitter.convert(sample, positionOnly, freeParameters);
        
        Assert.assertEquals(expectedRMS, fitter.getRMS(), 0.01 * expectedRMS);

        KeplerianPropagator prop = (KeplerianPropagator)fitter.getAdaptedPropagator();
        Orbit fitted = prop.getInitialState().getOrbit();

        final double eps = 1.0e-12;
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getX(),
                            fitted.getPVCoordinates().getPosition().getX(),
                            eps * orbit.getPVCoordinates().getPosition().getX());
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getY(),
                            fitted.getPVCoordinates().getPosition().getY(),
                            eps * orbit.getPVCoordinates().getPosition().getY());
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getZ(),
                            fitted.getPVCoordinates().getPosition().getZ(),
                            eps * orbit.getPVCoordinates().getPosition().getZ());

        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getX(),
                            fitted.getPVCoordinates().getVelocity().getX(),
                            -eps * orbit.getPVCoordinates().getVelocity().getX());
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getY(),
                            fitted.getPVCoordinates().getVelocity().getY(),
                            eps * orbit.getPVCoordinates().getVelocity().getY());
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getZ(),
                            fitted.getPVCoordinates().getVelocity().getZ(),
                            eps * orbit.getPVCoordinates().getVelocity().getZ());

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                     FramesFactory.getEME2000(), initDate, mu);
    }

}

