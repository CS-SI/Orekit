/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.util.List;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.data.BodiesElements;
import org.orekit.data.DelaunayArguments;
import org.orekit.data.FieldBodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.PoissonSeries;
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
import org.orekit.frames.PoleCorrection;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;


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

        /** {@inheritDoc} */
        @Override
        public FundamentalNutationArguments getNutationArguments(final TimeScale timeScale)
            throws OrekitException {
            return new FundamentalNutationArguments(this, timeScale,
                                                    getStream(NUTATION_ARGUMENTS), NUTATION_ARGUMENTS);
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<Double> getMeanObliquityFunction() throws OrekitException {

            // value from chapter 5, page 22
            final PolynomialNutation<DerivativeStructure> epsilonA =
                    new PolynomialNutation<DerivativeStructure>(84381.448    * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -46.8150   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   -0.00059  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                    0.001813 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeFunction<Double>() {

                /** {@inheritDoc} */
                @Override
                public Double value(final AbsoluteDate date) {
                    return epsilonA.value(evaluateTC(date));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getXYSpXY2Function()
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // X = 2004.3109″t - 0.42665″t² - 0.198656″t³ + 0.0000140″t⁴
            //     + 0.00006″t² cos Ω + sin ε0 { Σ [(Ai + Ai' t) sin(ARGUMENT) + Ai'' t cos(ARGUMENT)]}
            //     + 0.00204″t² sin Ω + 0.00016″t² sin 2(F - D + Ω),
            final PolynomialNutation<DerivativeStructure> xPolynomial =
                    new PolynomialNutation<DerivativeStructure>(0,
                                                                2004.3109 * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.42665  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.198656 * Constants.ARC_SECONDS_TO_RADIANS,
                                                                0.0000140 * Constants.ARC_SECONDS_TO_RADIANS);

            final double fXCosOm    = 0.00006 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fXSinOm    = 0.00204 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fXSin2FDOm = 0.00016 * Constants.ARC_SECONDS_TO_RADIANS;
            final double sinEps0   = FastMath.sin(getMeanObliquityFunction().value(getNutationReferenceEpoch()));

            final double deciMilliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4;
            final PoissonSeriesParser<DerivativeStructure> baseParser =
                    new PoissonSeriesParser<DerivativeStructure>(12).withFirstDelaunay(1);

            final PoissonSeriesParser<DerivativeStructure> xParser =
                    baseParser.
                    withSinCos(0, 7, deciMilliAS, -1, deciMilliAS).
                    withSinCos(1, 8, deciMilliAS,  9, deciMilliAS);
            final PoissonSeries<DerivativeStructure> xSum = xParser.parse(getStream(X_Y_SERIES), X_Y_SERIES);

            // Y = -0.00013″ - 22.40992″t² + 0.001836″t³ + 0.0011130″t⁴
            //     + Σ [(Bi + Bi' t) cos(ARGUMENT) + Bi'' t sin(ARGUMENT)]
            //    - 0.00231″t² cos Ω − 0.00014″t² cos 2(F - D + Ω)
            final PolynomialNutation<DerivativeStructure> yPolynomial =
                    new PolynomialNutation<DerivativeStructure>(-0.00013  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                0.0,
                                                                -22.40992 * Constants.ARC_SECONDS_TO_RADIANS,
                                                                0.001836  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                0.0011130 * Constants.ARC_SECONDS_TO_RADIANS);

            final double fYCosOm    = -0.00231 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fYCos2FDOm = -0.00014 * Constants.ARC_SECONDS_TO_RADIANS;

            final PoissonSeriesParser<DerivativeStructure> yParser =
                    baseParser.
                    withSinCos(0, -1, deciMilliAS, 10, deciMilliAS).
                    withSinCos(1, 12, deciMilliAS, 11, deciMilliAS);
            final PoissonSeries<DerivativeStructure> ySum = yParser.parse(getStream(X_Y_SERIES), X_Y_SERIES);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> xySum =
                    PoissonSeries.compile(xSum, ySum);

            // s = -XY/2 + 0.00385″t - 0.07259″t³ - 0.00264″ sin Ω - 0.00006″ sin 2Ω
            //     + 0.00074″t² sin Ω + 0.00006″t² sin 2(F - D + Ω)
            final double fST          =  0.00385 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST3         = -0.07259 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSSinOm      = -0.00264 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSSin2Om     = -0.00006 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST2SinOm    =  0.00074 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST2Sin2FDOm =  0.00006 * Constants.ARC_SECONDS_TO_RADIANS;

            return new TimeFunction<double[]>() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {

                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] xy             = xySum.value(elements);

                    final double omega     = elements.getOmega();
                    final double f         = elements.getF();
                    final double d         = elements.getD();
                    final double t         = elements.getTC();

                    final double cosOmega  = FastMath.cos(omega);
                    final double sinOmega  = FastMath.sin(omega);
                    final double sin2Omega = FastMath.sin(2 * omega);
                    final double cos2FDOm  = FastMath.cos(2 * (f - d + omega));
                    final double sin2FDOm  = FastMath.sin(2 * (f - d + omega));

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

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getPrecessionFunction() throws OrekitException {

            // set up the conventional polynomials
            // the following values are from Lieske et al. paper:
            // Expressions for the precession quantities based upon the IAU(1976) system of astronomical constants
            // http://articles.adsabs.harvard.edu/full/1977A%26A....58....1L
            // also available as equation 30 in IERS 2003 conventions
            final PolynomialNutation<DerivativeStructure> psiA =
                    new PolynomialNutation<DerivativeStructure>(   0.0,
                                                                5038.7784   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -1.07259  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -0.001147 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation<DerivativeStructure> omegaA =
                    new PolynomialNutation<DerivativeStructure>(getMeanObliquityFunction().value(getNutationReferenceEpoch()),
                                                                 0.0,
                                                                 0.05127   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.007726  * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation<DerivativeStructure> chiA =
                    new PolynomialNutation<DerivativeStructure>( 0.0,
                                                                10.5526   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -2.38064  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.001125 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final double tc = evaluateTC(date);
                    return new double[] {
                        psiA.value(tc), omegaA.value(tc), chiA.value(tc)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getNutationFunction()
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // set up Poisson series
            final double deciMilliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4;
            final PoissonSeriesParser<DerivativeStructure> baseParser =
                    new PoissonSeriesParser<DerivativeStructure>(10).withFirstDelaunay(1);

            final PoissonSeriesParser<DerivativeStructure> psiParser =
                    baseParser.
                    withSinCos(0, 7, deciMilliAS, -1, deciMilliAS).
                    withSinCos(1, 8, deciMilliAS, -1, deciMilliAS);
            final PoissonSeries<DerivativeStructure> psiSeries = psiParser.parse(getStream(PSI_EPSILON_SERIES), PSI_EPSILON_SERIES);

            final PoissonSeriesParser<DerivativeStructure> epsilonParser =
                    baseParser.
                    withSinCos(0, -1, deciMilliAS, 9, deciMilliAS).
                    withSinCos(1, -1, deciMilliAS, 10, deciMilliAS);
            final PoissonSeries<DerivativeStructure> epsilonSeries = epsilonParser.parse(getStream(PSI_EPSILON_SERIES), PSI_EPSILON_SERIES);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> psiEpsilonSeries =
                    PoissonSeries.compile(psiSeries, epsilonSeries);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] psiEpsilon = psiEpsilonSeries.value(elements);
                    return new double[] {
                        psiEpsilon[0], psiEpsilon[1], IAU1994ResolutionC7.value(elements)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getGMSTFunction(final TimeScale ut1)
            throws OrekitException {

            // Radians per second of time
            final double radiansPerSecond = MathUtils.TWO_PI / Constants.JULIAN_DAY;

            // constants from IERS 1996 page 21
            // the underlying model is IAU 1982 GMST-UT1
            final AbsoluteDate gmstReference =
                new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());
            final double gmst0 = 24110.54841;
            final double gmst1 = 8640184.812866;
            final double gmst2 = 0.093104;
            final double gmst3 = -6.2e-6;

            return new TimeFunction<DerivativeStructure>() {

                /** {@inheritDoc} */
                @Override
                public DerivativeStructure value(final AbsoluteDate date) {

                    // offset in Julian centuries from J2000 epoch (UT1 scale)
                    final double dtai = date.durationFrom(gmstReference);
                    final DerivativeStructure tut1 =
                            new DerivativeStructure(1, 1, dtai + ut1.offsetFromTAI(date), 1.0);
                    final DerivativeStructure tt = tut1.divide(Constants.JULIAN_CENTURY);

                    // Seconds in the day, adjusted by 12 hours because the
                    // UT1 is supplied as a Julian date beginning at noon.
                    final DerivativeStructure sd = tut1.add(Constants.JULIAN_DAY / 2).remainder(Constants.JULIAN_DAY);

                    // compute Greenwich mean sidereal time, in radians
                    return tt.multiply(gmst3).add(gmst2).multiply(tt).add(gmst1).multiply(tt).add(gmst0).add(sd).multiply(radiansPerSecond);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getGASTFunction(final TimeScale ut1,
                                                                 final EOPHistory eopHistory)
            throws OrekitException {

            // obliquity
            final TimeFunction<Double> epsilonA = getMeanObliquityFunction();

            // GMST function
            final TimeFunction<DerivativeStructure> gmst = getGMSTFunction(ut1);

            // nutation function
            final TimeFunction<double[]> nutation = getNutationFunction();

            return new TimeFunction<DerivativeStructure>() {

                /** {@inheritDoc} */
                @Override
                public DerivativeStructure value(final AbsoluteDate date) {

                    // compute equation of equinoxes
                    final double[] angles = nutation.value(date);
                    double deltaPsi = angles[0];
                    if (eopHistory != null) {
                        deltaPsi += eopHistory.getEquinoxNutationCorrection(date)[0];
                    }
                    final double eqe = deltaPsi  * FastMath.cos(epsilonA.value(date)) + angles[2];

                    // add mean sidereal time and equation of equinoxes
                    return gmst.value(date).add(eqe);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getEOPTidalCorrection()
            throws OrekitException {

            // set up nutation arguments
            // BEWARE! Using TT as the time scale here and not UT1 is intentional!
            // as this correction is used to compute UT1 itself, it is not surprising we cannot use UT1 yet,
            // however, using the close UTC as would seem logical make the comparison with interp.f from IERS fail
            // looking in the interp.f code, the same TT scale is used for both Delaunay and gamma argument
            final FundamentalNutationArguments arguments = getNutationArguments(TimeScalesFactory.getTT());

            // set up Poisson series
            final double milliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3;
            final PoissonSeriesParser<DerivativeStructure> xyParser = new PoissonSeriesParser<DerivativeStructure>(17).
                    withOptionalColumn(1).
                    withGamma(7).
                    withFirstDelaunay(2);
            final PoissonSeries<DerivativeStructure> xSeries =
                    xyParser.
                    withSinCos(0, 14, milliAS, 15, milliAS).
                    parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES), TIDAL_CORRECTION_XP_YP_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries =
                    xyParser.
                    withSinCos(0, 16, milliAS, 17, milliAS).
                    parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES),
                                                         TIDAL_CORRECTION_XP_YP_SERIES);

            final double deciMilliS = 1.0e-4;
            final PoissonSeriesParser<DerivativeStructure> ut1Parser = new PoissonSeriesParser<DerivativeStructure>(17).
                    withOptionalColumn(1).
                    withGamma(7).
                    withFirstDelaunay(2).
                    withSinCos(0, 16, deciMilliS, 17, deciMilliS);
            final PoissonSeries<DerivativeStructure> ut1Series =
                    ut1Parser.parse(getStream(TIDAL_CORRECTION_UT1_SERIES), TIDAL_CORRECTION_UT1_SERIES);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> correctionSeries =
                PoissonSeries.compile(xSeries, ySeries, ut1Series);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final FieldBodiesElements<DerivativeStructure> elements =
                            arguments.evaluateDerivative(date);
                    final DerivativeStructure[] correction = correctionSeries.value(elements);
                    return new double[] {
                        correction[0].getValue(),
                        correction[1].getValue(),
                        correction[2].getValue(),
                        -correction[2].getPartialDerivative(1) * Constants.JULIAN_DAY
                    };
                }
            };

        }

        /** {@inheritDoc} */
        public LoveNumbers getLoveNumbers() throws OrekitException {
            return loadLoveNumbers(LOVE_NUMBERS);
        }

        /** {@inheritDoc} */
        public TimeFunction<double[]> getTideFrequencyDependenceFunction(final TimeScale ut1)
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(ut1);

            // set up Poisson series
            final PoissonSeriesParser<DerivativeStructure> k20Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);
            final PoissonSeriesParser<DerivativeStructure> k21Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10);
            final PoissonSeriesParser<DerivativeStructure> k22Parser =
                    new PoissonSeriesParser<DerivativeStructure>(16).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);

            final double pico = 1.0e-12;
            final PoissonSeries<DerivativeStructure> c20Series =
                    k20Parser.
                  withSinCos(0, 18, -pico, 16, pico).
                    parse(getStream(K20_FREQUENCY_DEPENDENCE), K20_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> c21Series =
                    k21Parser.
                    withSinCos(0, 17, pico, 18, pico).
                    parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> s21Series =
                    k21Parser.
                    withSinCos(0, 18, -pico, 17, pico).
                    parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> c22Series =
                    k22Parser.
                    withSinCos(0, -1, pico, 16, pico).
                    parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> s22Series =
                    k22Parser.
                    withSinCos(0, 16, -pico, -1, pico).
                    parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> kSeries =
                PoissonSeries.compile(c20Series, c21Series, s21Series, c22Series, s22Series);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    return kSeries.value(arguments.evaluateAll(date));
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public double getPermanentTide() throws OrekitException {
            return 4.4228e-8 * -0.31460 * getLoveNumbers().getReal(2, 0);
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getSolidPoleTide(final EOPHistory eopHistory) {

            // constants from IERS 1996 page 47
            final double globalFactor = -1.348e-9 / Constants.ARC_SECONDS_TO_RADIANS;
            final double coupling     =  0.00112;

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final PoleCorrection pole = eopHistory.getPoleCorrection(date);
                    return new double[] {
                        globalFactor * (pole.getXp() + coupling * pole.getYp()),
                        globalFactor * (coupling * pole.getXp() - pole.getYp()),
                    };
                }
            };
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getOceanPoleTide(final EOPHistory eopHistory)
            throws OrekitException {

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    // there are no model for ocean pole tide prior to conventions 2010
                    return new double[] {
                        0.0, 0.0
                    };
                }
            };
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

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments(final TimeScale timeScale)
            throws OrekitException {
            return new FundamentalNutationArguments(this, timeScale,
                                                    getStream(NUTATION_ARGUMENTS), NUTATION_ARGUMENTS);
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<Double> getMeanObliquityFunction() throws OrekitException {

            // epsilon 0 value from chapter 5, page 41, other terms from equation 32 page 45
            final PolynomialNutation<DerivativeStructure> epsilonA =
                    new PolynomialNutation<DerivativeStructure>(84381.448    * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -46.84024  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   -0.00059  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                    0.001813 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeFunction<Double>() {

                /** {@inheritDoc} */
                @Override
                public Double value(final AbsoluteDate date) {
                    return epsilonA.value(evaluateTC(date));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getXYSpXY2Function()
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withPolynomialPart('t', PolynomialParser.Unit.MICRO_ARC_SECONDS).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);

            final PoissonSeries<DerivativeStructure> xSeries = parser.parse(getStream(X_SERIES), X_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries = parser.parse(getStream(Y_SERIES), Y_SERIES);
            final PoissonSeries<DerivativeStructure> sSeries = parser.parse(getStream(S_SERIES), S_SERIES);
            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> xys = PoissonSeries.compile(xSeries, ySeries, sSeries);

            // create a function evaluating the series
            return new TimeFunction<double[]>() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    return xys.value(arguments.evaluateAll(date));
                }

            };

        }


        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getPrecessionFunction() throws OrekitException {

            // set up the conventional polynomials
            // the following values are from equation 32 in IERS 2003 conventions
            final PolynomialNutation<DerivativeStructure> psiA =
                    new PolynomialNutation<DerivativeStructure>(    0.0,
                                                                 5038.47875   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   -1.07259   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   -0.001147  * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation<DerivativeStructure> omegaA =
                    new PolynomialNutation<DerivativeStructure>(getMeanObliquityFunction().value(getNutationReferenceEpoch()),
                                                                -0.02524   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                 0.05127   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.007726  * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation<DerivativeStructure> chiA =
                    new PolynomialNutation<DerivativeStructure>( 0.0,
                                                                10.5526   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -2.38064  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.001125 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final double tc = evaluateTC(date);
                    return new double[] {
                        psiA.value(tc), omegaA.value(tc), chiA.value(tc)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getNutationFunction()
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // set up Poisson series
            final double milliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3;
            final PoissonSeriesParser<DerivativeStructure> luniSolarParser =
                    new PoissonSeriesParser<DerivativeStructure>(14).withFirstDelaunay(1);
            final PoissonSeriesParser<DerivativeStructure> luniSolarPsiParser =
                    luniSolarParser.
                    withSinCos(0, 7, milliAS, 11, milliAS).
                    withSinCos(1, 8, milliAS, 12, milliAS);
            final PoissonSeries<DerivativeStructure> psiLuniSolarSeries =
                    luniSolarPsiParser.parse(getStream(LUNI_SOLAR_SERIES), LUNI_SOLAR_SERIES);
            final PoissonSeriesParser<DerivativeStructure> luniSolarEpsilonParser =
                    luniSolarParser.
                    withSinCos(0, 13, milliAS, 9, milliAS).
                    withSinCos(1, 14, milliAS, 10, milliAS);
            final PoissonSeries<DerivativeStructure> epsilonLuniSolarSeries =
                    luniSolarEpsilonParser.parse(getStream(LUNI_SOLAR_SERIES), LUNI_SOLAR_SERIES);

            final PoissonSeriesParser<DerivativeStructure> planetaryParser =
                    new PoissonSeriesParser<DerivativeStructure>(21).
                        withFirstDelaunay(2).
                        withFirstPlanetary(7);
            final PoissonSeriesParser<DerivativeStructure> planetaryPsiParser =
                    planetaryParser.withSinCos(0, 17, milliAS, 18, milliAS);
            final PoissonSeries<DerivativeStructure> psiPlanetarySeries =
                    planetaryPsiParser.parse(getStream(PLANETARY_SERIES), PLANETARY_SERIES);
            final PoissonSeriesParser<DerivativeStructure> planetaryEpsilonParser =
                    planetaryParser.withSinCos(0, 19, milliAS, 20, milliAS);
            final PoissonSeries<DerivativeStructure> epsilonPlanetarySeries =
                    planetaryEpsilonParser.parse(getStream(PLANETARY_SERIES), PLANETARY_SERIES);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> luniSolarSeries =
                    PoissonSeries.compile(psiLuniSolarSeries, epsilonLuniSolarSeries);
            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> planetarySeries =
                    PoissonSeries.compile(psiPlanetarySeries, epsilonPlanetarySeries);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] luniSolar = luniSolarSeries.value(elements);
                    final double[] planetary = planetarySeries.value(elements);
                    return new double[] {
                        luniSolar[0] + planetary[0], luniSolar[1] + planetary[1],
                        IAU1994ResolutionC7.value(elements)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getGMSTFunction(final TimeScale ut1)
            throws OrekitException {

            // Earth Rotation Angle
            final StellarAngleCapitaine era = new StellarAngleCapitaine(ut1);

            // Polynomial part of the apparent sidereal time series
            // which is the opposite of Equation of Origins (EO)
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PolynomialNutation<DerivativeStructure> minusEO =
                    parser.parse(getStream(GST_SERIES), GST_SERIES).getPolynomial();

            // create a function evaluating the series
            return new TimeFunction<DerivativeStructure>() {

                /** {@inheritDoc} */
                @Override
                public DerivativeStructure value(final AbsoluteDate date) {
                    return era.value(date).add(minusEO.value(dsEvaluateTC(date)));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getGASTFunction(final TimeScale ut1,
                                                                 final EOPHistory eopHistory)
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // mean obliquity function
            final TimeFunction<Double> epsilon = getMeanObliquityFunction();

            // set up Poisson series
            final double milliAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3;
            final PoissonSeriesParser<DerivativeStructure> luniSolarPsiParser =
                    new PoissonSeriesParser<DerivativeStructure>(14).
                        withFirstDelaunay(1).
                        withSinCos(0, 7, milliAS, 11, milliAS).
                        withSinCos(1, 8, milliAS, 12, milliAS);
            final PoissonSeries<DerivativeStructure> psiLuniSolarSeries =
                    luniSolarPsiParser.parse(getStream(LUNI_SOLAR_SERIES), LUNI_SOLAR_SERIES);

            final PoissonSeriesParser<DerivativeStructure> planetaryPsiParser =
                    new PoissonSeriesParser<DerivativeStructure>(21).
                        withFirstDelaunay(2).
                        withFirstPlanetary(7).
                        withSinCos(0, 17, milliAS, 18, milliAS);
            final PoissonSeries<DerivativeStructure> psiPlanetarySeries =
                    planetaryPsiParser.parse(getStream(PLANETARY_SERIES), PLANETARY_SERIES);

            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> gstParser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PoissonSeries<DerivativeStructure> gstSeries = gstParser.parse(getStream(GST_SERIES), GST_SERIES);
            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> psiGstSeries =
                    PoissonSeries.compile(psiLuniSolarSeries, psiPlanetarySeries, gstSeries);

            // ERA function
            final TimeFunction<DerivativeStructure> era = getEarthOrientationAngleFunction(ut1);

            return new TimeFunction<DerivativeStructure>() {

                /** {@inheritDoc} */
                @Override
                public DerivativeStructure value(final AbsoluteDate date) {

                    // evaluate equation of origins
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] angles = psiGstSeries.value(elements);
                    final double ddPsi    = (eopHistory == null) ? 0 : eopHistory.getEquinoxNutationCorrection(date)[0];
                    final double deltaPsi = angles[0] + angles[1] + ddPsi;
                    final double epsilonA = epsilon.value(date);

                    // subtract equation of origin from EA
                    // (hence add the series above which have the sign included)
                    return era.value(date).add(deltaPsi * FastMath.cos(epsilonA) + angles[2]);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getEOPTidalCorrection()
            throws OrekitException {

            // set up nutation arguments
            // BEWARE! Using TT as the time scale here and not UT1 is intentional!
            // as this correction is used to compute UT1 itself, it is not surprising we cannot use UT1 yet,
            // however, using the close UTC as would seem logical make the comparison with interp.f from IERS fail
            // looking in the interp.f code, the same TT scale is used for both Delaunay and gamma argument
            final FundamentalNutationArguments arguments = getNutationArguments(TimeScalesFactory.getTT());

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> xyParser = new PoissonSeriesParser<DerivativeStructure>(13).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3);
            final PoissonSeries<DerivativeStructure> xSeries =
                    xyParser.
                    withSinCos(0, 10, microAS, 11, microAS).
                    parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES), TIDAL_CORRECTION_XP_YP_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries =
                    xyParser.
                    withSinCos(0, 12, microAS, 13, microAS).
                    parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES), TIDAL_CORRECTION_XP_YP_SERIES);

            final double microS = 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> ut1Parser = new PoissonSeriesParser<DerivativeStructure>(11).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3).
                    withSinCos(0, 10, microS, 11, microS);
            final PoissonSeries<DerivativeStructure> ut1Series =
                    ut1Parser.parse(getStream(TIDAL_CORRECTION_UT1_SERIES), TIDAL_CORRECTION_UT1_SERIES);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> correctionSeries =
                PoissonSeries.compile(xSeries, ySeries, ut1Series);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final FieldBodiesElements<DerivativeStructure> elements =
                            arguments.evaluateDerivative(date);
                    final DerivativeStructure[] correction = correctionSeries.value(elements);
                    return new double[] {
                        correction[0].getValue(),
                        correction[1].getValue(),
                        correction[2].getValue(),
                        -correction[2].getPartialDerivative(1) * Constants.JULIAN_DAY
                    };
                }
            };

        }

        /** {@inheritDoc} */
        public LoveNumbers getLoveNumbers() throws OrekitException {
            return loadLoveNumbers(LOVE_NUMBERS);
        }

        /** {@inheritDoc} */
        public TimeFunction<double[]> getTideFrequencyDependenceFunction(final TimeScale ut1)
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(ut1);

            // set up Poisson series
            final PoissonSeriesParser<DerivativeStructure> k20Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);
            final PoissonSeriesParser<DerivativeStructure> k21Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10);
            final PoissonSeriesParser<DerivativeStructure> k22Parser =
                    new PoissonSeriesParser<DerivativeStructure>(16).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);

            final double pico = 1.0e-12;
            final PoissonSeries<DerivativeStructure> c20Series =
                    k20Parser.
                  withSinCos(0, 18, -pico, 16, pico).
                    parse(getStream(K20_FREQUENCY_DEPENDENCE), K20_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> c21Series =
                    k21Parser.
                    withSinCos(0, 17, pico, 18, pico).
                    parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> s21Series =
                    k21Parser.
                    withSinCos(0, 18, -pico, 17, pico).
                    parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> c22Series =
                    k22Parser.
                    withSinCos(0, -1, pico, 16, pico).
                    parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> s22Series =
                    k22Parser.
                    withSinCos(0, 16, -pico, -1, pico).
                    parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> kSeries =
                PoissonSeries.compile(c20Series, c21Series, s21Series, c22Series, s22Series);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    return kSeries.value(arguments.evaluateAll(date));
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public double getPermanentTide() throws OrekitException {
            return 4.4228e-8 * -0.31460 * getLoveNumbers().getReal(2, 0);
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getSolidPoleTide(final EOPHistory eopHistory)
            throws OrekitException {

            // annual pole from ftp://tai.bipm.org/iers/conv2003/chapter7/annual.pole
            final TimeScale utc = TimeScalesFactory.getUTC();
            final SimpleTimeStampedTableParser.RowConverter<MeanPole> converter =
                new SimpleTimeStampedTableParser.RowConverter<MeanPole>() {
                    /** {@inheritDoc} */
                    @Override
                    public MeanPole convert(final double[] rawFields) throws OrekitException {
                        return new MeanPole(new AbsoluteDate((int) rawFields[0], 1, 1, utc),
                                            rawFields[1] * Constants.ARC_SECONDS_TO_RADIANS,
                                            rawFields[2] * Constants.ARC_SECONDS_TO_RADIANS);
                    }
                };
            final SimpleTimeStampedTableParser<MeanPole> parser =
                    new SimpleTimeStampedTableParser<MeanPole>(3, converter);
            final List<MeanPole> annualPoleList = parser.parse(getStream(ANNUAL_POLE), ANNUAL_POLE);
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
            final double ratio        =  0.00115;

            return new TimeFunction<double[]>() {
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
                            final List<MeanPole> neighbors = annualCache.getNeighbors(date);
                            final HermiteInterpolator interpolator = new HermiteInterpolator();
                            for (final MeanPole neighbor : neighbors) {
                                interpolator.addSamplePoint(neighbor.getDate().durationFrom(date),
                                                            new double[] {
                                                                neighbor.getX(), neighbor.getY()
                                                            });
                            }
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
                        final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH);
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
                        // and Im(k₂)/Re(k₂) is very close to +0.00115
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
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getOceanPoleTide(final EOPHistory eopHistory)
            throws OrekitException {

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    // there are no model for ocean pole tide prior to conventions 2010
                    return new double[] {
                        0.0, 0.0
                    };
                }
            };
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

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments(final TimeScale timeScale)
            throws OrekitException {
            return new FundamentalNutationArguments(this, timeScale,
                                                    getStream(NUTATION_ARGUMENTS), NUTATION_ARGUMENTS);
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<Double> getMeanObliquityFunction() throws OrekitException {

            // epsilon 0 value from chapter 5, page 56, other terms from equation 5.40 page 65
            final PolynomialNutation<DerivativeStructure> epsilonA =
                    new PolynomialNutation<DerivativeStructure>(84381.406        * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -46.836769     * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   -0.0001831    * Constants.ARC_SECONDS_TO_RADIANS,
                                                                    0.00200340   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   -0.000000576  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   -0.0000000434 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeFunction<Double>() {

                /** {@inheritDoc} */
                @Override
                public Double value(final AbsoluteDate date) {
                    return epsilonA.value(evaluateTC(date));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getXYSpXY2Function() throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withPolynomialPart('t', PolynomialParser.Unit.MICRO_ARC_SECONDS).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);
            final PoissonSeries<DerivativeStructure> xSeries = parser.parse(getStream(X_SERIES), X_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries = parser.parse(getStream(Y_SERIES), Y_SERIES);
            final PoissonSeries<DerivativeStructure> sSeries = parser.parse(getStream(S_SERIES), S_SERIES);
            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> xys = PoissonSeries.compile(xSeries, ySeries, sSeries);

            // create a function evaluating the series
            return new TimeFunction<double[]>() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    return xys.value(arguments.evaluateAll(date));
                }

            };

        }

        /** {@inheritDoc} */
        public LoveNumbers getLoveNumbers() throws OrekitException {
            return loadLoveNumbers(LOVE_NUMBERS);
        }

        /** {@inheritDoc} */
        public TimeFunction<double[]> getTideFrequencyDependenceFunction(final TimeScale ut1)
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(ut1);

            // set up Poisson series
            final PoissonSeriesParser<DerivativeStructure> k20Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);
            final PoissonSeriesParser<DerivativeStructure> k21Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10);
            final PoissonSeriesParser<DerivativeStructure> k22Parser =
                    new PoissonSeriesParser<DerivativeStructure>(16).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10);

            final double pico = 1.0e-12;
            final PoissonSeries<DerivativeStructure> c20Series =
                    k20Parser.
                    withSinCos(0, 18, -pico, 16, pico).
                    parse(getStream(K20_FREQUENCY_DEPENDENCE), K20_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> c21Series =
                    k21Parser.
                    withSinCos(0, 17, pico, 18, pico).
                    parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> s21Series =
                    k21Parser.
                    withSinCos(0, 18, -pico, 17, pico).
                    parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> c22Series =
                    k22Parser.
                    withSinCos(0, -1, pico, 16, pico).
                    parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);
            final PoissonSeries<DerivativeStructure> s22Series =
                    k22Parser.
                    withSinCos(0, 16, -pico, -1, pico).
                    parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> kSeries =
                PoissonSeries.compile(c20Series, c21Series, s21Series, c22Series, s22Series);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    return kSeries.value(arguments.evaluateAll(date));
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public double getPermanentTide() throws OrekitException {
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
            final AbsoluteDate changeDate = new AbsoluteDate(2010, 1, 1, TimeScalesFactory.getTT());

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
            final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH);
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

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getSolidPoleTide(final EOPHistory eopHistory)
            throws OrekitException {

            // constants from IERS 2010, section 6.4
            final double globalFactor = -1.333e-9 / Constants.ARC_SECONDS_TO_RADIANS;
            final double ratio        =  0.00115;

            return new TimeFunction<double[]>() {
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
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getOceanPoleTide(final EOPHistory eopHistory)
            throws OrekitException {

            return new TimeFunction<double[]>() {
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
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getPrecessionFunction() throws OrekitException {

            // set up the conventional polynomials
            // the following values are from equation 5.40 in IERS 2010 conventions
            final PolynomialNutation<DerivativeStructure> psiA =
                    new PolynomialNutation<DerivativeStructure>(   0.0,
                                                                5038.481507     * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -1.0790069    * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -0.00114045   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                   0.000132851  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                  -0.0000000951 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation<DerivativeStructure> omegaA =
                    new PolynomialNutation<DerivativeStructure>(getMeanObliquityFunction().value(getNutationReferenceEpoch()),
                                                                -0.025754     * Constants.ARC_SECONDS_TO_RADIANS,
                                                                 0.0512623    * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.00772503   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.000000467  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                 0.0000003337 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation<DerivativeStructure> chiA =
                    new PolynomialNutation<DerivativeStructure>( 0.0,
                                                                10.556403     * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -2.3814292    * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.00121197   * Constants.ARC_SECONDS_TO_RADIANS,
                                                                 0.000170663  * Constants.ARC_SECONDS_TO_RADIANS,
                                                                -0.0000000560 * Constants.ARC_SECONDS_TO_RADIANS);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final double tc = evaluateTC(date);
                    return new double[] {
                        psiA.value(tc), omegaA.value(tc), chiA.value(tc)
                    };
                }
            };

        }

         /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getNutationFunction()
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);
            final PoissonSeries<DerivativeStructure> psiSeries     = parser.parse(getStream(PSI_SERIES), PSI_SERIES);
            final PoissonSeries<DerivativeStructure> epsilonSeries = parser.parse(getStream(EPSILON_SERIES), EPSILON_SERIES);
            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> psiEpsilonSeries =
                    PoissonSeries.compile(psiSeries, epsilonSeries);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] psiEpsilon = psiEpsilonSeries.value(elements);
                    return new double[] {
                        psiEpsilon[0], psiEpsilon[1], IAU1994ResolutionC7.value(elements)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getGMSTFunction(final TimeScale ut1) throws OrekitException {

            // Earth Rotation Angle
            final StellarAngleCapitaine era = new StellarAngleCapitaine(ut1);

            // Polynomial part of the apparent sidereal time series
            // which is the opposite of Equation of Origins (EO)
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS).
                        withPolynomialPart('t', Unit.ARC_SECONDS);
            final PolynomialNutation<DerivativeStructure> minusEO =
                    parser.parse(getStream(GST_SERIES), GST_SERIES).getPolynomial();

            // create a function evaluating the series
            return new TimeFunction<DerivativeStructure>() {

                /** {@inheritDoc} */
                @Override
                public DerivativeStructure value(final AbsoluteDate date) {
                    return era.value(date).add(minusEO.value(dsEvaluateTC(date)));
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getGASTFunction(final TimeScale ut1,
                                                                 final EOPHistory eopHistory)
            throws OrekitException {

            // set up nutation arguments
            final FundamentalNutationArguments arguments = getNutationArguments(null);

            // mean obliquity function
            final TimeFunction<Double> epsilon = getMeanObliquityFunction();

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> baseParser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, microAS, 3, microAS);
            final PoissonSeriesParser<DerivativeStructure> gstParser  = baseParser.withPolynomialPart('t', Unit.ARC_SECONDS);
            final PoissonSeries<DerivativeStructure> psiSeries        = baseParser.parse(getStream(PSI_SERIES), PSI_SERIES);
            final PoissonSeries<DerivativeStructure> gstSeries        = gstParser.parse(getStream(GST_SERIES), GST_SERIES);
            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> psiGstSeries =
                    PoissonSeries.compile(psiSeries, gstSeries);

            // ERA function
            final TimeFunction<DerivativeStructure> era = getEarthOrientationAngleFunction(ut1);

            return new TimeFunction<DerivativeStructure>() {

                /** {@inheritDoc} */
                @Override
                public DerivativeStructure value(final AbsoluteDate date) {

                    // evaluate equation of origins
                    final BodiesElements elements = arguments.evaluateAll(date);
                    final double[] angles = psiGstSeries.value(elements);
                    final double ddPsi    = (eopHistory == null) ? 0 : eopHistory.getEquinoxNutationCorrection(date)[0];
                    final double deltaPsi = angles[0] + ddPsi;
                    final double epsilonA = epsilon.value(date);

                    // subtract equation of origin from EA
                    // (hence add the series above which have the sign included)
                    return era.value(date).add(deltaPsi * FastMath.cos(epsilonA) + angles[1]);

                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<double[]> getEOPTidalCorrection()
            throws OrekitException {

            // set up nutation arguments
            // BEWARE! Using TT as the time scale here and not UT1 is intentional!
            // as this correction is used to compute UT1 itself, it is not surprising we cannot use UT1 yet,
            // however, using the close UTC as would seem logical make the comparison with interp.f from IERS fail
            // looking in the interp.f code, the same TT scale is used for both Delaunay and gamma argument
            final FundamentalNutationArguments arguments = getNutationArguments(TimeScalesFactory.getTT());

            // set up Poisson series
            final double microAS = Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> xyParser = new PoissonSeriesParser<DerivativeStructure>(13).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3);
            final PoissonSeries<DerivativeStructure> xSeries =
                    xyParser.
                    withSinCos(0, 10, microAS, 11, microAS).
                    parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES), TIDAL_CORRECTION_XP_YP_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries =
                    xyParser.
                    withSinCos(0, 12, microAS, 13, microAS).
                    parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES), TIDAL_CORRECTION_XP_YP_SERIES);

            final double microS = 1.0e-6;
            final PoissonSeriesParser<DerivativeStructure> ut1Parser = new PoissonSeriesParser<DerivativeStructure>(11).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3).
                    withSinCos(0, 10, microS, 11, microS);
            final PoissonSeries<DerivativeStructure> ut1Series =
                    ut1Parser.parse(getStream(TIDAL_CORRECTION_UT1_SERIES), TIDAL_CORRECTION_UT1_SERIES);

            @SuppressWarnings("unchecked")
            final PoissonSeries.CompiledSeries<DerivativeStructure> correctionSeries =
                    PoissonSeries.compile(xSeries, ySeries, ut1Series);

            return new TimeFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final AbsoluteDate date) {
                    final FieldBodiesElements<DerivativeStructure> elements =
                            arguments.evaluateDerivative(date);
                    final DerivativeStructure[] correction = correctionSeries.value(elements);
                    return new double[] {
                        correction[0].getValue(),
                        correction[1].getValue(),
                        correction[2].getValue(),
                        -correction[2].getPartialDerivative(1) * Constants.JULIAN_DAY
                    };
                }
            };

        }

    };

    /** IERS conventions resources base directory. */
    private static final String IERS_BASE = "/assets/org/orekit/IERS-conventions/";

    /** Get the reference epoch for fundamental nutation arguments.
     * @return reference epoch for fundamental nutation arguments
     * @since 6.1
     */
    public AbsoluteDate getNutationReferenceEpoch() {
        // IERS 1996, IERS 2003 and IERS 2010 use the same J2000.0 reference date
        return AbsoluteDate.J2000_EPOCH;
    }

    /** Evaluate the date offset between the current date and the {@link #getNutationReferenceEpoch() reference date}.
     * @param date current date
     * @return date offset in Julian centuries
     * @since 6.1
     */
    public double evaluateTC(final AbsoluteDate date) {
        return date.durationFrom(getNutationReferenceEpoch()) / Constants.JULIAN_CENTURY;
    }

    /** Evaluate the date offset between the current date and the {@link #getNutationReferenceEpoch() reference date}.
     * @param date current date
     * @return date offset in Julian centuries
     * @since 6.1
     */
    public DerivativeStructure dsEvaluateTC(final AbsoluteDate date) {
        return new DerivativeStructure(1, 1, evaluateTC(date), 1.0 / Constants.JULIAN_CENTURY);
    }

    /** Get the fundamental nutation arguments.
     * @param timeScale time scale for computing Greenwich Mean Sidereal Time
     * (typically {@link TimeScalesFactory#getUT1(IERSConventions, boolean) UT1})
     * @return fundamental nutation arguments
     * @exception OrekitException if fundamental nutation arguments cannot be loaded
     * @since 6.1
     */
    public abstract FundamentalNutationArguments getNutationArguments(final TimeScale timeScale)
        throws OrekitException;

    /** Get the function computing mean obliquity of the ecliptic.
     * @return function computing mean obliquity of the ecliptic
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<Double> getMeanObliquityFunction() throws OrekitException;

    /** Get the function computing the Celestial Intermediate Pole and Celestial Intermediate Origin components.
     * <p>
     * The returned function computes the two X, Y components of CIP and the S+XY/2 component of the non-rotating CIO.
     * </p>
     * @return function computing the Celestial Intermediate Pole and Celestial Intermediate Origin components
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<double[]> getXYSpXY2Function()
        throws OrekitException;

    /** Get the function computing the raw Earth Orientation Angle.
     * <p>
     * The raw angle does not contain any correction. If for example dTU1 correction
     * due to tidal effect is desired, it must be added afterward by the caller.
     * The returned value contain the angle as the value and the angular rate as
     * the first derivative.
     * </p>
     * @param ut1 UT1 time scale
     * @return function computing the rawEarth Orientation Angle, in the non-rotating origin paradigm,
     * the return value containing both the angle and its first time derivative
     * @since 6.1
     */
    public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction(final TimeScale ut1) {
        return new StellarAngleCapitaine(ut1);
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
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<double[]> getPrecessionFunction() throws OrekitException;

    /** Get the function computing the nutation angles.
     * <p>
     * The function returned computes the two classical angles ΔΨ and Δε,
     * and the correction to the equation of equinoxes introduced since 1997-02-27 by IAU 1994
     * resolution C7 (the correction is forced to 0 before this date)
     * </p>
     * @return function computing the nutation in longitude ΔΨ and Δε
     * and the correction of equation of equinoxes
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<double[]> getNutationFunction()
        throws OrekitException;

    /** Get the function computing Greenwich mean sidereal time, in radians.
     * @param ut1 UT1 time scale
     * @return function computing Greenwich mean sidereal time,
     * the return value containing both the angle and its first time derivative
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<DerivativeStructure> getGMSTFunction(TimeScale ut1)
        throws OrekitException;

    /** Get the function computing Greenwich apparent sidereal time, in radians.
     * @param ut1 UT1 time scale
     * @param eopHistory EOP history
     * @return function computing Greenwich apparent sidereal time,
     * the return value containing both the angle and its first time derivative
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<DerivativeStructure> getGASTFunction(TimeScale ut1,
                                                                      EOPHistory eopHistory)
        throws OrekitException;

    /** Get the function computing tidal corrections for Earth Orientation Parameters.
     * @return function computing tidal corrections for Earth Orientation Parameters,
     * for xp, yp, ut1 and lod respectively
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<double[]> getEOPTidalCorrection()
        throws OrekitException;

    /** Get the Love numbers.
     * @return Love numbers
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract LoveNumbers getLoveNumbers()
        throws OrekitException;

    /** Get the function computing frequency dependent terms (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
     * @param ut1 UT1 time scale
     * @return frequency dependence model for tides computation (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<double[]> getTideFrequencyDependenceFunction(TimeScale ut1)
        throws OrekitException;

    /** Get the permanent tide to be <em>removed</em> from ΔC₂₀ when zero-tide potentials are used.
     * @return permanent tide to remove
     * @exception OrekitException if table cannot be loaded
     */
    public abstract double getPermanentTide() throws OrekitException;

    /** Get the function computing solid pole tide (ΔC₂₁, ΔS₂₁).
     * @param eopHistory EOP history
     * @return model for solid pole tide (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<double[]> getSolidPoleTide(EOPHistory eopHistory)
        throws OrekitException;

    /** Get the function computing ocean pole tide (ΔC₂₁, ΔS₂₁).
     * @param eopHistory EOP history
     * @return model for ocean pole tide (ΔC₂₀, ΔC₂₁, ΔS₂₁, ΔC₂₂, ΔS₂₂).
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract TimeFunction<double[]> getOceanPoleTide(EOPHistory eopHistory)
        throws OrekitException;

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
         * @exception OrekitException if correction cannot be converted
         */
        double[] toNonRotating(AbsoluteDate date, double ddPsi, double ddEpsilon)
            throws OrekitException;

        /** Convert nutation corrections.
         * @param date current date
         * @param dX δX part of the nutation correction
         * @param dY δY part of the nutation correction
         * @return array containing δΔψ and δΔε
         * @exception OrekitException if correction cannot be converted
         */
        double[] toEquinox(AbsoluteDate date, double dX, double dY)
            throws OrekitException;

    }

    /** Create a function converting nutation corrections between
     * δX/δY and δΔψ/δΔε.
     * <ul>
     * <li>δX/δY nutation corrections are used with the Non-Rotating Origin paradigm.</li>
     * <li>δΔψ/δΔε nutation corrections are used with the equinox-based paradigm.</li>
     * </ul>
     * @return a new converter
     * @exception OrekitException if some convention table cannot be loaded
     * @since 6.1
     */
    public NutationCorrectionConverter getNutationCorrectionConverter()
        throws OrekitException {

        // get models parameters
        final TimeFunction<double[]> precessionFunction = getPrecessionFunction();
        final TimeFunction<Double> epsilonAFunction = getMeanObliquityFunction();
        final AbsoluteDate date0 = getNutationReferenceEpoch();
        final double cosE0 = FastMath.cos(epsilonAFunction.value(date0));

        return new NutationCorrectionConverter() {

            /** {@inheritDoc} */
            @Override
            public double[] toNonRotating(final AbsoluteDate date,
                                          final double ddPsi, final double ddEpsilon)
                throws OrekitException {
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
                                      final double dX, final double dY)
                throws OrekitException {
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
     * @exception OrekitException if the Love numbers embedded in the
     * library cannot be read
     */
    protected LoveNumbers loadLoveNumbers(final String nameLove) throws OrekitException {
        InputStream stream = null;
        BufferedReader reader = null;
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

            stream = IERSConventions.class.getResourceAsStream(nameLove);
            if (stream == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, nameLove);
            }

            // setup the reader
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            String line = reader.readLine();
            int lineNumber = 1;

            // look for the Love numbers
            while (line != null) {

                line = line.trim();
                if (!(line.isEmpty() || line.startsWith("#"))) {
                    final String[] fields = line.split("\\p{Space}+");
                    if (fields.length != 5) {
                        // this should never happen with files embedded within Orekit
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, nameLove, line);
                    }
                    final int n = Integer.parseInt(fields[0]);
                    final int m = Integer.parseInt(fields[1]);
                    if ((n < 2) || (n > 3) || (m < 0) || (m > n)) {
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

            return new LoveNumbers(real, imaginary, plus);

        } catch (IOException ioe) {
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, nameLove);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                // ignored here
            }
        }
    }

    /** Get a data stream.
     * @param name file name of the resource stream
     * @return stream
     */
    private static InputStream getStream(final String name) {
        return IERSConventions.class.getResourceAsStream(name);
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

        /** Start date for applying Moon corrections to the equation of the equinoxes.
         * This date corresponds to 1997-02-27T00:00:00 UTC, hence the 30s offset from TAI.
         */
        private static final AbsoluteDate MODEL_START =
            new AbsoluteDate(1997, 2, 27, 0, 0, 30, TimeScalesFactory.getTAI());

        /** Evaluate the correction.
         * @param arguments Delaunay for nutation
         * @return correction value (0 before 1997-02-27)
         */
        public static double value(final DelaunayArguments arguments) {
            if (arguments.getDate().compareTo(MODEL_START) >= 0) {

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

    };

    /** Stellar angle model.
     * <p>
     * The stellar angle computed here has been defined in the paper "A non-rotating origin on the
     * instantaneous equator: Definition, properties and use", N. Capitaine, Guinot B., and Souchay J.,
     * Celestial Mechanics, Volume 39, Issue 3, pp 283-307. It has been proposed as a conventional
     * conventional relationship between UT1 and Earth rotation in the paper "Definition of the Celestial
     * Ephemeris origin and of UT1 in the International Celestial Reference Frame", Capitaine, N.,
     * Guinot, B., and McCarthy, D. D., 2000, “,” Astronomy and Astrophysics, 355(1), pp. 398–405.
     * </p>
     * <p>
     * It is presented simply as stellar angle in IERS conventions 1996 but as since been adopted as
     * the conventional relationship defining UT1 from ICRF and is called Earth Rotation Angle in
     * IERS conventions 2003 and 2010.
     * </p>
     */
    private static class StellarAngleCapitaine implements TimeFunction<DerivativeStructure> {

        /** Reference date of Capitaine's Earth Rotation Angle model. */
        private static final AbsoluteDate REFERENCE_DATE = new AbsoluteDate(DateComponents.J2000_EPOCH,
                                                                            TimeComponents.H12,
                                                                            TimeScalesFactory.getTAI());

        /** Constant term of Capitaine's Earth Rotation Angle model. */
        private static final double ERA_0   = MathUtils.TWO_PI * 0.7790572732640;

        /** Rate term of Capitaine's Earth Rotation Angle model.
         * (radians per day, main part) */
        private static final double ERA_1A  = MathUtils.TWO_PI / Constants.JULIAN_DAY;

        /** Rate term of Capitaine's Earth Rotation Angle model.
         * (radians per day, fractional part) */
        private static final double ERA_1B  = ERA_1A * 0.00273781191135448;

        /** Total rate term of Capitaine's Earth Rotation Angle model. */
        private static final double ERA_1AB = ERA_1A + ERA_1B;

        /** UT1 time scale. */
        private final TimeScale ut1;

        /** Simple constructor.
         * @param ut1 UT1 time scale
         */
        StellarAngleCapitaine(final TimeScale ut1) {
            this.ut1 = ut1;
        }

        /** {@inheritDoc} */
        @Override
        public DerivativeStructure value(final AbsoluteDate date) {

            // split the date offset as a full number of days plus a smaller part
            final int secondsInDay = 86400;
            final double dt  = date.durationFrom(REFERENCE_DATE);
            final long days  = ((long) dt) / secondsInDay;
            final double dtA = secondsInDay * days;
            final double dtB = (dt - dtA) + ut1.offsetFromTAI(date);

            return new DerivativeStructure(1, 1,
                                           ERA_0 + ERA_1A * dtB + ERA_1B * (dtA + dtB),
                                           ERA_1AB);

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
}
