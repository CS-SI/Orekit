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
package org.orekit.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.BodiesElements;
import org.orekit.data.DataContext;
import org.orekit.data.DelaunayArguments;
import org.orekit.data.FieldBodiesElements;
import org.orekit.data.FieldDelaunayArguments;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.PoissonSeries;
import org.orekit.data.PoissonSeries.CompiledSeries;
import org.orekit.data.PoissonSeriesParser;
import org.orekit.data.PolynomialNutation;
import org.orekit.data.PolynomialParser;
import org.orekit.data.PolynomialParser.Unit;
import org.orekit.data.SimpleTimeStampedTableParser;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.FieldPoleCorrection;
import org.orekit.frames.PoleCorrection;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeStamped;
import org.orekit.time.TimeVectorFunction;


/** Supported IERS conventions.
 * @since 6.0
 * @author Luc Maisonobe
 */
public enum IERSConventions {

    /** Constant for IERS 1996 conventions. */
    IERS_1996 {

        /** Nutation arguments resources. */
        private static final String NUTATION_ARGUMENTS = IERS_BASE + "1996/nutation-arguments.txt";

        /** X series resources. */
        private static final String X_Y_SERIES         = IERS_BASE + "1996/tab5.4.txt";

        /** Psi series resources. */
        private static final String PSI_EPSILON_SERIES = IERS_BASE + "1996/tab5.1.txt";

        /** Tidal correction for xp, yp series resources. */
        private static final String TIDAL_CORRECTION_XP_YP_SERIES = IERS_BASE + "1996/tab8.4.txt";

        /** Tidal correction for UT1 resources. */
        private static final String TIDAL_CORRECTION_UT1_SERIES = IERS_BASE + "1996/tab8.3.txt";

        /** Love numbers resources. */
        private static final String LOVE_NUMBERS = IERS_BASE + "1996/tab6.1.txt";

        /** Frequency dependence model for k₂₀. */
        private static final String K20_FREQUENCY_DEPENDENCE = IERS_BASE + "1996/tab6.2b.txt";

        /** Frequency dependence model for k₂₁. */
        private static final String K21_FREQUENCY_DEPENDENCE = IERS_BASE + "1996/tab6.2a.txt";

        /** Frequency dependence model for k₂₂. */
        private static final String K22_FREQUENCY_DEPENDENCE = IERS_BASE + "1996/tab6.2c.txt";

        /** Tidal displacement frequency correction for diurnal tides. */
        private static final String TIDAL_DISPLACEMENT_CORRECTION_DIURNAL = IERS_BASE + "1996/tab7.3a.txt";

        /** Tidal displacement frequency correction for zonal tides. */
        private static final String TIDAL_DISPLACEMENT_CORRECTION_ZONAL = IERS_BASE + "1996/tab7.3b.txt";

        /** {@inheritDoc} */
        @Override
        public FundamentalNutationArguments getNutationArguments(final TimeScale timeScale,
                                                                 final TimeScales timeScales) {
            return load(NUTATION_ARGUMENTS, in -> new FundamentalNutationArguments(this, timeScale,
                                                                                            in, NUTATION_ARGUMENTS,
                                                                                            timeScales));
        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getMeanObliquityFunction(final TimeScales timeScales) {

            // value from chapter 5, page 22
            final PolynomialNutation epsilonA =
                    new PolynomialNutation(84381.448    * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.8150   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.00059  * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.001813 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {
                    return epsilonA.value(evaluateTC(date, timeScales));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return epsilonA.value(evaluateTC(date, timeScales));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getXYSpXY2Function(final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // X = 2004.3109″t - 0.42665″t² - 0.198656″t³ + 0.0000140″t⁴
            //     + 0.00006″t² cos Ω + sin ε0 { Σ [(Ai + Ai' t) sin(ARGUMENT) + Ai'' t cos(ARGUMENT)]}
            //     + 0.00204″t² sin Ω + 0.00016″t² sin 2(F - D + Ω),
            final PolynomialNutation xPolynomial =
                    new PolynomialNutation(0,
                                           2004.3109 * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.42665  * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.198656 * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0000140 * Constants.ARC_SECONDS_TO_RADIANS);

            final double fXCosOm    = 0.00006 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fXSinOm    = 0.00204 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fXSin2FDOm = 0.00016 * Constants.ARC_SECONDS_TO_RADIANS;
            final double sinEps0   = FastMath.sin(getMeanObliquityFunction(timeScales)
                    .value(getNutationReferenceEpoch(timeScales)));

            final double deciMilliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4;
            final PoissonSeriesParser baseParser =
                    new PoissonSeriesParser(12).withFirstDelaunay(1);

            final PoissonSeriesParser xParser =
                    baseParser.
                    withSinCos(0, 7, deciMilliAS, -1, deciMilliAS).
                    withSinCos(1, 8, deciMilliAS,  9, deciMilliAS);
            final PoissonSeries xSum = load(X_Y_SERIES, in -> xParser.parse(in, X_Y_SERIES));

            // Y = -0.00013″ - 22.40992″t² + 0.001836″t³ + 0.0011130″t⁴
            //     + Σ [(Bi + Bi' t) cos(ARGUMENT) + Bi'' t sin(ARGUMENT)]
            //    - 0.00231″t² cos Ω − 0.00014″t² cos 2(F - D + Ω)
            final PolynomialNutation yPolynomial =
                    new PolynomialNutation(-0.00013  * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0,
                                           -22.40992 * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.001836  * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0011130 * Constants.ARC_SECONDS_TO_RADIANS);

            final double fYCosOm    = -0.00231 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fYCos2FDOm = -0.00014 * Constants.ARC_SECONDS_TO_RADIANS;

            final PoissonSeriesParser yParser =
                    baseParser.
                    withSinCos(0, -1, deciMilliAS, 10, deciMilliAS).
                    withSinCos(1, 12, deciMilliAS, 11, deciMilliAS);
            final PoissonSeries ySum = load(X_Y_SERIES, in -> yParser.parse(in, X_Y_SERIES));

            final PoissonSeries.CompiledSeries xySum =
                    PoissonSeries.compile(xSum, ySum);

            // s = -XY/2 + 0.00385″t - 0.07259″t³ - 0.00264″ sin Ω - 0.00006″ sin 2Ω
            //     + 0.00074″t² sin Ω + 0.00006″t² sin 2(F - D + Ω)
            final double fST          =  0.00385 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST3         = -0.07259 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSSinOm      = -0.00264 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSSin2Om     = -0.00006 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST2SinOm    =  0.00074 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST2Sin2FDOm =  0.00006 * Constants.ARC_SECONDS_TO_RADIANS;

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {

                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] xy             = xySum.value(elements);

                    final double omega     = elements.getOmega();
                    final double f         = elements.getF();
                    final double d         = elements.getD();
                    final double t         = elements.getTC();

                    final SinCos scOmega   = FastMath.sinCos(omega);
                    final SinCos sc2omega  = SinCos.sum(scOmega, scOmega);
                    final SinCos sc2FD0m   = FastMath.sinCos(2 * (f - d + omega));
                    final double cosOmega  = scOmega.cos();
                    final double sinOmega  = scOmega.sin();
                    final double sin2Omega = sc2omega.sin();
                    final double cos2FDOm  = sc2FD0m.cos();
                    final double sin2FDOm  = sc2FD0m.sin();

                    final double x = xPolynomial.value(t) + sinEps0 * xy[0] +
                            t * t * (fXCosOm * cosOmega + fXSinOm * sinOmega + fXSin2FDOm * cos2FDOm);
                    final double y = yPolynomial.value(t) + xy[1] +
                            t * t * (fYCosOm * cosOmega + fYCos2FDOm * cos2FDOm);
                    final double sPxy2 = fSSinOm * sinOmega + fSSin2Om * sin2Omega +
                            t * (fST + t * (fST2SinOm * sinOmega + fST2Sin2FDOm * sin2FDOm + t * fST3));

                    return new double[] {
                        x, y, sPxy2
                    };

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {

                    final FieldBodiesElements<T> elements = arguments.evaluateAll(date);
                    final T[] xy             = xySum.value(elements);

                    final T omega     = elements.getOmega();
                    final T f         = elements.getF();
                    final T d         = elements.getD();
                    final T t         = elements.getTC();
                    final T t2        = t.multiply(t);

                    final FieldSinCos<T> scOmega  = FastMath.sinCos(omega);
                    final FieldSinCos<T> sc2omega = FieldSinCos.sum(scOmega, scOmega);
                    final FieldSinCos<T> sc2FD0m  = FastMath.sinCos(f.subtract(d).add(omega).multiply(2));
                    final T cosOmega  = scOmega.cos();
                    final T sinOmega  = scOmega.sin();
                    final T sin2Omega = sc2omega.sin();
                    final T cos2FDOm  = sc2FD0m.cos();
                    final T sin2FDOm  = sc2FD0m.sin();

                    final T x = xPolynomial.value(t).
                                add(xy[0].multiply(sinEps0)).
                                add(t2.multiply(cosOmega.multiply(fXCosOm).add(sinOmega.multiply(fXSinOm)).add(cos2FDOm.multiply(fXSin2FDOm))));
                    final T y = yPolynomial.value(t).
                                add(xy[1]).
                                add(t2.multiply(cosOmega.multiply(fYCosOm).add(cos2FDOm.multiply(fYCos2FDOm))));
                    final T sPxy2 = sinOmega.multiply(fSSinOm).
                                    add(sin2Omega.multiply(fSSin2Om)).
                                    add(t.multiply(fST3).add(sinOmega.multiply(fST2SinOm)).add(sin2FDOm.multiply(fST2Sin2FDOm)).multiply(t).add(fST).multiply(t));

                    final T[] a = MathArrays.buildArray(date.getField(), 3);
                    a[0] = x;
                    a[1] = y;
                    a[2] = sPxy2;
                    return a;

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getPrecessionFunction(final TimeScales timeScales) {

            // set up the conventional polynomials
            // the following values are from Lieske et al. paper:
            // Expressions for the precession quantities based upon the IAU(1976) system of astronomical constants
            // http://articles.adsabs.harvard.edu/full/1977A%26A....58....1L
            // also available as equation 30 in IERS 2003 conventions
            final PolynomialNutation psiA =
                    new PolynomialNutation(   0.0,
                                           5038.7784   * Constants.ARC_SECONDS_TO_RADIANS,
                                             -1.07259  * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.001147 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation omegaA =
                    new PolynomialNutation(getMeanObliquityFunction(timeScales)
                            .value(getNutationReferenceEpoch(timeScales)),
                                            0.0,
                                            0.05127   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.007726  * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation chiA =
                    new PolynomialNutation( 0.0,
                                           10.5526   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -2.38064  * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.001125 * Constants.ARC_SECONDS_TO_RADIANS);

            return new PrecessionFunction(psiA, omegaA, chiA, timeScales);

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getNutationFunction(final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // set up Poisson series
            final double deciMilliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4;
            final PoissonSeriesParser baseParser =
                    new PoissonSeriesParser(10).withFirstDelaunay(1);

            final PoissonSeriesParser psiParser =
                    baseParser.
                    withSinCos(0, 7, deciMilliAS, -1, deciMilliAS).
                    withSinCos(1, 8, deciMilliAS, -1, deciMilliAS);
            final PoissonSeries psiSeries = load(PSI_EPSILON_SERIES, in -> psiParser.parse(in, PSI_EPSILON_SERIES));

            final PoissonSeriesParser epsilonParser =
                    baseParser.
                    withSinCos(0, -1, deciMilliAS, 9, deciMilliAS).
                    withSinCos(1, -1, deciMilliAS, 10, deciMilliAS);
            final PoissonSeries epsilonSeries = load(PSI_EPSILON_SERIES, in -> epsilonParser.parse(in, PSI_EPSILON_SERIES));

            final PoissonSeries.CompiledSeries psiEpsilonSeries =
                    PoissonSeries.compile(psiSeries, epsilonSeries);

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] psiEpsilon = psiEpsilonSeries.value(elements);
                    return new double[] {
                        psiEpsilon[0], psiEpsilon[1], IAU1994ResolutionC7.value(elements, timeScales.getTAI())
                    };
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    final FieldBodiesElements<T> elements = arguments.evaluateAll(date);
                    final T[] psiEpsilon = psiEpsilonSeries.value(elements);
                    final T[] result = MathArrays.buildArray(date.getField(), 3);
                    result[0] = psiEpsilon[0];
                    result[1] = psiEpsilon[1];
                    result[2] = IAU1994ResolutionC7.value(elements, timeScales.getTAI());
                    return result;
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGMSTFunction(final TimeScale ut1,
                                                  final TimeScales timeScales) {

            // Radians per second of time
            final double radiansPerSecond = MathUtils.TWO_PI / Constants.JULIAN_DAY;

            // constants from IERS 1996 page 21
            // the underlying model is IAU 1982 GMST-UT1
            final AbsoluteDate gmstReference = new AbsoluteDate(
                    DateComponents.J2000_EPOCH, TimeComponents.H12, timeScales.getTAI());
            final double gmst0 = 24110.54841;
            final double gmst1 = 8640184.812866;
            final double gmst2 = 0.093104;
            final double gmst3 = -6.2e-6;

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {

                    // offset in Julian centuries from J2000 epoch (UT1 scale)
                    final double dtai = date.durationFrom(gmstReference);
                    final double tut1 = dtai + ut1.offsetFromTAI(date);
                    final double tt   = tut1 / Constants.JULIAN_CENTURY;

                    // Seconds in the day, adjusted by 12 hours because the
                    // UT1 is supplied as a Julian date beginning at noon.
                    final double sd = FastMath.IEEEremainder(tut1 + Constants.JULIAN_DAY / 2, Constants.JULIAN_DAY);

                    // compute Greenwich mean sidereal time, in radians
                    return ((((((tt * gmst3 + gmst2) * tt) + gmst1) * tt) + gmst0) + sd) * radiansPerSecond;

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {

                    // offset in Julian centuries from J2000 epoch (UT1 scale)
                    final T dtai = date.durationFrom(gmstReference);
                    final T tut1 = dtai.add(ut1.offsetFromTAI(date.toAbsoluteDate()));
                    final T tt   = tut1.divide(Constants.JULIAN_CENTURY);

                    // Seconds in the day, adjusted by 12 hours because the
                    // UT1 is supplied as a Julian date beginning at noon.
                    final T sd = tut1.add(Constants.JULIAN_DAY / 2).remainder(Constants.JULIAN_DAY);

                    // compute Greenwich mean sidereal time, in radians
                    return tt.multiply(gmst3).add(gmst2).multiply(tt).add(gmst1).multiply(tt).add(gmst0).add(sd).multiply(radiansPerSecond);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGMSTRateFunction(final TimeScale ut1,
                                                      final TimeScales timeScales) {

            // Radians per second of time
            final double radiansPerSecond = MathUtils.TWO_PI / Constants.JULIAN_DAY;

            // constants from IERS 1996 page 21
            // the underlying model is IAU 1982 GMST-UT1
            final AbsoluteDate gmstReference = new AbsoluteDate(
                    DateComponents.J2000_EPOCH, TimeComponents.H12, timeScales.getTAI());
            final double gmst1 = 8640184.812866;
            final double gmst2 = 0.093104;
            final double gmst3 = -6.2e-6;

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {

                    // offset in Julian centuries from J2000 epoch (UT1 scale)
                    final double dtai = date.durationFrom(gmstReference);
                    final double tut1 = dtai + ut1.offsetFromTAI(date);
                    final double tt   = tut1 / Constants.JULIAN_CENTURY;

                    // compute Greenwich mean sidereal time rate, in radians per second
                    return ((((tt * 3 * gmst3 + 2 * gmst2) * tt) + gmst1) / Constants.JULIAN_CENTURY + 1) * radiansPerSecond;

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {

                    // offset in Julian centuries from J2000 epoch (UT1 scale)
                    final T dtai = date.durationFrom(gmstReference);
                    final T tut1 = dtai.add(ut1.offsetFromTAI(date.toAbsoluteDate()));
                    final T tt   = tut1.divide(Constants.JULIAN_CENTURY);

                    // compute Greenwich mean sidereal time, in radians
                    return tt.multiply(3 * gmst3).add(2 * gmst2).multiply(tt).add(gmst1).divide(Constants.JULIAN_CENTURY).add(1).multiply(radiansPerSecond);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGASTFunction(final TimeScale ut1,
                                                  final EOPHistory eopHistory,
                                                  final TimeScales timeScales) {

            // obliquity
            final TimeScalarFunction epsilonA = getMeanObliquityFunction(timeScales);

            // GMST function
            final TimeScalarFunction gmst = getGMSTFunction(ut1, timeScales);

            // nutation function
            final TimeVectorFunction nutation = getNutationFunction(timeScales);

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {

                    // compute equation of equinoxes
                    final double[] angles = nutation.value(date);
                    double deltaPsi = angles[0];
                    if (eopHistory != null) {
                        deltaPsi += eopHistory.getEquinoxNutationCorrection(date)[0];
                    }
                    final double eqe = deltaPsi  * FastMath.cos(epsilonA.value(date)) + angles[2];

                    // add mean sidereal time and equation of equinoxes
                    return gmst.value(date) + eqe;

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {

                    // compute equation of equinoxes
                    final T[] angles = nutation.value(date);
                    T deltaPsi = angles[0];
                    if (eopHistory != null) {
                        deltaPsi = deltaPsi.add(eopHistory.getEquinoxNutationCorrection(date)[0]);
                    }
                    final T eqe = deltaPsi.multiply(epsilonA.value(date).cos()).add(angles[2]);

                    // add mean sidereal time and equation of equinoxes
                    return gmst.value(date).add(eqe);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getEOPTidalCorrection(final TimeScales timeScales) {

            // set up nutation arguments
            // BEWARE! Using TT as the time scale here and not UT1 is intentional!
            // as this correction is used to compute UT1 itself, it is not surprising we cannot use UT1 yet,
            // however, using the close UTC as would seem logical make the comparison with interp.f from IERS fail
            // looking in the interp.f code, the same TT scale is used for both Delaunay and gamma argument
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales.getTT(), timeScales);

            // set up Poisson series
            final double milliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3;
            final PoissonSeriesParser xyParser = new PoissonSeriesParser(17).
                    withOptionalColumn(1).
                    withGamma(7).
                    withFirstDelaunay(2);
            final PoissonSeries xSeries =
                            load(TIDAL_CORRECTION_XP_YP_SERIES, in -> xyParser.
                                                                      withSinCos(0, 14, milliAS, 15, milliAS).
                                                                      parse(in, TIDAL_CORRECTION_XP_YP_SERIES));
            final PoissonSeries ySeries =
                            load(TIDAL_CORRECTION_XP_YP_SERIES, in -> xyParser.
                                                                      withSinCos(0, 16, milliAS, 17, milliAS).
                                                                      parse(in, TIDAL_CORRECTION_XP_YP_SERIES));

            final double deciMilliS = 1.0e-4;
            final PoissonSeriesParser ut1Parser = new PoissonSeriesParser(17).
                    withOptionalColumn(1).
                    withGamma(7).
                    withFirstDelaunay(2);
            final PoissonSeries ut1Series =
                            load(TIDAL_CORRECTION_UT1_SERIES, in -> ut1Parser.
                                                                    withSinCos(0, 16, deciMilliS, 17, deciMilliS).
                                                                    parse(in, TIDAL_CORRECTION_UT1_SERIES));

            return new EOPTidalCorrection(arguments, xSeries, ySeries, ut1Series);

        }

        /** {@inheritDoc} */
        public LoveNumbers getLoveNumbers() {
            return loadLoveNumbers(LOVE_NUMBERS);
        }

        /** {@inheritDoc} */
        public TimeVectorFunction getTideFrequencyDependenceFunction(final TimeScale ut1,
                                                                     final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(ut1, timeScales);

            // set up Poisson series
            final PoissonSeriesParser k20Parser =
                    new PoissonSeriesParser(18).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);
            final PoissonSeriesParser k21Parser =
                    new PoissonSeriesParser(18).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10);
            final PoissonSeriesParser k22Parser =
                    new PoissonSeriesParser(16).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);

            final double pico = 1.0e-12;
            final PoissonSeries c20Series =
                            load(K20_FREQUENCY_DEPENDENCE, in -> k20Parser.
                                                                 withSinCos(0, 18, -pico, 16, pico).
                                                                 parse(in, K20_FREQUENCY_DEPENDENCE));
            final PoissonSeries c21Series =
                            load(K21_FREQUENCY_DEPENDENCE, in -> k21Parser.
                                                                 withSinCos(0, 17, pico, 18, pico).
                                                                 parse(in, K21_FREQUENCY_DEPENDENCE));
            final PoissonSeries s21Series =
                            load(K21_FREQUENCY_DEPENDENCE, in -> k21Parser.
                                                                 withSinCos(0, 18, -pico, 17, pico).
                                                                 parse(in, K21_FREQUENCY_DEPENDENCE));
            final PoissonSeries c22Series =
                            load(K22_FREQUENCY_DEPENDENCE, in -> k22Parser.
                                                                 withSinCos(0, -1, pico, 16, pico).
                                                                 parse(in, K22_FREQUENCY_DEPENDENCE));
            final PoissonSeries s22Series =
                            load(K22_FREQUENCY_DEPENDENCE, in -> k22Parser.
                                                                 withSinCos(0, 16, -pico, -1, pico).
                                                                 parse(in, K22_FREQUENCY_DEPENDENCE));

            return new TideFrequencyDependenceFunction(arguments,
                                                       c20Series,
                                                       c21Series, s21Series,
                                                       c22Series, s22Series);

        }

        /** {@inheritDoc} */
        @Override
        public double getPermanentTide() {
            return 4.4228e-8 * -0.31460 * getLoveNumbers().getReal(2, 0);
        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getSolidPoleTide(final EOPHistory eopHistory) {

            // constants from IERS 1996 page 47
            final double globalFactor = -1.348e-9 / Constants.ARC_SECONDS_TO_RADIANS;
            final double coupling     =  0.0112;

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final PoleCorrection pole = eopHistory.getPoleCorrection(date);
                    return new double[] {
                        globalFactor * (pole.getXp() + coupling * pole.getYp()),
                        globalFactor * (coupling * pole.getXp() - pole.getYp()),
                    };
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    final FieldPoleCorrection<T> pole = eopHistory.getPoleCorrection(date);
                    final T[] a = MathArrays.buildArray(date.getField(), 2);
                    a[0] = pole.getXp().add(pole.getYp().multiply(coupling)).multiply(globalFactor);
                    a[1] = pole.getXp().multiply(coupling).subtract(pole.getYp()).multiply(globalFactor);
                    return a;
                }

            };
        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getOceanPoleTide(final EOPHistory eopHistory) {

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    // there are no model for ocean pole tide prior to conventions 2010
                    return new double[] {
                        0.0, 0.0
                    };
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    // there are no model for ocean pole tide prior to conventions 2010
                    return MathArrays.buildArray(date.getField(), 2);
                }

            };
        }

        /** {@inheritDoc} */
        @Override
        public double[] getNominalTidalDisplacement() {

            //  // elastic Earth values
            //  return new double[] {
            //      // h⁽⁰⁾,                                  h⁽²⁾,   h₃,     hI diurnal, hI semi-diurnal,
            //      0.6026,                                  -0.0006, 0.292, -0.0025, -0.0022,
            //      // l⁽⁰⁾, l⁽¹⁾ diurnal, l⁽¹⁾ semi-diurnal, l⁽²⁾,   l₃,     lI diurnal, lI semi-diurnal
            //      0.0831,  0.0012,       0.0024,            0.0002, 0.015, -0.0007,    -0.0007,
            //      // H₀
            //      -0.31460
            //  };

            // anelastic Earth values
            return new double[] {
                // h⁽⁰⁾,                                  h⁽²⁾,   h₃,     hI diurnal, hI semi-diurnal,
                0.6078,                                  -0.0006, 0.292, -0.0025,    -0.0022,
                // l⁽⁰⁾, l⁽¹⁾ diurnal, l⁽¹⁾ semi-diurnal, l⁽²⁾,   l₃,     lI diurnal, lI semi-diurnal
                0.0847,  0.0012,       0.0024,            0.0002, 0.015, -0.0007,    -0.0007,
                // H₀
                -0.31460
            };

        }

        /** {@inheritDoc} */
        @Override
        public CompiledSeries getTidalDisplacementFrequencyCorrectionDiurnal() {
            return getTidalDisplacementFrequencyCorrectionDiurnal(TIDAL_DISPLACEMENT_CORRECTION_DIURNAL,
                                                                  18, 17, -1, 18, -1);
        }

        /** {@inheritDoc} */
        @Override
        public CompiledSeries getTidalDisplacementFrequencyCorrectionZonal() {
            return getTidalDisplacementFrequencyCorrectionZonal(TIDAL_DISPLACEMENT_CORRECTION_ZONAL,
                                                                20, 17, 19, 18, 20);
        }

    },

    /** Constant for IERS 2003 conventions. */
    IERS_2003 {

        /** Nutation arguments resources. */
        private static final String NUTATION_ARGUMENTS = IERS_BASE + "2003/nutation-arguments.txt";

        /** X series resources. */
        private static final String X_SERIES           = IERS_BASE + "2003/tab5.2a.txt";

        /** Y series resources. */
        private static final String Y_SERIES           = IERS_BASE + "2003/tab5.2b.txt";

        /** S series resources. */
        private static final String S_SERIES           = IERS_BASE + "2003/tab5.2c.txt";

        /** Luni-solar series resources. */
        private static final String LUNI_SOLAR_SERIES  = IERS_BASE + "2003/tab5.3a-first-table.txt";

        /** Planetary series resources. */
        private static final String PLANETARY_SERIES   = IERS_BASE + "2003/tab5.3b.txt";

        /** Greenwhich sidereal time series resources. */
        private static final String GST_SERIES         = IERS_BASE + "2003/tab5.4.txt";

        /** Tidal correction for xp, yp series resources. */
        private static final String TIDAL_CORRECTION_XP_YP_SERIES = IERS_BASE + "2003/tab8.2ab.txt";

        /** Tidal correction for UT1 resources. */
        private static final String TIDAL_CORRECTION_UT1_SERIES = IERS_BASE + "2003/tab8.3ab.txt";

        /** Love numbers resources. */
        private static final String LOVE_NUMBERS = IERS_BASE + "2003/tab6.1.txt";

        /** Frequency dependence model for k₂₀. */
        private static final String K20_FREQUENCY_DEPENDENCE = IERS_BASE + "2003/tab6.3b.txt";

        /** Frequency dependence model for k₂₁. */
        private static final String K21_FREQUENCY_DEPENDENCE = IERS_BASE + "2003/tab6.3a.txt";

        /** Frequency dependence model for k₂₂. */
        private static final String K22_FREQUENCY_DEPENDENCE = IERS_BASE + "2003/tab6.3c.txt";

        /** Annual pole table. */
        private static final String ANNUAL_POLE = IERS_BASE + "2003/annual.pole";

        /** Tidal displacement frequency correction for diurnal tides. */
        private static final String TIDAL_DISPLACEMENT_CORRECTION_DIURNAL = IERS_BASE + "2003/tab7.5a.txt";

        /** Tidal displacement frequency correction for zonal tides. */
        private static final String TIDAL_DISPLACEMENT_CORRECTION_ZONAL = IERS_BASE + "2003/tab7.5b.txt";

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments(final TimeScale timeScale,
                                                                 final TimeScales timeScales) {
            return load(NUTATION_ARGUMENTS, in -> new FundamentalNutationArguments(this, timeScale,
                                                                                   in, NUTATION_ARGUMENTS,
                                                                                   timeScales));
        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getMeanObliquityFunction(final TimeScales timeScales) {

            // epsilon 0 value from chapter 5, page 41, other terms from equation 32 page 45
            final PolynomialNutation epsilonA =
                    new PolynomialNutation(84381.448    * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.84024  * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.00059  * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.001813 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {
                    return epsilonA.value(evaluateTC(date, timeScales));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return epsilonA.value(evaluateTC(date, timeScales));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getXYSpXY2Function(final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser parser =
                    new PoissonSeriesParser(17).
                        withPolynomialPart('t', PolynomialParser.Unit.MICRO_ARC_SECONDS).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);

            final PoissonSeries xSeries = load(X_SERIES, in -> parser.parse(in, X_SERIES));
            final PoissonSeries ySeries = load(Y_SERIES, in -> parser.parse(in, Y_SERIES));
            final PoissonSeries sSeries = load(S_SERIES, in -> parser.parse(in, S_SERIES));
            final PoissonSeries.CompiledSeries xys = PoissonSeries.compile(xSeries, ySeries, sSeries);

            // create a function evaluating the series
            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    return xys.value(arguments.evaluateAll(date));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    return xys.value(arguments.evaluateAll(date));
                }

            };

        }


        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getPrecessionFunction(final TimeScales timeScales) {

            // set up the conventional polynomials
            // the following values are from equation 32 in IERS 2003 conventions
            final PolynomialNutation psiA =
                    new PolynomialNutation(    0.0,
                                            5038.47875   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -1.07259   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.001147  * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation omegaA =
                    new PolynomialNutation(getMeanObliquityFunction(timeScales)
                            .value(getNutationReferenceEpoch(timeScales)),
                                           -0.02524   * Constants.ARC_SECONDS_TO_RADIANS,
                                            0.05127   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.007726  * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation chiA =
                    new PolynomialNutation( 0.0,
                                           10.5526   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -2.38064  * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.001125 * Constants.ARC_SECONDS_TO_RADIANS);

            return new PrecessionFunction(psiA, omegaA, chiA, timeScales);

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getNutationFunction(final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // set up Poisson series
            final double milliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3;
            final PoissonSeriesParser luniSolarParser =
                    new PoissonSeriesParser(14).withFirstDelaunay(1);
            final PoissonSeriesParser luniSolarPsiParser =
                    luniSolarParser.
                    withSinCos(0, 7, milliAS, 11, milliAS).
                    withSinCos(1, 8, milliAS, 12, milliAS);
            final PoissonSeries psiLuniSolarSeries =
                            load(LUNI_SOLAR_SERIES, in -> luniSolarPsiParser.parse(in, LUNI_SOLAR_SERIES));
            final PoissonSeriesParser luniSolarEpsilonParser = luniSolarParser.
                                                               withSinCos(0, 13, milliAS, 9, milliAS).
                                                               withSinCos(1, 14, milliAS, 10, milliAS);
            final PoissonSeries epsilonLuniSolarSeries =
                            load(LUNI_SOLAR_SERIES, in -> luniSolarEpsilonParser.parse(in, LUNI_SOLAR_SERIES));

            final PoissonSeriesParser planetaryParser =
                    new PoissonSeriesParser(21).
                        withFirstDelaunay(2).
                        withFirstPlanetary(7);
            final PoissonSeriesParser planetaryPsiParser =
                    planetaryParser.withSinCos(0, 17, milliAS, 18, milliAS);
            final PoissonSeries psiPlanetarySeries =
                            load(PLANETARY_SERIES, in -> planetaryPsiParser.parse(in, PLANETARY_SERIES));
            final PoissonSeriesParser planetaryEpsilonParser =
                    planetaryParser.withSinCos(0, 19, milliAS, 20, milliAS);
            final PoissonSeries epsilonPlanetarySeries =
                            load(PLANETARY_SERIES, in -> planetaryEpsilonParser.parse(in, PLANETARY_SERIES));

            final PoissonSeries.CompiledSeries luniSolarSeries =
                    PoissonSeries.compile(psiLuniSolarSeries, epsilonLuniSolarSeries);
            final PoissonSeries.CompiledSeries planetarySeries =
                    PoissonSeries.compile(psiPlanetarySeries, epsilonPlanetarySeries);

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] luniSolar = luniSolarSeries.value(elements);
                    final double[] planetary = planetarySeries.value(elements);
                    return new double[] {
                        luniSolar[0] + planetary[0], luniSolar[1] + planetary[1],
                        IAU1994ResolutionC7.value(elements, timeScales.getTAI())
                    };
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    final FieldBodiesElements<T> elements = arguments.evaluateAll(date);
                    final T[] luniSolar = luniSolarSeries.value(elements);
                    final T[] planetary = planetarySeries.value(elements);
                    final T[] result = MathArrays.buildArray(date.getField(), 3);
                    result[0] = luniSolar[0].add(planetary[0]);
                    result[1] = luniSolar[1].add(planetary[1]);
                    result[2] = IAU1994ResolutionC7.value(elements, timeScales.getTAI());
                    return result;
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGMSTFunction(final TimeScale ut1,
                                                  final TimeScales timeScales) {

            // Earth Rotation Angle
            final StellarAngleCapitaine era =
                    new StellarAngleCapitaine(ut1, timeScales.getTAI());

            // Polynomial part of the apparent sidereal time series
            // which is the opposite of Equation of Origins (EO)
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser parser =
                    new PoissonSeriesParser(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PolynomialNutation minusEO = load(GST_SERIES, in -> parser.parse(in, GST_SERIES).getPolynomial());

            // create a function evaluating the series
            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {
                    return era.value(date) + minusEO.value(evaluateTC(date, timeScales));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return era.value(date).add(minusEO.value(evaluateTC(date, timeScales)));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGMSTRateFunction(final TimeScale ut1,
                                                      final TimeScales timeScales) {

            // Earth Rotation Angle
            final StellarAngleCapitaine era =
                    new StellarAngleCapitaine(ut1, timeScales.getTAI());

            // Polynomial part of the apparent sidereal time series
            // which is the opposite of Equation of Origins (EO)
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser parser =
                    new PoissonSeriesParser(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PolynomialNutation minusEO = load(GST_SERIES, in -> parser.parse(in, GST_SERIES).getPolynomial());

            // create a function evaluating the series
            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {
                    return era.getRate() + minusEO.derivative(evaluateTC(date, timeScales));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return minusEO.derivative(evaluateTC(date, timeScales)).add(era.getRate());
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGASTFunction(final TimeScale ut1,
                                                  final EOPHistory eopHistory,
                                                  final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // mean obliquity function
            final TimeScalarFunction epsilon = getMeanObliquityFunction(timeScales);

            // set up Poisson series
            final double milliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3;
            final PoissonSeriesParser luniSolarPsiParser =
                    new PoissonSeriesParser(14).
                        withFirstDelaunay(1).
                        withSinCos(0, 7, milliAS, 11, milliAS).
                        withSinCos(1, 8, milliAS, 12, milliAS);
            final PoissonSeries psiLuniSolarSeries =
                            load(LUNI_SOLAR_SERIES, in -> luniSolarPsiParser.parse(in, LUNI_SOLAR_SERIES));

            final PoissonSeriesParser planetaryPsiParser =
                    new PoissonSeriesParser(21).
                        withFirstDelaunay(2).
                        withFirstPlanetary(7).
                        withSinCos(0, 17, milliAS, 18, milliAS);
            final PoissonSeries psiPlanetarySeries =
                            load(PLANETARY_SERIES, in -> planetaryPsiParser.parse(in, PLANETARY_SERIES));


            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser gstParser =
                    new PoissonSeriesParser(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PoissonSeries gstSeries = load(GST_SERIES, in -> gstParser.parse(in, GST_SERIES));
            final PoissonSeries.CompiledSeries psiGstSeries =
                    PoissonSeries.compile(psiLuniSolarSeries, psiPlanetarySeries, gstSeries);

            // ERA function
            final TimeScalarFunction era =
                    getEarthOrientationAngleFunction(ut1, timeScales.getTAI());

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {

                    // evaluate equation of origins
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] angles = psiGstSeries.value(elements);
                    final double ddPsi    = (eopHistory == null) ? 0 : eopHistory.getEquinoxNutationCorrection(date)[0];
                    final double deltaPsi = angles[0] + angles[1] + ddPsi;
                    final double epsilonA = epsilon.value(date);

                    // subtract equation of origin from EA
                    // (hence add the series above which have the sign included)
                    return era.value(date) + deltaPsi * FastMath.cos(epsilonA) + angles[2];

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {

                    // evaluate equation of origins
                    final FieldBodiesElements<T> elements = arguments.evaluateAll(date);
                    final T[] angles = psiGstSeries.value(elements);
                    final T ddPsi    = (eopHistory == null) ? date.getField().getZero() : eopHistory.getEquinoxNutationCorrection(date)[0];
                    final T deltaPsi = angles[0].add(angles[1]).add(ddPsi);
                    final T epsilonA = epsilon.value(date);

                    // subtract equation of origin from EA
                    // (hence add the series above which have the sign included)
                    return era.value(date).add(deltaPsi.multiply(epsilonA.cos())).add(angles[2]);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getEOPTidalCorrection(final TimeScales timeScales) {

            // set up nutation arguments
            // BEWARE! Using TT as the time scale here and not UT1 is intentional!
            // as this correction is used to compute UT1 itself, it is not surprising we cannot use UT1 yet,
            // however, using the close UTC as would seem logical make the comparison with interp.f from IERS fail
            // looking in the interp.f code, the same TT scale is used for both Delaunay and gamma argument
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales.getTT(), timeScales);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser xyParser = new PoissonSeriesParser(13).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3);
            final double microS = 1.0e-6;
            final PoissonSeriesParser ut1Parser = new PoissonSeriesParser(11).
                            withOptionalColumn(1).
                            withGamma(2).
                            withFirstDelaunay(3);
            final PoissonSeries xSeries =
                            load(TIDAL_CORRECTION_XP_YP_SERIES, in -> xyParser.
                                                                      withSinCos(0, 10, microAS, 11, microAS).
                                                                      parse(in, TIDAL_CORRECTION_XP_YP_SERIES));
            final PoissonSeries ySeries =
                            load(TIDAL_CORRECTION_XP_YP_SERIES, in -> xyParser.
                                                                      withSinCos(0, 12, microAS, 13, microAS).
                                                                      parse(in, TIDAL_CORRECTION_XP_YP_SERIES));

            final PoissonSeries ut1Series =
                            load(TIDAL_CORRECTION_UT1_SERIES, in -> ut1Parser.
                                                                    withSinCos(0, 10, microS, 11, microS).
                                                                    parse(in, TIDAL_CORRECTION_UT1_SERIES));

            return new EOPTidalCorrection(arguments, xSeries, ySeries, ut1Series);

        }

        /** {@inheritDoc} */
        public LoveNumbers getLoveNumbers() {
            return loadLoveNumbers(LOVE_NUMBERS);
        }

        /** {@inheritDoc} */
        public TimeVectorFunction getTideFrequencyDependenceFunction(final TimeScale ut1,
                                                                     final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(ut1, timeScales);

            // set up Poisson series
            final PoissonSeriesParser k20Parser =
                    new PoissonSeriesParser(18).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);
            final PoissonSeriesParser k21Parser =
                    new PoissonSeriesParser(18).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10);
            final PoissonSeriesParser k22Parser =
                    new PoissonSeriesParser(16).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);

            final double pico = 1.0e-12;
            final PoissonSeries c20Series =
                            load(K20_FREQUENCY_DEPENDENCE, in -> k20Parser.
                                                                 withSinCos(0, 18, -pico, 16, pico).
                                                                 parse(in, K20_FREQUENCY_DEPENDENCE));
            final PoissonSeries c21Series =
                            load(K21_FREQUENCY_DEPENDENCE, in -> k21Parser.
                                                                 withSinCos(0, 17, pico, 18, pico).
                                                                 parse(in, K21_FREQUENCY_DEPENDENCE));
            final PoissonSeries s21Series =
                            load(K21_FREQUENCY_DEPENDENCE, in -> k21Parser.
                                                                 withSinCos(0, 18, -pico, 17, pico).
                                                                 parse(in, K21_FREQUENCY_DEPENDENCE));
            final PoissonSeries c22Series =
                            load(K22_FREQUENCY_DEPENDENCE, in -> k22Parser.
                                                                 withSinCos(0, -1, pico, 16, pico).
                                                                 parse(in, K22_FREQUENCY_DEPENDENCE));
            final PoissonSeries s22Series =
                            load(K22_FREQUENCY_DEPENDENCE, in -> k22Parser.
                                                                 withSinCos(0, 16, -pico, -1, pico).
                                                                 parse(in, K22_FREQUENCY_DEPENDENCE));

            return new TideFrequencyDependenceFunction(arguments,
                                                       c20Series,
                                                       c21Series, s21Series,
                                                       c22Series, s22Series);

        }

        /** {@inheritDoc} */
        @Override
        public double getPermanentTide() {
            return 4.4228e-8 * -0.31460 * getLoveNumbers().getReal(2, 0);
        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getSolidPoleTide(final EOPHistory eopHistory) {

            // annual pole from ftp://tai.bipm.org/iers/conv2003/chapter7/annual.pole
            final TimeScale utc = eopHistory.getTimeScales().getUTC();
            final SimpleTimeStampedTableParser.RowConverter<MeanPole> converter =
                new SimpleTimeStampedTableParser.RowConverter<MeanPole>() {
                    /** {@inheritDoc} */
                    @Override
                    public MeanPole convert(final double[] rawFields) {
                        return new MeanPole(new AbsoluteDate((int) rawFields[0], 1, 1, utc),
                                            rawFields[1] * Constants.ARC_SECONDS_TO_RADIANS,
                                            rawFields[2] * Constants.ARC_SECONDS_TO_RADIANS);
                    }
                };
            final SimpleTimeStampedTableParser<MeanPole> parser =
                    new SimpleTimeStampedTableParser<MeanPole>(3, converter);
            final List<MeanPole> annualPoleList = load(ANNUAL_POLE, in -> parser.parse(in, ANNUAL_POLE));
            final AbsoluteDate firstAnnualPoleDate = annualPoleList.get(0).getDate();
            final AbsoluteDate lastAnnualPoleDate  = annualPoleList.get(annualPoleList.size() - 1).getDate();
            final ImmutableTimeStampedCache<MeanPole> annualCache =
                    new ImmutableTimeStampedCache<MeanPole>(2, annualPoleList);

            // polynomial extension from IERS 2003, section 7.1.4, equations 23a and 23b
            final double xp0    = 0.054   * Constants.ARC_SECONDS_TO_RADIANS;
            final double xp0Dot = 0.00083 * Constants.ARC_SECONDS_TO_RADIANS / Constants.JULIAN_YEAR;
            final double yp0    = 0.357   * Constants.ARC_SECONDS_TO_RADIANS;
            final double yp0Dot = 0.00395 * Constants.ARC_SECONDS_TO_RADIANS / Constants.JULIAN_YEAR;

            // constants from IERS 2003, section 6.2
            final double globalFactor = -1.333e-9 / Constants.ARC_SECONDS_TO_RADIANS;
            final double ratio        =  0.0115;

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {

                    // we can't compute anything before the range covered by the annual pole file
                    if (date.compareTo(firstAnnualPoleDate) <= 0) {
                        return new double[] {
                            0.0, 0.0
                        };
                    }

                    // evaluate mean pole
                    double meanPoleX = 0;
                    double meanPoleY = 0;
                    if (date.compareTo(lastAnnualPoleDate) <= 0) {
                        // we are within the range covered by the annual pole file,
                        // we interpolate within it
                        try {
                            final HermiteInterpolator interpolator = new HermiteInterpolator();
                            annualCache.getNeighbors(date).forEach(neighbor ->
                                interpolator.addSamplePoint(neighbor.getDate().durationFrom(date),
                                                            new double[] {
                                                                neighbor.getX(), neighbor.getY()
                                                            }));
                            final double[] interpolated = interpolator.value(0);
                            meanPoleX = interpolated[0];
                            meanPoleY = interpolated[1];
                        } catch (TimeStampedCacheException tsce) {
                            // this should never happen
                            throw new OrekitInternalError(tsce);
                        }
                    } else {

                        // we are after the range covered by the annual pole file,
                        // we use the polynomial extension
                        final double t = date.durationFrom(
                                eopHistory.getTimeScales().getJ2000Epoch());
                        meanPoleX = xp0 + t * xp0Dot;
                        meanPoleY = yp0 + t * yp0Dot;

                    }

                    // evaluate wobble variables
                    final PoleCorrection correction = eopHistory.getPoleCorrection(date);
                    final double m1 = correction.getXp() - meanPoleX;
                    final double m2 = meanPoleY - correction.getYp();

                    return new double[] {
                        // the following correspond to the equations published in IERS 2003 conventions,
                        // section 6.2 page 65. In the publication, the equations read:
                        // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ − 0.0115m₂)
                        // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ + 0.0115m₁)
                        // However, it seems there are sign errors in these equations, which have
                        // been fixed in IERS 2010 conventions, section 6.4 page 94. In these newer
                        // publication, the equations read:
                        // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ + 0.0115m₂)
                        // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ − 0.0115m₁)
                        // the newer equations seem more consistent with the premises as the
                        // deformation due to the centrifugal potential has the form:
                        // −Ω²r²/2 sin 2θ Re [k₂(m₁ − im₂) exp(iλ)] where k₂ is the complex
                        // number 0.3077 + 0.0036i, so the real part in the previous equation is:
                        // A[Re(k₂) m₁ + Im(k₂) m₂)] cos λ + A[Re(k₂) m₂ - Im(k₂) m₁] sin λ
                        // identifying this with ∆C₂₁ cos λ + ∆S₂₁ sin λ we get:
                        // ∆C₂₁ = A Re(k₂) [m₁ + Im(k₂)/Re(k₂) m₂)]
                        // ∆S₂₁ = A Re(k₂) [m₂ - Im(k₂)/Re(k₂) m₁)]
                        // and Im(k₂)/Re(k₂) is very close to +0.0115
                        // As the equation as written in the IERS 2003 conventions are used in
                        // legacy systems, we have reproduced this alleged error here (and fixed it in
                        // the IERS 2010 conventions below) for validation purposes. We don't recommend
                        // using the IERS 2003 conventions for solid pole tide computation other than
                        // for validation or reproducibility of legacy applications behavior.
                        // As solid pole tide is small and as the sign change is on the smallest coefficient,
                        // the effect is quite small. A test case on a propagated orbit showed a position change
                        // slightly below 0.4m after a 30 days propagation on a Low Earth Orbit
                        globalFactor * (m1 - ratio * m2),
                        globalFactor * (m2 + ratio * m1),
                    };

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {

                    final AbsoluteDate aDate = date.toAbsoluteDate();

                    // we can't compute anything before the range covered by the annual pole file
                    if (aDate.compareTo(firstAnnualPoleDate) <= 0) {
                        return MathArrays.buildArray(date.getField(), 2);
                    }

                    // evaluate mean pole
                    T meanPoleX = date.getField().getZero();
                    T meanPoleY = date.getField().getZero();
                    if (aDate.compareTo(lastAnnualPoleDate) <= 0) {
                        // we are within the range covered by the annual pole file,
                        // we interpolate within it
                        try {
                            final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();
                            final T[] y = MathArrays.buildArray(date.getField(), 2);
                            final T zero = date.getField().getZero();
                            final FieldAbsoluteDate<T> central = new FieldAbsoluteDate<>(aDate, zero); // here, we attempt to get a constant date,
                                                                                                       // for example removing derivatives
                                                                                                       // if T was DerivativeStructure
                            annualCache.getNeighbors(aDate).forEach(neighbor -> {
                                y[0] = zero.add(neighbor.getX());
                                y[1] = zero.add(neighbor.getY());
                                interpolator.addSamplePoint(central.durationFrom(neighbor.getDate()).negate(), y);
                            });
                            final T[] interpolated = interpolator.value(date.durationFrom(central)); // here, we introduce derivatives again (in DerivativeStructure case)
                            meanPoleX = interpolated[0];
                            meanPoleY = interpolated[1];
                        } catch (TimeStampedCacheException tsce) {
                            // this should never happen
                            throw new OrekitInternalError(tsce);
                        }
                    } else {

                        // we are after the range covered by the annual pole file,
                        // we use the polynomial extension
                        final T t = date.durationFrom(
                                eopHistory.getTimeScales().getJ2000Epoch());
                        meanPoleX = t.multiply(xp0Dot).add(xp0);
                        meanPoleY = t.multiply(yp0Dot).add(yp0);

                    }

                    // evaluate wobble variables
                    final FieldPoleCorrection<T> correction = eopHistory.getPoleCorrection(date);
                    final T m1 = correction.getXp().subtract(meanPoleX);
                    final T m2 = meanPoleY.subtract(correction.getYp());

                    final T[] a = MathArrays.buildArray(date.getField(), 2);

                    // the following correspond to the equations published in IERS 2003 conventions,
                    // section 6.2 page 65. In the publication, the equations read:
                    // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ − 0.0115m₂)
                    // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ + 0.0115m₁)
                    // However, it seems there are sign errors in these equations, which have
                    // been fixed in IERS 2010 conventions, section 6.4 page 94. In these newer
                    // publication, the equations read:
                    // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ + 0.0115m₂)
                    // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ − 0.0115m₁)
                    // the newer equations seem more consistent with the premises as the
                    // deformation due to the centrifugal potential has the form:
                    // −Ω²r²/2 sin 2θ Re [k₂(m₁ − im₂) exp(iλ)] where k₂ is the complex
                    // number 0.3077 + 0.0036i, so the real part in the previous equation is:
                    // A[Re(k₂) m₁ + Im(k₂) m₂)] cos λ + A[Re(k₂) m₂ - Im(k₂) m₁] sin λ
                    // identifying this with ∆C₂₁ cos λ + ∆S₂₁ sin λ we get:
                    // ∆C₂₁ = A Re(k₂) [m₁ + Im(k₂)/Re(k₂) m₂)]
                    // ∆S₂₁ = A Re(k₂) [m₂ - Im(k₂)/Re(k₂) m₁)]
                    // and Im(k₂)/Re(k₂) is very close to +0.0115
                    // As the equation as written in the IERS 2003 conventions are used in
                    // legacy systems, we have reproduced this alleged error here (and fixed it in
                    // the IERS 2010 conventions below) for validation purposes. We don't recommend
                    // using the IERS 2003 conventions for solid pole tide computation other than
                    // for validation or reproducibility of legacy applications behavior.
                    // As solid pole tide is small and as the sign change is on the smallest coefficient,
                    // the effect is quite small. A test case on a propagated orbit showed a position change
                    // slightly below 0.4m after a 30 days propagation on a Low Earth Orbit
                    a[0] = m1.add(m2.multiply(-ratio)).multiply(globalFactor);
                    a[1] = m2.add(m1.multiply( ratio)).multiply(globalFactor);

                    return a;

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getOceanPoleTide(final EOPHistory eopHistory) {

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    // there are no model for ocean pole tide prior to conventions 2010
                    return new double[] {
                        0.0, 0.0
                    };
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    // there are no model for ocean pole tide prior to conventions 2010
                    return MathArrays.buildArray(date.getField(), 2);
                }

            };
        }

        /** {@inheritDoc} */
        @Override
        public double[] getNominalTidalDisplacement() {
            return new double[] {
                // h⁽⁰⁾,                                  h⁽²⁾,   h₃,     hI diurnal, hI semi-diurnal,
                0.6078,                                  -0.0006, 0.292, -0.0025,    -0.0022,
                // l⁽⁰⁾, l⁽¹⁾ diurnal, l⁽¹⁾ semi-diurnal, l⁽²⁾,   l₃,     lI diurnal, lI semi-diurnal
                0.0847,  0.0012,       0.0024,            0.0002, 0.015, -0.0007,    -0.0007,
                // H₀
                -0.31460
            };
        }

        /** {@inheritDoc} */
        @Override
        public CompiledSeries getTidalDisplacementFrequencyCorrectionDiurnal() {
            return getTidalDisplacementFrequencyCorrectionDiurnal(TIDAL_DISPLACEMENT_CORRECTION_DIURNAL,
                                                                  18, 15, 16, 17, 18);
        }

        /** {@inheritDoc} */
        @Override
        public CompiledSeries getTidalDisplacementFrequencyCorrectionZonal() {
            return getTidalDisplacementFrequencyCorrectionZonal(TIDAL_DISPLACEMENT_CORRECTION_ZONAL,
                                                                18, 15, 16, 17, 18);
        }

    },

    /** Constant for IERS 2010 conventions. */
    IERS_2010 {

        /** Nutation arguments resources. */
        private static final String NUTATION_ARGUMENTS = IERS_BASE + "2010/nutation-arguments.txt";

        /** X series resources. */
        private static final String X_SERIES           = IERS_BASE + "2010/tab5.2a.txt";

        /** Y series resources. */
        private static final String Y_SERIES           = IERS_BASE + "2010/tab5.2b.txt";

        /** S series resources. */
        private static final String S_SERIES           = IERS_BASE + "2010/tab5.2d.txt";

        /** Psi series resources. */
        private static final String PSI_SERIES         = IERS_BASE + "2010/tab5.3a.txt";

        /** Epsilon series resources. */
        private static final String EPSILON_SERIES     = IERS_BASE + "2010/tab5.3b.txt";

        /** Greenwhich sidereal time series resources. */
        private static final String GST_SERIES         = IERS_BASE + "2010/tab5.2e.txt";

        /** Tidal correction for xp, yp series resources. */
        private static final String TIDAL_CORRECTION_XP_YP_SERIES = IERS_BASE + "2010/tab8.2ab.txt";

        /** Tidal correction for UT1 resources. */
        private static final String TIDAL_CORRECTION_UT1_SERIES = IERS_BASE + "2010/tab8.3ab.txt";

        /** Love numbers resources. */
        private static final String LOVE_NUMBERS = IERS_BASE + "2010/tab6.3.txt";

        /** Frequency dependence model for k₂₀. */
        private static final String K20_FREQUENCY_DEPENDENCE = IERS_BASE + "2010/tab6.5b.txt";

        /** Frequency dependence model for k₂₁. */
        private static final String K21_FREQUENCY_DEPENDENCE = IERS_BASE + "2010/tab6.5a.txt";

        /** Frequency dependence model for k₂₂. */
        private static final String K22_FREQUENCY_DEPENDENCE = IERS_BASE + "2010/tab6.5c.txt";

        /** Tidal displacement frequency correction for diurnal tides. */
        private static final String TIDAL_DISPLACEMENT_CORRECTION_DIURNAL = IERS_BASE + "2010/tab7.3a.txt";

        /** Tidal displacement frequency correction for zonal tides. */
        private static final String TIDAL_DISPLACEMENT_CORRECTION_ZONAL = IERS_BASE + "2010/tab7.3b.txt";

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments(final TimeScale timeScale,
                                                                 final TimeScales timeScales) {
            return load(NUTATION_ARGUMENTS, in -> new FundamentalNutationArguments(this, timeScale,
                                                                                            in, NUTATION_ARGUMENTS,
                                                                                            timeScales));
        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getMeanObliquityFunction(final TimeScales timeScales) {

            // epsilon 0 value from chapter 5, page 56, other terms from equation 5.40 page 65
            final PolynomialNutation epsilonA =
                    new PolynomialNutation(84381.406        * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.836769     * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0001831    * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.00200340   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.000000576  * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0000000434 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {
                    return epsilonA.value(evaluateTC(date, timeScales));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return epsilonA.value(evaluateTC(date, timeScales));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getXYSpXY2Function(final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser parser =
                    new PoissonSeriesParser(17).
                        withPolynomialPart('t', PolynomialParser.Unit.MICRO_ARC_SECONDS).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);
            final PoissonSeries                xSeries = load(X_SERIES, in -> parser.parse(in, X_SERIES));
            final PoissonSeries                ySeries = load(Y_SERIES, in -> parser.parse(in, Y_SERIES));
            final PoissonSeries                sSeries = load(S_SERIES, in -> parser.parse(in, S_SERIES));
            final PoissonSeries.CompiledSeries xys     = PoissonSeries.compile(xSeries, ySeries, sSeries);

            // create a function evaluating the series
            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    return xys.value(arguments.evaluateAll(date));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    return xys.value(arguments.evaluateAll(date));
                }

            };

        }

        /** {@inheritDoc} */
        public LoveNumbers getLoveNumbers() {
            return loadLoveNumbers(LOVE_NUMBERS);
        }

        /** {@inheritDoc} */
        public TimeVectorFunction getTideFrequencyDependenceFunction(final TimeScale ut1,
                                                                     final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(ut1, timeScales);

            // set up Poisson series
            final PoissonSeriesParser k20Parser =
                    new PoissonSeriesParser(18).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);
            final PoissonSeriesParser k21Parser =
                    new PoissonSeriesParser(18).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10);
            final PoissonSeriesParser k22Parser =
                    new PoissonSeriesParser(16).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);

            final double pico = 1.0e-12;
            final PoissonSeries c20Series = load(K20_FREQUENCY_DEPENDENCE, in -> k20Parser.
                                                                                 withSinCos(0, 18, -pico, 16, pico).
                                                                                 parse(in, K20_FREQUENCY_DEPENDENCE));
            final PoissonSeries c21Series = load(K21_FREQUENCY_DEPENDENCE, in -> k21Parser.
                                                                                 withSinCos(0, 17, pico, 18, pico).
                                                                                 parse(in, K21_FREQUENCY_DEPENDENCE));
            final PoissonSeries s21Series = load(K21_FREQUENCY_DEPENDENCE, in -> k21Parser.
                                                                                 withSinCos(0, 18, -pico, 17, pico).
                                                                                 parse(in, K21_FREQUENCY_DEPENDENCE));
            final PoissonSeries c22Series = load(K22_FREQUENCY_DEPENDENCE, in -> k22Parser.
                                                                                 withSinCos(0, -1, pico, 16, pico).
                                                                                 parse(in, K22_FREQUENCY_DEPENDENCE));
            final PoissonSeries s22Series = load(K22_FREQUENCY_DEPENDENCE, in -> k22Parser.
                                                                                 withSinCos(0, 16, -pico, -1, pico).
                                                                                 parse(in, K22_FREQUENCY_DEPENDENCE));

            return new TideFrequencyDependenceFunction(arguments,
                                                       c20Series,
                                                       c21Series, s21Series,
                                                       c22Series, s22Series);

        }

        /** {@inheritDoc} */
        @Override
        public double getPermanentTide() {
            return 4.4228e-8 * -0.31460 * getLoveNumbers().getReal(2, 0);
        }

        /** Compute pole wobble variables m₁ and m₂.
         * @param date current date
         * @param eopHistory EOP history
         * @return array containing m₁ and m₂
         */
        private double[] computePoleWobble(final AbsoluteDate date, final EOPHistory eopHistory) {

            // polynomial model from IERS 2010, table 7.7
            final double f0 = Constants.ARC_SECONDS_TO_RADIANS / 1000.0;
            final double f1 = f0 / Constants.JULIAN_YEAR;
            final double f2 = f1 / Constants.JULIAN_YEAR;
            final double f3 = f2 / Constants.JULIAN_YEAR;
            final AbsoluteDate changeDate =
                    new AbsoluteDate(2010, 1, 1, eopHistory.getTimeScales().getTT());

            // evaluate mean pole
            final double[] xPolynomial;
            final double[] yPolynomial;
            if (date.compareTo(changeDate) <= 0) {
                xPolynomial = new double[] {
                    55.974 * f0, 1.8243 * f1, 0.18413 * f2, 0.007024 * f3
                };
                yPolynomial = new double[] {
                    346.346 * f0, 1.7896 * f1, -0.10729 * f2, -0.000908 * f3
                };
            } else {
                xPolynomial = new double[] {
                    23.513 * f0, 7.6141 * f1
                };
                yPolynomial = new double[] {
                    358.891 * f0,  -0.6287 * f1
                };
            }
            double meanPoleX = 0;
            double meanPoleY = 0;
            final double t = date.durationFrom(
                    eopHistory.getTimeScales().getJ2000Epoch());
            for (int i = xPolynomial.length - 1; i >= 0; --i) {
                meanPoleX = meanPoleX * t + xPolynomial[i];
            }
            for (int i = yPolynomial.length - 1; i >= 0; --i) {
                meanPoleY = meanPoleY * t + yPolynomial[i];
            }

            // evaluate wobble variables
            final PoleCorrection correction = eopHistory.getPoleCorrection(date);
            final double m1 = correction.getXp() - meanPoleX;
            final double m2 = meanPoleY - correction.getYp();

            return new double[] {
                m1, m2
            };

        }

        /** Compute pole wobble variables m₁ and m₂.
         * @param date current date
         * @param <T> type of the field elements
         * @param eopHistory EOP history
         * @return array containing m₁ and m₂
         */
        private <T extends CalculusFieldElement<T>> T[] computePoleWobble(final FieldAbsoluteDate<T> date, final EOPHistory eopHistory) {

            // polynomial model from IERS 2010, table 7.7
            final double f0 = Constants.ARC_SECONDS_TO_RADIANS / 1000.0;
            final double f1 = f0 / Constants.JULIAN_YEAR;
            final double f2 = f1 / Constants.JULIAN_YEAR;
            final double f3 = f2 / Constants.JULIAN_YEAR;
            final AbsoluteDate changeDate =
                    new AbsoluteDate(2010, 1, 1, eopHistory.getTimeScales().getTT());

            // evaluate mean pole
            final double[] xPolynomial;
            final double[] yPolynomial;
            if (date.toAbsoluteDate().compareTo(changeDate) <= 0) {
                xPolynomial = new double[] {
                    55.974 * f0, 1.8243 * f1, 0.18413 * f2, 0.007024 * f3
                };
                yPolynomial = new double[] {
                    346.346 * f0, 1.7896 * f1, -0.10729 * f2, -0.000908 * f3
                };
            } else {
                xPolynomial = new double[] {
                    23.513 * f0, 7.6141 * f1
                };
                yPolynomial = new double[] {
                    358.891 * f0,  -0.6287 * f1
                };
            }
            T meanPoleX = date.getField().getZero();
            T meanPoleY = date.getField().getZero();
            final T t = date.durationFrom(
                    eopHistory.getTimeScales().getJ2000Epoch());
            for (int i = xPolynomial.length - 1; i >= 0; --i) {
                meanPoleX = meanPoleX.multiply(t).add(xPolynomial[i]);
            }
            for (int i = yPolynomial.length - 1; i >= 0; --i) {
                meanPoleY = meanPoleY.multiply(t).add(yPolynomial[i]);
            }

            // evaluate wobble variables
            final FieldPoleCorrection<T> correction = eopHistory.getPoleCorrection(date);
            final T[] m = MathArrays.buildArray(date.getField(), 2);
            m[0] = correction.getXp().subtract(meanPoleX);
            m[1] = meanPoleY.subtract(correction.getYp());

            return m;

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getSolidPoleTide(final EOPHistory eopHistory) {

            // constants from IERS 2010, section 6.4
            final double globalFactor = -1.333e-9 / Constants.ARC_SECONDS_TO_RADIANS;
            final double ratio        =  0.0115;

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {

                    // evaluate wobble variables
                    final double[] wobbleM = computePoleWobble(date, eopHistory);

                    return new double[] {
                        // the following correspond to the equations published in IERS 2010 conventions,
                        // section 6.4 page 94. The equations read:
                        // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ + 0.0115m₂)
                        // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ − 0.0115m₁)
                        // These equations seem to fix what was probably a sign error in IERS 2003
                        // conventions section 6.2 page 65. In this older publication, the equations read:
                        // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ − 0.0115m₂)
                        // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ + 0.0115m₁)
                        globalFactor * (wobbleM[0] + ratio * wobbleM[1]),
                        globalFactor * (wobbleM[1] - ratio * wobbleM[0])
                    };

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {

                    // evaluate wobble variables
                    final T[] wobbleM = computePoleWobble(date, eopHistory);

                    final T[] a = MathArrays.buildArray(date.getField(), 2);

                    // the following correspond to the equations published in IERS 2010 conventions,
                    // section 6.4 page 94. The equations read:
                    // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ + 0.0115m₂)
                    // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ − 0.0115m₁)
                    // These equations seem to fix what was probably a sign error in IERS 2003
                    // conventions section 6.2 page 65. In this older publication, the equations read:
                    // ∆C₂₁ = −1.333 × 10⁻⁹ (m₁ − 0.0115m₂)
                    // ∆S₂₁ = −1.333 × 10⁻⁹ (m₂ + 0.0115m₁)
                    a[0] = wobbleM[0].add(wobbleM[1].multiply( ratio)).multiply(globalFactor);
                    a[1] = wobbleM[1].add(wobbleM[0].multiply(-ratio)).multiply(globalFactor);

                    return a;

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getOceanPoleTide(final EOPHistory eopHistory) {

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {

                    // evaluate wobble variables
                    final double[] wobbleM = computePoleWobble(date, eopHistory);

                    return new double[] {
                        // the following correspond to the equations published in IERS 2010 conventions,
                        // section 6.4 page 94 equation 6.24:
                        // ∆C₂₁ = −2.1778 × 10⁻¹⁰ (m₁ − 0.01724m₂)
                        // ∆S₂₁ = −1.7232 × 10⁻¹⁰ (m₂ − 0.03365m₁)
                        -2.1778e-10 * (wobbleM[0] - 0.01724 * wobbleM[1]) / Constants.ARC_SECONDS_TO_RADIANS,
                        -1.7232e-10 * (wobbleM[1] - 0.03365 * wobbleM[0]) / Constants.ARC_SECONDS_TO_RADIANS
                    };

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {

                    final T[] v = MathArrays.buildArray(date.getField(), 2);

                    // evaluate wobble variables
                    final T[] wobbleM = computePoleWobble(date, eopHistory);

                    // the following correspond to the equations published in IERS 2010 conventions,
                    // section 6.4 page 94 equation 6.24:
                    // ∆C₂₁ = −2.1778 × 10⁻¹⁰ (m₁ − 0.01724m₂)
                    // ∆S₂₁ = −1.7232 × 10⁻¹⁰ (m₂ − 0.03365m₁)
                    v[0] = wobbleM[0].subtract(wobbleM[1].multiply(0.01724)).multiply(-2.1778e-10 / Constants.ARC_SECONDS_TO_RADIANS);
                    v[1] = wobbleM[1].subtract(wobbleM[0].multiply(0.03365)).multiply(-1.7232e-10 / Constants.ARC_SECONDS_TO_RADIANS);

                    return v;

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getPrecessionFunction(final TimeScales timeScales) {

            // set up the conventional polynomials
            // the following values are from equation 5.40 in IERS 2010 conventions
            final PolynomialNutation psiA =
                    new PolynomialNutation(   0.0,
                                           5038.481507     * Constants.ARC_SECONDS_TO_RADIANS,
                                             -1.0790069    * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.00114045   * Constants.ARC_SECONDS_TO_RADIANS,
                                              0.000132851  * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.0000000951 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation omegaA =
                    new PolynomialNutation(getMeanObliquityFunction(timeScales)
                            .value(getNutationReferenceEpoch(timeScales)),
                                           -0.025754     * Constants.ARC_SECONDS_TO_RADIANS,
                                            0.0512623    * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.00772503   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.000000467  * Constants.ARC_SECONDS_TO_RADIANS,
                                            0.0000003337 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation chiA =
                    new PolynomialNutation( 0.0,
                                           10.556403     * Constants.ARC_SECONDS_TO_RADIANS,
                                           -2.3814292    * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.00121197   * Constants.ARC_SECONDS_TO_RADIANS,
                                            0.000170663  * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.0000000560 * Constants.ARC_SECONDS_TO_RADIANS);

            return new PrecessionFunction(psiA, omegaA, chiA, timeScales);

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getNutationFunction(final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser parser =
                    new PoissonSeriesParser(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);
            final PoissonSeries psiSeries     = load(PSI_SERIES,     in -> parser.parse(in, PSI_SERIES));
            final PoissonSeries epsilonSeries = load(EPSILON_SERIES, in -> parser.parse(in, EPSILON_SERIES));
            final PoissonSeries.CompiledSeries psiEpsilonSeries = PoissonSeries.compile(psiSeries, epsilonSeries);

            return new TimeVectorFunction() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] psiEpsilon = psiEpsilonSeries.value(elements);
                    return new double[] {
                        psiEpsilon[0], psiEpsilon[1],
                        IAU1994ResolutionC7.value(elements, timeScales.getTAI())
                    };
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
                    final FieldBodiesElements<T> elements = arguments.evaluateAll(date);
                    final T[] psiEpsilon = psiEpsilonSeries.value(elements);
                    final T[] result = MathArrays.buildArray(date.getField(), 3);
                    result[0] = psiEpsilon[0];
                    result[1] = psiEpsilon[1];
                    result[2] = IAU1994ResolutionC7.value(elements, timeScales.getTAI());
                    return result;
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGMSTFunction(final TimeScale ut1,
                                                  final TimeScales timeScales) {

            // Earth Rotation Angle
            final StellarAngleCapitaine era =
                    new StellarAngleCapitaine(ut1, timeScales.getTAI());

            // Polynomial part of the apparent sidereal time series
            // which is the opposite of Equation of Origins (EO)
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser parser =
                    new PoissonSeriesParser(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PolynomialNutation minusEO = load(GST_SERIES, in ->  parser.parse(in, GST_SERIES).getPolynomial());

            // create a function evaluating the series
            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {
                    return era.value(date) + minusEO.value(evaluateTC(date, timeScales));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return era.value(date).add(minusEO.value(evaluateTC(date, timeScales)));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGMSTRateFunction(final TimeScale ut1,
                                                      final TimeScales timeScales) {

            // Earth Rotation Angle
            final StellarAngleCapitaine era =
                    new StellarAngleCapitaine(ut1, timeScales.getTAI());

            // Polynomial part of the apparent sidereal time series
            // which is the opposite of Equation of Origins (EO)
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser parser =
                    new PoissonSeriesParser(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PolynomialNutation minusEO = load(GST_SERIES, in -> parser.parse(in, GST_SERIES).getPolynomial());

            // create a function evaluating the series
            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {
                    return era.getRate() + minusEO.derivative(evaluateTC(date, timeScales));
                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return minusEO.derivative(evaluateTC(date, timeScales)).add(era.getRate());
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeScalarFunction getGASTFunction(final TimeScale ut1,
                                                  final EOPHistory eopHistory,
                                                  final TimeScales timeScales) {

            // set up nutation arguments
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales);

            // mean obliquity function
            final TimeScalarFunction epsilon = getMeanObliquityFunction(timeScales);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser baseParser =
                    new PoissonSeriesParser(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);
            final PoissonSeriesParser          gstParser    = baseParser.withPolynomialPart('t', Unit.ARC_SECONDS);
            final PoissonSeries                psiSeries    = load(PSI_SERIES, in -> baseParser.parse(in, PSI_SERIES));
            final PoissonSeries                gstSeries    = load(GST_SERIES, in -> gstParser.parse(in, GST_SERIES));
            final PoissonSeries.CompiledSeries psiGstSeries = PoissonSeries.compile(psiSeries, gstSeries);

            // ERA function
            final TimeScalarFunction era =
                    getEarthOrientationAngleFunction(ut1, timeScales.getTAI());

            return new TimeScalarFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final AbsoluteDate date) {

                    // evaluate equation of origins
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] angles = psiGstSeries.value(elements);
                    final double ddPsi    = (eopHistory == null) ? 0 : eopHistory.getEquinoxNutationCorrection(date)[0];
                    final double deltaPsi = angles[0] + ddPsi;
                    final double epsilonA = epsilon.value(date);

                    // subtract equation of origin from EA
                    // (hence add the series above which have the sign included)
                    return era.value(date) + deltaPsi * FastMath.cos(epsilonA) + angles[1];

                }

                /** {@inheritDoc} */
                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {

                    // evaluate equation of origins
                    final FieldBodiesElements<T> elements = arguments.evaluateAll(date);
                    final T[] angles = psiGstSeries.value(elements);
                    final T ddPsi    = (eopHistory == null) ? date.getField().getZero() : eopHistory.getEquinoxNutationCorrection(date)[0];
                    final T deltaPsi = angles[0].add(ddPsi);
                    final T epsilonA = epsilon.value(date);

                    // subtract equation of origin from EA
                    // (hence add the series above which have the sign included)
                    return era.value(date).add(deltaPsi.multiply(epsilonA.cos())).add(angles[1]);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeVectorFunction getEOPTidalCorrection(final TimeScales timeScales) {

            // set up nutation arguments
            // BEWARE! Using TT as the time scale here and not UT1 is intentional!
            // as this correction is used to compute UT1 itself, it is not surprising we cannot use UT1 yet,
            // however, using the close UTC as would seem logical make the comparison with interp.f from IERS fail
            // looking in the interp.f code, the same TT scale is used for both Delaunay and gamma argument
            final FundamentalNutationArguments arguments =
                    getNutationArguments(timeScales.getTT(), timeScales);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser xyParser  = new PoissonSeriesParser(13).
                                                  withOptionalColumn(1).
                                                  withGamma(2).
                                                  withFirstDelaunay(3);
            final double microS = 1.0e-6;
            final PoissonSeriesParser ut1Parser = new PoissonSeriesParser(11).
                                                  withOptionalColumn(1).
                                                  withGamma(2).
                                                  withFirstDelaunay(3);
            final PoissonSeries xSeries =
                            load(TIDAL_CORRECTION_XP_YP_SERIES, xpIn -> xyParser.
                                                                        withSinCos(0, 10, microAS, 11, microAS).
                                                                        parse(xpIn, TIDAL_CORRECTION_XP_YP_SERIES));
            final PoissonSeries ySeries =
                            load(TIDAL_CORRECTION_XP_YP_SERIES, ypIn -> xyParser.
                                                                        withSinCos(0, 12, microAS, 13, microAS).
                                                                        parse(ypIn, TIDAL_CORRECTION_XP_YP_SERIES));
            final PoissonSeries ut1Series =
                            load(TIDAL_CORRECTION_UT1_SERIES, ut1In -> ut1Parser.
                                                                       withSinCos(0, 10, microS, 11, microS).
                                                                       parse(ut1In, TIDAL_CORRECTION_UT1_SERIES));

            return new EOPTidalCorrection(arguments, xSeries, ySeries, ut1Series);

        }

        /** {@inheritDoc} */
        @Override
        public double[] getNominalTidalDisplacement() {
            return new double[] {
                // h⁽⁰⁾,                                  h⁽²⁾,   h₃,     hI diurnal, hI semi-diurnal,
                0.6078,                                  -0.0006, 0.292, -0.0025,    -0.0022,
                // l⁽⁰⁾, l⁽¹⁾ diurnal, l⁽¹⁾ semi-diurnal, l⁽²⁾,   l₃,     lI diurnal, lI semi-diurnal
                0.0847,  0.0012,       0.0024,            0.0002, 0.015, -0.0007,    -0.0007,
                // H₀
                -0.31460
            };
        }

        /** {@inheritDoc} */
        @Override
        public CompiledSeries getTidalDisplacementFrequencyCorrectionDiurnal() {
            return getTidalDisplacementFrequencyCorrectionDiurnal(TIDAL_DISPLACEMENT_CORRECTION_DIURNAL,
                                                                  18, 15, 16, 17, 18);
        }

        /** {@inheritDoc} */
        @Override
        public CompiledSeries getTidalDisplacementFrequencyCorrectionZonal() {
            return getTidalDisplacementFrequencyCorrectionZonal(TIDAL_DISPLACEMENT_CORRECTION_ZONAL,
                                                                18, 15, 16, 17, 18);
        }

    };

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\p{Space}+");

    /** IERS conventions resources base directory. */
    private static final String IERS_BASE = "/assets/org/orekit/IERS-conventions/";

    /** Get the reference epoch for fundamental nutation arguments.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return reference epoch for fundamental nutation arguments
     * @since 6.1
     * @see #getNutationReferenceEpoch(TimeScales)
     */
    @DefaultDataContext
    public AbsoluteDate getNutationReferenceEpoch() {
        return getNutationReferenceEpoch(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the reference epoch for fundamental nutation arguments.
     *
     * @param timeScales to use for the reference epoch.
     * @return reference epoch for fundamental nutation arguments
     * @since 10.1
     */
    public AbsoluteDate getNutationReferenceEpoch(final TimeScales timeScales) {
        // IERS 1996, IERS 2003 and IERS 2010 use the same J2000.0 reference date
        return timeScales.getJ2000Epoch();
    }

    /** Evaluate the date offset between the current date and the {@link #getNutationReferenceEpoch() reference date}.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param date current date
     * @return date offset in Julian centuries
     * @since 6.1
     * @see #evaluateTC(AbsoluteDate, TimeScales)
     */
    @DefaultDataContext
    public double evaluateTC(final AbsoluteDate date) {
        return evaluateTC(date, DataContext.getDefault().getTimeScales());
    }

    /**
     * Evaluate the date offset between the current date and the {@link
     * #getNutationReferenceEpoch() reference date}.
     *
     * @param date       current date
     * @param timeScales used in the evaluation.
     * @return date offset in Julian centuries
     * @since 10.1
     */
    public double evaluateTC(final AbsoluteDate date, final TimeScales timeScales) {
        return date.durationFrom(getNutationReferenceEpoch(timeScales)) /
                Constants.JULIAN_CENTURY;
    }

    /** Evaluate the date offset between the current date and the {@link #getNutationReferenceEpoch() reference date}.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param date current date
     * @param <T> type of the field elements
     * @return date offset in Julian centuries
     * @since 9.0
     * @see #evaluateTC(FieldAbsoluteDate, TimeScales)
     */
    @DefaultDataContext
    public <T extends CalculusFieldElement<T>> T evaluateTC(final FieldAbsoluteDate<T> date) {
        return evaluateTC(date, DataContext.getDefault().getTimeScales());
    }

    /** Evaluate the date offset between the current date and the {@link #getNutationReferenceEpoch() reference date}.
     * @param <T> type of the field elements
     * @param date current date
     * @param timeScales used in the evaluation.
     * @return date offset in Julian centuries
     * @since 10.1
     */
    public <T extends CalculusFieldElement<T>> T evaluateTC(final FieldAbsoluteDate<T> date,
                                                        final TimeScales timeScales) {
        return date.durationFrom(getNutationReferenceEpoch(timeScales))
                .divide(Constants.JULIAN_CENTURY);
    }

    /**
     * Get the fundamental nutation arguments. Does not compute GMST based values: gamma,
     * gammaDot.
     *
     * @param timeScales other time scales used in the computation including TAI and TT.
     * @return fundamental nutation arguments
     * @see #getNutationArguments(TimeScale, TimeScales)
     * @since 10.1
     */
    protected FundamentalNutationArguments getNutationArguments(
            final TimeScales timeScales) {

        return getNutationArguments(null, timeScales);
    }

    /** Get the fundamental nutation arguments.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param timeScale time scale for computing Greenwich Mean Sidereal Time
     * (typically {@link TimeScales#getUT1(IERSConventions, boolean) UT1})
     * @return fundamental nutation arguments
     * @since 6.1
     * @see #getNutationArguments(TimeScale, TimeScales)
     * @see #getNutationArguments(TimeScales)
     */
    @DefaultDataContext
    public FundamentalNutationArguments getNutationArguments(final TimeScale timeScale) {
        return getNutationArguments(timeScale, DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the fundamental nutation arguments.
     *
     * @param timeScale time scale for computing Greenwich Mean Sidereal Time (typically
     *                  {@link TimeScales#getUT1(IERSConventions, boolean) UT1})
     * @param timeScales other time scales used in the computation including TAI and TT.
     * @return fundamental nutation arguments
     * @since 10.1
     */
    public abstract FundamentalNutationArguments getNutationArguments(
            TimeScale timeScale,
            TimeScales timeScales);

    /** Get the function computing mean obliquity of the ecliptic.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return function computing mean obliquity of the ecliptic
     * @since 6.1
     * @see #getMeanObliquityFunction(TimeScales)
     */
    @DefaultDataContext
    public TimeScalarFunction getMeanObliquityFunction() {
        return getMeanObliquityFunction(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the function computing mean obliquity of the ecliptic.
     *
     * @param timeScales used in computing the function.
     * @return function computing mean obliquity of the ecliptic
     * @since 10.1
     */
    public abstract TimeScalarFunction getMeanObliquityFunction(TimeScales timeScales);

    /** Get the function computing the Celestial Intermediate Pole and Celestial Intermediate Origin components.
     * <p>
     * The returned function computes the two X, Y components of CIP and the S+XY/2 component of the non-rotating CIO.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return function computing the Celestial Intermediate Pole and Celestial Intermediate Origin components
     * @since 6.1
     * @see #getXYSpXY2Function(TimeScales)
     */
    @DefaultDataContext
    public TimeVectorFunction getXYSpXY2Function() {
        return getXYSpXY2Function(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the function computing the Celestial Intermediate Pole and Celestial
     * Intermediate Origin components.
     * <p>
     * The returned function computes the two X, Y components of CIP and the S+XY/2
     * component of the non-rotating CIO.
     * </p>
     *
     * @param timeScales used to define the function.
     * @return function computing the Celestial Intermediate Pole and Celestial
     * Intermediate Origin components
     * @since 10.1
     */
    public abstract TimeVectorFunction getXYSpXY2Function(TimeScales timeScales);

    /** Get the function computing the raw Earth Orientation Angle.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * <p>
     * The raw angle does not contain any correction. If for example dTU1 correction
     * due to tidal effect is desired, it must be added afterward by the caller.
     * The returned value contain the angle as the value and the angular rate as
     * the first derivative.
     * </p>
     * @param ut1 UT1 time scale
     * @return function computing the rawEarth Orientation Angle, in the non-rotating origin paradigm
     * @since 6.1
     * @see #getEarthOrientationAngleFunction(TimeScale, TimeScale)
     */
    @DefaultDataContext
    public TimeScalarFunction getEarthOrientationAngleFunction(final TimeScale ut1) {
        return getEarthOrientationAngleFunction(ut1,
                DataContext.getDefault().getTimeScales().getTAI());
    }

    /** Get the function computing the raw Earth Orientation Angle.
     * <p>
     * The raw angle does not contain any correction. If for example dTU1 correction
     * due to tidal effect is desired, it must be added afterward by the caller.
     * The returned value contain the angle as the value and the angular rate as
     * the first derivative.
     * </p>
     * @param ut1 UT1 time scale
     * @param tai TAI time scale
     * @return function computing the rawEarth Orientation Angle, in the non-rotating origin paradigm
     * @since 10.1
     */
    public TimeScalarFunction getEarthOrientationAngleFunction(final TimeScale ut1,
                                                               final TimeScale tai) {
        return new StellarAngleCapitaine(ut1, tai);
    }

    /** Get the function computing the precession angles.
     * <p>
     * The function returned computes the three precession angles
     * ψ<sub>A</sub> (around Z axis), ω<sub>A</sub> (around X axis)
     * and χ<sub>A</sub> (around Z axis). The constant angle ε₀
     * for the fourth rotation (around X axis) can be retrieved by evaluating the
     * function returned by {@link #getMeanObliquityFunction()} at {@link
     * #getNutationReferenceEpoch() nutation reference epoch}.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return function computing the precession angle
     * @since 6.1
     * @see #getPrecessionFunction(TimeScales)
     */
    @DefaultDataContext
    public TimeVectorFunction getPrecessionFunction()
    {
        return getPrecessionFunction(DataContext.getDefault().getTimeScales());
    }

    /** Get the function computing the precession angles.
     * <p>
     * The function returned computes the three precession angles
     * ψ<sub>A</sub> (around Z axis), ω<sub>A</sub> (around X axis)
     * and χ<sub>A</sub> (around Z axis). The constant angle ε₀
     * for the fourth rotation (around X axis) can be retrieved by evaluating the
     * function returned by {@link #getMeanObliquityFunction()} at {@link
     * #getNutationReferenceEpoch() nutation reference epoch}.
     * </p>
     * @return function computing the precession angle
     * @since 10.1
     * @param timeScales used to define the function.
     */
    public abstract TimeVectorFunction getPrecessionFunction(TimeScales timeScales);

    /** Get the function computing the nutation angles.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * <p>
     * The function returned computes the two classical angles ΔΨ and Δε,
     * and the correction to the equation of equinoxes introduced since 1997-02-27 by IAU 1994
     * resolution C7 (the correction is forced to 0 before this date)
     * </p>
     * @return function computing the nutation in longitude ΔΨ and Δε
     * and the correction of equation of equinoxes
     * @since 6.1
     */
    @DefaultDataContext
    public TimeVectorFunction getNutationFunction() {
        return getNutationFunction(DataContext.getDefault().getTimeScales());
    }

    /** Get the function computing the nutation angles.
     * <p>
     * The function returned computes the two classical angles ΔΨ and Δε,
     * and the correction to the equation of equinoxes introduced since 1997-02-27 by IAU 1994
     * resolution C7 (the correction is forced to 0 before this date)
     * </p>
     * @return function computing the nutation in longitude ΔΨ and Δε
     * and the correction of equation of equinoxes
     * @param timeScales used in the computation including TAI and TT.
     * @since 10.1
     */
    public abstract TimeVectorFunction getNutationFunction(TimeScales timeScales);

    /** Get the function computing Greenwich mean sidereal time, in radians.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param ut1 UT1 time scale
     * @return function computing Greenwich mean sidereal time
     * @since 6.1
     * @see #getGMSTFunction(TimeScale, TimeScales)
     */
    @DefaultDataContext
    public TimeScalarFunction getGMSTFunction(final TimeScale ut1) {
        return getGMSTFunction(ut1, DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the function computing Greenwich mean sidereal time, in radians.
     *
     * @param ut1 UT1 time scale
     * @param timeScales other time scales used in the computation including TAI and TT.
     * @return function computing Greenwich mean sidereal time
     * @since 10.1
     */
    public abstract TimeScalarFunction getGMSTFunction(TimeScale ut1,
                                                       TimeScales timeScales);

    /** Get the function computing Greenwich mean sidereal time rate, in radians per second.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param ut1 UT1 time scale
     * @return function computing Greenwich mean sidereal time rate
     * @since 9.0
     * @see #getGMSTRateFunction(TimeScale, TimeScales)
     */
    @DefaultDataContext
    public TimeScalarFunction getGMSTRateFunction(final TimeScale ut1) {
        return getGMSTRateFunction(ut1,
                DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the function computing Greenwich mean sidereal time rate, in radians per
     * second.
     *
     * @param ut1 UT1 time scale
     * @param timeScales other time scales used in the computation including TAI and TT.
     * @return function computing Greenwich mean sidereal time rate
     * @since 10.1
     */
    public abstract TimeScalarFunction getGMSTRateFunction(TimeScale ut1,
                                                           TimeScales timeScales);

    /**
     * Get the function computing Greenwich apparent sidereal time, in radians.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context} if
     * {@code eopHistory == null}.
     *
     * @param ut1        UT1 time scale
     * @param eopHistory EOP history. If {@code null} then no nutation correction is
     *                   applied for EOP.
     * @return function computing Greenwich apparent sidereal time
     * @since 6.1
     * @see #getGASTFunction(TimeScale, EOPHistory, TimeScales)
     */
    @DefaultDataContext
    public TimeScalarFunction getGASTFunction(final TimeScale ut1,
                                              final EOPHistory eopHistory) {
        final TimeScales timeScales = eopHistory != null ?
                eopHistory.getTimeScales() :
                DataContext.getDefault().getTimeScales();
        return getGASTFunction(ut1, eopHistory, timeScales);
    }

    /**
     * Get the function computing Greenwich apparent sidereal time, in radians.
     *
     * @param ut1        UT1 time scale
     * @param eopHistory EOP history. If {@code null} then no nutation correction is
     *                   applied for EOP.
     * @param timeScales        TAI time scale.
     * @return function computing Greenwich apparent sidereal time
     * @since 10.1
     */
    public abstract TimeScalarFunction getGASTFunction(TimeScale ut1,
                                                       EOPHistory eopHistory,
                                                       TimeScales timeScales);

    /** Get the function computing tidal corrections for Earth Orientation Parameters.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return function computing tidal corrections for Earth Orientation Parameters,
     * for xp, yp, ut1 and lod respectively
     * @since 6.1
     * @see #getEOPTidalCorrection(TimeScales)
     */
    @DefaultDataContext
    public TimeVectorFunction getEOPTidalCorrection() {
        return getEOPTidalCorrection(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the function computing tidal corrections for Earth Orientation Parameters.
     *
     * @param timeScales used in the computation. The TT and TAI scales are used.
     * @return function computing tidal corrections for Earth Orientation Parameters, for
     * xp, yp, ut1 and lod respectively
     * @since 10.1
     */
    public abstract TimeVectorFunction getEOPTidalCorrection(TimeScales timeScales);

    /** Get the Love numbers.
     * @return Love numbers
          * @since 6.1
     */
    public abstract LoveNumbers getLoveNumbers();

    /** Get the function computing frequency dependent terms (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param ut1 UT1 time scale
     * @return frequency dependence model for tides computation (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
     * @since 6.1
     * @see #getTideFrequencyDependenceFunction(TimeScale, TimeScales)
     */
    @DefaultDataContext
    public TimeVectorFunction getTideFrequencyDependenceFunction(final TimeScale ut1) {
        return getTideFrequencyDependenceFunction(ut1,
                DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the function computing frequency dependent terms (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂,
     * ΔS₂₂).
     *
     * @param ut1 UT1 time scale
     * @param timeScales other time scales used in the computation including TAI and TT.
     * @return frequency dependence model for tides computation (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂,
     * ΔS₂₂).
     * @since 10.1
     */
    public abstract TimeVectorFunction getTideFrequencyDependenceFunction(
            TimeScale ut1,
            TimeScales timeScales);

    /** Get the permanent tide to be <em>removed</em> from ΔC₂₀ when zero-tide potentials are used.
     * @return permanent tide to remove
     */
    public abstract double getPermanentTide();

    /** Get the function computing solid pole tide (ΔC₂₁, ΔS₂₁).
     * @param eopHistory EOP history
     * @return model for solid pole tide (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
          * @since 6.1
     */
    public abstract TimeVectorFunction getSolidPoleTide(EOPHistory eopHistory);

    /** Get the function computing ocean pole tide (ΔC₂₁, ΔS₂₁).
     * @param eopHistory EOP history
     * @return model for ocean pole tide (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
          * @since 6.1
     */
    public abstract TimeVectorFunction getOceanPoleTide(EOPHistory eopHistory);

    /** Get the nominal values of the displacement numbers.
     * @return an array containing h⁽⁰⁾, h⁽²⁾, h₃, hI diurnal, hI semi-diurnal,
     * l⁽⁰⁾, l⁽¹⁾ diurnal, l⁽¹⁾ semi-diurnal, l⁽²⁾, l₃, lI diurnal, lI semi-diurnal,
     * H₀ permanent deformation amplitude
     * @since 9.1
     */
    public abstract double[] getNominalTidalDisplacement();

    /** Get the correction function for tidal displacement for diurnal tides.
     * <ul>
     *  <li>f[0]: radial correction, longitude cosine part</li>
     *  <li>f[1]: radial correction, longitude sine part</li>
     *  <li>f[2]: North correction, longitude cosine part</li>
     *  <li>f[3]: North correction, longitude sine part</li>
     *  <li>f[4]: East correction, longitude cosine part</li>
     *  <li>f[5]: East correction, longitude sine part</li>
     * </ul>
     * @return correction function for tidal displacement
     * @since 9.1
     */
    public abstract CompiledSeries getTidalDisplacementFrequencyCorrectionDiurnal();

    /** Get the correction function for tidal displacement for diurnal tides.
     * <ul>
     *  <li>f[0]: radial correction, longitude cosine part</li>
     *  <li>f[1]: radial correction, longitude sine part</li>
     *  <li>f[2]: North correction, longitude cosine part</li>
     *  <li>f[3]: North correction, longitude sine part</li>
     *  <li>f[4]: East correction, longitude cosine part</li>
     *  <li>f[5]: East correction, longitude sine part</li>
     * </ul>
     * @param tableName name for the diurnal tides table
     * @param cols total number of columns of the diurnal tides table
     * @param rIp column holding ∆Rf(ip) in the diurnal tides table, counting from 1
     * @param rOp column holding ∆Rf(op) in the diurnal tides table, counting from 1
     * @param tIp column holding ∆Tf(ip) in the diurnal tides table, counting from 1
     * @param tOp column holding ∆Tf(op) in the diurnal tides table, counting from 1
     * @return correction function for tidal displacement for diurnal tides
          * @since 9.1
     */
    protected static CompiledSeries getTidalDisplacementFrequencyCorrectionDiurnal(final String tableName, final int cols,
                                                                                   final int rIp, final int rOp,
                                                                                   final int tIp, final int tOp) {

        // radial component, missing the sin 2φ factor; this corresponds to:
        //  - equation 15a in IERS conventions 1996, chapter 7
        //  - equation 16a in IERS conventions 2003, chapter 7
        //  - equation 7.12a in IERS conventions 2010, chapter 7
        final PoissonSeries drCos = load(tableName, in -> new PoissonSeriesParser(cols).
                                                          withOptionalColumn(1).
                                                          withDoodson(4, 3).
                                                          withFirstDelaunay(10).
                                                          withSinCos(0, rIp, +1.0e-3, rOp, +1.0e-3).
                                                          parse(in, tableName));
        final PoissonSeries drSin = load(tableName, in -> new PoissonSeriesParser(cols).
                                                          withOptionalColumn(1).
                                                          withDoodson(4, 3).
                                                          withFirstDelaunay(10).
                                                          withSinCos(0, rOp, -1.0e-3, rIp, +1.0e-3).
                                                          parse(in, tableName));

        // North component, missing the cos 2φ factor; this corresponds to:
        //  - equation 15b in IERS conventions 1996, chapter 7
        //  - equation 16b in IERS conventions 2003, chapter 7
        //  - equation 7.12b in IERS conventions 2010, chapter 7
        final PoissonSeries dnCos = load(tableName, in -> new PoissonSeriesParser(cols).
                                                          withOptionalColumn(1).
                                                          withDoodson(4, 3).
                                                          withFirstDelaunay(10).
                                                          withSinCos(0, tIp, +1.0e-3, tOp, +1.0e-3).
                                                          parse(in, tableName));
        final PoissonSeries dnSin = load(tableName, in -> new PoissonSeriesParser(cols).
                                                          withOptionalColumn(1).
                                                          withDoodson(4, 3).
                                                          withFirstDelaunay(10).
                                                          withSinCos(0, tOp, -1.0e-3, tIp, +1.0e-3).
                                                          parse(in, tableName));

        // East component, missing the sin φ factor; this corresponds to:
        //  - equation 15b in IERS conventions 1996, chapter 7
        //  - equation 16b in IERS conventions 2003, chapter 7
        //  - equation 7.12b in IERS conventions 2010, chapter 7
        final PoissonSeries deCos = load(tableName, in -> new PoissonSeriesParser(cols).
                                                          withOptionalColumn(1).
                                                          withDoodson(4, 3).
                                                          withFirstDelaunay(10).
                                                          withSinCos(0, tOp, -1.0e-3, tIp, +1.0e-3).
                                                          parse(in, tableName));
        final PoissonSeries deSin = load(tableName, in -> new PoissonSeriesParser(cols).
                                                          withOptionalColumn(1).
                                                          withDoodson(4, 3).
                                                          withFirstDelaunay(10).
                                                          withSinCos(0, tIp, -1.0e-3, tOp, -1.0e-3).
                                                          parse(in, tableName));

        return PoissonSeries.compile(drCos, drSin, dnCos, dnSin, deCos, deSin);

    }

    /** Get the correction function for tidal displacement for zonal tides.
     * <ul>
     *  <li>f[0]: radial correction</li>
     *  <li>f[1]: North correction</li>
     * </ul>
     * @return correction function for tidal displacement
     * @since 9.1
     */
    public abstract CompiledSeries getTidalDisplacementFrequencyCorrectionZonal();

    /** Get the correction function for tidal displacement for zonal tides.
     * <ul>
     *  <li>f[0]: radial correction</li>
     *  <li>f[1]: North correction</li>
     * </ul>
     * @param tableName name for the zonal tides table
     * @param cols total number of columns of the table
     * @param rIp column holding ∆Rf(ip) in the table, counting from 1
     * @param rOp column holding ∆Rf(op) in the table, counting from 1
     * @param tIp column holding ∆Tf(ip) in the table, counting from 1
     * @param tOp column holding ∆Tf(op) in the table, counting from 1
     * @return correction function for tidal displacement for zonal tides
          * @since 9.1
     */
    protected static CompiledSeries getTidalDisplacementFrequencyCorrectionZonal(final String tableName, final int cols,
                                                                                 final int rIp, final int rOp,
                                                                                 final int tIp, final int tOp) {

        // radial component, missing the 3⁄2 sin² φ - 1⁄2 factor; this corresponds to:
        //  - equation 16a in IERS conventions 1996, chapter 7
        //  - equation 17a in IERS conventions 2003, chapter 7
        //  - equation 7.13a in IERS conventions 2010, chapter 7
        final PoissonSeries dr = load(tableName, in -> new PoissonSeriesParser(cols).
                                                       withOptionalColumn(1).
                                                       withDoodson(4, 3).
                                                       withFirstDelaunay(10).
                                                       withSinCos(0, rOp, +1.0e-3, rIp, +1.0e-3).
                                                       parse(in, tableName));

        // North component, missing the sin 2φ factor; this corresponds to:
        //  - equation 16b in IERS conventions 1996, chapter 7
        //  - equation 17b in IERS conventions 2003, chapter 7
        //  - equation 7.13b in IERS conventions 2010, chapter 7
        final PoissonSeries dn = load(tableName, in -> new PoissonSeriesParser(cols).
                                                       withOptionalColumn(1).
                                                       withDoodson(4, 3).
                                                       withFirstDelaunay(10).
                                                       withSinCos(0, tOp, +1.0e-3, tIp, +1.0e-3).
                                                       parse(in, tableName));

        return PoissonSeries.compile(dr, dn);

    }

    /** Interface for functions converting nutation corrections between
     * δΔψ/δΔε to δX/δY.
     * <ul>
     * <li>δΔψ/δΔε nutation corrections are used with the equinox-based paradigm.</li>
     * <li>δX/δY nutation corrections are used with the Non-Rotating Origin paradigm.</li>
     * </ul>
     * @since 6.1
     */
    public interface NutationCorrectionConverter {

        /** Convert nutation corrections.
         * @param date current date
         * @param ddPsi δΔψ part of the nutation correction
         * @param ddEpsilon δΔε part of the nutation correction
         * @return array containing δX and δY
         */
        double[] toNonRotating(AbsoluteDate date, double ddPsi, double ddEpsilon);

        /** Convert nutation corrections.
         * @param date current date
         * @param dX δX part of the nutation correction
         * @param dY δY part of the nutation correction
         * @return array containing δΔψ and δΔε
         */
        double[] toEquinox(AbsoluteDate date, double dX, double dY);

    }

    /** Create a function converting nutation corrections between
     * δX/δY and δΔψ/δΔε.
     * <ul>
     * <li>δX/δY nutation corrections are used with the Non-Rotating Origin paradigm.</li>
     * <li>δΔψ/δΔε nutation corrections are used with the equinox-based paradigm.</li>
     * </ul>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return a new converter
     * @since 6.1
     * @see #getNutationCorrectionConverter(TimeScales)
     */
    @DefaultDataContext
    public NutationCorrectionConverter getNutationCorrectionConverter() {
        return getNutationCorrectionConverter(DataContext.getDefault().getTimeScales());
    }

    /** Create a function converting nutation corrections between
     * δX/δY and δΔψ/δΔε.
     * <ul>
     * <li>δX/δY nutation corrections are used with the Non-Rotating Origin paradigm.</li>
     * <li>δΔψ/δΔε nutation corrections are used with the equinox-based paradigm.</li>
     * </ul>
     * @return a new converter
     * @since 10.1
     * @param timeScales used to define the conversion.
     */
    public NutationCorrectionConverter getNutationCorrectionConverter(
            final TimeScales timeScales) {

        // get models parameters
        final TimeVectorFunction precessionFunction = getPrecessionFunction(timeScales);
        final TimeScalarFunction epsilonAFunction = getMeanObliquityFunction(timeScales);
        final AbsoluteDate date0 = getNutationReferenceEpoch(timeScales);
        final double cosE0 = FastMath.cos(epsilonAFunction.value(date0));

        return new NutationCorrectionConverter() {

            /** {@inheritDoc} */
            @Override
            public double[] toNonRotating(final AbsoluteDate date,
                                          final double ddPsi, final double ddEpsilon) {
                // compute precession angles psiA, omegaA and chiA
                final double[] angles = precessionFunction.value(date);

                // conversion coefficients
                final double sinEA = FastMath.sin(epsilonAFunction.value(date));
                final double c     = angles[0] * cosE0 - angles[2];

                // convert nutation corrections (equation 23/IERS-2003 or 5.25/IERS-2010)
                return new double[] {
                    sinEA * ddPsi + c * ddEpsilon,
                    ddEpsilon - c * sinEA * ddPsi
                };

            }

            /** {@inheritDoc} */
            @Override
            public double[] toEquinox(final AbsoluteDate date,
                                      final double dX, final double dY) {
                // compute precession angles psiA, omegaA and chiA
                final double[] angles   = precessionFunction.value(date);

                // conversion coefficients
                final double sinEA = FastMath.sin(epsilonAFunction.value(date));
                final double c     = angles[0] * cosE0 - angles[2];
                final double opC2  = 1 + c * c;

                // convert nutation corrections (inverse of equation 23/IERS-2003 or 5.25/IERS-2010)
                return new double[] {
                    (dX - c * dY) / (sinEA * opC2),
                    (dY + c * dX) / opC2
                };
            }

        };

    }

    /** Load the Love numbers.
     * @param nameLove name of the Love number resource
     * @return Love numbers
     */
    protected LoveNumbers loadLoveNumbers(final String nameLove) {
        try {

            // allocate the three triangular arrays for real, imaginary and time-dependent numbers
            final double[][] real      = new double[4][];
            final double[][] imaginary = new double[4][];
            final double[][] plus      = new double[4][];
            for (int i = 0; i < real.length; ++i) {
                real[i]      = new double[i + 1];
                imaginary[i] = new double[i + 1];
                plus[i]      = new double[i + 1];
            }

            try (InputStream stream = IERSConventions.class.getResourceAsStream(nameLove)) {

                if (stream == null) {
                    // this should never happen with files embedded within Orekit
                    throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, nameLove);
                }

                int lineNumber = 1;
                String line = null;
                // setup the reader
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                    line = reader.readLine();

                    // look for the Love numbers
                    while (line != null) {

                        line = line.trim();
                        if (!(line.isEmpty() || line.startsWith("#"))) {
                            final String[] fields = SEPARATOR.split(line);
                            if (fields.length != 5) {
                                // this should never happen with files embedded within Orekit
                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, nameLove, line);
                            }
                            final int n = Integer.parseInt(fields[0]);
                            final int m = Integer.parseInt(fields[1]);
                            if (n < 2 || n > 3 || m < 0 || m > n) {
                                // this should never happen with files embedded within Orekit
                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, nameLove, line);

                            }
                            real[n][m]      = Double.parseDouble(fields[2]);
                            imaginary[n][m] = Double.parseDouble(fields[3]);
                            plus[n][m]      = Double.parseDouble(fields[4]);
                        }

                        // next line
                        lineNumber++;
                        line = reader.readLine();

                    }
                } catch (NumberFormatException nfe) {
                    // this should never happen with files embedded within Orekit
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, nameLove, line);
                }
            }

            return new LoveNumbers(real, imaginary, plus);

        } catch (IOException ioe) {
            // this should never happen with files embedded within Orekit
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, nameLove);
        }
    }

    /** Load resources.
     * @param name name of the resource
     * @param loader loaader for the resource
     * @param <T> type of the processed data
     * @return processed data
     */
    private static <T> T load(final String name, final Function<InputStream, T> loader) {
        try (InputStream is = IERSConventions.class.getResourceAsStream(name)) {
            return loader.apply(is);
        } catch (IOException ioe) {
            // this should never happen with internal streams
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR, ioe);
        }
    }

    /** Correction to equation of equinoxes.
     * <p>IAU 1994 resolution C7 added two terms to the equation of equinoxes
     * taking effect since 1997-02-27 for continuity
     * </p>
     */
    private static class IAU1994ResolutionC7 {

        /** First Moon correction term for the Equation of the Equinoxes. */
        private static final double EQE1 =     0.00264  * Constants.ARC_SECONDS_TO_RADIANS;

        /** Second Moon correction term for the Equation of the Equinoxes. */
        private static final double EQE2 =     0.000063 * Constants.ARC_SECONDS_TO_RADIANS;


        /** Evaluate the correction.
         * @param arguments Delaunay for nutation
         * @param tai TAI time scale.
         * @return correction value (0 before 1997-02-27)
         */
        public static double value(final DelaunayArguments arguments,
                                   final TimeScale tai) {
            /* Start date for applying Moon corrections to the equation of the equinoxes.
             * This date corresponds to 1997-02-27T00:00:00 UTC, hence the 30s offset from TAI.
             */
            final AbsoluteDate modelStart = new AbsoluteDate(1997, 2, 27, 0, 0, 30, tai);
            if (arguments.getDate().compareTo(modelStart) >= 0) {

                // IAU 1994 resolution C7 added two terms to the equation of equinoxes
                // taking effect since 1997-02-27 for continuity

                // Mean longitude of the ascending node of the Moon
                final double om = arguments.getOmega();

                // add the two correction terms
                return EQE1 * FastMath.sin(om) + EQE2 * FastMath.sin(om + om);

            } else {
                return 0.0;
            }
        }

        /** Evaluate the correction.
         * @param arguments Delaunay for nutation
         * @param tai TAI time scale.
         * @param <T> type of the field elements
         * @return correction value (0 before 1997-02-27)
         */
        public static <T extends CalculusFieldElement<T>> T value(
                final FieldDelaunayArguments<T> arguments,
                final TimeScale tai) {
            /* Start date for applying Moon corrections to the equation of the equinoxes.
             * This date corresponds to 1997-02-27T00:00:00 UTC, hence the 30s offset from TAI.
             */
            final AbsoluteDate modelStart = new AbsoluteDate(1997, 2, 27, 0, 0, 30, tai);
            if (arguments.getDate().toAbsoluteDate().compareTo(modelStart) >= 0) {

                // IAU 1994 resolution C7 added two terms to the equation of equinoxes
                // taking effect since 1997-02-27 for continuity

                // Mean longitude of the ascending node of the Moon
                final T om = arguments.getOmega();

                // add the two correction terms
                return om.sin().multiply(EQE1).add(om.add(om).sin().multiply(EQE2));

            } else {
                return arguments.getDate().getField().getZero();
            }
        }

    };

    /** Stellar angle model.
     * <p>
     * The stellar angle computed here has been defined in the paper "A non-rotating origin on the
     * instantaneous equator: Definition, properties and use", N. Capitaine, Guinot B., and Souchay J.,
     * Celestial Mechanics, Volume 39, Issue 3, pp 283-307. It has been proposed as a conventional
     * conventional relationship between UT1 and Earth rotation in the paper "Definition of the Celestial
     * Ephemeris origin and of UT1 in the International Celestial Reference Frame", Capitaine, N.,
     * Guinot, B., and McCarthy, D. D., 2000, Astronomy and Astrophysics, 355(1), pp. 398–405.
     * </p>
     * <p>
     * It is presented simply as stellar angle in IERS conventions 1996 but as since been adopted as
     * the conventional relationship defining UT1 from ICRF and is called Earth Rotation Angle in
     * IERS conventions 2003 and 2010.
     * </p>
     */
    private static class StellarAngleCapitaine implements TimeScalarFunction {

        /** Constant term of Capitaine's Earth Rotation Angle model. */
        private static final double ERA_0   = MathUtils.TWO_PI * 0.7790572732640;

        /** Rate term of Capitaine's Earth Rotation Angle model.
         * (radians per day, main part) */
        private static final double ERA_1A  = MathUtils.TWO_PI / Constants.JULIAN_DAY;

        /** Rate term of Capitaine's Earth Rotation Angle model.
         * (radians per day, fractional part) */
        private static final double ERA_1B  = ERA_1A * 0.00273781191135448;

        /** UT1 time scale. */
        private final TimeScale ut1;

        /** Reference date of Capitaine's Earth Rotation Angle model. */
        private final AbsoluteDate referenceDate;

        /** Simple constructor.
         * @param ut1 UT1 time scale
         * @param tai TAI time scale
         */
        StellarAngleCapitaine(final TimeScale ut1, final TimeScale tai) {
            this.ut1 = ut1;
            referenceDate = new AbsoluteDate(
                    DateComponents.J2000_EPOCH,
                    TimeComponents.H12,
                    tai);
        }

        /** Get the rotation rate.
         * @return rotation rate
         */
        public double getRate() {
            return ERA_1A + ERA_1B;
        }

        /** {@inheritDoc} */
        @Override
        public double value(final AbsoluteDate date) {

            // split the date offset as a full number of days plus a smaller part
            final int secondsInDay = 86400;
            final double dt  = date.durationFrom(referenceDate);
            final long days  = ((long) dt) / secondsInDay;
            final double dtA = secondsInDay * days;
            final double dtB = (dt - dtA) + ut1.offsetFromTAI(date);

            return ERA_0 + ERA_1A * dtB + ERA_1B * (dtA + dtB);

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {

            // split the date offset as a full number of days plus a smaller part
            final int secondsInDay = 86400;
            final T dt  = date.durationFrom(referenceDate);
            final long days  = ((long) dt.getReal()) / secondsInDay;
            final double dtA = secondsInDay * days;
            final T dtB = dt.subtract(dtA).add(ut1.offsetFromTAI(date.toAbsoluteDate()));

            return dtB.add(dtA).multiply(ERA_1B).add(dtB.multiply(ERA_1A)).add(ERA_0);

        }

    }

    /** Mean pole. */
    private static class MeanPole implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131028l;

        /** Date. */
        private final AbsoluteDate date;

        /** X coordinate. */
        private double x;

        /** Y coordinate. */
        private double y;

        /** Simple constructor.
         * @param date date
         * @param x x coordinate
         * @param y y coordinate
         */
        MeanPole(final AbsoluteDate date, final double x, final double y) {
            this.date = date;
            this.x    = x;
            this.y    = y;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** Get x coordinate.
         * @return x coordinate
         */
        public double getX() {
            return x;
        }

        /** Get y coordinate.
         * @return y coordinate
         */
        public double getY() {
            return y;
        }

    }

    /** Local class for precession function. */
    private class PrecessionFunction implements TimeVectorFunction {

        /** Polynomial nutation for psiA. */
        private final PolynomialNutation psiA;

        /** Polynomial nutation for omegaA. */
        private final PolynomialNutation omegaA;

        /** Polynomial nutation for chiA. */
        private final PolynomialNutation chiA;

        /** Time scales to use. */
        private final TimeScales timeScales;

        /** Simple constructor.
         * @param psiA polynomial nutation for psiA
         * @param omegaA polynomial nutation for omegaA
         * @param chiA polynomial nutation for chiA
         * @param timeScales used in the computation.
         */
        PrecessionFunction(final PolynomialNutation psiA,
                           final PolynomialNutation omegaA,
                           final PolynomialNutation chiA,
                           final TimeScales timeScales) {
            this.psiA   = psiA;
            this.omegaA = omegaA;
            this.chiA   = chiA;
            this.timeScales = timeScales;
        }


        /** {@inheritDoc} */
        @Override
        public double[] value(final AbsoluteDate date) {
            final double tc = evaluateTC(date, timeScales);
            return new double[] {
                psiA.value(tc), omegaA.value(tc), chiA.value(tc)
            };
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
            final T[] a = MathArrays.buildArray(date.getField(), 3);
            final T tc = evaluateTC(date, timeScales);
            a[0] = psiA.value(tc);
            a[1] = omegaA.value(tc);
            a[2] = chiA.value(tc);
            return a;
        }

    }

    /** Local class for tides frequency function. */
    private static class TideFrequencyDependenceFunction implements TimeVectorFunction {

        /** Nutation arguments. */
        private final FundamentalNutationArguments arguments;

        /** Correction series. */
        private final PoissonSeries.CompiledSeries kSeries;

        /** Simple constructor.
         * @param arguments nutation arguments
         * @param c20Series correction series for the C20 term
         * @param c21Series correction series for the C21 term
         * @param s21Series correction series for the S21 term
         * @param c22Series correction series for the C22 term
         * @param s22Series correction series for the S22 term
         */
        TideFrequencyDependenceFunction(final FundamentalNutationArguments arguments,
                                        final PoissonSeries c20Series,
                                        final PoissonSeries c21Series,
                                        final PoissonSeries s21Series,
                                        final PoissonSeries c22Series,
                                        final PoissonSeries s22Series) {
            this.arguments = arguments;
            this.kSeries   = PoissonSeries.compile(c20Series, c21Series, s21Series, c22Series, s22Series);
        }


        /** {@inheritDoc} */
        @Override
        public double[] value(final AbsoluteDate date) {
            return kSeries.value(arguments.evaluateAll(date));
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {
            return kSeries.value(arguments.evaluateAll(date));
        }

    }

    /** Local class for EOP tidal corrections. */
    private static class EOPTidalCorrection implements TimeVectorFunction {

        /** Nutation arguments. */
        private final FundamentalNutationArguments arguments;

        /** Correction series. */
        private final PoissonSeries.CompiledSeries correctionSeries;

        /** Simple constructor.
         * @param arguments nutation arguments
         * @param xSeries correction series for the x coordinate
         * @param ySeries correction series for the y coordinate
         * @param ut1Series correction series for the UT1
         */
        EOPTidalCorrection(final FundamentalNutationArguments arguments,
                           final PoissonSeries xSeries,
                           final PoissonSeries ySeries,
                           final PoissonSeries ut1Series) {
            this.arguments        = arguments;
            this.correctionSeries = PoissonSeries.compile(xSeries, ySeries, ut1Series);
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final AbsoluteDate date) {
            final BodiesElements elements = arguments.evaluateAll(date);
            final double[] correction    = correctionSeries.value(elements);
            final double[] correctionDot = correctionSeries.derivative(elements);
            return new double[] {
                correction[0],
                correction[1],
                correction[2],
                correctionDot[2] * (-Constants.JULIAN_DAY)
            };
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> T[] value(final FieldAbsoluteDate<T> date) {

            final FieldBodiesElements<T> elements = arguments.evaluateAll(date);
            final T[] correction    = correctionSeries.value(elements);
            final T[] correctionDot = correctionSeries.derivative(elements);
            final T[] a = MathArrays.buildArray(date.getField(), 4);
            a[0] = correction[0];
            a[1] = correction[1];
            a[2] = correction[2];
            a[3] = correctionDot[2].multiply(-Constants.JULIAN_DAY);
            return a;
        }

    }

}
