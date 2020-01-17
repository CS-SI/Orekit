/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;

public class IRNSSOrbitalElementsTest {

    @Test
    public void testDefaultMethods() {
        IRNSSOrbitalElements goe = new IRNSSOrbitalElements() {
            public AbsoluteDate getDate() { return null; }
            public int    getWeek()       { return 0; }
            public double getTime()       { return 0; }
            public double getSma()        { return 0; }
            public double getPa()         { return 0; }
            public int    getPRN()        { return 0; }
            public double getOmegaDot()   { return 0; }
            public double getOmega0()     { return 0; }
            public double getMeanMotion() { return 0; }
            public double getM0()         { return 0; }
            public double getIDot()       { return 0; }
            public double getI0()         { return 0; }
            public double getE()          { return 0; }
            public double getCus()        { return 0; }
            public double getCuc()        { return 0; }
            public double getCrs()        { return 0; }
            public double getCrc()        { return 0; }
            public double getCis()        { return 0; }
            public double getCic()        { return 0; }
        };
        Assert.assertEquals(0,   goe.getIODEC());
        Assert.assertEquals(0.0, goe.getAf0(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, goe.getAf1(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, goe.getAf2(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, goe.getToc(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, goe.getTGD(), Precision.SAFE_MIN);
    }

}

