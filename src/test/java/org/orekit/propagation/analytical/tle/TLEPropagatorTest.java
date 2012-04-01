/* Copyright 2002-2012 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.analytical.tle;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;


public class TLEPropagatorTest {

    private TLE tle;
    private double period;
    
    @Test
    public void testSlaveMode() throws OrekitException {

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        AbsoluteDate initDate = tle.getDate();
        SpacecraftState initialState = propagator.getInitialState();
        
        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(period));

        // Check results
        Assert.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assert.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);

    }

    @Test
    public void testEphemerisMode() throws OrekitException {

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        propagator.setEphemerisMode();
        
        AbsoluteDate initDate = tle.getDate();
        SpacecraftState initialState = propagator.getInitialState();
        
        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        AbsoluteDate endDate = initDate.shiftedBy(period);
        propagator.propagate(endDate);
        
        // get the ephemeris 
        BoundedPropagator boundedProp = propagator.getGeneratedEphemeris();

        // get the initial state from the ephemeris and check if it is the same as
        // the initial state from the TLE
        SpacecraftState boundedState = boundedProp.propagate(initDate);
        
        // Check results
        Assert.assertEquals(initialState.getA(), boundedState.getA(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), boundedState.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), boundedState.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(), boundedState.getHx(), 0.);
        Assert.assertEquals(initialState.getHy(), boundedState.getHy(), 0.);
        Assert.assertEquals(initialState.getLM(), boundedState.getLM(), 1e-14);

        SpacecraftState finalState = boundedProp.propagate(endDate);

        // Check results
        Assert.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assert.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        
        // setup a TLE for a GPS satellite
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";
        
        tle = new TLE(line1, line2);
        
        // the period of the GPS satellite
        period = 717.97 * 60.0;
    }

}

