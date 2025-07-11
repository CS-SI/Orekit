/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.time;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.gnss.PredefinedTimeSystem;

public class ClockTimeScaleTest {

    @Test
    public void testGalileo() {
        final String name = "Galileo+offset";
        final AbsoluteDate        t0    = new AbsoluteDate(2020, 4, 1,
                                                           TimeScalesFactory.getUTC());
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -10),
                                                                  FastMath.scalb(1.0, -11),
                                                                  FastMath.scalb(1.0, -12));
        final ClockTimeScale positive = new ClockTimeScale(name,
                                                           PredefinedTimeSystem.GALILEO.getTimeScale(TimeScalesFactory.getTimeScales()),
                                                           clock);
        Assertions.assertEquals(name, positive.getName());
        Assertions.assertEquals(1.00 / 1024.0 - 19.0, positive.offsetFromTAI(t0).toDouble(),                   1.0e-15);
        Assertions.assertEquals(1.75 / 1024.0 - 19.0, positive.offsetFromTAI(t0.shiftedBy(1.0)).toDouble(), 1.0e-15);
        Assertions.assertEquals(3.00 / 1024.0 - 19.0, positive.offsetFromTAI(t0.shiftedBy(2.0)).toDouble(), 1.0e-15);

        FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        Assertions.assertEquals(1.00 / 1024.0 - 19.0, positive.offsetFromTAI(t064).getReal(),                   1.0e-15);
        Assertions.assertEquals(1.75 / 1024.0 - 19.0, positive.offsetFromTAI(t064.shiftedBy(1.0)).getReal(), 1.0e-15);
        Assertions.assertEquals(3.00 / 1024.0 - 19.0, positive.offsetFromTAI(t064.shiftedBy(2.0)).getReal(), 1.0e-15);

    }

    @Test
    public void testUTC() {
        final String name = "UTC+offset";
        final AbsoluteDate        t0    = new AbsoluteDate(2020, 4, 1,
                                                           TimeScalesFactory.getUTC());
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -10),
                                                                  FastMath.scalb(1.0, -11),
                                                                  FastMath.scalb(1.0, -12));
        final ClockTimeScale positive = new ClockTimeScale(name,
                                                           TimeScalesFactory.getUTC(),
                                                           clock);
        Assertions.assertEquals(name, positive.getName());
        Assertions.assertEquals(1.00 / 1024.0 - 37.0, positive.offsetFromTAI(t0).toDouble(),                1.0e-15);
        Assertions.assertEquals(1.75 / 1024.0 - 37.0, positive.offsetFromTAI(t0.shiftedBy(1.0)).toDouble(), 1.0e-15);
        Assertions.assertEquals(3.00 / 1024.0 - 37.0, positive.offsetFromTAI(t0.shiftedBy(2.0)).toDouble(), 1.0e-15);

        FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        Assertions.assertEquals(1.00 / 1024.0 - 37.0, positive.offsetFromTAI(t064).getReal(),                1.0e-15);
        Assertions.assertEquals(1.75 / 1024.0 - 37.0, positive.offsetFromTAI(t064.shiftedBy(1.0)).getReal(), 1.0e-15);
        Assertions.assertEquals(3.00 / 1024.0 - 37.0, positive.offsetFromTAI(t064.shiftedBy(2.0)).getReal(), 1.0e-15);

    }

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
