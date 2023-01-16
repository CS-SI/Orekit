/* Copyright 2002-2023 CS GROUP
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

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldLegendrePolynomials;
import org.orekit.utils.LegendrePolynomials;
import org.orekit.utils.ParameterDriver;

/** The Vienna3 tropospheric delay model for radio techniques.
 *  The Vienna model data are given with a time interval of 6 hours.
 *  <p>
 *  The empirical coefficients b<sub>h</sub>, b<sub>w</sub>, c<sub>h</sub>
 *  and c<sub>w</sub> are computed with spherical harmonics.
 *  In that respect, they are considerably more advanced than those of
 *  {@link ViennaOneModel VMF1} model.
 *  </p>
 *
 * @see "Landskron, D. & Böhm, J. J Geod (2018)
 *      VMF3/GPT3: refined discrete and empirical troposphere mapping functions
 *      92: 349. https://doi.org/10.1007/s00190-017-1066-2"
 *
 * @see "Landskron D (2017) Modeling tropospheric delays for space geodetic
 *      techniques. Dissertation, Department of Geodesy and Geoinformation, TU Wien, Supervisor: J. Böhm.
 *      http://repositum.tuwien.ac.at/urn:nbn:at:at-ubtuw:1-100249"
 *
 * @author Bryan Cazabonne
 */
public class ViennaThreeModel implements DiscreteTroposphericModel, MappingFunction {

    /** The a coefficient for the computation of the wet and hydrostatic mapping functions.*/
    private final double[] coefficientsA;

