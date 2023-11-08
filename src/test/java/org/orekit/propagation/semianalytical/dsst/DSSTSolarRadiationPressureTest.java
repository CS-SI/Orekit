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
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DSSTSolarRadiationPressureTest {

    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException {

        final Frame earthFrame = FramesFactory.getGCRF();
        final AbsoluteDate initDate = new AbsoluteDate(2003, 9, 16, 0, 0, 0, TimeScalesFactory.getUTC());
        final double mu = 3.986004415E14;
        // a  = 42166258 m
        // ex = 6.532127416888538E-6
        // ey = 9.978642849310487E-5
        // hx = -5.69711879850274E-6
        // hy = 6.61038518895005E-6
        // lM = 8.56084687583949 rad
        final Orbit orbit = new EquinoctialOrbit(4.2166258E7,
                                                 6.532127416888538E-6,
                                                 9.978642849310487E-5,
                                                 -5.69711879850274E-6,
                                                 6.61038518895005E-6,
                                                 8.56084687583949,
                                                 PositionAngleType.TRUE,
                                                 earthFrame,
                                                 initDate,
                                                 mu);

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                 Constants.WGS84_EARTH_FLATTENING,
                                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                            mu);
        // Attitude of the satellite
        Rotation rotation =  new Rotation(0.9999999999999984,
                                          1.653020584550675E-8,
                                          -4.028108631990782E-8,
                                          -3.539139805514139E-8,
                                          false);
        Vector3D rotationRate = new Vector3D(0., 0., 0.);
        Vector3D rotationAcceleration = new Vector3D(0., 0., 0.);
        TimeStampedAngularCoordinates orientation = new TimeStampedAngularCoordinates(initDate,
                                                                                      rotation,
                                                                                      rotationRate,
                                                                                      rotationAcceleration);
        final Attitude att = new Attitude(earthFrame, orientation);

        // Spacecraft state
        final SpacecraftState state = new SpacecraftState(orbit, att, 1000.0);
        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);

        // Force model parameters
        final double[] parameters = srp.getParameters(orbit.getDate());
        // Initialize force model
        srp.initializeShortPeriodTerms(auxiliaryElements, PropagationType.MEAN, parameters);

        // Register the attitude provider to the force model
        AttitudeProvider attitudeProvider = new FrameAlignedProvider(rotation);
        srp.registerAttitudeProvider(attitudeProvider );

        // Compute the mean element rate
        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);
        final double[] daidt = srp.getMeanElementRate(state, auxiliaryElements, parameters);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assertions.assertEquals( 6.840790448823038E-8,    elements[0], 1.e-23);
        Assertions.assertEquals(-2.990943627915497E-11,   elements[1], 1.e-26);
        Assertions.assertEquals(-2.538400074176317E-10,   elements[2], 1.e-25);
        Assertions.assertEquals( 2.037839945151859E-13,   elements[3], 1.e-28);
        Assertions.assertEquals(-2.3338909771295392E-14,  elements[4], 1.e-29);
        Assertions.assertEquals( 1.6082478750869883E-11,  elements[5], 1.e-26);

    }

    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException {

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

        final BoxAndSolarArraySpacecraft boxAndWing = new BoxAndSolarArraySpacecraft(5.0, 2.0, 2.0,
                                                                                     sun,
                                                                                     50.0, Vector3D.PLUS_J,
                                                                                     2.0, 0.1,
                                                                                     0.2, 0.6);

        final AttitudeProvider attitudeProvider = new LofOffset(meanState.getFrame(),
                                                                LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        final DSSTForceModel srp = new DSSTSolarRadiationPressure(sun,
                                                                  new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                       Constants.WGS84_EARTH_FLATTENING,
                                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, false)),
                                                                  boxAndWing,
                                                                  meanState.getMu());

        //Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(meanState.getOrbit(), 1);

        // Set the force models
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();

        srp.registerAttitudeProvider(attitudeProvider);
        shortPeriodTerms.addAll(srp.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, srp.getParameters(meanState.getDate())));
        srp.updateShortPeriodTerms(srp.getParametersAllValues(), meanState);

        double[] y = new double[6];
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }
        Assertions.assertEquals( 0.3668654523023707,   y[0], 1.e-15);
        Assertions.assertEquals(-2.5673332283107E-10,  y[1], 1.e-23);
        Assertions.assertEquals(-3.84959877691969E-9,  y[2], 1.e-23);
        Assertions.assertEquals(-3.069285299519558E-9, y[3], 1.e-24);
        Assertions.assertEquals(-4.908870542277221E-9, y[4], 1.e-24);
        Assertions.assertEquals(-2.38549338428359E-9,  y[5], 1.e-23);
    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
}
