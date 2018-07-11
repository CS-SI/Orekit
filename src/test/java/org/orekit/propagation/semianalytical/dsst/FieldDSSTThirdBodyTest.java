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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
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
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
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
        
        final double mu = 3.986004415E14;
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
                                                                zero.add(mu));
        
        final T mass = zero.add(1000.0);
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit, mass);
        
        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), mu);
        
        final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), 1);


        final T[] elements = MathArrays.buildArray(field, 7);
        Arrays.fill(elements, zero);
        
        final T[] daidt = moon.getMeanElementRate(state, auxiliaryElements, moon.getParameters(field));
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }
        
        Assert.assertEquals(0.0,                    elements[0].getReal(), eps);
        Assert.assertEquals(4.346622384804537E-10,  elements[1].getReal(), eps);
        Assert.assertEquals(7.293879548440941E-10,  elements[2].getReal(), eps);
        Assert.assertEquals(7.465699631747887E-11,  elements[3].getReal(), eps);
        Assert.assertEquals(3.9170221137233836E-10, elements[4].getReal(), eps);
        Assert.assertEquals(-3.178319341840074E-10, elements[5].getReal(), eps);

    }

    @Test
    public void testShortPeriodTerms() throws IllegalArgumentException, OrekitException {
        doTestShortPeriodTerms(Decimal64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
    private <T extends RealFieldElement<T>> void doTestShortPeriodTerms(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        final T zero = field.getZero();
 
        final FieldSpacecraftState<T> meanState = getGEOState(field);
        
        final DSSTForceModel moon    = new DSSTThirdBody(CelestialBodyFactory.getMoon(), meanState.getMu().getReal());

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(moon);

        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(meanState.getOrbit(), 1);

        // Set the force models
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();

        for (final DSSTForceModel force : forces) {
            force.registerAttitudeProvider(null);
            shortPeriodTerms.addAll(force.initialize(aux, false, force.getParameters(field)));
            force.updateShortPeriodTerms(force.getParameters(field), meanState);
        }

        T[] y = MathArrays.buildArray(field, 6);
        Arrays.fill(y, zero);
        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }
        
        Assert.assertEquals(-413.20633326933154,    y[0].getReal(), 1.0e-15);
        Assert.assertEquals(-1.8060137920197483E-5, y[1].getReal(), 1.0e-20);
        Assert.assertEquals(-2.8416367511811057E-5, y[2].getReal(), 1.4e-20);
        Assert.assertEquals(-2.791424363476855E-6,  y[3].getReal(), 1.0e-21);
        Assert.assertEquals(1.8817187527805853E-6,  y[4].getReal(), 1.0e-21);
        Assert.assertEquals(-3.423664701811889E-5,  y[5].getReal(), 1.0e-20);

    }

    private <T extends RealFieldElement<T>> FieldSpacecraftState<T> getGEOState(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
                    
        final T zero = field.getZero();
        // No shadow at this date
        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field, new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                                      TimeScalesFactory.getUTC());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(zero.add(42164000),
                                                                zero.add(10e-3),
                                                                zero.add(10e-3),
                                                                zero.add(FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3)),
                                                                zero.add(FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3)),
                                                                zero.add(0.1),
                                                                PositionAngle.TRUE,
                                                                FramesFactory.getEME2000(),
                                                                initDate,
                                                                zero.add(3.986004415E14));
        return new FieldSpacecraftState<>(orbit);
    }


    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data");
    }

}
