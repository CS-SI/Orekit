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
package org.orekit.models.earth.troposphere;

import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

public class ViennaThreePathDelayTest extends AbstractPathDelayTest<ViennaThree> {

    protected ViennaThree buildTroposphericModel(final PressureTemperatureHumidityProvider provider) {
        return new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                                  new ConstantAzimuthalGradientProvider(null),
                                                  new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                                  TimeScalesFactory.getUTC());
    }

    @Test
    @Override
    public void testFixedHeight() {
        doTestFixedHeight(null);
    }

    @Test
    @Override
    public void testFieldFixedHeight() {
        doTestFieldFixedHeight(Binary64Field.getInstance(), null);
    }

    @Test
    @Override
    public void testFixedElevation() {
        doTestFixedElevation(null);
    }

    @Test
    @Override
    public void testFieldFixedElevation() {
        doTestFieldFixedElevation(Binary64Field.getInstance(), null);
    }

    @Test
    @Override
    public void testDelay() {
        doTestDelay(new AbsoluteDate(),
                    new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 100.0), new TrackingCoordinates(FastMath.toRadians(30.0), FastMath.toRadians(10.0), 0.0),
                    null,
                    2.1993, 0.069, 12.2124, 0.3916, 12.6041);
    }

    @Test
    @Override
    public void testFieldDelay() {
        doTestDelay(Binary64Field.getInstance(),
                    new AbsoluteDate(),
                    new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 100.0), new TrackingCoordinates(FastMath.toRadians(30.0), FastMath.toRadians(10.0), 0.0),
                    null,
                    2.1993, 0.069, 12.2124, 0.3916, 12.6041);
    }

    @Test
    public void testDelayWithAzimuthalAsymmetry() {
        final AbsoluteDate  date      = new AbsoluteDate();
        final GeodeticPoint point     = new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 100.0);
        final ViennaThree   model     = new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00123462, 0.00047101)),
                                                        new ConstantAzimuthalGradientProvider(new AzimuthalGradientCoefficients(12.0, 4.5,
                                                                                                                                0.8, 1.25)),
                                                        new ConstantTroposphericModel(new TroposphericDelay(2.1993, 0.0690, 0, 0)),
                                                        TimeScalesFactory.getUTC());
        final TroposphericDelay delay = model.pathDelay(new TrackingCoordinates(FastMath.toRadians(30.0), FastMath.toRadians(10.0), 0.0),
                                                        point,
                                                        model.getParameters(date), date);
        Assertions.assertEquals( 2.1993,                      delay.getZh(),    1.0e-4);
        Assertions.assertEquals( 0.069,                       delay.getZw(),    1.0e-4);
        Assertions.assertEquals(12.2124 + 373.8241,           delay.getSh(),    1.0e-4); // second term is due to azimuthal gradient
        Assertions.assertEquals( 0.3916 +  38.9670,           delay.getSw(),    1.0e-4); // second term is due to azimuthal gradient
        Assertions.assertEquals(12.6041 + 373.8241 + 38.9670, delay.getDelay(), 1.0e-4);
    }

}
