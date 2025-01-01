/* Copyright 2022-2025 Thales Alenia Space
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
import org.orekit.utils.TrackingCoordinates;


public class CanonicalSaastamoinenModelTest extends AbstractPathDelayTest<CanonicalSaastamoinenModel> {

    @Override
    protected CanonicalSaastamoinenModel buildTroposphericModel() {
        return new CanonicalSaastamoinenModel();
    }

    @Test
    @Override
    public void testDelay() {
        doTestDelay(defaultDate, defaultPoint,
                    new TrackingCoordinates(FastMath.toRadians(192), FastMath.toRadians(5), 1.4e6),
                    2.30697, 0.115797, 26.46948, -2.20852, 24.26096);
    }

    @Test
    @Override
    public void testFieldDelay() {
        doTestDelay(Binary64Field.getInstance(),
                    defaultDate, defaultPoint,
                    new TrackingCoordinates(FastMath.toRadians(192), FastMath.toRadians(5), 1.4e6),
                    2.30697, 0.115797, 26.46948, -2.20852, 24.26096);
    }

    @Test
    public void testDelayHighElevation() {
        doTestDelay(defaultDate, defaultPoint,
                    new TrackingCoordinates(FastMath.toRadians(192), FastMath.toRadians(60), 1.4e6),
                    2.30697, 0.115797, 2.66386, 0.13280, 2.79666);
    }

    @Test
    public void testFieldDelayHighElevation() {
        doTestDelay(Binary64Field.getInstance(),
                    defaultDate, defaultPoint,
                    new TrackingCoordinates(FastMath.toRadians(192), FastMath.toRadians(60), 1.4e6),
                    2.30697, 0.115797, 2.66386, 0.13280, 2.79666);
    }

}
