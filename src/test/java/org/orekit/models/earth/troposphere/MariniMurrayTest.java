/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth.troposphere;

import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TrackingCoordinates;

public class MariniMurrayTest extends AbstractPathDelayTest<MariniMurray> {

    protected MariniMurray buildTroposphericModel() {
        // ruby laser with wavelength 694.3 nm
        return new MariniMurray(694.3, TroposphericModelUtils.NANO_M);
    }

    @Test
    @Override
    public void testDelay() {
        doTestDelay(AbsoluteDate.J2000_EPOCH,
                    new GeodeticPoint(FastMath.toRadians(45), FastMath.toRadians(45), 100), new TrackingCoordinates(0.0, FastMath.toRadians(10), 0.0),
                    2.3883, 0.001656, 13.2516, 0.009522, 13.2611);
    }

    @Test
    @Override
    public void testFieldDelay() {
        doTestDelay(Binary64Field.getInstance(),
                    AbsoluteDate.J2000_EPOCH,
                    new GeodeticPoint(FastMath.toRadians(45), FastMath.toRadians(45), 100), new TrackingCoordinates(0.0, FastMath.toRadians(10), 0.0),
                    2.3883, 0.001656, 13.2516, 0.009522, 13.2611);
    }

}
