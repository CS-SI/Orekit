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
package org.orekit.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;

public class CR3BPFactoryTest {
    
    @Test
    public void getSunJupiterCR3BP() {
    	CR3BPSystem sunJupiterCR3BP = CR3BPFactory.getSunJupiterCR3BP();
    	Assert.assertNotNull(sunJupiterCR3BP);
    }
    
    @Test
    public void getEarthMoonCR3BP() {
    	CR3BPSystem earthMoonCR3BP = CR3BPFactory.getEarthMoonCR3BP();
    	Assert.assertNotNull(earthMoonCR3BP);
    }
    
    @Test
    public void getSunEarthCR3BP() {
    	CR3BPSystem sunEarthCR3BP = CR3BPFactory.getSunEarthCR3BP();
    	Assert.assertNotNull(sunEarthCR3BP);
    }

    @Before
    public void setUp() {
    	Utils.setDataRoot("regular-data");
    }

}
