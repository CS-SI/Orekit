/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.bodies;

import org.junit.Before;
import org.junit.Test;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;

public class SolarBodyNoDataTest {

    @Test(expected = OrekitException.class)
    public void noMercury() throws OrekitException {
        SolarSystemBody.getMercury();
    }

    @Test(expected = OrekitException.class)
    public void noVenus() throws OrekitException {
        SolarSystemBody.getVenus();
    }

    @Test(expected = OrekitException.class)
    public void noEarthMoonBarycenter() throws OrekitException {
        SolarSystemBody.getEarthMoonBarycenter();
    }

    @Test(expected = OrekitException.class)
    public void noMars() throws OrekitException {
        SolarSystemBody.getMars();
    }

    @Test(expected = OrekitException.class)
    public void noJupiter() throws OrekitException {
        SolarSystemBody.getJupiter();
    }

    @Test(expected = OrekitException.class)
    public void noSaturn() throws OrekitException {
        SolarSystemBody.getSaturn();
    }

    @Test(expected = OrekitException.class)
    public void noUranus() throws OrekitException {
        SolarSystemBody.getUranus();
    }

    @Test(expected = OrekitException.class)
    public void noNeptune() throws OrekitException {
        SolarSystemBody.getNeptune();
    }

    @Test(expected = OrekitException.class)
    public void noPluto() throws OrekitException {
        SolarSystemBody.getPluto();
    }

    @Test(expected = OrekitException.class)
    public void noMoon() throws OrekitException {
        SolarSystemBody.getMoon();
    }

    @Test(expected = OrekitException.class)
    public void noSun() throws OrekitException {
        SolarSystemBody.getSun();
    }

    @Before
    public void setUp() {
        String root = getClass().getClassLoader().getResource("no-data").getPath();
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
    }

}
