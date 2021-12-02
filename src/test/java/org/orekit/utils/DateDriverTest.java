/* Copyright 2002-2021 CS GROUP
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
package org.orekit.utils;


import org.junit.Assert;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;

public class DateDriverTest {

    @Test
    public void testBase() {
        DateDriver driver = new DateDriver(AbsoluteDate.J2000_EPOCH, "start", true);
        Assert.assertEquals("start", driver.getName());
        Assert.assertTrue(driver.isStart());
        Assert.assertFalse(driver.isSelected());
        Assert.assertEquals(0.0,   driver.getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
        Assert.assertNull(driver.getReferenceDate());
        driver.setNormalizedValue(0.001);
        Assert.assertEquals(0.001, driver.getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, driver.getMinValue(), 1.0);
        Assert.assertEquals(Double.POSITIVE_INFINITY, driver.getMaxValue(), 1.0);
    }

}