    /** Values of hydrostatic and wet delays as provided by the Vienna model. */
    private final double[] zenithDelay;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Build a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param coefficientA The a coefficients for the computation of the wet and hydrostatic mapping functions.
     * @param zenithDelay Values of hydrostatic and wet delays
     * @see #ViennaThreeModel(double[], double[], TimeScale)
     */
    @DefaultDataContext
    public ViennaThreeModel(final double[] coefficientA, final double[] zenithDelay) {
        this(coefficientA, zenithDelay,
             DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Build a new instance.
     * @param coefficientA The a coefficients for the computation of the wet and hydrostatic mapping functions.
     * @param zenithDelay Values of hydrostatic and wet delays
     * @param utc UTC time scale.
     * @since 10.1
     */
    public ViennaThreeModel(final double[] coefficientA,
                            final double[] zenithDelay,
                            final TimeScale utc) {
        this.coefficientsA = coefficientA.clone();
        this.zenithDelay   = zenithDelay.clone();
        this.utc           = utc;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double elevation, final GeodeticPoint point,
                                   final AbsoluteDate date) {
        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(utc);
        final int dofyear = dtc.getDate().getDayOfYear();

        // Compute Legendre Polynomials Pnm(cos(0.5 * pi - phi))
        final int degree = 12;
        final int order  = 12;
        final LegendrePolynomials p = new LegendrePolynomials(degree, order, FastMath.cos(0.5 * FastMath.PI - point.getLatitude()));

        // Compute coefficients bh, bw, ch and cw with spherical harmonics
        double a0Bh = 0.;
        double a0Bw = 0.;
        double a0Ch = 0.;
        double a0Cw = 0.;
        double a1Bh = 0.;
        double a1Bw = 0.;
        double a1Ch = 0.;
        double a1Cw = 0.;
        double b1Bh = 0.;
        double b1Bw = 0.;
        double b1Ch = 0.;
        double b1Cw = 0.;
        double a2Bh = 0.;
        double a2Bw = 0.;
        double a2Ch = 0.;
        double a2Cw = 0.;
        double b2Bh = 0.;
        double b2Bw = 0.;
        double b2Ch = 0.;
        double b2Cw = 0.;
        final LegendreFunctions AnmBnm = new LegendreFunctions();
        int j = 0;
        for (int n = 0; n <= 12; n++) {
            for (int m = 0; m <= n; m++) {
                final SinCos sc = FastMath.sinCos(m * point.getLongitude());
                final double pCosmLambda = p.getPnm(n, m) * sc.cos();
                final double pSinmLambda = p.getPnm(n, m) * sc.sin();

                a0Bh = a0Bh + (AnmBnm.getAnmBh(j, 0) * pCosmLambda + AnmBnm.getBnmBh(j, 0) * pSinmLambda);
                a0Bw = a0Bw + (AnmBnm.getAnmBw(j, 0) * pCosmLambda + AnmBnm.getBnmBw(j, 0) * pSinmLambda);
                a0Ch = a0Ch + (AnmBnm.getAnmCh(j, 0) * pCosmLambda + AnmBnm.getBnmCh(j, 0) * pSinmLambda);
                a0Cw = a0Cw + (AnmBnm.getAnmCw(j, 0) * pCosmLambda + AnmBnm.getBnmCw(j, 0) * pSinmLambda);

                a1Bh = a1Bh + (AnmBnm.getAnmBh(j, 1) * pCosmLambda + AnmBnm.getBnmBh(j, 1) * pSinmLambda);
                a1Bw = a1Bw + (AnmBnm.getAnmBw(j, 1) * pCosmLambda + AnmBnm.getBnmBw(j, 1) * pSinmLambda);
                a1Ch = a1Ch + (AnmBnm.getAnmCh(j, 1) * pCosmLambda + AnmBnm.getBnmCh(j, 1) * pSinmLambda);
                a1Cw = a1Cw + (AnmBnm.getAnmCw(j, 1) * pCosmLambda + AnmBnm.getBnmCw(j, 1) * pSinmLambda);

                b1Bh = b1Bh + (AnmBnm.getAnmBh(j, 2) * pCosmLambda + AnmBnm.getBnmBh(j, 2) * pSinmLambda);
                b1Bw = b1Bw + (AnmBnm.getAnmBw(j, 2) * pCosmLambda + AnmBnm.getBnmBw(j, 2) * pSinmLambda);
                b1Ch = b1Ch + (AnmBnm.getAnmCh(j, 2) * pCosmLambda + AnmBnm.getBnmCh(j, 2) * pSinmLambda);
                b1Cw = b1Cw + (AnmBnm.getAnmCw(j, 2) * pCosmLambda + AnmBnm.getBnmCw(j, 2) * pSinmLambda);

                a2Bh = a2Bh + (AnmBnm.getAnmBh(j, 3) * pCosmLambda + AnmBnm.getBnmBh(j, 3) * pSinmLambda);
                a2Bw = a2Bw + (AnmBnm.getAnmBw(j, 3) * pCosmLambda + AnmBnm.getBnmBw(j, 3) * pSinmLambda);
                a2Ch = a2Ch + (AnmBnm.getAnmCh(j, 3) * pCosmLambda + AnmBnm.getBnmCh(j, 3) * pSinmLambda);
                a2Cw = a2Cw + (AnmBnm.getAnmCw(j, 3) * pCosmLambda + AnmBnm.getBnmCw(j, 3) * pSinmLambda);

                b2Bh = b2Bh + (AnmBnm.getAnmBh(j, 4) * pCosmLambda + AnmBnm.getBnmBh(j, 4) * pSinmLambda);
                b2Bw = b2Bw + (AnmBnm.getAnmBw(j, 4) * pCosmLambda + AnmBnm.getBnmBw(j, 4) * pSinmLambda);
                b2Ch = b2Ch + (AnmBnm.getAnmCh(j, 4) * pCosmLambda + AnmBnm.getBnmCh(j, 4) * pSinmLambda);
                b2Cw = b2Cw + (AnmBnm.getAnmCw(j, 4) * pCosmLambda + AnmBnm.getBnmCw(j, 4) * pSinmLambda);

                j = j + 1;
            }
        }

        // Eq. 6
        final double bh = computeSeasonalFit(dofyear, a0Bh, a1Bh, a2Bh, b1Bh, b2Bh);
        final double bw = computeSeasonalFit(dofyear, a0Bw, a1Bw, a2Bw, b1Bw, b2Bw);
        final double ch = computeSeasonalFit(dofyear, a0Ch, a1Ch, a2Ch, b1Ch, b2Ch);
        final double cw = computeSeasonalFit(dofyear, a0Cw, a1Cw, a2Cw, b1Cw, b2Cw);

        // Compute Mapping Function Eq. 4
        final double[] function = new double[2];
        function[0] = TroposphericModelUtils.mappingFunction(coefficientsA[0], bh, ch, elevation);
        function[1] = TroposphericModelUtils.mappingFunction(coefficientsA[1], bw, cw, elevation);

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final T elevation, final FieldGeodeticPoint<T> point,
                                                              final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T zero         = field.getZero();

        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(utc);
        final int dofyear = dtc.getDate().getDayOfYear();

        // Compute Legendre Polynomials Pnm(cos(0.5 * pi - phi))
        final int degree = 12;
        final int order  = 12;
        final FieldLegendrePolynomials<T> p = new FieldLegendrePolynomials<>(degree, order, FastMath.cos(point.getLatitude().negate().add(zero.getPi().multiply(0.5))));

        // Compute coefficients bh, bw, ch and cw with spherical harmonics
        T a0Bh = zero;
        T a0Bw = zero;
        T a0Ch = zero;
        T a0Cw = zero;
        T a1Bh = zero;
        T a1Bw = zero;
        T a1Ch = zero;
        T a1Cw = zero;
        T b1Bh = zero;
        T b1Bw = zero;
        T b1Ch = zero;
        T b1Cw = zero;
        T a2Bh = zero;
        T a2Bw = zero;
        T a2Ch = zero;
        T a2Cw = zero;
        T b2Bh = zero;
        T b2Bw = zero;
        T b2Ch = zero;
        T b2Cw = zero;
        final LegendreFunctions AnmBnm = new LegendreFunctions();
        int j = 0;
        for (int n = 0; n <= 12; n++) {
            for (int m = 0; m <= n; m++) {
                final FieldSinCos<T> sc = FastMath.sinCos(point.getLongitude().multiply(m));
                final T pCosmLambda = p.getPnm(n, m).multiply(sc.cos());
                final T pSinmLambda = p.getPnm(n, m).multiply(sc.sin());

                a0Bh = a0Bh.add(pCosmLambda.multiply(AnmBnm.getAnmBh(j, 0)).add(pSinmLambda.multiply(AnmBnm.getBnmBh(j, 0))));
                a0Bw = a0Bw.add(pCosmLambda.multiply(AnmBnm.getAnmBw(j, 0)).add(pSinmLambda.multiply(AnmBnm.getBnmBw(j, 0))));
                a0Ch = a0Ch.add(pCosmLambda.multiply(AnmBnm.getAnmCh(j, 0)).add(pSinmLambda.multiply(AnmBnm.getBnmCh(j, 0))));
                a0Cw = a0Cw.add(pCosmLambda.multiply(AnmBnm.getAnmCw(j, 0)).add(pSinmLambda.multiply(AnmBnm.getBnmCw(j, 0))));

                a1Bh = a1Bh.add(pCosmLambda.multiply(AnmBnm.getAnmBh(j, 1)).add(pSinmLambda.multiply(AnmBnm.getBnmBh(j, 1))));
                a1Bw = a1Bw.add(pCosmLambda.multiply(AnmBnm.getAnmBw(j, 1)).add(pSinmLambda.multiply(AnmBnm.getBnmBw(j, 1))));
                a1Ch = a1Ch.add(pCosmLambda.multiply(AnmBnm.getAnmCh(j, 1)).add(pSinmLambda.multiply(AnmBnm.getBnmCh(j, 1))));
                a1Cw = a1Cw.add(pCosmLambda.multiply(AnmBnm.getAnmCw(j, 1)).add(pSinmLambda.multiply(AnmBnm.getBnmCw(j, 1))));

                b1Bh = b1Bh.add(pCosmLambda.multiply(AnmBnm.getAnmBh(j, 2)).add(pSinmLambda.multiply(AnmBnm.getBnmBh(j, 2))));
                b1Bw = b1Bw.add(pCosmLambda.multiply(AnmBnm.getAnmBw(j, 2)).add(pSinmLambda.multiply(AnmBnm.getBnmBw(j, 2))));
                b1Ch = b1Ch.add(pCosmLambda.multiply(AnmBnm.getAnmCh(j, 2)).add(pSinmLambda.multiply(AnmBnm.getBnmCh(j, 2))));
                b1Cw = b1Cw.add(pCosmLambda.multiply(AnmBnm.getAnmCw(j, 2)).add(pSinmLambda.multiply(AnmBnm.getBnmCw(j, 2))));

                a2Bh = a2Bh.add(pCosmLambda.multiply(AnmBnm.getAnmBh(j, 3)).add(pSinmLambda.multiply(AnmBnm.getBnmBh(j, 3))));
                a2Bw = a2Bw.add(pCosmLambda.multiply(AnmBnm.getAnmBw(j, 3)).add(pSinmLambda.multiply(AnmBnm.getBnmBw(j, 3))));
                a2Ch = a2Ch.add(pCosmLambda.multiply(AnmBnm.getAnmCh(j, 3)).add(pSinmLambda.multiply(AnmBnm.getBnmCh(j, 3))));
                a2Cw = a2Cw.add(pCosmLambda.multiply(AnmBnm.getAnmCw(j, 3)).add(pSinmLambda.multiply(AnmBnm.getBnmCw(j, 3))));

                b2Bh = b2Bh.add(pCosmLambda.multiply(AnmBnm.getAnmBh(j, 4)).add(pSinmLambda.multiply(AnmBnm.getBnmBh(j, 4))));
                b2Bw = b2Bw.add(pCosmLambda.multiply(AnmBnm.getAnmBw(j, 4)).add(pSinmLambda.multiply(AnmBnm.getBnmBw(j, 4))));
                b2Ch = b2Ch.add(pCosmLambda.multiply(AnmBnm.getAnmCh(j, 4)).add(pSinmLambda.multiply(AnmBnm.getBnmCh(j, 4))));
                b2Cw = b2Cw.add(pCosmLambda.multiply(AnmBnm.getAnmCw(j, 4)).add(pSinmLambda.multiply(AnmBnm.getBnmCw(j, 4))));

                j = j + 1;
            }
        }

        // Eq. 6
        final T bh = computeSeasonalFit(dofyear, a0Bh, a1Bh, a2Bh, b1Bh, b2Bh);
        final T bw = computeSeasonalFit(dofyear, a0Bw, a1Bw, a2Bw, b1Bw, b2Bw);
        final T ch = computeSeasonalFit(dofyear, a0Ch, a1Ch, a2Ch, b1Ch, b2Ch);
        final T cw = computeSeasonalFit(dofyear, a0Cw, a1Cw, a2Cw, b1Cw, b2Cw);

        // Compute Mapping Function Eq. 4
        final T[] function = MathArrays.buildArray(field, 2);
        function[0] = TroposphericModelUtils.mappingFunction(zero.add(coefficientsA[0]), bh, ch, elevation);
        function[1] = TroposphericModelUtils.mappingFunction(zero.add(coefficientsA[1]), bw, cw, elevation);

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        // zenith delay
        final double[] delays = computeZenithDelay(point, parameters, date);
        // mapping function
        final double[] mappingFunction = mappingFactors(elevation, point, date);
        // Tropospheric path delay
        return delays[0] * mappingFunction[0] + delays[1] * mappingFunction[1];
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        // zenith delay
        final T[] delays = computeZenithDelay(point, parameters, date);
        // mapping function
        final T[] mappingFunction = mappingFactors(elevation, point, date);
        // Tropospheric path delay
        return delays[0].multiply(mappingFunction[0]).add(delays[1].multiply(mappingFunction[1]));
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>T[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public double[] computeZenithDelay(final GeodeticPoint point, final double[] parameters, final AbsoluteDate date) {
        return zenithDelay.clone();
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>T[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param <T> type of the elements
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public <T extends CalculusFieldElement<T>> T[] computeZenithDelay(final FieldGeodeticPoint<T> point, final T[] parameters,
                                                                  final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T zero = field.getZero();
        final T[] delays = MathArrays.buildArray(field, 2);
        delays[0] = zero.add(zenithDelay[0]);
        delays[1] = zero.add(zenithDelay[1]);
        return delays;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Computes the empirical temporal information for the mapping function
     * coefficients b and c. A seasonal fit formula is performed.
     * @param doy day of year
     * @param A0 Mean value of the coefficient
     * @param A1 Annual amplitude of the coefficient
     * @param A2 Semi-annual amplitude of the coefficient
     * @param B1 Annual amplitude of the coefficient
     * @param B2 Semi-annual amplitude of the coefficient
     * @return the mapping function coefficient at a given day.
     */
    private double computeSeasonalFit(final int doy, final double A0, final double A1,
                                      final double A2, final double B1, final double B2) {

        final double coef = (doy / 365.25) * 2 * FastMath.PI;
        final SinCos sc1  = FastMath.sinCos(coef);
        final SinCos sc2  = FastMath.sinCos(2.0 * coef);

        return A0 +
               A1 * sc1.cos() + B1 * sc1.sin() +
               A2 * sc2.cos() + B2 * sc2.sin();
    }

    /** Computes the empirical temporal information for the mapping function
     * coefficients b and c. A seasonal fit formula is performed.
     * @param <T> type of the elements
     * @param doy day of year
     * @param A0 Mean value of the coefficient
     * @param A1 Annual amplitude of the coefficient
     * @param A2 Semi-annual amplitude of the coefficient
     * @param B1 Annual amplitude of the coefficient
     * @param B2 Semi-annual amplitude of the coefficient
     * @return the mapping function coefficient at a given day.
     */
    private <T extends CalculusFieldElement<T>> T computeSeasonalFit(final int doy, final T A0, final T A1,
                                                                 final T A2, final T B1, final T B2) {
        final T coef = A0.getPi().multiply(2.0).multiply(doy / 365.25);
        final FieldSinCos<T> sc1  = FastMath.sinCos(coef);
        final FieldSinCos<T> sc2  = FastMath.sinCos(coef.multiply(2.0));

        return A0.add(
               A1.multiply(sc1.cos())).add(B1.multiply(sc1.sin())).add(
               A2.multiply(sc2.cos())).add(B2.multiply(sc2.sin()));
    }

    /** Class for the values of the coefficients a<sub>nm</sub> and b<sub>nm</sub>
     *  of the spherical harmonics expansion.
     */
    private static class LegendreFunctions {

        /** Legendre functions a<sub>nm</sub> for coefficient b<sub>h</sub>.*/
        private static final double[][] BH_A = {
            {0.00271285863109945, -1.39197786008938e-06, 1.34955672002719e-06, 2.71686279717968e-07, 1.56659301773925e-06},
            {9.80476624811974e-06, -5.83922611260673e-05, -2.07307023860417e-05, 1.14628726961148e-06, 4.93610283608719e-06},
            {-1.03443106534268e-05, -2.05536138785961e-06, 2.09692641914244e-06, -1.55491034130965e-08, -1.89706404675801e-07},
            {-3.00353961749658e-05, 2.37284447073503e-05, 2.02236885378918e-05, 1.69276006349609e-06, 8.72156681243892e-07},
            {-7.99121077044035e-07, -5.39048313389504e-06, -4.21234502039861e-06, -2.70944149806894e-06, -6.80894455531746e-07},
            {7.51439609883296e-07, 3.85509708865520e-07, 4.41508016098164e-08, -2.07507808307757e-08, 4.95354985050743e-08},
            {2.21790962160087e-05, -5.56986238775212e-05, -1.81287885563308e-05, -4.41076013532589e-06, 4.93573223917278e-06},
            {-4.47639989737328e-06, -2.60452893072120e-06, 2.56376320011189e-06, 4.41600992220479e-07, 2.93437730332869e-07},
            {8.14992682244945e-07, 2.03945571424434e-07, 1.11832498659806e-08, 3.25756664234497e-08, 3.01029040414968e-08},
            {-7.96927680907488e-08, -3.66953150925865e-08, -6.74742632186619e-09, -1.30315731273651e-08, -2.00748924306947e-09},
            {-2.16138375166934e-05, 1.67350317962556e-05, 1.93768260076821e-05, 1.99595120161850e-06, -2.42463528222014e-06},
            {5.34360283708044e-07, -3.64189022040600e-06, -2.99935375194279e-06, -2.06880962903922e-06, -9.40815692626002e-07},
            {6.80235884441822e-07, 1.33023436079845e-07, -1.80349593705226e-08, 2.51276252565192e-08, -1.43240592002794e-09},
            {-7.13790897253802e-08, 7.81998506267559e-09, 1.13826909570178e-09, -5.89629600214654e-09, -4.20760865522804e-09},
            {-5.80109372399116e-09, 1.13702284491976e-09, 7.29046067602764e-10, -9.10468988754012e-10, -2.58814364808642e-10},
            {1.75558618192965e-05, -2.85579168876063e-05, -1.47442190284602e-05, -6.29300414335248e-06, -5.12204538913460e-07},
            {-1.90788558291310e-06, -1.62144845155361e-06, 7.57239241641566e-07, 6.93365788711348e-07, 6.88855644570695e-07},
            {2.27050351488552e-07, 1.03925791277660e-07, -3.31105076632079e-09, 2.88065761026675e-08, -8.00256848229136e-09},
            {-2.77028851807614e-08, -5.96251132206930e-09, 2.95987495527251e-10, -5.87644249625625e-09, -3.28803981542337e-09},
            {-1.89918479865558e-08, 3.54083436578857e-09, 8.10617835854935e-10, 4.99207055948336e-10, -1.52691648387663e-10},
            {1.04022499586096e-09, -2.36437143845013e-10, -2.25110813484842e-10, -7.39850069252329e-11, 7.95929405440911e-11},
            {-3.11579421267630e-05, -3.43576336877494e-06, 5.81663608263384e-06, 8.31534700351802e-07, 4.02619520312154e-06},
            {6.00037066879001e-07, -1.12538760056168e-07, -3.86745332115590e-07, -3.88218746020826e-07, -6.83764967176388e-07},
            {-9.79583981249316e-08, 9.14964449851003e-08, 4.77779838549237e-09, 2.44283811750703e-09, -6.26361079345158e-09},
            {-2.37742207548109e-08, -5.53336301671633e-09, -3.73625445257115e-09, -1.92304189572886e-09, -7.18681390197449e-09},
            {-6.58203463929583e-09, 9.28456148541896e-10, 2.47218904311077e-10, 1.10664919110218e-10, -4.20390976974043e-11},
            {9.45857603373426e-10, -3.29683402990254e-11, -8.15440375865127e-11, -1.21615589356628e-12, -9.70713008848085e-12},
            {1.61377382316176e-10, 6.84326027598147e-12, -4.66898885683671e-12, 2.31211355085535e-12, 2.39195112937346e-12},
            {2.99634365075821e-07, 8.14391615472128e-06, 6.70458490942443e-06, -9.92542646762000e-07, -3.04078064992750e-06},
            {-6.52697933801393e-07, 2.87255329776428e-07, -1.78227609772085e-08, 2.65525429849935e-07, 8.60650570551813e-08},
            {-1.62727164011710e-07, 1.09102479325892e-07, 4.97827431850001e-09, 7.86649963082937e-11, -6.67193813407656e-09},
            {-2.96370000987760e-09, 1.20008401576557e-09, 1.75885448022883e-09, -1.74756709684384e-09, 3.21963061454248e-09},
            {-9.91101697778560e-10, 7.54541713140752e-10, -2.95880967800875e-10, 1.81009160501278e-10, 8.31547411640954e-11},
            {1.21268051949609e-10, -5.93572774509587e-11, -5.03295034994351e-11, 3.05383430975252e-11, 3.56280438509939e-11},
            {6.92012970333794e-11, -9.02885345797597e-12, -3.44151832744880e-12, 2.03164894681921e-12, -5.44852265137606e-12},
            {5.56731263672800e-12, 3.57272150106101e-12, 2.25885622368678e-12, -2.44508240047675e-13, -6.83314378535235e-13},
            {3.96883487797254e-06, -4.57100506169608e-06, -3.30208117813256e-06, 3.32599719134845e-06, 4.26539325549339e-06},
            {1.10123151770973e-06, 4.58046760144882e-07, 1.86831972581926e-07, -1.60092770735081e-07, -5.58956114867062e-07},
            {-3.40344900506653e-08, 2.87649741373047e-08, -1.83929753066251e-08, -9.74179203885847e-09, -2.42064137485043e-09},
            {-6.49731596932566e-09, -3.07048108404447e-09, -2.84380614669848e-09, 1.55123146524283e-09, 4.53694984588346e-10},
            {5.45175793803325e-10, -3.73287624700125e-10, -1.16293122618336e-10, 7.25845618602690e-11, -4.34112440021627e-11},
            {1.89481447552805e-10, 3.67431482211078e-12, -1.72180065021194e-11, 1.47046319023226e-11, 1.31920481414062e-11},
            {2.10125915737167e-12, -3.08420783495975e-12, -4.87748712363020e-12, 1.16363599902490e-14, 1.26698255558605e-13},
            {-8.07894928696254e-12, 9.19344620512607e-13, 3.26929173307443e-13, 2.00438149416495e-13, -9.57035765212079e-15},
            {1.38737151773284e-12, 1.09340178371420e-13, 5.15714202449053e-14, -5.92156438588931e-14, -3.29586752336143e-14},
            {6.38137197198254e-06, 4.62426300749908e-06, 4.42334454191034e-06, 1.15374736092349e-06, -2.61859702227253e-06},
            {-2.25320619636149e-07, 3.21907705479353e-07, -3.34834530764823e-07, -4.82132753601810e-07, -3.22410936343355e-07},
            {3.48894515496995e-09, 3.49951261408458e-08, -6.01128959281142e-09, 4.78213900943443e-09, 1.46012816168576e-08},
            {-9.66682871952083e-11, 3.75806627535317e-09, 2.38984004956705e-09, 2.07545049877203e-09, 1.58573595632766e-09},
            {1.06834370693917e-09, -4.07975055112153e-10, -2.37598937943957e-10, 5.89327007480137e-11, 1.18891820437634e-10},
            {5.22433722695807e-11, 6.02011995016293e-12, -7.80605402956048e-12, 1.50873145627341e-11, -1.40550093106311e-12},
            {2.13396242187279e-13, -1.71939313965536e-12, -3.57625378660975e-14, -5.01675184988446e-14, -1.07805487368797e-12},
            {-1.24352330043311e-12, 8.26105883301606e-13, 4.63606970128517e-13, 6.39517888984486e-14, -7.35135439920086e-14},
            {-5.39023859065631e-13, 2.54188315588243e-14, 1.30933833278664e-14, 6.06153473304781e-15, -4.24722717533726e-14},
            {3.12767756884813e-14, -2.29517847871632e-15, 2.53117304424948e-16, 7.07504914138118e-16, -1.20089065310688e-15},
            {2.08311178819214e-06, -1.22179185044174e-06, -2.98842190131044e-06, 3.07310218974299e-06, 2.27100346036619e-06},
            {-3.94601643855452e-07, -5.44014825116083e-07, -6.16955333162507e-08, -2.31954821580670e-07, 1.14010813005310e-07},
            {6.11067575043044e-08, -3.93240193194272e-08, -1.62979132528933e-08, 1.01339204652581e-08, 1.97319601566071e-08},
            {2.57770508710055e-09, 1.87799543582899e-09, 1.95407654714372e-09, 1.15276419281270e-09, 2.25397005402120e-09},
            {7.16926338026236e-10, -3.65857693313858e-10, -1.54864067050915e-11, 6.50770211276549e-11, -7.85160007413546e-12},
            {4.90007693914221e-12, 3.31649396536340e-12, 4.81664871165640e-13, 7.26080745617085e-12, 2.30960953372164e-12},
            {9.75489202240545e-13, -1.68967954531421e-13, 7.38383391334110e-13, -3.58435515913239e-13, -3.01564710027450e-13},
            {-3.79533601922805e-13, 2.76681830946617e-13, 1.21480375553803e-13, -1.57729077644850e-14, -8.87664977818700e-14},
            {-3.96462845480288e-14, 2.94155690934610e-14, 6.78413205760717e-15, -4.12135802787361e-15, -1.46373307795619e-14},
            {-8.64941937408121e-15, -1.91822620970386e-15, -8.01725413560744e-16, 5.02941051180784e-16, -1.07572628474344e-15},
            {-4.13816294742758e-15, -7.43602019785880e-17, -5.54248556346072e-17, -4.83999456005158e-17, -1.19622559730466e-16},
            {-8.34852132750364e-07, -7.45794677612056e-06, -6.58132648865533e-06, -1.38608110346732e-06, 5.32326534882584e-07},
            {-2.75513802414150e-07, 3.64713745106279e-08, -7.12385417940442e-08, -7.86206067228882e-08, 2.28048393207161e-08},
            {-4.26696415431918e-08, -4.65599668635087e-09, 7.35037936327566e-09, 1.17098354115804e-08, 1.44594777658035e-08},
            {1.12407689274199e-09, 7.62142529563709e-10, -6.72563708415472e-10, -1.18094592485992e-10, -1.17043815733292e-09},
            {1.76612225246125e-10, -1.01188552503192e-10, 7.32546072616968e-11, 1.79542821801610e-11, -2.23264859965402e-11},
            {-9.35960722512375e-12, 1.90894283812231e-12, -6.34792824525760e-13, 3.98597963877826e-12, -4.47591409078971e-12},
            {-3.34623858556099e-12, 4.56384903915853e-14, 2.72561108521416e-13, -3.57942733300468e-15, 1.99794810657713e-13},
            {-6.16775522568954e-14, 8.25316968328823e-14, 7.19845814260518e-14, -2.92415710855106e-14, -5.49570017444031e-15},
            {-8.50728802453217e-15, 8.38161600916267e-15, 3.43651657459983e-15, -8.19429434115910e-16, -4.08905746461100e-15},
            {4.39042894275548e-15, -3.69440485320477e-16, 1.22249256876779e-16, -2.09359444520984e-16, -3.34211740264257e-16},
            {-5.36054548134225e-16, 3.29794204041989e-17, 2.13564354374585e-17, -1.37838993720865e-18, -1.29188342867753e-17},
            {-3.26421841529845e-17, 7.38235405234126e-18, 2.49291659676210e-18, 8.18252735459593e-19, 1.73824952279230e-20},
            {4.67237509268208e-06, 1.93611283787239e-06, 9.39035455627622e-07, -5.84565118072823e-07, -1.76198705802101e-07},
            {-3.33739157421993e-07, 4.12139555299163e-07, 1.58754695700856e-07, 1.37448753329669e-07, 1.04722936936873e-07},
            {6.64200603076386e-09, 1.45412222625734e-08, 1.82498796118030e-08, 2.86633517581614e-09, 1.06066984548100e-09},
            {5.25549696746655e-09, -1.33677183394083e-09, 7.60804375937931e-11, -1.07918624219037e-10, 8.09178898247941e-10},
            {1.89318454110039e-10, 9.23092164791765e-11, 5.51434573131180e-11, 3.86696392289240e-11, -1.15208165047149e-11},
            {-1.02252706006226e-12, -7.25921015411136e-13, -1.98110126887620e-12, -2.18964868282672e-13, -7.18834476685625e-13},
            {-2.69770025318548e-12, -2.17850340796321e-14, 4.73040820865871e-13, 1.57947421572149e-13, 1.86925164972766e-13},
            {1.07831718354771e-13, 2.26681841611017e-14, 2.56046087047783e-14, -1.14995851659554e-14, -2.27056907624485e-14},
            {6.29825154734712e-15, 8.04458225889001e-16, 9.53173540411138e-16, 1.16892301877735e-15, -1.04324684545047e-15},
            {-5.57345639727027e-16, -2.93949227634932e-16, 7.47621406284534e-18, -5.36416885470756e-17, -2.87213280230513e-16},
            {1.73219775047208e-16, 2.05017387523061e-17, 9.08873886345587e-18, -2.86881547225742e-18, -1.25303645304992e-17},
            {-7.30829109684568e-18, 2.03711261415353e-18, 7.62162636124024e-19, -7.54847922012517e-19, -8.85105098195030e-19},
            {5.62039968280587e-18, -1.38144206573507e-19, 1.68028711767211e-20, 1.81223858251981e-19, -8.50245194985878e-20}
        };

        /** Legendre functions a<sub>nm</sub> for coefficient b<sub>w</sub>.*/
        private static final double[][] BW_A = {
            {0.00136127467401223, -6.83476317823061e-07, -1.37211986707674e-06, 7.02561866200582e-07, -2.16342338010651e-07},
            {-9.53197486400299e-06, 6.58703762338336e-06, 2.42000663952044e-06, -6.04283463108935e-07, 2.02144424676990e-07},
            {-6.76728911259359e-06, 6.03830755085583e-07, -8.72568628835897e-08, 2.21750344140938e-06, 1.05146032931020e-06},
            {-3.21102832397338e-05, -7.88685357568093e-06, -2.55495673641049e-06, -1.99601934456719e-06, -4.62005252198027e-07},
            {-7.84639263523250e-07, 3.11624739733849e-06, 9.02170019697389e-07, 6.37066632506008e-07, -9.44485038780872e-09},
            {2.19476873575507e-06, -2.20580510638233e-07, 6.94761415598378e-07, 4.80770865279717e-07, -1.34357837196401e-07},
            {2.18469215148328e-05, -1.80674174262038e-06, -1.52754285605060e-06, -3.51212288219241e-07, 2.73741237656351e-06},
            {2.85579058479116e-06, 1.57201369332361e-07, -2.80599072875081e-07, -4.91267304946072e-07, -2.11648188821805e-07},
            {2.81729255594770e-06, 3.02487362536122e-07, -1.64836481475431e-07, -2.11607615408593e-07, -6.47817762225366e-08},
            {1.31809947620223e-07, -1.58289524114549e-07, -7.05580919885505e-08, 5.56781440550867e-08, 1.23403290710365e-08},
            {-1.29252282695869e-05, -1.07247072037590e-05, -3.31109519638196e-06, 2.13776673779736e-06, -1.49519398373391e-07},
            {1.81685152305722e-06, -1.17362204417861e-06, -3.19205277136370e-08, 4.09166457255416e-07, 1.53286667406152e-07},
            {1.63477723125362e-06, -2.68584775517243e-08, 4.94662064805191e-09, -7.09027987928288e-08, 4.44353430574937e-08},
            {-2.13090618917978e-07, 4.05836983493219e-08, 2.94495876336549e-08, -1.75005469063176e-08, -3.03015988647002e-09},
            {-2.16074435298006e-09, 9.37631708987675e-09, -2.05996036369828e-08, 6.97068002894092e-09, -8.90988987979604e-09},
            {1.38047798906967e-05, 2.05528261553901e-05, 1.59072148872708e-05, 7.34088731264443e-07, 1.28226710383580e-06},
            {7.08175753966264e-07, -9.27988276636505e-07, 1.60535820026081e-07, -3.27296675122065e-07, -2.20518321170684e-07},
            {1.90932483086199e-07, -7.44215272759193e-08, 1.81330673333187e-08, 4.37149649043616e-08, 4.18884335594172e-08},
            {-5.37009063880924e-08, 2.22870057779431e-08, 1.73740123037651e-08, -4.45137302235032e-09, 9.44721910524571e-09},
            {-6.83406949047909e-08, -1.95046676795923e-10, 2.57535903049686e-09, 4.82643164083020e-09, 3.37657333705158e-09},
            {3.96128688448981e-09, -6.63809403270686e-10, 2.44781464212534e-10, 5.92280853590699e-11, -4.78502591970721e-10},
            {1.75859399041414e-05, -2.81238050668481e-06, -2.43670534594848e-06, 3.58244562699714e-06, -1.76547446732691e-06},
            {-1.06451311473304e-07, 1.54336689617184e-06, -2.00690000442673e-07, 1.38790047911880e-09, -1.62490619890017e-07},
            {-2.72757421686155e-07, 1.71139266205398e-07, -2.55080309401917e-08, -8.40793079489831e-09, -1.01129447760167e-08},
            {2.92966025844079e-08, -2.07556718857313e-08, 5.45985315647905e-09, 8.76857690274150e-09, 1.06785510440474e-08},
            {-1.22059608941331e-08, 6.52491630264276e-09, -1.79332492326928e-10, 3.75921793745396e-10, -7.06416506254786e-10},
            {1.63224355776652e-09, 4.95586028736232e-10, -3.07879011759040e-10, -7.78354087544277e-11, 1.43959047067250e-10},
            {3.86319414653663e-10, -2.06467134617933e-10, 4.37330971382694e-11, -5.00421056263711e-11, -9.40237773015723e-12},
            {-1.23856142706451e-05, 7.61047394008415e-06, -1.99104114578138e-07, 6.86177748886858e-07, -1.09466747592827e-07},
            {2.99866062403128e-07, 1.87525561397390e-07, 4.99374806994715e-08, 4.86229763781404e-07, 4.46570575517658e-07},
            {-5.05748332368430e-07, 1.95523624722285e-08, -9.17535435911345e-08, -2.56671607433547e-08, -7.11896201616653e-08},
            {-2.66062200406494e-08, -5.40470019739274e-09, -2.29718660244954e-09, -3.73328592264404e-09, 3.38748313712376e-09},
            {5.30855327954894e-10, 5.28851845648032e-10, -2.22278913745418e-10, -5.52628653064771e-11, -9.24825145219684e-10},
            {6.03737227573716e-10, -3.52190673510919e-12, -1.30371720641414e-10, -9.12787239944822e-12, 6.42187285537238e-12},
            {1.78081862458539e-10, 2.93772078656037e-12, -1.04698379945322e-11, -2.82260024833024e-11, -5.61810459067525e-12},
            {9.35003092299580e-12, -8.23133834521577e-13, 5.54878414224198e-13, -3.62943215777181e-13, 2.38858933771653e-12},
            {-1.31216096107331e-05, -5.70451670731759e-06, -5.11598683573971e-06, -4.99990779887599e-06, 1.27389320221511e-07},
            {-1.23108260369048e-06, 5.53093245213587e-07, 8.60093183929302e-07, 2.65569700925696e-07, 1.95485134805575e-07},
            {-2.29647072638049e-07, -5.45266515081825e-08, 2.85298129762263e-08, 1.98167939680185e-08, 5.52227340898335e-09},
            {-2.73844745019857e-08, -4.48345173291362e-10, -1.93967347049382e-09, -1.41508853776629e-09, -1.75456962391145e-09},
            {-2.68863184376108e-11, -2.20546981683293e-09, 6.56116990576877e-10, 1.27129855674922e-10, -2.32334506413213e-10},
            {1.98303136881156e-10, 6.04782006047075e-11, 2.91291115431570e-11, 6.18098615782757e-11, -3.82682292530379e-11},
            {9.48294455071158e-12, -3.05873596453015e-13, 5.31539408055057e-13, -7.31016438665600e-12, -1.19921002209198e-11},
            {-2.25188050845725e-11, -3.91627574966393e-13, -6.80217235976769e-13, 5.91033607278405e-13, 5.02991534452191e-13},
            {1.29532063896247e-12, 1.66337285851564e-13, 3.25543028344555e-13, 1.89143357962363e-13, 3.32288378169726e-13},
            {-2.45864358781728e-06, 4.49460524898260e-06, 1.03890496648813e-06, -2.73783420376785e-06, 7.12695730642593e-07},
            {-9.27805078535168e-07, -4.97733876686731e-07, 9.18680298906510e-08, -2.47200617423980e-07, 6.16163630140379e-08},
            {-1.39623661883136e-08, -1.12580495666505e-07, 2.61821435950379e-08, -2.31875562002885e-08, 5.72679835033659e-08},
            {-9.52538983318497e-09, -5.40909215302433e-09, 1.88698793952475e-09, -4.08127746406372e-09, 1.09534895853812e-10},
            {3.79767457525741e-09, 1.11549801373366e-10, -6.45504957274111e-10, 3.05477141010356e-10, 1.26261210565856e-10},
            {5.08813577945300e-11, 1.43250547678637e-11, 8.81616572082448e-12, 2.58968878880804e-11, 3.83421818249954e-11},
            {8.95094368142044e-12, -3.26220304555971e-12, -1.28047847191896e-12, 2.67562170258942e-12, 2.72195031576670e-12},
            {-6.47181697409757e-12, 1.13776457455685e-12, 2.84856274334969e-13, -7.63667272085395e-14, -1.34451657758826e-13},
            {-1.25291265888343e-12, 8.63500441050317e-14, -1.21307856635548e-13, 5.12570529540511e-14, 3.32389276976573e-14},
            {3.73573418085813e-14, -5.37808783042784e-16, -4.23430408270850e-16, -4.75110565740493e-15, 6.02553212780166e-15},
            {8.95483987262751e-06, -3.90778212666235e-06, -1.12115019808259e-06, 1.78678942093383e-06, 1.46806344157962e-06},
            {-4.59185232678613e-07, 1.09497995905419e-07, 1.31663977640045e-07, 4.20525791073626e-08, -9.71470741607431e-08},
            {1.63399802579572e-07, 1.50909360648645e-08, -1.11480472593347e-08, -1.84000857674573e-08, 7.82124614794256e-09},
            {1.22887452385094e-08, -4.06647399822746e-10, -6.49120327585597e-10, 8.63651225791194e-10, -2.73440085913102e-09},
            {2.51748630889583e-09, 4.79895880425564e-10, -2.44908073860844e-10, 2.56735882664876e-10, -1.64815306286912e-10},
            {4.85671381736718e-11, -2.51742732115131e-11, -2.60819437993179e-11, 6.12728324086123e-12, 2.16833310896138e-11},
            {4.11389702320298e-12, -8.09433180989935e-13, -1.19812498226024e-12, 1.46885737888520e-12, 3.15807685137836e-12},
            {-1.47614580597013e-12, 4.66726413909320e-13, 1.72089709006255e-13, 1.13854935381418e-13, 2.77741161317003e-13},
            {-1.02257724967727e-13, 1.10394382923502e-13, -3.14153505370805e-15, 2.41103099110106e-14, 2.13853053149771e-14},
            {-3.19080885842786e-14, -9.53904307973447e-15, 2.74542788156379e-15, 2.33797859107844e-15, -2.53192474907304e-15},
            {-5.87702222126367e-15, -1.80133850930249e-15, -3.09793125614454e-16, -1.04197538975295e-16, 3.72781664701327e-16},
            {1.86187054729085e-06, 8.33098045333428e-06, 3.18277735484232e-06, -7.68273797022231e-07, -1.52337222261696e-06},
            {-5.07076646593648e-07, -8.61959553442156e-07, -3.51690005432816e-07, -4.20797082902431e-07, -3.07652993252673e-07},
            {-7.38992472164147e-08, -8.39473083080280e-08, -2.51587083298935e-08, 7.30691259725451e-09, -3.19457155958983e-08},
            {-1.99777182012924e-09, -3.21265085916022e-09, -4.84477421865675e-10, -1.82924814205799e-09, -3.46664344655997e-10},
            {-7.05788559634927e-11, 1.21840735569025e-10, 7.97347726425926e-11, 1.08275679614409e-10, -1.17891254809785e-10},
            {1.10299718947774e-11, -3.22958261390263e-11, -1.43535798209229e-11, 6.87096504209595e-12, -6.64963212272352e-12},
            {-6.47393639740084e-12, 1.03156978325120e-12, -9.20099775082358e-14, -2.40150316641949e-13, 1.14008812047857e-12},
            {-1.23957846397250e-13, 2.85996703969692e-13, 1.91579874982553e-13, 5.20597174693064e-14, -4.06741434883370e-14},
            {-2.35479068911236e-14, 1.97847338186993e-14, 1.58935977518516e-15, -2.32217195254742e-15, -8.48611789490575e-15},
            {1.03992320391626e-14, 1.54017082092642e-15, 1.05950035082788e-16, -1.17870898461353e-15, -1.10937420707372e-15},
            {-1.09011948374520e-15, -6.04168007633584e-16, -9.10901998157436e-17, 1.98379116989461e-16, -1.03715496658498e-16},
            {-1.38171942108278e-16, -6.33037999097522e-17, -1.38777695011470e-17, 1.94191397045401e-17, 5.70055906754485e-18},
            {1.92989406002085e-06, -3.82662130483128e-06, -4.60189561036048e-07, 2.24290587856309e-06, 1.40544379451550e-06},
            {6.49033717633394e-08, 2.41396114435326e-07, 2.73948898223321e-07, 1.10633664439332e-07, -3.19555270171075e-08},
            {-2.91988966963297e-08, -6.03828192816571e-09, 1.18462386444840e-08, 1.32095545004128e-08, -5.06572721528914e-09},
            {7.31079058474148e-09, -8.42775299751834e-10, 1.10190810090667e-09, 1.96592273424306e-09, -2.13135932785688e-09},
            {7.06656405314388e-11, 1.43441125783756e-10, 1.46962246686924e-10, 7.44592776425197e-11, -3.64331892799173e-11},
            {-2.52393942119372e-11, 1.07520964869263e-11, 5.84669886072094e-12, 6.52029744217103e-12, 1.82947123132059e-12},
            {-4.15669940115121e-12, -1.95963254053648e-13, 2.16977822834301e-13, -2.84701408462031e-13, 4.27194601040231e-13},
            {3.07891105454129e-13, 1.91523190672955e-13, 1.05367297580989e-13, -5.28136363920236e-14, -3.53364110005917e-14},
            {7.02156663274738e-15, 9.52230536780849e-15, -3.41019408682733e-15, -3.59825303352899e-15, -2.62576411636150e-15},
            {-1.75110277413804e-15, 5.29265220719483e-16, 4.45015980897919e-16, -3.80179856341347e-16, -4.32917763829695e-16},
            {1.16038609651443e-16, -6.69643574373352e-17, 2.65667154817303e-17, -9.76010333683956e-17, 4.07312981076655e-17},
            {5.72659246346386e-18, 1.30357528108671e-18, 2.49193258417535e-18, 1.76247014075584e-18, 7.59614374197688e-19},
            {1.03352170833303e-17, -2.30633516638829e-18, 2.84777940620193e-18, -7.72161347944693e-19, 6.07028034506380e-19}
        };

        /** Legendre functions a<sub>nm</sub> for coefficient c<sub>h</sub>.*/
        private static final double[][] CH_A = {
            {0.0571481238161787, 3.35402081801137e-05, 3.15988141788728e-05, -1.34477341887086e-05, -2.61831023577773e-07},
            {5.77367395845715e-05, -0.000669057185209558, -6.51057691648904e-05, -1.61830149147091e-06, 8.96771209464758e-05},
            {-8.50773002452907e-05, -4.87106614880272e-05, 4.03431160775277e-05, 2.54090162741464e-06, -5.59109319864264e-06},
            {0.00150536423187709, 0.000611682258892697, 0.000369730024614855, -1.95658439780282e-05, -3.46246726553700e-05},
            {-2.32168718433966e-05, -0.000127478686553809, -9.00292451740728e-05, -6.07834315901830e-05, -1.04628419422714e-05},
            {-1.38607250922551e-06, -3.97271603842309e-06, -8.16155320152118e-07, 5.73266706046665e-07, 2.00366060212696e-07},
            {6.52491559188663e-05, -0.00112224323460183, -0.000344967958304075, -7.67282640947300e-05, 0.000107907110551939},
            {-0.000138870461448036, -7.29995695401936e-05, 5.35986591445824e-05, 9.03804869703890e-06, 8.61370129482732e-06},
            {-9.98524443968768e-07, -6.84966792665998e-08, 1.47478021860771e-07, 1.94857794008064e-06, 7.17176852732910e-07},
            {1.27066367911720e-06, 1.12113289164288e-06, 2.71525688515375e-07, -2.76125723009239e-07, -1.05429690305013e-07},
            {-0.000377264999981652, 0.000262691217024294, 0.000183639785837590, 3.93177048515576e-06, -6.66187081899168e-06},
            {-4.93720951871921e-05, -0.000102820030405771, -5.69904376301748e-05, -3.79603438055116e-05, -3.96726017834930e-06},
            {-2.21881958961135e-06, -1.40207117987894e-06, 1.60956630798516e-07, 2.06121145135022e-06, 6.50944708093149e-07},
            {2.21876332411271e-07, 1.92272880430386e-07, -6.44016558013941e-09, -1.40954921332410e-07, -4.26742169137667e-07},
            {-3.51738525149881e-08, 2.89616194332516e-08, -3.40343352397886e-08, -2.89763392721812e-08, -6.40980581663785e-10},
            {3.51240856823468e-05, -0.000725895015345786, -0.000322514037108045, -0.000106143759981636, 4.08153152459337e-05},
            {-2.36269716929413e-05, -4.20691836557932e-05, 1.43926743222922e-05, 2.61811210631784e-05, 2.09610762194903e-05},
            {-7.91765756673890e-07, 1.64556789159745e-06, -9.43930166276555e-07, 6.46641738736139e-07, -5.91509547299176e-07},
            {3.92768838766879e-07, -1.98027731703690e-07, -5.41303590057253e-08, -4.21705797874207e-07, -6.06042329660681e-08},
            {-1.56650141024305e-08, 7.61808165752027e-08, -1.81900460250934e-08, 1.30196216971675e-08, 1.08616031342379e-08},
            {-2.80964779829242e-08, -7.25951488826103e-09, -2.59789823306225e-09, -2.79271942407154e-09, 4.10558774868586e-09},
            {-0.000638227857648286, -0.000154814045363391, 7.78518327501759e-05, -2.95961469342381e-05, 1.15965225055757e-06},
            {4.47833146915112e-06, 1.33712284237555e-05, 3.61048816552123e-06, -2.50717844073547e-06, -1.28100822021734e-05},
            {-2.26958070007455e-06, 2.57779960912242e-06, 1.08395653197976e-06, 1.29403393862805e-07, -1.04854652812567e-06},
            {-3.98954043463392e-07, -2.26931182815454e-07, -1.09169545045028e-07, -1.49509536031939e-07, -3.98376793949903e-07},
            {2.30418911071110e-08, 1.23098508481555e-08, -1.71161401463708e-08, 2.35829696577657e-09, 1.31136164162040e-08},
            {3.69423793101582e-09, 3.49231027561927e-10, -1.18581468768647e-09, 5.43180735828820e-10, 5.43192337651588e-10},
            {-1.38608847117992e-09, -1.86719145546559e-10, -8.13477384765498e-10, 2.01919878240491e-10, 1.00067892622287e-10},
            {-4.35499078415956e-05, 0.000450727967957804, 0.000328978494268850, -3.05249478582848e-05, -3.21914834544310e-05},
            {1.24887940973241e-05, 1.34275239548403e-05, 1.11275518344713e-06, 7.46733554562851e-06, -2.12458664760353e-06},
            {9.50250784948476e-07, 2.34367372695203e-06, -5.43099244798980e-07, -4.35196904508734e-07, -8.31852234345897e-07},
            {5.91775478636535e-09, -1.48970922508592e-07, 2.99840061173840e-08, -1.30595933407792e-07, 1.27136765045597e-07},
            {-1.78491083554475e-08, 1.76864919393085e-08, -1.96740493482011e-08, 1.21096708004261e-08, 2.95518703155064e-10},
            {1.75053510088658e-09, -1.31414287871615e-09, -1.44689439791928e-09, 1.14682483668460e-09, 1.74488616540169e-09},
            {1.08152964586251e-09, -3.85678162063266e-10, -2.77851016629979e-10, 3.89890578625590e-11, -2.54627365853495e-10},
            {-1.88340955578221e-10, 5.19645384002867e-11, 2.14131326027631e-11, 1.24027770392728e-11, -9.42818962431967e-12},
            {0.000359777729843898, -0.000111692619996219, -6.87103418744904e-05, 0.000115128973879551, 7.59796247722486e-05},
            {5.23717968000879e-05, 1.32279078116467e-05, -5.72277317139479e-07, -7.56326558610214e-06, -1.95749622214651e-05},
            {1.00109213210139e-06, -2.75515216592735e-07, -1.13393194050846e-06, -4.75049734870663e-07, -3.21499480530932e-07},
            {-2.07013716598890e-07, -7.31392258077707e-08, -3.96445714084160e-08, 3.21390452929387e-08, -1.43738764991525e-08},
            {2.03081434931767e-09, -1.35423687136122e-08, -4.47637454261816e-09, 2.18409121726643e-09, -3.74845286805217e-09},
            {3.17469255318367e-09, 2.44221027314129e-10, -2.46820614760019e-10, 7.55851003884434e-10, 6.98980592550891e-10},
            {9.89541493531067e-11, -2.78762878057315e-11, -2.10947962916771e-10, 3.77882267360636e-11, -1.20009542671532e-12},
            {5.01720575730940e-11, 1.66470417102135e-11, -7.50624817938091e-12, 9.97880221482238e-12, 4.87141864438892e-12},
            {2.53137945301589e-11, 1.93030083090772e-12, -1.44708804231290e-12, -1.77837100743423e-12, -8.10068935490951e-13},
            {0.000115735341520738, 0.000116910591048350, 8.36315620479475e-05, 1.61095702669207e-05, -7.53084853489862e-05},
            {-9.76879433427199e-06, 9.16968438003335e-06, -8.72755127288830e-06, -1.30077933880053e-05, -9.78841937993320e-06},
            {1.04902782517565e-07, 2.14036988364936e-07, -7.19358686652888e-07, 1.12529592946332e-07, 7.07316352860448e-07},
            {7.63177265285080e-08, 1.22781974434290e-07, 8.99971272969286e-08, 5.63482239352990e-08, 4.31054352285547e-08},
            {3.29855763107355e-09, -6.95004336734441e-09, -6.52491370576354e-09, 1.97749180391742e-09, 3.51941791940498e-09},
            {3.85373745846559e-10, 1.65754130924183e-10, -3.31326088103057e-10, 5.93256024580436e-10, 1.27725220636915e-10},
            {-1.08840956376565e-10, -4.56042860268189e-11, -4.77254322645633e-12, -2.94405398621875e-12, -3.07199979999475e-11},
            {2.07389879095010e-11, 1.51186798732451e-11, 9.28139802941848e-12, 5.92738269687687e-12, 9.70337402306505e-13},
            {-2.85879708060306e-12, 1.92164314717053e-13, 4.02664678967890e-14, 5.18246319204277e-13, -7.91438726419423e-13},
            {6.91890667590734e-13, -8.49442290988352e-14, -5.54404947212402e-15, 9.71093377538790e-15, -5.33714333415971e-14},
            {-5.06132972789792e-05, -4.28348772058883e-05, -6.90746551020305e-05, 8.48380415176836e-05, 7.04135614675053e-05},
            {-1.27945598849788e-05, -1.92362865537803e-05, -2.30971771867138e-06, -8.98515975724166e-06, 5.25675205004752e-06},
            {-8.71907027470177e-07, -1.02091512861164e-06, -1.69548051683864e-07, 4.87239045855761e-07, 9.13163249899837e-07},
            {-6.23651943425918e-08, 6.98993315829649e-08, 5.91597766733390e-08, 4.36227124230661e-08, 6.45321798431575e-08},
            {-1.46315079552637e-10, -7.85142670184337e-09, 1.48788168857903e-09, 2.16870499912160e-09, -1.16723047065545e-09},
            {3.31888494450352e-10, 1.90931898336457e-10, -3.13671901557599e-11, 2.60711798190524e-10, 8.45240112207997e-11},
            {1.36645682588537e-11, -5.68830303783976e-12, 1.57518923848140e-11, -1.61935794656758e-11, -4.16568077748351e-12},
            {9.44684950971905e-13, 7.30313977131995e-12, 3.14451447892684e-12, 6.49029875639842e-13, -9.66911019905919e-13},
            {-8.13097374090024e-13, 5.23351897822186e-13, 8.94349188113951e-14, -1.33327759673270e-13, -4.04549450989029e-13},
            {-3.76176467005839e-14, -6.19953702289713e-14, -3.74537190139726e-14, 1.71275486301958e-14, -3.81946773167132e-14},
            {-4.81393385544160e-14, 3.66084990006325e-15, 3.10432030972253e-15, -4.10964475657416e-15, -6.58644244242900e-15},
            {-7.81077363746945e-05, -0.000254773632197303, -0.000214538508009518, -3.80780934346726e-05, 1.83495359193990e-05},
            {5.89140224113144e-06, -3.17312632433258e-06, -3.81872516710791e-06, -2.27592226861647e-06, 1.57044619888023e-06},
            {-1.44272505088690e-06, -1.10236588903758e-07, 2.64336813084693e-07, 4.76074163332460e-07, 4.28623587694570e-07},
            {3.98889120733904e-08, -1.29638005554027e-08, -4.13668481273828e-08, 1.27686793719542e-09, -3.54202962042383e-08},
            {1.60726837551750e-09, -2.70750776726156e-09, 2.79387092681070e-09, -3.01419734793998e-10, -1.29101669438296e-10},
            {-2.55708290234943e-10, 2.27878015173471e-11, -6.43063443462716e-12, 1.26531554846856e-10, -1.65822147437220e-10},
            {-3.35886470557484e-11, -3.51895009091595e-12, 5.80698399963198e-12, -2.84881487149207e-12, 8.91708061745902e-12},
            {-3.12788523950588e-12, 3.35366912964637e-12, 2.52236848033838e-12, -8.12801050709184e-13, -2.63510394773892e-13},
            {6.83791881183142e-14, 2.41583263270381e-13, 8.58807794189356e-14, -5.12528492761045e-14, -1.40961725631276e-13},
            {-1.28585349115321e-14, -2.11049721804969e-14, 5.26409596614749e-15, -4.31736582588616e-15, -1.60991602619068e-14},
            {-9.35623261461309e-15, -3.94384886372442e-16, 5.04633016896942e-16, -5.40268998456055e-16, -1.07857944298104e-15},
            {8.79756791888023e-16, 4.52529935675330e-16, 1.36886341163227e-16, -1.12984402980452e-16, 6.30354561057224e-18},
            {0.000117829256884757, 2.67013591698442e-05, 2.57913446775250e-05, -4.40766244878807e-05, -1.60651761172523e-06},
            {-1.87058092029105e-05, 1.34371169060024e-05, 5.59131416451555e-06, 4.50960364635647e-06, 2.87612873904633e-06},
            {2.79835536517287e-07, 8.93092708148293e-07, 8.37294601021795e-07, -1.99029785860896e-08, -8.87240405168977e-08},
            {4.95854313394905e-08, -1.44694570735912e-08, 2.51662229339375e-08, -3.87086600452258e-09, 2.29741919071270e-08},
            {4.71497840986162e-09, 2.47509999454076e-09, 1.67323845102824e-09, 8.14196768283530e-10, -3.71467396944165e-10},
            {-1.07340743907054e-10, -8.07691657949326e-11, -5.99381660248133e-11, 2.33173929639378e-12, -2.26994195544563e-11},
            {-3.83130441984224e-11, -5.82499946138714e-12, 1.43286311435124e-11, 3.15150503353387e-12, 5.97891025146774e-12},
            {-5.64389191072230e-13, 9.57258316335954e-13, 1.12055192185939e-12, -4.42417706775420e-13, -9.93190361616481e-13},
            {1.78188860269677e-13, 7.82582024904950e-14, 5.18061650118009e-14, 2.13456507353387e-14, -5.26202113779510e-14},
            {-8.18481324740893e-15, -3.71256746886786e-15, 4.23508855164371e-16, -2.91292502923102e-15, -1.15454205389350e-14},
            {6.16578691696810e-15, 6.74087154080877e-16, 5.71628946437034e-16, -2.05251213979975e-16, -7.25999138903781e-16},
            {9.35481959699383e-17, 6.23535830498083e-17, 3.18076728802060e-18, -2.92353209354587e-17, 7.65216088665263e-19},
            {2.34173078531701e-17, -8.30342420281772e-18, -4.33602329912952e-18, 1.90226281379981e-18, -7.85507922718903e-19}
        };

        /** Legendre functions a<sub>nm</sub> for coefficient c<sub>w</sub>.*/
        private static final double[][] CW_A = {
            {0.0395329695826997, -0.000131114380761895, -0.000116331009006233, 6.23548420410646e-05, 5.72641113425116e-05},
            {-0.000441837640880650, 0.000701288648654908, 0.000338489802858270, 3.76700309908602e-05, -8.70889013574699e-06},
            {1.30418530496887e-05, -0.000185046547597376, 4.31032103066723e-05, 0.000105583334124319, 3.23045436993589e-05},
            {3.68918433448519e-05, -0.000219433014681503, 3.46768613485000e-06, -9.17185187163528e-05, -3.69243242456081e-05},
            {-6.50227201116778e-06, 2.07614874282187e-05, -5.09131314798362e-05, -3.08053225174359e-05, -4.18483655873918e-05},
            {2.67879176459056e-05, -6.89303730743691e-05, 2.11046783217168e-06, 1.93163912538178e-05, -1.97877143887704e-06},
            {0.000393937595007422, -0.000452948381236406, -0.000136517846073846, 0.000138239247989489, 0.000133175232977863},
            {5.00214539435002e-05, 3.57229726719727e-05, -9.38010547535432e-07, -3.52586798317563e-05, -7.01218677681254e-06},
            {3.91965314099929e-05, 1.02236686806489e-05, -1.95710695226022e-05, -5.93904795230695e-06, 3.24339769876093e-06},
            {6.68158778290653e-06, -8.10468752307024e-06, -9.91192994096109e-06, -1.89755520007723e-07, -3.26799467595579e-06},
            {0.000314196817753895, -0.000296548447162009, -0.000218410153263575, -1.57318389871000e-05, 4.69789570185785e-05},
            {0.000104597721123977, -3.31000119089319e-05, 5.60326793626348e-05, 4.71895007710715e-05, 3.57432326236664e-05},
            {8.95483021572039e-06, 1.44019305383365e-05, 4.87912790492931e-06, -3.45826387853503e-06, 3.23960320438157e-06},
            {-1.35249651009930e-05, -2.49349762695977e-06, -2.51509483521132e-06, -9.14254874104858e-07, -8.57897406100890e-07},
            {-1.68143325235195e-06, 1.72073417594235e-06, 1.38765993969565e-06, 4.09770982137530e-07, -6.60908742097123e-07},
            {-0.000639889366487161, 0.00120194042474696, 0.000753258598887703, 3.87356377414663e-05, 1.31231811175345e-05},
            {2.77062763606783e-05, -9.51425270178477e-06, -6.61068056107547e-06, -1.38713669012109e-05, 9.84662092961671e-06},
            {-2.69398078539471e-06, 6.50860676783123e-06, 3.80855926988090e-06, -1.98076068364785e-06, 1.17187335666772e-06},
            {-2.63719028151905e-06, 5.03149473656743e-07, 7.38964893399716e-07, -8.38892485369078e-07, 1.30943917775613e-06},
            {-1.56634992245479e-06, -2.97026487417045e-08, 5.06602801102463e-08, -4.60436007958792e-08, -1.62536449440997e-07},
            {-2.37493912770935e-07, 1.69781593069938e-08, 8.35178275224265e-08, -4.83564044549811e-08, -4.96448864199318e-08},
            {0.00134012259587597, -0.000250989369253194, -2.97647945512547e-05, -6.47889968094926e-05, 8.41302130716859e-05},
            {-0.000113287184900929, 4.78918993866293e-05, -3.14572113583139e-05, -2.10518256626847e-05, -2.03933633847417e-05},
            {-4.97413321312139e-07, 3.72599822034753e-06, -3.53221588399266e-06, -1.05232048036416e-06, -2.74821498198519e-06},
            {4.81988542428155e-06, 4.21400219782474e-07, 1.02814808667637e-06, 4.40299068486188e-09, 3.37103399036634e-09},
            {1.10140301678818e-08, 1.90257670180182e-07, -1.00831353341885e-08, 1.44860642389714e-08, -5.29882089987747e-08},
            {6.12420414245775e-08, -4.48953461152996e-09, -1.38837603709003e-08, -2.05533675904779e-08, 1.49517908802329e-09},
            {9.17090243673643e-10, -9.24878857867367e-09, -2.30856560363943e-09, -4.36348789716735e-09, -4.45808881183025e-10},
            {-0.000424912699609112, -0.000114365438471564, -0.000403200981827193, 4.19949560550194e-05, -3.02068483713739e-05},
            {3.85435472851225e-05, -5.70726887668306e-05, 4.96313706308613e-07, 1.02395703617082e-05, 5.85550000567006e-06},
            {-7.38204470183331e-06, -4.56638770109511e-06, -3.94007992121367e-06, -2.16666812189101e-06, -4.55694264113194e-06},
            {5.89841165408527e-07, 1.40862905173449e-08, 1.08149086563211e-07, -2.18592601537944e-07, -3.78927431428119e-07},
            {4.85164687450468e-08, 8.34273921293655e-08, 1.47489605513673e-08, 6.01494125001291e-08, 6.43812884159484e-09},
            {1.13055580655363e-08, 3.50568765400469e-09, -5.09396162501750e-09, -1.83362063152411e-09, -4.11227251553035e-09},
            {3.16454132867156e-09, -1.39634794131087e-09, -7.34085003895929e-10, -7.55541371271796e-10, -1.57568747643705e-10},
            {1.27572900992112e-09, -3.51625955080441e-10, -4.84132020565098e-10, 1.52427274930711e-10, 1.27466120431317e-10},
            {-0.000481655666236529, -0.000245423313903835, -0.000239499902816719, -0.000157132947351028, 5.54583099258017e-05},
            {-1.52987254785589e-05, 2.78383892116245e-05, 4.32299123991860e-05, 1.70981319744327e-05, -1.35090841769225e-06},
            {-8.65400907717798e-06, -6.51882656990376e-06, -2.43810171017369e-07, 8.54348785752623e-07, 2.98371863248143e-07},
            {-1.68155571776752e-06, -3.53602587563318e-07, -1.00404435881759e-07, -2.14162249012859e-08, -2.42131535531526e-07},
            {-1.08048603277187e-08, -9.78850785763030e-08, -2.32906554437417e-08, 2.22003630858805e-08, -2.27230368089683e-09},
            {-5.98864391551041e-09, 7.38970926486848e-09, 3.61322835311957e-09, 3.70037329172919e-09, -3.41121137081362e-09},
            {-7.33113754909726e-10, -9.08374249335220e-11, -1.78204392133739e-10, 8.28618491929026e-11, -1.32966817912373e-10},
            {-5.23340481314676e-10, 1.36403528233346e-10, -7.04478837151279e-11, -6.83175201536443e-12, -2.86040864071134e-12},
            {3.75347503578356e-11, -1.08518134138781e-11, -2.53583751744508e-12, 1.00168232812303e-11, 1.74929602713312e-11},
            {-0.000686805336370570, 0.000591849814585706, 0.000475117378328026, -2.59339398048415e-05, 3.74825110514968e-05},
            {3.35231363034093e-05, 2.38331521146909e-05, 7.43545963794093e-06, -3.41430817541849e-06, 7.20180957675353e-06},
            {3.60564374432978e-07, -3.13300039589662e-06, -6.38974746108020e-07, -8.63985524672024e-07, 2.43367665208655e-06},
            {-4.09605238516094e-07, -2.51158699554904e-07, -1.29359217235188e-07, -2.27744642483133e-07, 7.04065989970205e-08},
            {6.74886341820129e-08, -1.02009407061935e-08, -3.30790296448812e-08, 1.64959795655031e-08, 1.40641779998855e-08},
            {1.31706886235108e-09, -1.06243701278671e-09, -2.85573799673944e-09, 3.72566568681289e-09, 2.48402582003925e-09},
            {-3.68427463251097e-11, -1.90028122983781e-10, -3.98586561768697e-11, 1.14458831693287e-11, -2.27722300377854e-12},
            {-7.90029729611056e-11, 3.81213646526419e-11, 4.63303426711788e-11, 1.52294835905903e-11, -2.99094751490726e-12},
            {-2.36146602045017e-11, 1.03852674709985e-11, -4.47242126307100e-12, 5.30884113537806e-12, 1.68499023262969e-12},
            {-3.30107358134527e-13, -4.73989085379655e-13, 5.17199549822684e-13, 2.34951744478255e-13, 2.05931351608192e-13},
            {0.000430215687511780, -0.000132831373000014, -3.41830835017045e-05, 4.70312161436033e-06, -3.84807179340006e-05},
            {1.66861163032403e-05, -8.10092908523550e-06, 8.20658107437905e-06, 6.12399025026683e-06, -1.85536495631911e-06},
            {1.53552093641337e-06, 2.19486495660361e-06, -1.07253805120137e-06, -4.72141767909137e-07, 4.00744581573216e-07},
            {2.56647305130757e-07, -8.07492046592274e-08, -2.05858469296168e-07, 1.09784168930599e-07, -7.76823030181225e-08},
            {1.77744008115031e-08, 1.64134677817420e-08, 4.86163044879020e-09, 1.13334251800856e-08, -7.17260621115426e-09},
            {1.61133063219326e-09, -1.85414677057024e-09, -2.13798537812651e-09, 1.15255123229679e-09, 2.24504700129464e-09},
            {1.23344223096739e-10, -1.20385012169848e-10, -2.18038256346433e-12, 3.23033120628279e-11, 8.01179568213400e-11},
            {-6.55745274387847e-12, 1.22127104697198e-11, 5.83805016355883e-12, -8.31201582509817e-12, 1.90985373872656e-12},
            {-2.89199983667265e-12, 5.05962500506667e-12, 1.28092925110279e-12, 5.60353813743813e-13, 1.76753731968770e-12},
            {-1.61678729774956e-13, -3.92206170988615e-13, -9.04941327579237e-14, 1.89847694200763e-13, 4.10008676756463e-14},
            {-1.16808369005656e-13, -9.97464591430510e-14, 7.46366550245722e-15, 2.53398578153179e-14, 1.06510689748906e-14},
            {-0.000113716921384790, -0.000131902722651488, -0.000162844886485788, 7.90171538739454e-06, -0.000178768066961413},
            {-2.13146535366500e-06, -3.57818705543597e-05, -1.50825855069298e-05, -2.17909259570022e-05, -8.19332236308581e-06},
            {-2.88001138617357e-06, -2.09957465440793e-06, 6.81466526687552e-08, 3.58308906974448e-07, -4.18502067223724e-07},
            {-1.10761444317605e-07, 6.91773860777929e-08, 8.17125372450372e-08, -2.16476237959181e-08, 7.59221970502074e-08},
            {-9.56994224818941e-09, 6.64104921728432e-09, 6.33077902928348e-09, 2.85721181743727e-09, -6.39666681678123e-09},
            {4.62558627839842e-10, -1.69014863754621e-09, -2.80260429599733e-10, 4.27558937623863e-11, -1.66926133269027e-10},
            {-7.23385132663753e-11, 5.51961193545280e-11, 3.04070791942335e-11, 3.23227055919062e-12, 8.47312431934829e-11},
            {-1.61189613765486e-11, 1.66868155925172e-11, 1.05370341694715e-11, -4.41495859079592e-12, -2.24939051401750e-12},
            {-8.72229568056267e-13, 1.88613726203286e-12, 1.21711137534390e-14, -1.13342372297867e-12, -6.87151975256052e-13},
            {7.99311988544090e-15, 4.46150979586709e-14, 7.50406779454998e-14, -3.20385428942275e-14, -1.26543636054393e-14},
            {4.80503817699514e-14, -3.35545623603729e-14, -1.18546423610485e-14, 4.19419209985980e-15, -1.73525614436880e-14},
            {-1.20464898830163e-15, -8.80752065000456e-16, -1.22214298993313e-15, 1.69928513019657e-15, 1.93593051311405e-16},
            {1.68528879784841e-05, 3.57144412031081e-05, -1.65999910125077e-05, 5.40370336805755e-05, 0.000118138122851376},
            {-3.28151779115881e-05, 1.04231790790798e-05, -2.80761862890640e-06, 2.98996152515593e-06, -2.67641158709985e-06},
            {-2.08664816151978e-06, -1.64463884697475e-06, 6.79099429284834e-08, 7.23955842946495e-07, -6.86378427465657e-07},
            {-2.88205823027255e-09, 2.38319699493291e-09, 1.14169347509045e-07, 8.12981074994402e-08, -1.56957943666988e-07},
            {-7.09711403570189e-09, 6.29470515502988e-09, 3.50833306577579e-09, 8.31289199649054e-09, -2.14221463168338e-09},
            {-8.11910123910038e-10, 3.34047829618955e-10, 3.70619377446490e-10, 3.30426088213373e-10, 4.86297305597865e-11},
            {1.98628160424161e-11, -4.98557831380098e-12, -5.90523187802174e-12, -1.27027116925122e-12, 1.49982368570355e-11},
            {2.62289263262748e-12, 3.91242360693861e-12, 6.56035499387192e-12, -1.17412941089401e-12, -9.40878197853394e-13},
            {-3.37805010124487e-13, 5.39454874299593e-13, -2.41569839991525e-13, -2.41572016820792e-13, -3.01983673057198e-13},
            {-1.85034053857964e-13, 4.31132161871815e-14, 4.13497222026824e-15, -4.60075514595980e-14, -1.92454846400146e-14},
            {2.96113888929854e-15, -1.11688534391626e-14, 3.76275373238932e-15, -3.72593295948136e-15, 1.98205490249604e-16},
            {1.40074667864629e-15, -5.15564234798333e-16, 3.56287382196512e-16, 5.07242777691587e-16, -2.30405782826134e-17},
            {2.96822530176851e-16, -4.77029898301223e-17, 1.12782285532775e-16, 1.58443229778573e-18, 8.22141904662969e-17}
        };

        /** Legendre functions b<sub>nm</sub> for coefficient b<sub>h</sub>.*/
        private static final double[][] BH_B = {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {-2.29210587053658e-06, -2.33805004374529e-06, -7.49312880102168e-07, -5.12022747852006e-07, 5.88926055066172e-07},
            {0, 0, 0, 0, 0},
            {-4.63382754843690e-06, -2.23853015662938e-06, 8.14830531656518e-07, 1.15453269407116e-06, -4.53555450927571e-07},
            {-6.92432096320778e-07, -2.98734455136141e-07, 1.48085153955641e-08, 1.37881746148773e-07, -6.92492118460215e-09},
            {0, 0, 0, 0, 0},
            {-1.91507979850310e-06, -1.83614825459598e-06, -7.46807436870647e-07, -1.28329122348007e-06, 5.04937180063059e-07},
            {-8.07527103916713e-07, 2.83997840574570e-08, -6.01890498063025e-08, -2.48339507554546e-08, 2.46284627824308e-08},
            {-2.82995069303093e-07, 1.38818274596408e-09, 3.22731214161408e-09, 2.87731153972404e-10, 1.53895537278496e-08},
            {0, 0, 0, 0, 0},
            {-6.68210270956800e-07, -2.19104833297845e-06, 1.30116691657253e-07, 4.78445730433450e-07, -4.40344300914051e-07},
            {-2.36946755740436e-07, -1.32730991878204e-07, 1.83669593693860e-08, 7.90218931983569e-08, -4.70161979232584e-08},
            {1.07746083292179e-07, -4.17088637760330e-09, -1.83296035841109e-09, -5.80243971371211e-09, -2.11682361167439e-09},
            {-5.44712355496109e-08, 1.89717032256923e-09, 2.27327316287804e-10, 7.78400728280038e-10, 8.82380487618991e-12},
            {0, 0, 0, 0, 0},
            {-5.61707049615673e-08, -1.09066447089585e-06, -2.25742250174119e-07, -8.64367795924377e-07, 1.06411275240680e-08},
            {2.41782935157918e-08, -3.65762298303819e-08, -6.93420659586875e-08, -3.97316214341991e-08, -2.08767816486390e-08},
            {6.38293030383436e-08, 1.11377936334470e-08, 6.91424941454782e-09, 1.39887159955004e-09, 5.25428749022906e-09},
            {1.09291268489958e-08, 1.23935926756516e-10, 3.92917259954515e-10, -1.79144682483562e-10, -9.11802874917597e-10},
            {-4.40957607823325e-09, 1.45751390560667e-10, 1.24641258165301e-10, -6.45810339804674e-11, -8.92894658893326e-12},
            {0, 0, 0, 0, 0},
            {1.54754294162102e-08, -1.60154742388847e-06, -4.08425188394881e-07, 6.18170290113531e-09, -2.58919765162122e-07},
            {1.37130642286873e-08, -6.67813955828458e-08, -7.01410996605609e-09, 3.82732572660461e-08, -2.73381870915135e-08},
            {2.19113155379218e-08, 4.11027496396868e-09, 6.33816020485226e-09, -1.49242411327524e-09, -6.14224941851705e-10},
            {6.26573021218961e-09, 5.17137416480052e-10, -3.49784328298676e-10, 1.13578756343208e-10, 2.80414613398411e-10},
            {1.65048133258794e-11, 1.00047239417239e-10, 1.05124654878499e-10, -3.03826002621926e-11, 4.57155388334682e-11},
            {6.20221691418381e-11, 9.75852610098156e-12, -5.46716005756984e-12, 1.31643349569537e-11, 3.61618775715470e-12},
            {0, 0, 0, 0, 0},
            {-1.03938913012708e-06, -1.78417431315664e-07, 2.86040141364439e-07, 1.83508599345952e-08, -1.34452220464346e-07},
            {-4.36557481393662e-08, 7.49780206868834e-09, -8.62829428674082e-09, 5.50577793039009e-09, -9.46897502333254e-09},
            {3.43193738406672e-10, 1.13545447306468e-08, 1.25242388852214e-09, 6.03221501959620e-10, 1.57172070361180e-09},
            {-4.73307591021391e-10, 1.70855824051391e-10, -2.62470421477037e-11, 2.04525835988874e-10, -1.17859695928164e-10},
            {-3.36185995299839e-10, 3.19243054562183e-11, 1.17589412418126e-10, -1.35478747434514e-12, 5.11192214558542e-11},
            {3.19640547592136e-11, 2.94297823804643e-12, -1.00651526276990e-11, -1.67028733953153e-12, 3.03938833625503e-12},
            {1.68928641118173e-11, -7.90032886682002e-13, -1.40899773539137e-12, 7.76937592393354e-13, 7.32539820298651e-13},
            {0, 0, 0, 0, 0},
            {2.32949756055277e-07, 1.46237594908093e-07, -1.07770884952484e-07, 1.26824870644476e-07, -2.36345735961108e-08},
            {8.89572676497766e-08, 7.24810004121931e-08, 2.67583556180119e-08, 2.48434796111361e-08, -3.55004782858686e-09},
            {-1.00823909773603e-08, 8.84433929029076e-10, -2.55502517594511e-10, -5.48034274059119e-10, -8.50241938494079e-10},
            {1.13259819566467e-09, 5.55186945221216e-10, 7.63679807785295e-11, -1.70067998092043e-11, 1.57081965572493e-10},
            {-2.37748192185353e-10, 2.45463764948000e-11, 3.23208414802860e-11, -2.72624834520723e-12, 8.14449183666500e-12},
            {-1.54977633126025e-11, 4.58754903157884e-12, -1.25864665839074e-12, 2.44139868157872e-12, -1.82827441958193e-12},
            {3.28285563794513e-12, -1.10072329225465e-12, -7.23470501810935e-13, 5.85309745620389e-13, 4.11317589687125e-13},
            {4.57596974384170e-13, 9.84198128213558e-14, 3.34503817702830e-14, 7.08431086558307e-15, 2.79891177268807e-14},
            {0, 0, 0, 0, 0},
            {-3.67820719155580e-07, 6.98497901205902e-07, 1.83397388750300e-07, 2.39730262495372e-07, -2.58441984368194e-07},
            {5.17793954077994e-08, 5.54614175977835e-08, 1.75026214305232e-09, -2.55518450411346e-09, -6.12272723006537e-09},
            {-7.94292648157198e-09, -1.01709107852895e-09, -1.49251241812310e-09, 9.32827213605682e-10, -8.24490722043118e-10},
            {1.36410408475679e-11, 2.16390220454971e-10, 1.24934806872235e-10, -6.82507825145903e-11, -4.01575177719668e-11},
            {-1.41619917600555e-11, -1.54733230409082e-11, 1.36792829351538e-11, 1.11157862104733e-12, 2.08548465892268e-11},
            {-3.56521723755846e-12, 4.47877185884557e-12, -6.34096209274637e-16, -1.13010624512348e-12, -2.82018136861041e-13},
            {2.22758955943441e-12, -4.63876465559380e-13, -5.80688019272507e-13, 2.45878690598655e-13, 1.49997666808106e-13},
            {-6.26833903786958e-14, 2.73416335780807e-14, 1.91842340758425e-14, 1.67405061129010e-14, -2.45268543953704e-17},
            {1.81972870222228e-14, 5.43036245069085e-15, 1.92476637107321e-15, 8.78498602508626e-17, -1.42581647227657e-15},
            {0, 0, 0, 0, 0},
            {9.74322164613392e-07, -5.23101820582724e-07, -2.81997898176227e-07, 4.54762451707384e-08, -3.34645078118827e-08},
            {-6.75813194549663e-09, 3.49744702199583e-08, -5.09170419895883e-09, 5.24359476874755e-09, 4.96664262534662e-09},
            {4.53858847892396e-10, -1.49347392165963e-09, -2.00939511362154e-09, 9.30987163387955e-10, 9.74450200826854e-11},
            {-4.92900885858693e-10, 5.34223033225688e-12, 1.08501839729368e-10, -6.43526142089173e-11, -3.11063319142619e-11},
            {1.38469246386690e-11, -7.91180584906922e-12, 2.26641656746936e-13, 4.55251515177956e-12, 6.05270575117769e-12},
            {4.02247935664225e-12, 1.82776657951829e-12, -1.28348801405445e-13, -2.16257301300350e-13, -5.54363979435025e-14},
            {4.15005914461687e-13, -2.00647573581168e-13, -1.67278251942946e-13, 1.30332398257985e-13, 1.52742363652434e-13},
            {6.36376500056974e-14, 1.65794532815776e-14, -3.80832559052662e-15, -6.40262894005341e-16, 2.42577181848072e-15},
            {-5.55273521249151e-15, 3.69725182221479e-15, 2.02114207545759e-15, -4.50870833392161e-16, 9.62950493696677e-17},
            {1.00935904205024e-17, 6.54751873609395e-17, -1.09138810997186e-16, -8.62396750098759e-17, -3.82788257844306e-17},
            {0, 0, 0, 0, 0},
            {4.21958510903678e-07, -8.30678271007705e-08, -3.47006439555247e-07, -3.36442823712421e-08, 9.90739768222027e-08},
            {2.64389033612742e-08, 2.65825090066479e-09, -1.28895513428522e-08, -7.07182694980098e-10, 7.10907165301180e-09},
            {6.31203524153492e-09, -1.67038260990134e-09, 1.33104703539822e-09, 8.34376495185149e-10, -2.52478613522612e-10},
            {1.18414896299279e-10, -2.57745052288455e-11, 2.88295935685818e-11, -3.27782977418354e-11, -1.05705000036156e-11},
            {-4.20826459055091e-12, -6.97430607432268e-12, -3.90660545970607e-12, -3.90449239948755e-13, -4.60384797517466e-13},
            {-9.47668356558200e-13, 6.53305025354881e-13, 2.63240185434960e-13, 1.40129115015734e-13, 3.85788887132074e-14},
            {2.23947810407291e-13, 7.35262771548253e-15, -3.83348211931292e-14, 4.20376514344176e-14, 4.26445836468461e-14},
            {-3.88008154470596e-16, 2.28561424667750e-15, -8.73599966653373e-16, 2.14321147947665e-15, 6.38631825071920e-16},
            {-8.62165565535721e-15, 1.79742912149810e-15, 1.01541125038661e-15, -7.91027655831866e-17, -4.06505132825230e-16},
            {-2.35355054392189e-16, -6.13997759731013e-17, -2.73490528665965e-17, 2.63895177155121e-17, -4.47531057245187e-18},
            {6.01909706823530e-17, 5.35520010856833e-18, -2.15530106132531e-18, -2.46778496746231e-18, -7.09947296442799e-19},
            {0, 0, 0, 0, 0},
            {-3.75005956318736e-07, -5.39872297906819e-07, -1.19929654883034e-07, 4.52771083775007e-08, 1.82790552943564e-07},
            {7.82606642505646e-09, -1.68890832383153e-08, -8.45995188378997e-09, 1.42958730598502e-09, 3.21075754133531e-09},
            {4.28818421913782e-09, -1.07501469928219e-09, 8.84086350297418e-10, 9.74171228764155e-10, 8.59877149602304e-12},
            {1.28983712172521e-10, -6.96375160373676e-11, -2.13481436408896e-11, 1.33516375568179e-11, -1.65864626508258e-11},
            {-4.48914384622368e-12, 9.68953616831263e-13, -1.61372463422897e-12, -2.09683563440448e-12, -1.90096826314068e-12},
            {-1.12626619779175e-13, 3.34903159106509e-14, -1.21721528343657e-13, 7.46246339290354e-14, 3.68424909859186e-13},
            {5.08294274367790e-14, 2.83036159977090e-14, 1.48074873486387e-14, -9.59633528834945e-15, -1.26231060951100e-14},
            {-4.01464098583541e-16, 1.97047929526674e-15, -5.29967950447497e-16, -3.59120406619931e-16, 1.69690933982683e-16},
            {-1.73919209873841e-15, 7.52792462841274e-16, 3.65589287101147e-16, -7.79247612043812e-17, -8.24599670368999e-17},
            {-4.61555616150128e-17, 4.94529746019753e-19, -1.09858157212270e-17, 3.95550811124928e-18, 3.23972399884100e-18},
            {-2.27040686655766e-17, -3.27855689001215e-18, -3.30649011116861e-19, 9.08748546536849e-19, 8.92197599890994e-19},
            {5.67241944733762e-18, 3.84449400209976e-19, 1.77668058015537e-19, 2.00432838283455e-20, -2.00801461564767e-19}
        };

        /** Legendre functions b<sub>nm</sub> for coefficient b<sub>w</sub>.*/
        private static final double[][] BW_B = {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {-9.56715196386889e-06, -3.68040633020420e-08, 1.27846786489883e-07, 1.32525487755973e-06, 1.53075361125066e-06},
            {0, 0, 0, 0, 0},
            {-7.17682617983607e-06, 2.89994188119445e-06, -2.97763578173405e-07, 8.95742089134942e-07, 3.44416325304006e-07},
            {-8.02661132285210e-07, 3.66738692077244e-07, -3.02880965723280e-07, 3.54144282036103e-07, -1.68873066391463e-07},
            {0, 0, 0, 0, 0},
            {-2.89640569283461e-06, -7.83566373343614e-07, -8.36667214682577e-07, -7.41891843549121e-07, -9.23922655636489e-08},
            {-1.06144662284862e-06, 1.57709930505924e-07, 1.04203025714319e-07, 1.20783300488461e-07, -1.38726055821134e-07},
            {-4.16549018672265e-07, -1.35220897698872e-07, -6.40269964829901e-08, 1.63258283210837e-08, -2.57958025095959e-08},
            {0, 0, 0, 0, 0},
            {3.52324885892419e-06, -2.26705543513814e-07, 1.53835589488292e-06, -3.75263061267433e-07, 3.69384057396017e-07},
            {-2.06569149157664e-07, -9.36260183227175e-08, -3.55985284353048e-08, -9.13671163891094e-08, 6.93156256562600e-09},
            {1.32437594740782e-07, 4.44349887272663e-08, -3.38192451721674e-08, -3.97263855781102e-08, -1.93087822995800e-09},
            {-1.29595244818942e-07, -1.40852985547683e-08, 1.42587592939760e-09, 7.05779876554001e-09, -1.00996269264535e-08},
            {0, 0, 0, 0, 0},
            {4.06960756215938e-06, -1.97898540226986e-06, 7.21905857553588e-08, -1.19908881538755e-06, -5.67561861536903e-08},
            {6.53369660286999e-08, -2.42818687866392e-07, -1.66203004559493e-08, -2.41512414151897e-08, 4.45426333411018e-08},
            {1.44650670663281e-07, 8.50666367433859e-09, -4.61165612004307e-09, 4.88527987491045e-09, 1.06277326713172e-08},
            {1.86770937103513e-08, -6.44197940288930e-10, -7.60456736846174e-09, -9.97186468682689e-10, 8.73229752697716e-10},
            {-1.00206566229113e-08, 1.33934372663121e-09, 1.41691503439220e-09, 8.72352590578753e-10, -8.04561626629829e-10},
            {0, 0, 0, 0, 0},
            {3.07161843116618e-06, 1.82962085656470e-06, 1.87728623016069e-07, 7.10611617623261e-07, 2.26499092250481e-07},
            {4.50766403064905e-08, -1.67752393078256e-07, 2.47844723639070e-08, -3.56484348424869e-09, -1.56634836636584e-08},
            {3.77011651881090e-08, -7.23045828480496e-09, 5.22995988863761e-09, -1.03740320341306e-09, 4.57839777217789e-09},
            {8.09495635883121e-09, -3.01977244420529e-10, -2.30104544933093e-09, 3.63658580939428e-10, 4.39320811714867e-10},
            {9.37087629961269e-11, 1.00780920426635e-09, 1.28140539913350e-10, -6.65795285522138e-12, 4.71732796198631e-11},
            {-8.88504487069155e-11, -1.63253810435461e-10, 7.22669710644299e-11, 5.64715132584527e-11, -1.08949308197617e-12},
            {0, 0, 0, 0, 0},
            {-2.64054293284174e-07, -2.37611606117256e-06, -1.83671059706264e-06, -3.12199354841993e-07, -1.05598289276114e-07},
            {7.41706968747147e-08, -1.64359098062646e-08, -3.09750224040234e-08, -9.68640079410317e-09, -7.90399057863403e-08},
            {-1.00254376564271e-08, 1.12528248631191e-08, -2.67841549174100e-09, -2.69481819323647e-09, 1.56550607475331e-09},
            {-2.18568129350729e-09, 6.26422056977450e-10, 1.95007291427316e-09, 3.14226463591125e-10, -3.62000388344482e-10},
            {-9.30451291747549e-10, 5.62175549482704e-11, 1.01022849902012e-10, 5.18675856498499e-11, 5.37561696283235e-11},
            {5.33151334468794e-11, 1.07571307336725e-10, -1.31714567944652e-11, -4.17524405900018e-11, -2.16737797893502e-12},
            {4.69916869001309e-11, -4.34516364859583e-12, -6.61054225868897e-12, -5.75845818545368e-12, -2.32180293529175e-12},
            {0, 0, 0, 0, 0},
            {-3.50305843086926e-06, 1.76085131953403e-06, 8.16661224478572e-07, 4.09111042640801e-07, -9.85414469804995e-08},
            {1.44670876127274e-07, -1.41331228923029e-08, -3.06530152369269e-08, -1.46732098927996e-08, -2.30660839364244e-08},
            {-2.00043052422933e-08, 1.72145861031776e-09, 2.13714615094209e-09, 1.02982676689194e-09, -1.64945224692217e-10},
            {1.23552540016991e-09, 1.42028470911613e-09, 8.79622616627508e-10, -7.44465600265154e-10, -7.17124672589442e-11},
            {-6.67749524914644e-10, -5.77722874934050e-11, 3.40077806879472e-11, 4.26176076541840e-11, 8.23189659748212e-11},
            {-4.62771648935992e-11, -7.24005305716782e-13, 1.18233730497485e-12, 5.18156973532267e-12, -1.53329687155297e-12},
            {4.75581699468619e-12, -3.79782291469732e-12, 1.33077109836853e-12, -1.02426020107120e-12, 3.10385019249130e-13},
            {1.66486090578792e-12, 1.08573672403649e-12, 1.26268044166279e-13, -1.23509297742757e-13, -1.81842007284038e-13},
            {0, 0, 0, 0, 0},
            {9.93870680202303e-08, -1.85264736035628e-06, -5.58942734710854e-07, -5.54183448316270e-07, -3.95581289689398e-08},
            {7.88329069002365e-08, 2.04810091451078e-08, 3.74588851000076e-09, 3.42429296613803e-08, -2.00840228416712e-08},
            {-5.93700447329696e-10, -6.57499436973459e-10, -6.90560448220751e-09, 3.56586371051089e-09, 7.33310245621566e-11},
            {-6.38101662363634e-11, 4.23668020216529e-10, -2.43764895979202e-10, -9.31466610703172e-11, -3.17491457845975e-10},
            {1.50943725382470e-11, -6.11641188685078e-11, -4.37018785685645e-11, -2.32871158949602e-11, 4.19757251950526e-11},
            {-1.18165328825853e-11, -9.91299557532438e-13, 6.40908678055865e-14, 2.41049422936434e-12, -8.20746054454953e-14},
            {6.01892101914838e-12, -8.78487122873450e-13, -1.58887481332294e-12, -3.13556902469604e-13, 5.14523727801645e-14},
            {-1.50791729401891e-13, -1.45234807159695e-13, 1.65302377570887e-13, -5.77094211651483e-15, 9.22218953528393e-14},
            {-1.85618902787381e-14, 5.64333811864051e-14, -9.94311377945570e-15, -2.40992156199999e-15, -2.19196760659665e-14},
            {0, 0, 0, 0, 0},
            {-8.16252352075899e-08, 1.61725487723444e-06, 9.55522506715921e-07, 4.02436267433511e-07, -2.80682052597712e-07},
            {7.68684790328630e-09, -5.00940723761353e-09, -2.43640127974386e-08, -2.59119930503129e-08, 3.35015169182094e-08},
            {7.97903115186673e-09, 3.73803883416618e-09, 3.27888334636662e-09, 1.37481300578804e-09, -1.10677168734482e-10},
            {-1.67853012769912e-09, -1.61405252173139e-10, -1.98841576520056e-10, -1.46591506832192e-11, 9.35710487804660e-11},
            {4.08807084343221e-11, -3.74514169689568e-11, -3.03638493323910e-11, -5.02332555734577e-12, -8.03417498408344e-12},
            {6.48922619024579e-12, 1.96166891023817e-12, -1.96968755122868e-12, -5.20970156382361e-12, -1.62656885103402e-12},
            {1.28603518902875e-12, -4.88146958435109e-13, -3.37034886991840e-13, 1.37393696103000e-14, 4.41398325716943e-14},
            {1.48670014793021e-13, 4.41636026364555e-14, 2.06210477976005e-14, -3.43717583585390e-14, -1.21693704024213e-14},
            {-1.67624180330244e-14, 6.59317111144238e-15, 2.57238525440646e-15, -3.21568425020512e-17, 5.29659568026553e-15},
            {7.85453466393227e-16, 6.91252183915939e-16, -1.20540764178454e-15, -3.85803892583301e-16, 3.46606994632006e-16},
            {0, 0, 0, 0, 0},
            {2.86710087625579e-06, -1.68179842305865e-06, -8.48306772016870e-07, -7.08798062479598e-07, -1.27469453733635e-07},
            {2.11824305734993e-09, 2.02274279084379e-08, 1.61862253091554e-08, 3.25597167111807e-08, 3.40868964045822e-09},
            {1.21757111431438e-08, 1.68405530472906e-09, 1.55379338018638e-09, -3.81467795805531e-10, 2.53316405545058e-09},
            {-9.98413758659768e-11, 5.38382145421318e-10, 3.92629628330704e-10, -1.43067134097778e-10, 3.74959329667113e-12},
            {-1.57270407028909e-11, -9.02797202317592e-12, 8.45997059887690e-12, 4.71474382524218e-12, 5.41880986596427e-12},
            {-1.20658618702054e-12, 7.12940685593433e-13, 1.02148613026937e-12, 1.63063852348169e-13, 1.74048793197708e-13},
            {3.80559390991789e-13, 1.19678271353485e-13, 9.72859455604188e-14, 5.42642400031729e-14, 8.18796710714586e-14},
            {-4.69629218656902e-14, 5.59889038686206e-15, 2.05363292795059e-15, 5.38599403288686e-15, -2.68929559474202e-15},
            {-1.88759348081742e-14, 5.20975954705924e-15, -4.43585653096395e-16, 5.57436617793556e-16, -3.95922805817677e-16},
            {-9.80871456373282e-16, 2.50857658461759e-17, -1.24253000050963e-16, 6.00857065211394e-17, 3.53799635311500e-18},
            {2.49370713054872e-16, -1.49119714269816e-17, -3.12276052640583e-17, -2.42001662334001e-17, -1.69766504318143e-17},
            {0, 0, 0, 0, 0},
            {-1.69222102455713e-06, 1.64277906173064e-06, 5.28855114364096e-07, 4.28159853268650e-07, -1.57362445882665e-07},
            {1.67656782413678e-08, -3.77746114074055e-08, -2.21564555842165e-08, -3.37071806992217e-08, 1.47454008739800e-08},
            {1.06080499491408e-08, 3.21990403709678e-09, 3.87301757435359e-09, 2.92241827834347e-10, -1.86619473655742e-11},
            {1.62399669665839e-10, 3.51322865845172e-10, 2.67086377702958e-11, -1.31596563625491e-10, 3.14164569507034e-11},
            {-2.02180016657259e-11, 2.03305178342732e-11, 6.34969032565839e-12, 5.99522296668787e-12, -4.46275273451008e-12},
            {-9.88409290158885e-13, -1.47692750858224e-13, 3.14655550730530e-13, -2.41857189187879e-13, 4.47727504501486e-13},
            {1.71430777754854e-13, 1.73950835042486e-13, 5.92323956541558e-14, 8.06625710171825e-15, 2.33252485755634e-14},
            {-1.74184545690134e-15, -8.18003353124179e-16, -6.62369006497819e-16, 4.16303374396147e-15, 7.06513748014024e-15},
            {-6.02936238677014e-15, 1.89241084885229e-15, 1.99097881944270e-17, -6.99974290696640e-16, -2.69504942597709e-17},
            {-4.65632962602379e-16, 3.70281995445114e-18, -9.04232973763345e-17, 2.20847370761932e-17, 7.62909453726566e-17},
            {-6.25921477907943e-17, -2.10532795609842e-17, -1.03808073867183e-17, 1.15091380049019e-18, 4.66794445408388e-19},
            {9.39427013576903e-18, 9.17044662931859e-19, 2.04132745117549e-18, -1.72364063154625e-19, -1.18098896532163e-18}
        };

        /** Legendre functions b<sub>nm</sub> for coefficient c<sub>h</sub>.*/
        private static final double[][] CH_B = {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {3.44092035729033e-05, -1.21876825440561e-05, -1.87490665238967e-05, -2.60980336247863e-05, 4.31639313264615e-06},
            {0, 0, 0, 0, 0},
            {-2.60125613000133e-05, 1.70570295762269e-05, 3.08331896996832e-05, 1.66256596588688e-05, -1.07841055501996e-05},
            {8.74011641844073e-06, -2.25874169896607e-06, 6.50985196673747e-07, 1.30424765493752e-06, -1.85081244549542e-07},
            {0, 0, 0, 0, 0},
            {3.77496505484964e-05, -1.08198973553337e-05, -1.67717574544937e-05, -3.22476096673598e-05, 1.12281888201134e-05},
            {-7.68623378647958e-07, -4.01400837153063e-06, -2.16390246700835e-06, -1.76912959937924e-06, -1.12740084951955e-06},
            {-2.37092815818895e-06, -9.52317223759653e-07, -2.22722065579131e-07, -6.25157619772530e-08, 1.86582003894639e-08},
            {0, 0, 0, 0, 0},
            {-6.10254317785872e-05, -2.51815503068494e-05, 2.01046207874667e-05, 7.21107723367308e-06, -1.30692058660457e-05},
            {-9.60655417241537e-06, -7.31381721742373e-06, -2.52767927589636e-06, 9.09039973214621e-07, -6.76454911344246e-07},
            {-2.25743206384908e-08, 2.33058746737575e-07, 2.24746779293445e-07, 6.78551351968876e-08, 1.25076011387284e-07},
            {-2.25744112770133e-07, -1.44429560891636e-07, -2.96810417448652e-08, -5.93858519742856e-08, -2.43210229455420e-08},
            {0, 0, 0, 0, 0},
            {7.45721015256308e-06, -3.81396821676410e-05, -1.41086198468687e-05, -2.28514517574713e-05, 7.28638705683277e-06},
            {-5.77517778169692e-06, -3.93061211403839e-06, -2.17369763310752e-06, -1.48060935583664e-07, -2.74200485662814e-07},
            {4.52962035878238e-07, 9.80990375495214e-07, 4.67492045269286e-07, -8.31032252212116e-09, 1.69426023427740e-07},
            {7.20536791795515e-10, 2.75612253452141e-09, 2.47772119382536e-09, 4.30621825021233e-09, -2.86498479499428e-08},
            {-2.46253956492716e-08, -3.10300833499669e-09, 8.06559148724445e-09, 2.98197408430123e-10, 6.32503656532846e-09},
            {0, 0, 0, 0, 0},
            {-6.01147094179306e-05, -3.16631758509869e-05, 4.10038115100010e-06, 3.55215057231403e-07, -2.23606515237408e-06},
            {-2.85937516921923e-06, -3.67775706610630e-06, -5.06445540401637e-07, 8.21776759711184e-07, -5.98690271725558e-07},
            {7.77122595418965e-07, 3.60896376754085e-07, 3.88610487893381e-07, -4.39533892679537e-08, -6.26882227849174e-08},
            {1.05759993661891e-07, 2.58009912408833e-08, -1.51356049060972e-08, -1.13335813107412e-09, 5.37470857850370e-10},
            {7.99831506181984e-09, 1.67423735327465e-09, 2.94736760548677e-09, -1.56727133704788e-09, 8.46186800849124e-10},
            {3.07727104043851e-09, 3.93584215798484e-10, 3.86721562770643e-11, 1.72181091277391e-10, -2.16915737920145e-10},
            {0, 0, 0, 0, 0},
            {-1.16335389078126e-05, -1.39864676661484e-05, 2.52546278407717e-06, -8.79152625440188e-06, -8.97665132187974e-06},
            {-3.95874550504316e-06, -1.17976262528730e-07, 7.03189926369300e-07, 3.38907065351535e-07, -3.67714052493558e-07},
            {2.29082449370440e-07, 5.72961531093329e-07, 4.21969662578894e-08, 1.24112958141431e-08, 9.56404486571888e-08},
            {1.44631865298671e-09, 6.19368473895584e-09, 1.67110424041236e-09, 2.57979463602951e-09, -6.90806907510366e-09},
            {1.77235802019153e-09, -8.14388846228970e-10, 4.50421956523579e-09, 5.67452314909707e-10, 2.47610443675560e-09},
            {4.85932343880617e-10, 2.24864117422804e-10, -2.22534534468511e-10, -7.96395824973477e-11, 3.12587399902493e-12},
            {-3.20173937255409e-11, -1.29872402028088e-11, -4.24092901203818e-11, 2.66570185704416e-11, -5.25164954403909e-12},
            {0, 0, 0, 0, 0},
            {-1.36010179191872e-05, 1.77873053642413e-05, 4.80988546657119e-06, 3.46859608161212e-06, -1.73247520896541e-06},
            {2.00020483116258e-06, 2.43393064079673e-06, 1.21478843695862e-06, 1.95582820041644e-07, -3.11847995109088e-07},
            {-8.13287218979310e-09, 1.05206830238665e-08, 6.54040136224164e-09, -1.96402660575990e-08, -1.40379796070732e-08},
            {4.01291020310740e-08, 2.92634301047947e-08, 6.04179709273169e-09, 8.61849065020545e-10, 5.98065429697245e-09},
            {-1.06149335032911e-09, -4.39748495862323e-10, 8.83040310269353e-10, 3.49392227277679e-10, 8.57722299002622e-10},
            {-1.25049888909390e-11, 2.05203288281631e-10, 1.37817670505319e-11, 6.82057794430145e-11, -9.41515631694254e-11},
            {7.47196022644130e-12, -2.51369898528782e-11, -2.12196687809200e-11, 1.55282119505201e-11, 9.99224438231805e-12},
            {-7.90534019004874e-13, 3.55824506982589e-12, 8.00835777767281e-13, 8.73460019069655e-13, 1.34176126600106e-12},
            {0, 0, 0, 0, 0},
            {3.12855262465316e-05, 1.31629386003608e-05, 2.65598119437581e-06, 8.68923340949135e-06, -7.51164082949678e-06},
            {1.56870792650533e-06, 1.89227301685370e-06, 4.15620385341985e-07, -2.74253787880603e-07, -4.28826210119200e-07},
            {-9.99176994565587e-08, -1.10785129426286e-07, -1.10318125091182e-07, 6.22726507350764e-09, -3.39214566386250e-08},
            {1.24872975018433e-08, 1.10663206077249e-08, 5.40658975901469e-09, -2.79119137105115e-09, -2.47500096192502e-09},
            {1.11518917154060e-10, -4.21965763244849e-10, 3.26786005211229e-10, 1.93488254914545e-10, 7.00774679999972e-10},
            {1.50889220040757e-10, 1.03130002661366e-10, -3.09481760816903e-11, -4.47656630703759e-11, -7.36245021803800e-12},
            {-1.91144562110285e-12, -1.11355583995978e-11, -1.76207323352556e-11, 8.15289793192265e-12, 3.45078925412654e-12},
            {-2.73248710476019e-12, -1.65089342283056e-13, -2.20125355220819e-13, 5.32589191504356e-13, 5.70008982140874e-13},
            {8.06636928368811e-13, 1.30893069976672e-13, 9.72079137767479e-14, 3.87410156264322e-14, -5.56410013263563e-14},
            {0, 0, 0, 0, 0},
            {2.02454485403216e-05, -9.77720471118669e-06, -4.35467548126223e-06, 2.19599868869063e-06, -3.26670819043690e-06},
            {-3.21839256310540e-08, 8.38760368015005e-07, -5.08058835724060e-07, 4.16177282491396e-08, 1.53842592762120e-07},
            {-1.57377633165313e-07, -7.86803586842404e-08, -7.40444711426898e-08, 3.15259864117954e-08, 5.60536231567172e-09},
            {-3.26080428920229e-10, -3.14576780695439e-09, 8.46796096612981e-10, -2.59329379174262e-09, -8.01054756588382e-10},
            {-4.58725236153576e-11, -6.87847958546571e-11, 8.18226480126754e-12, 1.81082075625897e-10, 1.74510532938256e-10},
            {7.60233505328792e-11, 4.76463939581321e-11, -2.47198455442033e-11, -8.83439688929965e-12, 5.93967446277316e-13},
            {-8.92919292558887e-12, -4.38524572312029e-12, -4.02709146060896e-12, 4.84344426425295e-12, 5.12869042781520e-12},
            {1.91518361809952e-12, 3.06846255371817e-13, -2.44830265306345e-13, 7.86297493099244e-14, 2.72347805801980e-13},
            {9.09936624159538e-14, 7.20650818861447e-15, 2.45383991578283e-14, -4.79580974186462e-15, 3.64604724046944e-14},
            {-4.63611142770709e-14, 1.73908246420636e-15, -4.41651410674801e-15, -6.61409045306922e-16, -1.60016049099639e-15},
            {0, 0, 0, 0, 0},
            {6.17105245892845e-06, -1.04342983738457e-05, -1.72711741097994e-05, -8.16815967888426e-07, 3.42789959967593e-06},
            {-2.44014060833825e-07, 2.06991837444652e-07, -3.85805819475679e-07, 1.67162359832166e-08, 4.15139610402483e-07},
            {8.18199006804020e-08, -3.20013409049159e-08, 5.94000906771151e-08, 2.24122167188946e-08, -1.33796186160409e-08},
            {7.66269294674338e-11, -6.07862178874828e-10, 4.95795757186248e-10, -3.07589245481422e-10, 3.44456287710689e-10},
            {-1.84076250254929e-10, -1.30985312312781e-10, -1.52547325533276e-10, -2.51000125929512e-11, -1.93924012590455e-11},
            {-2.93307452197665e-11, 2.88627386757582e-11, 5.58812021182217e-12, -1.68692874069187e-13, 1.80464313900575e-12},
            {-9.59053874473003e-13, 6.04803122874761e-13, -9.80015608958536e-13, 1.70530372034214e-12, 1.70458664160775e-12},
            {2.80169588226043e-13, 9.09573148053551e-14, 2.16449186617004e-14, 1.15550091496353e-13, 4.97772796761321e-14},
            {-3.04524400761371e-14, 3.42845631349694e-14, 2.44230630602064e-14, 5.76017546103056e-16, -9.74409465961093e-15},
            {5.98765340844291e-15, -2.63942474859535e-15, -1.80204805804437e-15, -1.84981819321183e-16, -5.85073392163660e-16},
            {-2.37069441910133e-15, 2.87429226086856e-16, -1.67055963193389e-16, 2.72110684914090e-18, 8.46646962667892e-17},
            {0, 0, 0, 0, 0},
            {-2.71386164105722e-05, -1.41834938338454e-05, -2.00777928859929e-07, 5.94329804681196e-07, 8.61856994375586e-06},
            {-3.93656495458664e-08, -6.36432821807576e-07, -2.47887475106438e-07, -2.64906446204966e-08, 1.10689794197004e-07},
            {5.25319489188562e-08, 9.00866357158695e-09, 5.00693379572512e-08, 2.47269011056404e-08, -7.27648556194598e-09},
            {1.87207107149043e-09, -1.46428282396138e-09, -2.71812237167257e-10, 8.44902265891466e-10, -5.62683870906027e-10},
            {-1.08295119666184e-10, 4.75553388543793e-11, -5.49429386495686e-11, -6.60907871731611e-11, -5.97347322824822e-11},
            {-4.95118306815571e-12, 5.31083735234970e-13, -1.93679746327378e-12, -1.61770521840510e-12, 1.23276727202510e-11},
            {6.68582682909900e-13, 7.38288575160449e-13, 5.47630483499201e-13, -1.00770258118914e-13, -1.65564928475981e-13},
            {5.80963409268471e-14, 6.93474288078737e-14, 6.60728092794315e-15, -5.21029056725202e-15, -1.11283532854883e-16},
            {-4.10567742688903e-15, 1.62252646805882e-14, 1.00774699865989e-14, -2.44793214897877e-16, -1.59283906414563e-15},
            {1.84669506619904e-17, 8.28473337813919e-17, -1.53400662078899e-16, -5.01060672199689e-17, -2.20727935766132e-16},
            {2.65355116203636e-16, -3.70233146147684e-17, 3.52689394451586e-18, -8.62215942516328e-18, 9.26909361974526e-18},
            {9.94266950643135e-17, 4.17028699663441e-18, -7.65153491125819e-21, -5.62131270981041e-18, -3.03732817297438e-18}
        };

        /** Legendre functions b<sub>nm</sub> for coefficient c<sub>w</sub>.*/
        private static final double[][] CW_B = {
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {-0.000209104872912563, -1.41530274973540e-05, 3.00318745764815e-05, -1.82864291318284e-05, -7.62965409959238e-06},
            {0, 0, 0, 0, 0},
            {-0.000186336519900275, 0.000191256553935638, 7.28356195304996e-05, 3.59637869639906e-05, -2.53927226167388e-05},
            {0.000108195343799485, -6.97050977217619e-05, -6.68037133871099e-05, 2.30387653190503e-05, -1.22735483925784e-05},
            {0, 0, 0, 0, 0},
            {0.000119941091277039, -7.70547844186875e-05, -8.15376297964528e-05, 1.06005789545203e-05, 2.31177232268720e-05},
            {-1.77494760217164e-05, -1.37061385686605e-05, -1.74805936475816e-05, -6.91745900867532e-07, -7.10231790947787e-06},
            {-1.47564103733219e-05, 2.08890785485260e-06, 3.19876879447867e-06, 9.43984664503715e-07, -4.90480527577521e-06},
            {0, 0, 0, 0, 0},
            {4.93300138389457e-05, -6.77641298460617e-05, -3.25043347246397e-05, 8.33226714911921e-06, 8.11499972792905e-06},
            {-2.80449863471272e-05, -1.04367606414606e-05, 1.64473584641163e-07, -3.57420965807816e-06, 2.95887156564038e-06},
            {1.88835280111533e-06, 5.69125761193702e-07, -2.22757382799409e-06, -1.96699131032252e-07, -2.91861219283659e-07},
            {-4.69918971436680e-06, -7.00778948636735e-07, 2.97544157334673e-09, 3.86100512544410e-07, 2.30939653701027e-07},
            {0, 0, 0, 0, 0},
            {1.77050610394149e-05, -3.18353071311574e-05, 3.04232260950316e-05, -6.26821316488169e-05, -1.75094810002378e-06},
            {9.25605901565775e-06, -8.25179123302247e-06, 6.74032752408358e-06, 3.22192289084524e-06, 6.09414500075259e-06},
            {4.28233825242200e-06, 2.10470570087927e-07, -4.75050074985668e-07, -4.89382663470592e-07, 8.75232347469207e-07},
            {8.50393520366934e-07, 1.58764911467186e-07, -2.16267638321210e-07, -7.43341300487416e-10, 1.75131729813230e-07},
            {-2.87064111623119e-07, 4.50393893102830e-08, 6.63315044416690e-08, 7.61199387418853e-08, -6.05694385243652e-09},
            {0, 0, 0, 0, 0},
            {-1.95692079507947e-05, 5.15486098887851e-05, 3.00852761598173e-05, 1.21485028343416e-05, -6.72450521493428e-06},
            {5.34496867088158e-06, 3.90973451680699e-06, 3.70148924718425e-06, 5.73731499938212e-08, 5.52258220288780e-07},
            {3.39950838185315e-07, -5.63443976772634e-07, 4.52082211980595e-07, -2.57094645806243e-07, -6.84885762924729e-08},
            {2.15793276880684e-07, 2.05911354090873e-07, 1.33747872341142e-08, -2.07997626478952e-08, -3.69812938736019e-08},
            {2.11952749403224e-09, 4.04317822544732e-08, 2.40972024883650e-09, 8.56289126938059e-09, 2.31035283490200e-08},
            {-2.08402298813248e-09, -8.50243600879112e-09, 2.60895410117768e-09, -6.69156841738591e-10, -5.16280278087006e-09},
            {0, 0, 0, 0, 0},
            {0.000124901291436683, -5.70770326719086e-05, -8.44887248105015e-05, -3.11442665354698e-05, -1.12982893252046e-05},
            {-8.38934444233944e-06, 1.56860091415414e-06, -1.77704563531825e-06, -5.70219068898717e-08, -4.30377735031244e-06},
            {3.72965318017681e-07, 6.98175439446187e-07, 1.75760544807919e-08, 1.59731284857151e-07, 3.62363848767891e-07},
            {-2.32148850787091e-07, -4.21888751852973e-08, 8.35926113952108e-08, -2.24572480575674e-08, -6.92114100904503e-08},
            {-2.92635642210745e-09, 3.38086229163415e-09, 4.72186694662901e-09, -8.32354437305758e-11, 4.19673890995627e-09},
            {-1.26452887692900e-09, 1.91309690886864e-09, 1.54755631983655e-09, -1.09865169400249e-09, 1.83645326319994e-10},
            {9.92539437011905e-10, -2.96318203488300e-10, 1.17466020823486e-10, -5.00185957995526e-10, -8.54777591408537e-11},
            {0, 0, 0, 0, 0},
            {-0.000182885335404854, 7.27424724520089e-05, 3.05286278023427e-05, 2.55324463432562e-05, -6.39859510763234e-06},
            {-5.21449265232557e-06, -6.70572386081398e-06, -3.95473351292738e-06, -6.41023334372861e-07, -3.11616331059009e-06},
            {2.37090789071727e-07, 3.58427517014705e-07, 2.55709192777007e-07, 8.44593804408541e-08, 9.27243162355359e-09},
            {7.24370898432057e-08, -7.43945120337710e-09, 8.61751911975683e-10, -2.34651212610623e-08, 2.94052921681456e-09},
            {-1.22127317934425e-08, -3.89758984276768e-09, 4.12890383904924e-11, 2.06528068002723e-09, 1.73488696972270e-09},
            {-5.44137406907620e-10, -4.81034553189921e-10, -2.56101759039694e-11, 3.21880564410154e-10, -2.70195343165250e-11},
            {1.08394225300546e-10, -7.99525492688661e-11, 1.73850287030654e-10, -8.06390014426271e-11, -7.63143364291160e-13},
            {-3.41446959267441e-11, 2.72675729042792e-11, 5.69674704865345e-12, -3.38402998344892e-12, -2.96732381931007e-12},
            {0, 0, 0, 0, 0},
            {2.91161315987250e-05, -7.24641166590735e-05, -8.58323519857884e-06, -1.14037444255820e-05, 1.32244819451517e-05},
            {1.24266748259826e-06, -4.13127038469802e-06, -8.47496394492885e-07, 5.48722958754267e-07, -1.98288551821205e-06},
            {-1.70671245196917e-08, 1.36891127083540e-08, -2.80901972249870e-07, -5.45369793946222e-09, -9.58796303763498e-08},
            {1.14115335901746e-08, 2.79308166429178e-08, -1.71144803132413e-08, 4.86116243565380e-09, -8.13061459952280e-09},
            {-1.19144311035824e-09, -1.28197815211763e-09, -1.22313592972373e-09, 6.23116336753674e-10, 2.11527825898689e-09},
            {4.94618645030426e-10, -1.01554483531252e-10, -3.58808808952276e-10, 1.23499783028794e-10, -1.21017599361833e-10},
            {1.33959569836451e-10, -1.87140898812283e-11, -3.04265350158941e-11, -1.42907553051431e-11, -1.09873858099638e-11},
            {1.30277419203512e-11, -4.95312627777245e-12, 2.23070215544358e-12, 1.66450226016423e-12, 6.26222944728474e-12},
            {-4.40721204874728e-12, 2.99575133064885e-12, -1.54917262009097e-12, 8.90015664527060e-14, -1.59135267012937e-12},
            {0, 0, 0, 0, 0},
            {-4.17667211323160e-05, 1.39005215116294e-05, 1.46521361817829e-05, 3.23485458024416e-05, -8.57936261085263e-06},
            {9.48491026524450e-07, 1.67749735481991e-06, 6.80159475477603e-07, -1.34558044496631e-06, 1.62108231492249e-06},
            {-2.67545753355631e-07, -3.31848493018159e-08, 1.05837219557465e-07, 1.55587655479400e-07, -2.84996014386667e-08},
            {-5.15113778734878e-08, 8.83630725241303e-09, 3.36579455982772e-09, -6.22350102096402e-09, 5.03959133095369e-09},
            {2.04635880823035e-11, -1.07923589059151e-09, -6.96482137669712e-10, -4.70238500452793e-10, -6.60277903598297e-10},
            {-2.41897168749189e-11, 1.33547763615216e-10, -5.13534673658908e-11, -8.32767177662817e-11, 5.72614717082428e-11},
            {7.55170562359940e-12, -1.57123461699055e-11, -1.48874069619124e-11, -7.10529462981252e-13, -7.99006335025107e-12},
            {2.41883156738960e-12, 2.97346980183361e-12, 1.28719977731450e-12, -2.49240876894143e-12, 6.71155595793198e-13},
            {4.16995565336914e-13, -1.71584521275288e-13, -7.23064067359978e-14, 2.45405880599037e-13, 4.43532934905830e-13},
            {3.56937508828997e-14, 2.43012511260300e-14, -7.96090778289326e-14, -1.59548529636358e-14, 8.99103763000507e-15},
            {0, 0, 0, 0, 0},
            {0.000117579258399489, -4.52648448635772e-05, -2.69130037097862e-05, -3.82266335794366e-05, -4.36549257701084e-06},
            {-1.43270371215502e-06, 1.21565440183855e-06, 8.53701136074284e-07, 1.52709810023665e-06, 1.22382663462904e-06},
            {3.06089147519664e-07, 9.79084123751975e-08, 7.96524661441178e-08, 4.54770947973458e-08, 2.22842369458882e-07},
            {-9.94254707745127e-09, 1.43251376378012e-08, 1.93911753685160e-08, -6.52214645690987e-09, -1.97114016452408e-09},
            {-9.20751919828404e-10, -9.44312829629076e-10, 7.24196738163952e-11, -6.71801072324561e-11, 2.33146774065873e-10},
            {-1.43544298956410e-11, 1.78464235318769e-10, 7.69950023012326e-11, -4.22390057304453e-12, 3.05176324574816e-11},
            {-7.88053753973990e-12, -3.20207793051003e-12, 1.01527407317625e-12, 6.02788185858449e-12, 1.14919530900453e-11},
            {-1.21558899266069e-12, 5.31300597882986e-13, 3.44023865079264e-13, -6.22598216726224e-14, -5.47031650765402e-14},
            {-4.15627948750943e-13, 2.77620907292721e-13, -8.99784134364011e-14, 1.07254247320864e-13, 6.85990080564196e-14},
            {-3.91837863922901e-14, 9.74714976816180e-15, 6.79982450963903e-15, -2.41420876658572e-15, -2.20889384455344e-15},
            {9.25912068402776e-15, -4.02621719248224e-15, -2.43952036351187e-15, -1.97006876049866e-15, 1.03065621527869e-16},
            {0, 0, 0, 0, 0},
            {-0.000103762036940193, 4.38145356960292e-05, 2.43406920349913e-05, 7.89103527673736e-06, -1.66841465339160e-05},
            {-1.18428449371744e-06, -1.30188721737259e-06, -1.88013557116650e-06, -1.01342046295303e-06, 9.21813037802502e-07},
            {1.51836068712460e-07, 1.11362553803933e-07, 1.55375052233052e-07, 1.94450910788747e-09, -1.73093755828342e-08},
            {-3.77758211813121e-09, 1.23323969583610e-08, 1.72510045250302e-09, -1.88609789458597e-09, 1.28937597985937e-09},
            {-1.07947760393523e-09, 5.26051570105365e-10, -3.67657536332496e-11, 3.16110123523840e-10, -3.24273198242170e-10},
            {-2.00385649209820e-12, 2.54703869682390e-11, 4.08563622440851e-12, -4.83350348928636e-11, -3.98153443845079e-13},
            {2.73094467727215e-12, 5.08900664114903e-12, -7.66669089075134e-13, 2.50015592643012e-12, 4.29763262853853e-12},
            {6.53946487537890e-13, -2.24958413781008e-13, 6.74638861781238e-15, 3.28537647613903e-14, 2.54199700290116e-13},
            {-1.09122051193505e-13, 8.36362392931501e-14, -3.90750153912300e-14, -5.44915910741950e-14, 2.43816947219217e-14},
            {-1.41882561550134e-14, 1.00455397812713e-14, 2.63347255121581e-15, 1.53043256823601e-15, 2.49081021428095e-15},
            {-1.17256193152654e-15, 1.05648985031971e-16, 1.31778372453016e-16, 1.44815198666577e-16, -3.72532768618480e-16},
            {2.66203457773766e-16, -7.67224608659658e-17, 3.51487351031864e-18, 4.10287131339291e-17, -6.72171711728514e-17}
        };

        /** Build a new instance. */
        LegendreFunctions() {

        }

        /** Get the value of the a<sub>nm</sub> coefficient for b<sub>h</sub>.
         * @param n index
         * @param m index
         * @return the a<sub>nm</sub> coefficient for b<sub>h</sub>
         */
        public double getAnmBh(final int n, final int m) {
            return BH_A[n][m];
        }

        /** Get the value of the a<sub>nm</sub> coefficient for b<sub>w</sub>.
         * @param n index
         * @param m index
         * @return the a<sub>nm</sub> coefficient for b<sub>w</sub>
         */
        public double getAnmBw(final int n, final int m) {
            return BW_A[n][m];
        }

        /** Get the value of the a<sub>nm</sub> coefficient for c<sub>h</sub>.
         * @param n index
         * @param m index
         * @return the a<sub>nm</sub> coefficient for c<sub>h</sub>
         */
        public double getAnmCh(final int n, final int m) {
            return CH_A[n][m];
        }

        /** Get the value of the a<sub>nm</sub> coefficient for c<sub>w</sub>.
         * @param n index
         * @param m index
         * @return the a<sub>nm</sub> coefficient for c<sub>w</sub>
         */
        public double getAnmCw(final int n, final int m) {
            return CW_A[n][m];
        }

        /** Get the value of the b<sub>nm</sub> coefficient for b<sub>h</sub>.
         * @param n index
         * @param m index
         * @return the b<sub>nm</sub> coefficient for b<sub>h</sub>
         */
        public double getBnmBh(final int n, final int m) {
            return BH_B[n][m];
        }

        /** Get the value of the b<sub>nm</sub> coefficient for b<sub>w</sub>.
         * @param n index
         * @param m index
         * @return the b<sub>nm</sub> coefficient for b<sub>w</sub>
         */
        public double getBnmBw(final int n, final int m) {
            return BW_B[n][m];
        }

        /** Get the value of the b<sub>nm</sub> coefficient for c<sub>h</sub>.
         * @param n index
         * @param m index
         * @return the b<sub>nm</sub> coefficient for c<sub>h</sub>
         */
        public double getBnmCh(final int n, final int m) {
            return CH_B[n][m];
        }

        /** Get the value of the b<sub>nm</sub> coefficient for c<sub>w</sub>.
         * @param n index
         * @param m index
         * @return the b<sub>nm</sub> coefficient for c<sub>w</sub>
         */
        public double getBnmCw(final int n, final int m) {
            return CW_B[n][m];
        }

    }
}
