/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.utils;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;


public class IERSConventionsTest {

    @Test
    public void testConventionsNumber() {
        Assert.assertEquals(3, IERSConventions.values().length);
    }

    @Test
    public void testIERS1996() throws OrekitException {
        Assert.assertNotNull(IERSConventions.IERS_1996.getEarthOrientationAngleFunction());
    }

    @Test
    public void testIERS2003() throws OrekitException {
        Assert.assertNotNull(IERSConventions.IERS_2003.getEarthOrientationAngleFunction());
    }

    @Test
    public void testIERS2010() throws OrekitException {
        Assert.assertNotNull(IERSConventions.IERS_2010.getEarthOrientationAngleFunction());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
