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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.models.earth.weather.HeightDependentPressureTemperatureHumidityConverter;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

public abstract class AbstractPathDelayTest<T extends TroposphericModel> {

    protected abstract T buildTroposphericModel();

    // default values for doTestMappingFactors
    protected AbsoluteDate                        defaultDate;
    protected GeodeticPoint                       defaultPoint;
    protected TrackingCoordinates                 defaultTrackingCoordinates;

    @BeforeEach
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:atmosphere");
        defaultDate                = new AbsoluteDate(2018, 11, 25, 12, 0, 0, TimeScalesFactory.getUTC());
        defaultPoint               = new GeodeticPoint(FastMath.toRadians(37.5), FastMath.toRadians(277.5), 824.0);
        defaultTrackingCoordinates = new TrackingCoordinates(0.0, FastMath.toRadians(38.0), 0.0);
    }

    @Test
    public abstract void testDelay();

    protected void doTestDelay(final AbsoluteDate date, final GeodeticPoint point,
                               final TrackingCoordinates trackingCoordinates,
                               final double expectedZh, final double expectedZw,
                               final double expectedSh, final double expectedSw,
                               final double expectedDelay) {
        T model = buildTroposphericModel();
        final TroposphericDelay td = model.pathDelay(trackingCoordinates, point,
                                                     model.getParameters(date), date);
        Assertions.assertEquals(expectedZh,    td.getZh(),    0.0001);
        Assertions.assertEquals(expectedZw,    td.getZw(),    0.0001);
        Assertions.assertEquals(expectedSh,    td.getSh(),    0.0001);
        Assertions.assertEquals(expectedSw,    td.getSw(),    0.0001);
        Assertions.assertEquals(expectedDelay, td.getDelay(), 0.0001);
    }

    @Test
    public abstract void testFieldDelay();

    protected <F extends CalculusFieldElement<F>> void doTestDelay(final Field<F> field,
                                                                   final AbsoluteDate date, final GeodeticPoint point,
                                                                   final TrackingCoordinates trackingCoordinates,
                                                                   final double expectedZh, final double expectedZw,
                                                                   final double expectedSh, final double expectedSw,
                                                                   final double expectedDelay) {
        T model = buildTroposphericModel();

        F zero = field.getZero();
        FieldTrackingCoordinates<F> trackingCoordinatesF =
            new FieldTrackingCoordinates<>(zero.newInstance(trackingCoordinates.getAzimuth()),
                                           zero.newInstance(trackingCoordinates.getElevation()),
                                           zero.newInstance(trackingCoordinates.getRange()));
        FieldGeodeticPoint<F> pointF = new FieldGeodeticPoint<>(field, point);
        FieldAbsoluteDate<F> dateF = new FieldAbsoluteDate<>(field, date);
        final double[] parameters = model.getParameters(date);
        F[] parametersF = MathArrays.buildArray(field, parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            parametersF[i] = zero.newInstance(parameters[i]);
        }
        final FieldTroposphericDelay<F> td = model.pathDelay(trackingCoordinatesF, pointF, parametersF, dateF);
        Assertions.assertEquals(expectedZh,    td.getZh().getReal(),    0.0001);
        Assertions.assertEquals(expectedZw,    td.getZw().getReal(),    0.0001);
        Assertions.assertEquals(expectedSh,    td.getSh().getReal(),    0.0001);
        Assertions.assertEquals(expectedSw,    td.getSw().getReal(),    0.0001);
        Assertions.assertEquals(expectedDelay, td.getDelay().getReal(), 0.0001);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), 350.0);
        T model = buildTroposphericModel();
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(elev), 0.0),
                                                 point,
                                                 model.getParameters(date), date).getDelay();
            Assertions.assertTrue(Precision.compareTo(delay, lastDelay, 1.0e-6) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void testFieldFixedHeight() {
        doTestFixedHeight(Binary64Field.getInstance());
    }

    private <F extends CalculusFieldElement<F>> void doTestFixedHeight(final Field<F> field) {
        final F zero = field.getZero();
        final FieldAbsoluteDate<F> date = new FieldAbsoluteDate<>(field);
        final FieldGeodeticPoint<F>
            point = new FieldGeodeticPoint<>(zero.newInstance(FastMath.toRadians(45.0)),
                                             zero.newInstance(FastMath.toRadians(45.0)),
                                             zero.newInstance(350.0));
        T model = buildTroposphericModel();
        F lastDelay = zero.newInstance(Double.MAX_VALUE);
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final F delay = model.pathDelay(new FieldTrackingCoordinates<>(zero,
                                                                           zero.newInstance(FastMath.toRadians(elev)),
                                                                           zero),
                                            point,
                                            model.getParameters(field), date).getDelay();
            Assertions.assertTrue(Precision.compareTo(delay.getReal(), lastDelay.getReal(), 1.0e-6) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void testFixedElevation() {
        T model = buildTroposphericModel();
        HeightDependentPressureTemperatureHumidityConverter converter =
                        new HeightDependentPressureTemperatureHumidityConverter(new CIPM2007());
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing height of the station
        for (double height = 0; height < 5000; height += 100) {
            final double delay = model.pathDelay(defaultTrackingCoordinates,
                                                 new GeodeticPoint(defaultPoint.getLatitude(),
                                                                   defaultPoint.getLongitude(),
                                                                   height),
                                                 model.getParameters(defaultDate), defaultDate).getDelay();
            // some models have small noise, hence the 0.1mm margin
            Assertions.assertTrue(delay < lastDelay + 0.0001);
            lastDelay = delay;
        }
    }

    @Test
    public void testFieldFixedElevation() {
        doTestFieldFixedElevation(Binary64Field.getInstance());
    }

    private <F extends CalculusFieldElement<F>> void doTestFieldFixedElevation(final Field<F> field) {
        final F zero = field.getZero();
        T model = buildTroposphericModel();
        HeightDependentPressureTemperatureHumidityConverter converter =
                        new HeightDependentPressureTemperatureHumidityConverter(new CIPM2007());
        F lastDelay = zero.newInstance(Double.MAX_VALUE);
        // delay shall decline with increasing height of the station
        final FieldAbsoluteDate<F> date = new FieldAbsoluteDate<>(field, defaultDate);
        for (double height = 0; height < 5000; height += 100) {
            final F delay = model.pathDelay(new FieldTrackingCoordinates<>(field, defaultTrackingCoordinates),
                                            new FieldGeodeticPoint<>(zero.newInstance(defaultPoint.getLatitude()),
                                                                     zero.newInstance(defaultPoint.getLongitude()),
                                                                     zero.newInstance(height)),
                                            model.getParameters(date.getField(), date), date).getDelay();
            // some models have small noise, hence the 0.1mm margin
            Assertions.assertTrue(delay.getReal() < lastDelay.getReal() + 0.0001);
            lastDelay = delay;
        }
    }

    protected void doTestVsOtherModel(final TroposphericModel referenceModel,
                                      final PressureTemperatureHumidityProvider referenceWeatherProvider,
                                      final TroposphericModel testedModel,
                                      final PressureTemperatureHumidityProvider testedWeatherProvider,
                                      final double tolZh, final double tolZw, final double tolSh, final double tolSw) {
        double maxErrorZh = 0;
        double maxErrorZw = 0;
        double maxErrorSh = 0;
        double maxErrorSw = 0;
        for (double elevation = FastMath.toRadians(5.0); elevation < MathUtils.SEMI_PI; elevation += 0.01) {
            final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(2.75, elevation, 1.4e6);
            final TroposphericDelay referenceDelay = referenceModel.pathDelay(trackingCoordinates, defaultPoint,
                                                                              referenceModel.getParameters(defaultDate),
                                                                              defaultDate);
            final TroposphericDelay testedDelay = testedModel.pathDelay(trackingCoordinates, defaultPoint,
                                                                        testedModel.getParameters(defaultDate),
                                                                        defaultDate);
            maxErrorZh = FastMath.max(maxErrorZh, FastMath.abs(testedDelay.getZh() - referenceDelay.getZh()));
            maxErrorZw = FastMath.max(maxErrorZw, FastMath.abs(testedDelay.getZw() - referenceDelay.getZw()));
            maxErrorSh = FastMath.max(maxErrorSh, FastMath.abs(testedDelay.getSh() - referenceDelay.getSh()));
            maxErrorSw = FastMath.max(maxErrorSw, FastMath.abs(testedDelay.getSw() - referenceDelay.getSw()));
        }

        Assertions.assertEquals(0.0, maxErrorZh, tolZh);
        Assertions.assertEquals(0.0, maxErrorZw, tolZw);
        Assertions.assertEquals(0.0, maxErrorSh, tolSh);
        Assertions.assertEquals(0.0, maxErrorSw, tolSw);

    }

}
