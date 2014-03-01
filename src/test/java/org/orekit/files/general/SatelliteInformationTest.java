/* Copyright 2014  Applied Defense Solutions, Inc.
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
package org.orekit.files.general;

import org.junit.Assert;
import org.junit.Test;

public class SatelliteInformationTest {

    @Test
    public void testSatelliteInformation() {
        SatelliteInformation satInformation = new SatelliteInformation("");
        Assert.assertNotNull(satInformation);
    }

    @Test
    public void testGetSetSatelliteId() {
        String satIdExpected = "SatID";
        SatelliteInformation satInformation = new SatelliteInformation(satIdExpected);
        Assert.assertEquals(satIdExpected, satInformation.getSatelliteId());
                
        satIdExpected = "SatID2";
        satInformation.setSatelliteId(satIdExpected);
        Assert.assertEquals(satIdExpected, satInformation.getSatelliteId());            
    }

    @Test
    public void testGetSetAccuracy() {
        int accuracyExpected = 0; 
        SatelliteInformation satInformation = new SatelliteInformation("");
        Assert.assertEquals(accuracyExpected, satInformation.getAccuracy());
                
        accuracyExpected = 10;
        satInformation.setAccuracy(accuracyExpected);
        Assert.assertEquals(accuracyExpected, satInformation.getAccuracy());
    }

}
