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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.utils.Constants;

public class TLEConverterTest {

    private TLE geoTLE;

    private TLE leoTLE;

    @Test
    public void testWrongParametersSize() throws OrekitException {
        try {
            PropagatorBuilder builder =
                            new TLEPropagatorBuilder(leoTLE.getSatelliteNumber(),
                                                     leoTLE.getClassification(),
                                                     leoTLE.getLaunchYear(),
                                                     leoTLE.getLaunchNumber(),
                                                     leoTLE.getLaunchPiece(),
                                                     leoTLE.getElementNumber(),
                                                     leoTLE.getRevolutionNumberAtEpoch(),
                                                     OrbitType.CIRCULAR, PositionAngle.TRUE);
            final List<String> empty = Collections.emptyList();
            builder.setFreeParameters(empty);
            builder.buildPropagator(leoTLE.getDate(), new double[3]);
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
                            new TLEPropagatorBuilder(leoTLE.getSatelliteNumber(),
                                                     leoTLE.getClassification(),
                                                     leoTLE.getLaunchYear(),
                                                     leoTLE.getLaunchNumber(),
                                                     leoTLE.getLaunchPiece(),
                                                     leoTLE.getElementNumber(),
                                                     leoTLE.getRevolutionNumberAtEpoch(),
                                                     OrbitType.CIRCULAR, PositionAngle.TRUE);
            builder.setFreeParameters(Arrays.asList(name));
            builder.buildPropagator(leoTLE.getDate(), new double[3]);
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
                            new TLEPropagatorBuilder(leoTLE.getSatelliteNumber(),
                                                     leoTLE.getClassification(),
                                                     leoTLE.getLaunchYear(),
                                                     leoTLE.getLaunchNumber(),
                                                     leoTLE.getLaunchPiece(),
                                                     leoTLE.getElementNumber(),
                                                     leoTLE.getRevolutionNumberAtEpoch(),
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
                            new TLEPropagatorBuilder(leoTLE.getSatelliteNumber(),
                                                     leoTLE.getClassification(),
                                                     leoTLE.getLaunchYear(),
                                                     leoTLE.getLaunchNumber(),
                                                     leoTLE.getLaunchPiece(),
                                                     leoTLE.getElementNumber(),
                                                     leoTLE.getRevolutionNumberAtEpoch(),
                                                     OrbitType.CIRCULAR, PositionAngle.TRUE);
            builder.setParameter(name, 0.0);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oiae.getSpecifier());
            Assert.assertEquals(name, oiae.getParts()[0]);
        }
    }

