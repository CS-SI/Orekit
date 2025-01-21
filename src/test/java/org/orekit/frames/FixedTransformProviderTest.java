/* Copyright 2002-2025 CS GROUP
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
package org.orekit.frames;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;


public class FixedTransformProviderTest {

    @Test
    public void testEME2000() {
        Frame gcrf    = FramesFactory.getGCRF();
        Frame eme2000 = FramesFactory.getEME2000();
        TransformProvider fixed = new FixedTransformProvider(gcrf.getTransformTo(eme2000,
                                                                                 AbsoluteDate.J2000_EPOCH));
        for (double dt = 0; dt < Constants.JULIAN_YEAR; dt += Constants.JULIAN_DAY) {
            final AbsoluteDate t = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            final Transform expectedIdentity =
                            new Transform(t, fixed.getTransform(t), eme2000.getTransformTo(gcrf, t));
            Assertions.assertEquals(0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assertions.assertEquals(0, expectedIdentity.getRotation().getAngle(), 1.0e-15);
        }
    }

}
