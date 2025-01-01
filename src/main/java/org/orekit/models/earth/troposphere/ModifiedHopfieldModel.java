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

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** The modified Hopfield model.
 * <p>
 * This model from Hopfield 1969, 1970, 1972 is described in equations
 * 5.105, 5.106, 5.107 and 5.108 in Guochang Xu, GPS - Theory, Algorithms
 * and Applications, Springer, 2007.
 * </p>
 * @author Luc Maisonobe
 * @see "Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007"
 * @since 12.1
 */
public class ModifiedHopfieldModel implements TroposphericModel {

    /** Constant for dry altitude effect. */
    private static final double HD0 = 40136.0;

    /** Slope for dry altitude effect. */
    private static final double HD1 = 148.72;

    /** Temperature reference. */
    private static final double T0 = 273.16;

    /** Constant for wet altitude effect. */
    private static final double HW0 = 11000.0;

    /** Dry delay factor. */
    private static final double ND = 77.64e-6;

    /** Wet delay factor, degree 1. */
    private static final double NW1 = -12.96e-6;

    /** Wet delay factor, degree 2. */
    private static final double NW2 = 0.371800;

    /** BAse radius. */
    private static final double RE = 6378137.0;

    /** Create a new Hopfield model.
     */
    public ModifiedHopfieldModel() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates,
                                       final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {

        // zenith angle
        final double zenithAngle = MathUtils.SEMI_PI - trackingCoordinates.getElevation();

        // dry component
        final double hd  = HD0 + HD1 * (weather.getTemperature() - T0);
        final double nd  = ND * TroposphericModelUtils.HECTO_PASCAL.fromSI(weather.getPressure()) /
                           weather.getTemperature();

        // wet component
        final double hw  = HW0;
        final double nw  = (NW1 + NW2 / weather.getTemperature()) / weather.getTemperature();

        return  new TroposphericDelay(delay(0.0,         hd, nd),
                                      delay(0.0,         hw, nw),
                                      delay(zenithAngle, hd, nd),
                                      delay(zenithAngle, hw, nw));

    }

    /** {@inheritDoc}
     * <p>
     * The Saastamoinen model is not defined for altitudes below 0.0. for continuity
     * reasons, we use the value for h = 0 when altitude is negative.
     * </p>
     * <p>
     * There are also numerical issues for elevation angles close to zero. For continuity reasons,
     * elevations lower than a threshold will use the value obtained
     * for the threshold itself.
     * </p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {

        // zenith angle
        final T zenithAngle = trackingCoordinates.getElevation().negate().add(MathUtils.SEMI_PI);

        // dry component
        final T hd = weather.getTemperature().subtract(T0).multiply(HD1).add(HD0);
        final T nd = TroposphericModelUtils.HECTO_PASCAL.fromSI(weather.getPressure()).
                     multiply(ND).
                     divide(weather.getTemperature());

        // wet component
        final T hw = date.getField().getZero().newInstance(HW0);
        final T nw = weather.getTemperature().reciprocal().multiply(NW2).add(NW1).divide(weather.getTemperature());

        return  new FieldTroposphericDelay<>(delay(date.getField().getZero(), hd, nd),
                                             delay(date.getField().getZero(), hw, nw),
                                             delay(zenithAngle,               hd, nd),
                                             delay(zenithAngle,               hw, nw));

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Compute the 9 terms sum delay.
     * @param zenithAngle zenith angle
     * @param hi altitude effect
     * @param ni delay factor
     * @return 9 terms sum delay
     */
    private double delay(final double zenithAngle, final double hi, final double ni) {

        // equation 5.107
        final SinCos scZ   = FastMath.sinCos(zenithAngle);
        final double rePhi = RE + hi;
        final double reS   = RE * scZ.sin();
        final double reC   = RE * scZ.cos();
        final double ri    = FastMath.sqrt(rePhi * rePhi - reS * reS) - reC;

        final double ai    = -scZ.cos() / hi;
        final double bi    = -scZ.sin() * scZ.sin() / (2 * hi * RE);
        final double ai2   = ai * ai;
        final double bi2   = bi * bi;

        final double f1i   = 1;
        final double f2i   = 4 * ai;
        final double f3i   = 6 * ai2 + 4 * bi;
        final double f4i   = 4 * ai * (ai2 + 3 * bi);
        final double f5i   = ai2 * ai2 + 12 * ai2 * bi + 6 * bi2;
        final double f6i   = 4 * ai * bi * (ai2 + 3 * bi);
        final double f7i   = bi2 * (6 * ai2 + 4 * bi);
        final double f8i   = 4 * ai * bi * bi2;
        final double f9i   = bi2 * bi2;

        return ni * (ri * (f1i +
                           ri * (f2i / 2 +
                                 ri * (f3i / 3 +
                                       ri * (f4i / 4 +
                                             ri * (f5i / 5 +
                                                   ri * (f6i / 6 +
                                                          ri * (f7i / 7 +
                                                                ri * (f8i / 8 +
                                                                      ri * f9i / 9)))))))));

    }

    /** Compute the 9 terms sum delay.
     * @param <T> type of the elements
     * @param zenithAngle zenith angle
     * @param hi altitude effect
     * @param ni delay factor
     * @return 9 terms sum delay
     */
    private <T extends CalculusFieldElement<T>> T delay(final T zenithAngle, final T hi, final T ni) {

        // equation 5.107
        final FieldSinCos<T> scZ   = FastMath.sinCos(zenithAngle);
        final T rePhi = hi.add(RE);
        final T reS   = scZ.sin().multiply(RE);
        final T reC   = scZ.cos().multiply(RE);
        final T ri    = FastMath.sqrt(rePhi.multiply(rePhi).subtract(reS.multiply(reS))).subtract(reC);

        final T ai    = scZ.cos().negate().divide(hi);
        final T bi    = scZ.sin().multiply(scZ.sin()).negate().divide(hi.add(hi).multiply(RE));
        final T ai2   = ai.multiply(ai);
        final T bi2   = bi.multiply(bi);

        final T f1i   = ai.getField().getOne();
        final T f2i   = ai.multiply(4);
        final T f3i   = ai2.multiply(6).add(bi.multiply(4));
        final T f4i   = ai.multiply(4).multiply(ai2.add(bi.multiply(3)));
        final T f5i   = ai2.multiply(ai2).add(ai2.multiply(12).multiply(bi)).add(bi2.multiply(6));
        final T f6i   = ai.multiply(4).multiply(bi).multiply(ai2.add(bi.multiply(3)));
        final T f7i   = bi2.multiply(ai2.multiply(6).add(bi.multiply(4)));
        final T f8i   = ai.multiply(4).multiply(bi).multiply(bi2);
        final T f9i   = bi2.multiply(bi2);

        return ni.
               multiply(ri.
                        multiply(f1i.
                                 add(ri.
                                     multiply(f2i.divide(2).
                                              add(ri.
                                                  multiply(f3i.divide(3).
                                                           add(ri.
                                                               multiply(f4i.divide(4).
                                                                        add(ri.
                                                                            multiply(f5i.divide(5).
                                                                                     add(ri.
                                                                                         multiply(f6i.divide(6).
                                                                                                  add(ri.
                                                                                                      multiply(f7i.divide(7).
                                                                                                               add(ri.
                                                                                                                   multiply(f8i.divide(8).
                                                                                                                            add(ri.multiply(f9i.divide(9)))))))))))))))))));

    }

}