    @Test
    public void testSupportedParameters() throws OrekitException {
        PropagatorBuilder builder =
                        new TLEPropagatorBuilder(leoTLE.getSatelliteNumber(),
                                                 leoTLE.getClassification(),
                                                 leoTLE.getLaunchYear(),
                                                 leoTLE.getLaunchNumber(),
                                                 leoTLE.getLaunchPiece(),
                                                 leoTLE.getElementNumber(),
                                                 leoTLE.getRevolutionNumberAtEpoch(),
                                                 OrbitType.CIRCULAR, PositionAngle.TRUE);
        List<String> supported = builder.getSupportedParameters();
        Assert.assertEquals(2, supported.size());
        Assert.assertEquals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                            supported.get(0));
        Assert.assertEquals(TLEPropagatorBuilder.B_STAR,
                            supported.get(1));
        Assert.assertEquals(TLEPropagator.getMU(),
                            builder.getParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT),
                            1.0e-5);
        Assert.assertEquals(0.0,
                            builder.getParameter(TLEPropagatorBuilder.B_STAR),
                            1.0e-15);
        builder.setParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                             Constants.JPL_SSD_MARS_SYSTEM_GM);
        builder.setParameter(TLEPropagatorBuilder.B_STAR, 1.2);
        Assert.assertEquals(Constants.JPL_SSD_MARS_SYSTEM_GM,
                            builder.getParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT),
                            1.0e-5);
        Assert.assertEquals(1.2,
                            builder.getParameter(TLEPropagatorBuilder.B_STAR),
                            1.0e-15);
    }

    @Test
    public void testConversionGeoPositionVelocity() throws OrekitException {
        checkFit(geoTLE, 86400, 300, 1.0e-3, false, false, 8.600e-8);
    }

    @Test
    public void testConversionGeoPositionOnly() throws OrekitException {
        checkFit(geoTLE, 86400, 300, 1.0e-3, true, false, 1.058e-7);
    }

    @Test
    public void testConversionLeoPositionVelocityWithoutBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, false, false, 10.77);
    }

    @Test
    public void testConversionLeoPositionOnlyWithoutBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, true, false, 15.23);
    }

    @Test
    public void testConversionLeoPositionVelocityWithBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, false, true, 7.82e-6);
    }

    @Test
    public void testConversionLeoPositionOnlyWithBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, true, true, 5.79e-7);
    }

    protected void checkFit(final TLE tle,
                            final double duration,
                            final double stepSize,
                            final double threshold,
                            final boolean positionOnly,
                            final boolean withBStar,
                            final double expectedRMS)
        throws OrekitException {

        Propagator p = TLEPropagator.selectExtrapolator(tle);
        List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt < duration; dt += stepSize) {
            sample.add(p.propagate(tle.getDate().shiftedBy(dt)));
        }

        TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle.getSatelliteNumber(),
                                                                tle.getClassification(),
                                                                tle.getLaunchYear(),
                                                                tle.getLaunchNumber(),
                                                                tle.getLaunchPiece(),
                                                                tle.getElementNumber(),
                                                                tle.getRevolutionNumberAtEpoch(),
                                                                OrbitType.CARTESIAN,
                                                                PositionAngle.TRUE);

        FiniteDifferencePropagatorConverter fitter = new FiniteDifferencePropagatorConverter(builder, threshold, 1000);

        if (withBStar) {
            fitter.convert(sample, positionOnly, TLEPropagatorBuilder.B_STAR);
        } else {
            fitter.convert(sample, positionOnly);
        }

        TLEPropagator prop = (TLEPropagator)fitter.getAdaptedPropagator();
        TLE fitted = prop.getTLE();

        Assert.assertEquals(expectedRMS, fitter.getRMS(), 0.001 * expectedRMS);

        Assert.assertEquals(tle.getSatelliteNumber(),         fitted.getSatelliteNumber());
        Assert.assertEquals(tle.getClassification(),          fitted.getClassification());
        Assert.assertEquals(tle.getLaunchYear(),              fitted.getLaunchYear());
        Assert.assertEquals(tle.getLaunchNumber(),            fitted.getLaunchNumber());
        Assert.assertEquals(tle.getLaunchPiece(),             fitted.getLaunchPiece());
        Assert.assertEquals(tle.getElementNumber(),           fitted.getElementNumber());
        Assert.assertEquals(tle.getRevolutionNumberAtEpoch(), fitted.getRevolutionNumberAtEpoch());

        final double eps = 1.0e-5;
        Assert.assertEquals(tle.getMeanMotion(), fitted.getMeanMotion(), eps * tle.getMeanMotion());
        Assert.assertEquals(tle.getE(), fitted.getE(), eps * tle.getE());
        Assert.assertEquals(tle.getI(), fitted.getI(), eps * tle.getI());
        Assert.assertEquals(tle.getPerigeeArgument(), fitted.getPerigeeArgument(), eps * tle.getPerigeeArgument());
        Assert.assertEquals(tle.getRaan(), fitted.getRaan(), eps * tle.getRaan());
        Assert.assertEquals(tle.getMeanAnomaly(), fitted.getMeanAnomaly(), eps * tle.getMeanAnomaly());

        if (withBStar) {
            Assert.assertEquals(tle.getBStar(), fitted.getBStar(), eps * tle.getBStar());
        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        geoTLE = new TLE("1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                         "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        leoTLE = new TLE("1 31135U 07013A   11003.00000000  .00000816  00000+0  47577-4 0    11",
                         "2 31135   2.4656 183.9084 0021119 236.4164  60.4567 15.10546832    15");
    }

}

