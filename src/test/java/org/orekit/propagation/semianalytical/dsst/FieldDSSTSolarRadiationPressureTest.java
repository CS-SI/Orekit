/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

public class FieldDSSTSolarRadiationPressureTest {

    
    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException, OrekitException {
        doTestGetMeanElementRate(Decimal64Field.getInstance());
    }
    
    private <T extends RealFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        
        final T zero = field.getZero();
        
        final Frame earthFrame = FramesFactory.getGCRF();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2003, 9, 16, 0, 0, 0, TimeScalesFactory.getUTC());
        
        // a  = 42166258 m
        // ex = 6.532127416888538E-6
        // ey = 9.978642849310487E-5
        // hx = -5.69711879850274E-6
        // hy = 6.61038518895005E-6
        // lM = 8.56084687583949 rad
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(4.2166258E7),
                                                                zero.add(6.532127416888538E-6),
                                                                zero.add(9.978642849310487E-5),
                                                                zero.add(-5.69711879850274E-6),
                                                                zero.add(6.61038518895005E-6),
                                                                zero.add(8.56084687583949),
                                                                PositionAngle.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                3.986004415E14);

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS);

        // Register the attitude provider to the force model
        Rotation rotation =  new Rotation(0.9999999999999984,
                                          1.653020584550675E-8,
                                          -4.028108631990782E-8,
                                          -3.539139805514139E-8,
                                          false);
        AttitudeProvider attitudeProvider = new InertialProvider(rotation);
        srp.registerAttitudeProvider(attitudeProvider );

        // Attitude of the satellite
        FieldRotation<T> fieldRotation = new FieldRotation<>(field, rotation);
        FieldVector3D<T> rotationRate = new FieldVector3D<>(zero, zero, zero);
        FieldVector3D<T> rotationAcceleration = new FieldVector3D<>(zero, zero, zero);
        TimeStampedFieldAngularCoordinates<T> orientation = new TimeStampedFieldAngularCoordinates<>(initDate,
                                                                                                     fieldRotation,
                                                                                                     rotationRate,
                                                                                                     rotationAcceleration);
        final FieldAttitude<T> att = new FieldAttitude<>(earthFrame, orientation);
        
        // Spacecraft state
        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, att, mass);
        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);
        
        // Compute the mean element rate
        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);
        final T[] daidt = srp.getMeanElementRate(state, auxiliaryElements);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }

        Assert.assertEquals(6.843966348263062E-8, elements[0].getReal(), 1.1e-11);
        Assert.assertEquals(-2.990913371084091E-11, elements[1].getReal(), 2.2e-19);
        Assert.assertEquals(-2.538374405334012E-10, elements[2].getReal(), 8.e-19);
        Assert.assertEquals(2.0384702426501394E-13, elements[3].getReal(), 2.e-20);
        Assert.assertEquals(-2.3346333406116967E-14, elements[4].getReal(), 8.5e-22);
        Assert.assertEquals(1.6087485237156322E-11, elements[5].getReal(), 1.7e-18);

    }
    
    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

}
