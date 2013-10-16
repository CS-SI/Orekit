/* Copyright 2002-2013 CS Systèmes d'Information
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
import java.lang.reflect.Array;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.EOPHistory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;


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
        protected FundamentalNutationArguments getNutationArguments(final TimeFunction<DerivativeStructure> gmstFunction)
            throws OrekitException {
            return new FundamentalNutationArguments(this, gmstFunction,
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

            final PoissonSeriesParser<DerivativeStructure> baseParser =
                    new PoissonSeriesParser<DerivativeStructure>(12).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4).
                        withFirstDelaunay(1);

            final PoissonSeriesParser<DerivativeStructure> xParser = baseParser.withSinCos(0, 7, -1).withSinCos(1, 8, 9);
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

            final PoissonSeriesParser<DerivativeStructure> yParser = baseParser.withSinCos(0, -1, 10).withSinCos(1, 12, 11);
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
            final PoissonSeriesParser<DerivativeStructure> baseParser =
                    new PoissonSeriesParser<DerivativeStructure>(10).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4).
                        withFirstDelaunay(1);

            final PoissonSeriesParser<DerivativeStructure> psiParser = baseParser.withSinCos(0, 7, -1).withSinCos(1, 8, -1);
            final PoissonSeries<DerivativeStructure> psiSeries = psiParser.parse(getStream(PSI_EPSILON_SERIES), PSI_EPSILON_SERIES);

            final PoissonSeriesParser<DerivativeStructure> epsilonParser = baseParser.withSinCos(0, -1, 9).withSinCos(1, -1, 10);
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
            final TimeFunction<DerivativeStructure> gmstFunction = getGMSTFunction(TimeScalesFactory.getTT());
            final FundamentalNutationArguments arguments = getNutationArguments(gmstFunction);

            // set up Poisson series
            final PoissonSeriesParser<DerivativeStructure> xyParser = new PoissonSeriesParser<DerivativeStructure>(17).
                    withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3).
                    withOptionalColumn(1).
                    withGamma(7).
                    withFirstDelaunay(2);
            final PoissonSeries<DerivativeStructure> xSeries =
                    xyParser.withSinCos(0, 14, 15).parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES),
                                                         TIDAL_CORRECTION_XP_YP_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries =
                    xyParser.withSinCos(0, 16, 17).parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES),
                                                         TIDAL_CORRECTION_XP_YP_SERIES);

            final PoissonSeriesParser<DerivativeStructure> ut1Parser = new PoissonSeriesParser<DerivativeStructure>(17).
                    withFactor(1.0e-4).
                    withOptionalColumn(1).
                    withGamma(7).
                    withFirstDelaunay(2).
                    withSinCos(0, 16, 17);
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
        public PoissonSeries<DerivativeStructure>[] getTideFrequencyDependenceModel()
            throws OrekitException {

            final PoissonSeriesParser<DerivativeStructure> k20Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10).
                        withSinCos(0, 16, 18);
            final PoissonSeriesParser<DerivativeStructure> k21Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10).
                        withSinCos(0, 17, 18);
            final PoissonSeriesParser<DerivativeStructure> k22Parser =
                    new PoissonSeriesParser<DerivativeStructure>(16).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10).
                        withSinCos(0, 16, -1);


            @SuppressWarnings("unchecked")
            final PoissonSeries<DerivativeStructure>[] kSeries =
                    (PoissonSeries<DerivativeStructure>[]) Array.newInstance(PoissonSeries.class, 3);
            kSeries[0] = k20Parser.parse(getStream(K20_FREQUENCY_DEPENDENCE), K20_FREQUENCY_DEPENDENCE);
            kSeries[1] = k21Parser.parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            kSeries[2] = k22Parser.parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);

            return kSeries;

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

        /** {@inheritDoc} */
        protected FundamentalNutationArguments getNutationArguments(final TimeFunction<DerivativeStructure> gmstFunction)
            throws OrekitException {
            return new FundamentalNutationArguments(this, gmstFunction,
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
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                        withPolynomialPart('t', PolynomialParser.Unit.MICRO_ARC_SECONDS).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, 3);

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
            final PoissonSeriesParser<DerivativeStructure> luniSolarParser =
                    new PoissonSeriesParser<DerivativeStructure>(14).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3).
                        withFirstDelaunay(1);
            final PoissonSeriesParser<DerivativeStructure> luniSolarPsiParser =
                    luniSolarParser.withSinCos(0, 7, 11).withSinCos(1, 8, 12);
            final PoissonSeries<DerivativeStructure> psiLuniSolarSeries =
                    luniSolarPsiParser.parse(getStream(LUNI_SOLAR_SERIES), LUNI_SOLAR_SERIES);
            final PoissonSeriesParser<DerivativeStructure> luniSolarEpsilonParser =
                    luniSolarParser.withSinCos(0, 13, 9).withSinCos(1, 14, 10);
            final PoissonSeries<DerivativeStructure> epsilonLuniSolarSeries =
                    luniSolarEpsilonParser.parse(getStream(LUNI_SOLAR_SERIES), LUNI_SOLAR_SERIES);

            final PoissonSeriesParser<DerivativeStructure> planetaryParser =
                    new PoissonSeriesParser<DerivativeStructure>(21).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3).
                        withFirstDelaunay(2).
                        withFirstPlanetary(7);
            final PoissonSeriesParser<DerivativeStructure> planetaryPsiParser = planetaryParser.withSinCos(0, 17, 18);
            final PoissonSeries<DerivativeStructure> psiPlanetarySeries =
                    planetaryPsiParser.parse(getStream(PLANETARY_SERIES), PLANETARY_SERIES);
            final PoissonSeriesParser<DerivativeStructure> planetaryEpsilonParser = planetaryParser.withSinCos(0, 19, 20);
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
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, 3).
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
            final PoissonSeriesParser<DerivativeStructure> luniSolarPsiParser =
                    new PoissonSeriesParser<DerivativeStructure>(14).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3).
                        withFirstDelaunay(1).
                        withSinCos(0, 7, 11).
                        withSinCos(1, 8, 12);
            final PoissonSeries<DerivativeStructure> psiLuniSolarSeries =
                    luniSolarPsiParser.parse(getStream(LUNI_SOLAR_SERIES), LUNI_SOLAR_SERIES);

            final PoissonSeriesParser<DerivativeStructure> planetaryPsiParser =
                    new PoissonSeriesParser<DerivativeStructure>(21).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3).
                        withFirstDelaunay(2).
                        withFirstPlanetary(7).
                        withSinCos(0, 17, 18);
            final PoissonSeries<DerivativeStructure> psiPlanetarySeries =
                    planetaryPsiParser.parse(getStream(PLANETARY_SERIES), PLANETARY_SERIES);

            final PoissonSeriesParser<DerivativeStructure> gstParser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, 3).
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
            final TimeFunction<DerivativeStructure> gmstFunction = getGMSTFunction(TimeScalesFactory.getTT());
            final FundamentalNutationArguments arguments = getNutationArguments(gmstFunction);

            // set up Poisson series
            final PoissonSeriesParser<DerivativeStructure> xyParser = new PoissonSeriesParser<DerivativeStructure>(13).
                    withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3);
            final PoissonSeries<DerivativeStructure> xSeries =
                    xyParser.withSinCos(0, 10, 11).parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES),
                                                         TIDAL_CORRECTION_XP_YP_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries =
                    xyParser.withSinCos(0, 12, 13).parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES),
                                                         TIDAL_CORRECTION_XP_YP_SERIES);

            final PoissonSeriesParser<DerivativeStructure> ut1Parser = new PoissonSeriesParser<DerivativeStructure>(11).
                    withFactor(1.0e-6).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3).
                    withSinCos(0, 10, 11);
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
        public PoissonSeries<DerivativeStructure>[] getTideFrequencyDependenceModel()
            throws OrekitException {

            final PoissonSeriesParser<DerivativeStructure> k20Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10).
                        withSinCos(0, 16, 18);
            final PoissonSeriesParser<DerivativeStructure> k21Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10).
                        withSinCos(0, 17, 18);
            final PoissonSeriesParser<DerivativeStructure> k22Parser =
                    new PoissonSeriesParser<DerivativeStructure>(16).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10).
                        withSinCos(0, 16, -1);

            @SuppressWarnings("unchecked")
            final PoissonSeries<DerivativeStructure>[] kSeries =
                    (PoissonSeries<DerivativeStructure>[]) Array.newInstance(PoissonSeries.class, 3);
            kSeries[0] = k20Parser.parse(getStream(K20_FREQUENCY_DEPENDENCE), K20_FREQUENCY_DEPENDENCE);
            kSeries[1] = k21Parser.parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            kSeries[2] = k22Parser.parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);

            return kSeries;

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
        protected FundamentalNutationArguments getNutationArguments(final TimeFunction<DerivativeStructure> gmstFunction)
            throws OrekitException {
            return new FundamentalNutationArguments(this, gmstFunction,
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
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                        withPolynomialPart('t', PolynomialParser.Unit.MICRO_ARC_SECONDS).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, 3);
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
        public PoissonSeries<DerivativeStructure>[] getTideFrequencyDependenceModel()
            throws OrekitException {

            final PoissonSeriesParser<DerivativeStructure> k20Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10).
                        withSinCos(0, 16, 18);
            final PoissonSeriesParser<DerivativeStructure> k21Parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 3).
                        withFirstDelaunay(10).
                        withSinCos(0, 17, 18);
            final PoissonSeriesParser<DerivativeStructure> k22Parser =
                    new PoissonSeriesParser<DerivativeStructure>(16).
                        withFactor(1.0e-12).
                        withOptionalColumn(1).
                        withDoodson(4, 2).
                        withFirstDelaunay(10).
                        withSinCos(0, 16, -1);

            @SuppressWarnings("unchecked")
            final PoissonSeries<DerivativeStructure>[] kSeries =
                    (PoissonSeries<DerivativeStructure>[]) Array.newInstance(PoissonSeries.class, 3);
            kSeries[0] = k20Parser.parse(getStream(K20_FREQUENCY_DEPENDENCE), K20_FREQUENCY_DEPENDENCE);
            kSeries[1] = k21Parser.parse(getStream(K21_FREQUENCY_DEPENDENCE), K21_FREQUENCY_DEPENDENCE);
            kSeries[2] = k22Parser.parse(getStream(K22_FREQUENCY_DEPENDENCE), K22_FREQUENCY_DEPENDENCE);

            return kSeries;

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
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, 3);
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
            final PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, 3).
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
            final PoissonSeriesParser<DerivativeStructure> baseParser =
                    new PoissonSeriesParser<DerivativeStructure>(17).
                        withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                        withFirstDelaunay(4).
                        withFirstPlanetary(9).
                        withSinCos(0, 2, 3);
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
            final TimeFunction<DerivativeStructure> gmstFunction = getGMSTFunction(TimeScalesFactory.getTT());
            final FundamentalNutationArguments arguments = getNutationArguments(gmstFunction);

            // set up Poisson series
            final PoissonSeriesParser<DerivativeStructure> xyParser = new PoissonSeriesParser<DerivativeStructure>(13).
                    withFactor(Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3);
            final PoissonSeries<DerivativeStructure> xSeries =
                    xyParser.withSinCos(0, 10, 11).parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES),
                                                         TIDAL_CORRECTION_XP_YP_SERIES);
            final PoissonSeries<DerivativeStructure> ySeries =
                    xyParser.withSinCos(0, 12, 13).parse(getStream(TIDAL_CORRECTION_XP_YP_SERIES),
                                                         TIDAL_CORRECTION_XP_YP_SERIES);

            final PoissonSeriesParser<DerivativeStructure> ut1Parser = new PoissonSeriesParser<DerivativeStructure>(11).
                    withFactor(1.0e-6).
                    withOptionalColumn(1).
                    withGamma(2).
                    withFirstDelaunay(3).
                    withSinCos(0, 10, 11);
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
     * @param gmstFunction function computing Greenwich Mean Sidereal Time
     * @return fundamental nutation arguments
     * @exception OrekitException if fundamental nutation arguments cannot be loaded
     * @since 6.1
     */
    protected abstract FundamentalNutationArguments getNutationArguments(TimeFunction<DerivativeStructure> gmstFunction)
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
     * &psi;<sub>A</sub> (around Z axis), &omega;<sub>A</sub> (around X axis)
     * and &chi;<sub>A</sub> (around Z axis). The constant angle &epsilon;<sub>0</sub>
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
     * The function returned computes the two classical angles &Delta;&Psi; and &Delta;&epsilon;,
     * and the correction to the equation of equinoxes introduced since 1997-02-27 by IAU 1994
     * resolution C7 (the correction is forced to 0 before this date)
     * </p>
     * @return function computing the nutation in longitude &Delta;&Psi; and &Delta;&epsilon;
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
    public abstract TimeFunction<DerivativeStructure> getGASTFunction(final TimeScale ut1,
                                                                      final EOPHistory eopHistory)
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

    /** Get the frequency dependence model for tides computation (k₂₀, k₂₁, k₂₂).
     * @return frequency dependence model for tides computation (k₂₀, k₂₁, k₂₂).
     * @exception OrekitException if table cannot be loaded
     * @since 6.1
     */
    public abstract PoissonSeries<DerivativeStructure>[] getTideFrequencyDependenceModel()
        throws OrekitException;

    /** Interface for functions converting nutation corrections between
     * &delta;&Delta;&psi;/&delta;&Delta;&epsilon; to &delta;X/&delta;Y.
     * <ul>
     * <li>&delta;&Delta;&psi;/&delta;&Delta;&epsilon; nutation corrections are used with the equinox-based paradigm.</li>
     * <li>&delta;X/&delta;Y nutation corrections are used with the Non-Rotating Origin paradigm.</li>
     * </ul>
     * @since 6.1
     */
    public interface NutationCorrectionConverter {

        /** Convert nutation corrections.
         * @param date current date
         * @param ddPsi &delta;&Delta;&psi; part of the nutation correction
         * @param ddEpsilon &delta;&Delta;&epsilon; part of the nutation correction
         * @return array containing &delta;X and &delta;Y
         * @exception OrekitException if correction cannot be converted
         */
        double[] toNonRotating(AbsoluteDate date, double ddPsi, double ddEpsilon)
            throws OrekitException;

        /** Convert nutation corrections.
         * @param date current date
         * @param dX &delta;X part of the nutation correction
         * @param dY &delta;Y part of the nutation correction
         * @return array containing &delta;&Delta;&psi; and &delta;&Delta;&epsilon;
         * @exception OrekitException if correction cannot be converted
         */
        double[] toEquinox(AbsoluteDate date, double dX, double dY)
            throws OrekitException;

    }

    /** Create a function converting nutation corrections between
     * &delta;X/&delta;Y and &delta;&Delta;&psi;/&delta;&Delta;&epsilon;.
     * <ul>
     * <li>&delta;X/&delta;Y nutation corrections are used with the Non-Rotating Origin paradigm.</li>
     * <li>&delta;&Delta;&psi;/&delta;&Delta;&epsilon; nutation corrections are used with the equinox-based paradigm.</li>
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
        public StellarAngleCapitaine(final TimeScale ut1) {
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

}
