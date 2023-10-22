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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class DSSTAtmosphericDragTest {

    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException, OrekitException {

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(2, 0);

        final Frame earthFrame = FramesFactory.getEME2000();
        final AbsoluteDate initDate = new AbsoluteDate(2003, 07, 01, 0, 0, 0, TimeScalesFactory.getUTC());
        final double mu = 3.986004415E14;
        // a  = 7204535.84810944 m
        // ex = -0.001119677138261611
        // ey = 5.333650671984143E-4
        // hx = 0.847841707880348
        // hy = 0.7998014061193262
        // lM = 3.897842092486239 rad
        final Orbit orbit = new EquinoctialOrbit(7204535.84810944,
                                                 -0.001119677138261611,
                                                 5.333650671984143E-4,
                                                 0.847841707880348,
                                                 0.7998014061193262,
                                                 3.897842092486239,
                                                 PositionAngleType.TRUE,
                                                 earthFrame,
                                                 initDate,
                                                 mu);

         // Drag Force Model
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(provider.getAe(),
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            CelestialBodyFactory.getEarth().getBodyOrientedFrame());
        final Atmosphere atm = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
        final double cd = 2.0;
        final double area = 25.0;
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, area, mu);

        // Attitude of the satellite
        Rotation rotation =  new Rotation(1.0, 0.0, 0.0, 0.0, false);
        Vector3D rotationRate = new Vector3D(0., 0., 0.);
        Vector3D rotationAcceleration = new Vector3D(0., 0., 0.);
        TimeStampedAngularCoordinates orientation = new TimeStampedAngularCoordinates(initDate, rotation, rotationRate, rotationAcceleration);
        final Attitude att = new Attitude(earthFrame, orientation);

        final SpacecraftState state = new SpacecraftState(orbit, att, 1000.0);
        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        // Force model parameters
        final double[] parameters = drag.getParameters(orbit.getDate());
        // Initialize force model
        drag.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, parameters);

        // Register the attitude provider to the force model
        AttitudeProvider attitudeProvider = new FrameAlignedProvider(rotation);
        drag.registerAttitudeProvider(attitudeProvider );

        // Compute the mean element rate
        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);
        final double[] daidt = drag.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals(-3.415320567871035E-5, elements[0], 1.e-20);
        Assertions.assertEquals(6.276312897745139E-13, elements[1], 1.9e-27);
        Assertions.assertEquals(-9.303357008691404E-13, elements[2], 0.7e-27);
        Assertions.assertEquals(-7.052316604063199E-14, elements[3], 1.e-28);
        Assertions.assertEquals(-6.793277250493389E-14, elements[4], 3.e-29);
        Assertions.assertEquals(-1.3565284454826392E-15, elements[5], 1.e-27);

    }

    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException, OrekitException {

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());

        final Orbit orbit = new EquinoctialOrbit(7069219.9806427825,
                                                 -4.5941811292223825E-4,
                                                 1.309932339472599E-4,
                                                 -1.002996107003202,
                                                 0.570979900577994,
                                                 2.62038786211518,
                                                 PositionAngleType.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);

        final SpacecraftState meanState = new SpacecraftState(orbit);

        final CelestialBody    sun   = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                  true));

        final BoxAndSolarArraySpacecraft boxAndWing = new BoxAndSolarArraySpacecraft(5.0, 2.0, 2.0,
                                                                                     sun,
                                                                                     50.0, Vector3D.PLUS_J,
                                                                                     2.0, 0.1,
                                                                                     0.2, 0.6);

        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
        final AttitudeProvider attitudeProvider = new LofOffset(meanState.getFrame(),
                                                                LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        final DSSTForceModel drag = new DSSTAtmosphericDrag(atmosphere, boxAndWing, meanState.getMu());

        //Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(meanState.getOrbit(), 1);

        // Set the force models
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();

        drag.registerAttitudeProvider(attitudeProvider);
        shortPeriodTerms.addAll(drag.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, drag.getParameters(meanState.getDate())));
        drag.updateShortPeriodTerms(drag.getParametersAllValues(), meanState);

        double[] y = new double[6];
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }

        Assertions.assertEquals( 0.0396665723326745000,   y[0], 1.e-15);
        Assertions.assertEquals(-1.52943814431706260e-8,  y[1], 1.e-23);
        Assertions.assertEquals(-2.36149298285121920e-8,  y[2], 1.e-23);
        Assertions.assertEquals(-5.90158033654418600e-11, y[3], 1.e-25);
        Assertions.assertEquals( 1.02876397430632310e-11, y[4], 1.e-24);
        Assertions.assertEquals( 2.53842752377756570e-8,  y[5], 1.e-23);
    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
}
