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
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class FieldDSSTThirdBodyTest {
    
    private static final double eps  = 3.5e-25;

    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException, OrekitException {
        doTestGetMeanElementRate(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestGetMeanElementRate(final Field<T> field) 
        throws IllegalArgumentException, OrekitException {
        
        final T zero = field.getZero();
        
        final Frame earthFrame = FramesFactory.getEME2000();
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, 2003, 07, 01, 0, 0, 00.000, TimeScalesFactory.getUTC());
        
        // a    = 42163393.0 m
        // ex =  -0.25925449177598586
        // ey =  -0.06946703170551687
        // hx =   0.15995912655021305
        // hy =  -0.5969755874197339
        // lM   = 15.47576793123677 rad
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(4.2163393E7),
                                                                zero.add(-0.25925449177598586),
                                                                zero.add(-0.06946703170551687),
                                                                zero.add(0.15995912655021305),
                                                                zero.add(-0.5969755874197339),
                                                                zero.add(15.47576793123677),
                                                                PositionAngle.TRUE,
                                                                earthFrame,
                                                                initDate,
                                                                3.986004415E14);
        
        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, mass);
        
        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);
        
        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon());

        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);
        
        final T[] daidt = moon.getMeanElementRate(state, auxiliaryElements, moon.getParameters(field));
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }
        
        Assert.assertEquals(0.0, elements[0].getReal(), eps);
        Assert.assertEquals(4.346622384804537E-10, elements[1].getReal(), eps);
        Assert.assertEquals(7.293879548440941E-10, elements[2].getReal(), eps);
        Assert.assertEquals(7.465699631747887E-11, elements[3].getReal(), eps);
        Assert.assertEquals(3.9170221137233836E-10, elements[4].getReal(), eps);
        Assert.assertEquals(-3.178319341840074E-10, elements[5].getReal(), eps);

    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data");
    }

}
