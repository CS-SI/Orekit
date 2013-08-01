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

import java.io.InputStream;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.NutationFunction;
import org.orekit.data.PoissonSeries;
import org.orekit.data.PolynomialNutation;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
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

        /** {@inheritDoc} */
        @Override
        public boolean precessionSupported() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public boolean nutationSupported() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public boolean nonRotatingOriginSupported() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public FundamentalNutationArguments getNutationArguments() throws OrekitException {
            return loadArguments(IERS_BASE + "1996/nutation-arguments.txt");
        }

        /** {@inheritDoc} */
        @Override
        public double getEpsilon0() {
            // value from chapter 5, page 25
            return 84381.448 * Constants.ARC_SECONDS_TO_RADIANS;
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getPrecessionZetaFunction() throws OrekitException {
            return new PolynomialNutation(0.0,
                                          2306.2181 * Constants.ARC_SECONDS_TO_RADIANS,
                                          0.30188   * Constants.ARC_SECONDS_TO_RADIANS,
                                          0.017998  * Constants.ARC_SECONDS_TO_RADIANS);

        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getPrecessionThetaFunction() throws OrekitException {
            return new PolynomialNutation(0.0,
                                          2004.3109 * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.42665  * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.041833 * Constants.ARC_SECONDS_TO_RADIANS);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getPrecessionZFunction() throws OrekitException {
            return new PolynomialNutation(0.0,
                                          2306.2181 * Constants.ARC_SECONDS_TO_RADIANS,
                                          1.09468   * Constants.ARC_SECONDS_TO_RADIANS,
                                          0.018203  * Constants.ARC_SECONDS_TO_RADIANS);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getNutationInLongitudeFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "1996/tab5.1-psi.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getNutationInObliquityFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "1996/tab5.1-epsilon.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getMeanObliquityOfEclipticFunction() {
            return new PolynomialNutation(84381.448    * Constants.ARC_SECONDS_TO_RADIANS,
                                            -46.8150   * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.00059  * Constants.ARC_SECONDS_TO_RADIANS,
                                              0.001813 * Constants.ARC_SECONDS_TO_RADIANS);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getEquationOfEquinoxesCorrectionFunction() {
            return new NutationFunction() {

                /** First Moon correction term for the Equation of the Equinoxes. */
                private final double eqe1 =     0.00264  * Constants.ARC_SECONDS_TO_RADIANS;

                /** Second Moon correction term for the Equation of the Equinoxes. */
                private final double eqe2 =     0.000063 * Constants.ARC_SECONDS_TO_RADIANS;

                /** Start date for applying Moon corrections to the equation of the equinoxes.
                 * This date corresponds to 1997-02-27T00:00:00 UTC, hence the 30s offset from TAI.
                 */
                private final AbsoluteDate newEQEModelStart =
                    new AbsoluteDate(1997, 2, 27, 0, 0, 30, TimeScalesFactory.getTAI());

                /** {@inheritDoc} */
                @Override
                public double value(final BodiesElements elements) {
                    if (elements.getDate().compareTo(newEQEModelStart) >= 0) {

                        // IAU 1994 resolution C7 added two terms to the equation of equinoxes
                        // taking effect since 1997-02-27 for continuity

                        // Mean longitude of the ascending node of the Moon
                        final double om = elements.getOmega();

                        // add the two correction terms
                        return eqe1 * FastMath.sin(om) + eqe2 * FastMath.sin(om + om);

                    } else {
                        return 0.0;
                    }
                }

            };
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getXFunction() throws OrekitException {

            // X = 2004.3109″t - 0.42665″t² - 0.198656″t³ + 0.0000140″t⁴
            //     + 0.00006″t² cos Ω + sin ε0 { Σ [(Ai + Ai' t) sin(ARGUMENT) + Ai'' t cos(ARGUMENT)]}
            //     + 0.00204″t² sin Ω + 0.00016″t² sin 2(F - D + Ω),
            final PolynomialNutation polynomial =
                    new PolynomialNutation(0,
                                           2004.3109 * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.42665  * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.198656 * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0000140 * Constants.ARC_SECONDS_TO_RADIANS);

            final double fCosOm    = 0.00006 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSinOm    = 0.00204 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSin2FDOm = 0.00016 * Constants.ARC_SECONDS_TO_RADIANS;
            final double sinEps0   = FastMath.sin(getEpsilon0());

            final NutationFunction sum =
                    loadPoissonSeries(IERS_BASE + "1996/tab5.4-x.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);

            return new NutationFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final BodiesElements elements) {
                    final double omega     = elements.getOmega();
                    final double f         = elements.getF();
                    final double d         = elements.getD();
                    final double t         = elements.getTC();
                    final double cosOmega  = FastMath.cos(omega);
                    final double sinOmega  = FastMath.sin(omega);
                    final double cos2FDOm  = FastMath.cos(2 * (f - d + omega));
                    return polynomial.value(elements) + sinEps0 * sum.value(elements) +
                           t * t * (fCosOm * cosOmega + fSinOm * sinOmega + fSin2FDOm * cos2FDOm);
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getYFunction() throws OrekitException {

            // Y = -0.00013″ - 22.40992″t² + 0.001836″t³ + 0.0011130″t⁴
            //     + Σ [(Bi + Bi' t) cos(ARGUMENT) + Bi'' t sin(ARGUMENT)]
            //    - 0.00231″t² cos Ω − 0.00014″t² cos 2(F - D + Ω)
            final PolynomialNutation polynomial =
                    new PolynomialNutation(-0.00013,
                                           0.0,
                                           -22.40992 * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.001836  * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0011130 * Constants.ARC_SECONDS_TO_RADIANS);

            final double fCosOm    = -0.00231 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fCos2FDOm = -0.00014 * Constants.ARC_SECONDS_TO_RADIANS;

            final NutationFunction sum =
                    loadPoissonSeries(IERS_BASE + "1996/tab5.4-y.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);

            return new NutationFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final BodiesElements elements) {
                    final double omega    = elements.getOmega();
                    final double f        = elements.getF();
                    final double d        = elements.getD();
                    final double t        = elements.getTC();
                    final double cosOmega = FastMath.cos(omega);
                    final double cos2FDOm = FastMath.cos(2 * (f - d + omega));
                    return polynomial.value(elements) + sum.value(elements) +
                           t * t * (fCosOm * cosOmega + fCos2FDOm * cos2FDOm);
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getSXY2XFunction() throws OrekitException {
            // s = -XY/2 + 0.00385″t - 0.07259″t³ - 0.00264″ sin Ω - 0.00006″ sin 2Ω
            //     + 0.00074″t² sin Ω + 0.00006″t² sin 2(F - D + Ω)

            final double fT          =  0.00385 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fT3         = -0.07259 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSinOm      = -0.00264 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSin2Om     = -0.00006 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fT2SinOm    =  0.00074 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fT2Sin2FDOm =  0.00006 * Constants.ARC_SECONDS_TO_RADIANS;

            return new NutationFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final BodiesElements elements) {
                    final double omega     = elements.getOmega();
                    final double f         = elements.getF();
                    final double d         = elements.getD();
                    final double t         = elements.getTC();
                    final double sinOmega  = FastMath.sin(omega);
                    final double sin2Omega = FastMath.sin(2 * omega);
                    final double sin2FDOm  = FastMath.sin(2 * (f - d + omega));
                    return fSinOm * sinOmega + fSin2Om * sin2Omega +
                           t * (fT + t * (fT2SinOm * sinOmega + fT2Sin2FDOm * sin2FDOm + t * fT3));
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException {
            return new StellarAngleCapitaine(this);
        }

    },

    /** Constant for IERS 2003 conventions. */
    IERS_2003 {

        /** {@inheritDoc} */
        @Override
        public boolean precessionSupported() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public boolean nonRotatingOriginSupported() {
            return true;
        }

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments() throws OrekitException {
            return loadArguments(IERS_BASE + "2003/nutation-arguments.txt");
        }

        /** {@inheritDoc} */
        @Override
        public double getEpsilon0() {
            // value from chapter 5, page 41
            return 84381.448 * Constants.ARC_SECONDS_TO_RADIANS;
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getXFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "2003/tab5.2a.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getYFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "2003/tab5.2b.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getSXY2XFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "2003/tab5.2c.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException {
            return new StellarAngleCapitaine(this);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getPrecessionZetaFunction() throws OrekitException {
            // the following values are from equation 33 in IERS 2003 conventions
            return new PolynomialNutation(2.5976176    * Constants.ARC_SECONDS_TO_RADIANS,
                                          2306.0809506 * Constants.ARC_SECONDS_TO_RADIANS,
                                          0.3019015    * Constants.ARC_SECONDS_TO_RADIANS,
                                          0.0179663    * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.0000327   * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.0000002   * Constants.ARC_SECONDS_TO_RADIANS);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getPrecessionThetaFunction() throws OrekitException {
            // the following values are from equation 33 in IERS 2003 conventions
            return new PolynomialNutation(0.0,
                                          2004.1917476 * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.4269353   * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.0418251   * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.0000601   * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.0000001   * Constants.ARC_SECONDS_TO_RADIANS);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getPrecessionZFunction() throws OrekitException {
            // the following values are from equation 33 in IERS 2003 conventions
            return new PolynomialNutation(-2.5976176   * Constants.ARC_SECONDS_TO_RADIANS,
                                          2306.0803226 * Constants.ARC_SECONDS_TO_RADIANS,
                                          1.0947790    * Constants.ARC_SECONDS_TO_RADIANS,
                                          0.0182273    * Constants.ARC_SECONDS_TO_RADIANS,
                                          0.0000470    * Constants.ARC_SECONDS_TO_RADIANS,
                                          -0.0000003   * Constants.ARC_SECONDS_TO_RADIANS);
        }

    },

    /** Constant for IERS 2010 conventions. */
    IERS_2010 {

        /** {@inheritDoc} */
        @Override
        public boolean nonRotatingOriginSupported() {
            return true;
        }

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments() throws OrekitException {
            return loadArguments(IERS_BASE + "2010/nutation-arguments.txt");
        }

        /** {@inheritDoc} */
        @Override
        public double getEpsilon0() {
            // value from chapter 5, page 56
            return 84381.406 * Constants.ARC_SECONDS_TO_RADIANS;
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getXFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "2010/tab5.2a.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getYFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "2010/tab5.2b.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction getSXY2XFunction() throws OrekitException {
            return loadPoissonSeries(IERS_BASE + "2010/tab5.2d.txt",
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                     Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException {
            return new StellarAngleCapitaine(this);
        }

    };

    /** IERS conventions resources base directory. */
    private static final String IERS_BASE = "/assets/org/orekit/IERS-conventions/";

    /** Check if {@link #getPrecessionZetaFunction()}, {@link #getPrecessionThetaFunction()}
     * and {@link #getPrecessionZFunction()} methods are supported.
     * @return true if all methods are supported, false if calling any of them would
     * trigger an exception
     */
    public boolean precessionSupported() {
        // by default, we consider we do not support precession functions
        return false;
    }

    /** Check if {@link #getNutationInLongitudeFunction()}, {@link
     * #getNutationInObliquityFunction()}, {link {@link #getEquationOfEquinoxesCorrectionFunction()}
     * and {@link #getMeanObliquityOfEclipticFunction()} methods are supported.
     * @return true if all methods are supported, false if calling any of them would
     * trigger an exception
     */
    public boolean nutationSupported() {
        // by default, we consider we do not support nutation functions
        return false;
    }

    /** Check if {@link #getXFunction()}, {@link #getYFunction()}, {@link
     * #getSXY2XFunction()} and {@link #getEarthOrientationAngleFunction()} methods are supported.
     * @return true if all methods are supported, false if calling any of them would
     * trigger an exception
     */
    public boolean nonRotatingOriginSupported() {
        // by default, we consider we do not support non-rotating origin functions
        return false;
    }

    /** Get the fundamental nutation arguments.
     * @return fundamental nutation arguments
     * @exception OrekitException if fundamental nutation arguments cannot be loaded
     */
    public abstract FundamentalNutationArguments getNutationArguments() throws OrekitException;

    /** Get the obliquity of the ecliptic at J2000.0 (for nutation models).
     * <p>
     * This value is the one used for IAU precession/nutation models, it <em>may</em>
     * be different from the one listed in the numerical standards at the beginning
     * of each conventions. As an example consider IERS conventions 1996. This value
     * is set to 84381.448 arcseconds for nutation models (chapter 5, page 25), but the
     * value from the numerical standards (chapter 4, page 19) is 84381.412 arcseconds.
     * </p>
     * @return obliquity of the ecliptic at J2000.0 (for nutation models)
     */
    public abstract double getEpsilon0();

    /** Get the function computing the X pole component.
     * @return function computing the X pole component
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getXFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "xCIP", toString());
    }

    /** Get the function computing the Y pole component.
     * @return function computing the Y pole component
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getYFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "yCIP", toString());

    }

    /** Get the function computing the S + XY/2 pole component.
     * @return function computing the S + XY/2 pole component
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getSXY2XFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "sCIO", toString());

    }

    /** Get the function computing the raw Earth Orientation Angle.
     * <p>
     * The raw angle does not contain any correction. If for example dTU1 correction
     * due to tidal effect is desired, it must be added afterward by the caller.
     * The returned value contain the angle as the value and the angular rate as
     * the first derivative.
     * </p>
     * @return function computing the rawEarth Orientation Angle, in the non-rotating origin paradigm,
     * the return value containing both the angle and its first time derivative
     * @exception OrekitException if table cannot be loaded
     */
    public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "ERA", toString());

    }

    /** Get the function computing the precession angle &zeta;<sub>A</sub>.
     * @return function computing the precession angle &zeta;<sub>A</sub>
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getPrecessionZetaFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "\u03b6A", toString());

    }

    /** Get the function computing the precession angle &theta;<sub>A</sub>.
     * @return function computing the precession angle &theta;<sub>A</sub>
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getPrecessionThetaFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "\u03b8A", toString());

    }

    /** Get the function computing the precession angle z<sub>A</sub>.
     * @return function computing the precession angle z<sub>A</sub>
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getPrecessionZFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "zA", toString());

    }

    /** Get the function computing the nutation in longitude &Delta;&Psi;.
     * @return function computing the nutation in longitude &Delta;&Psi;
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getNutationInLongitudeFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "\u0394\u03c8", toString());

    }

    /** Get the function computing the nutation in obliquity &Delta;&epsilon;.
     * @return function computing the nutation in obliquity &Delta;&epsilon;
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getNutationInObliquityFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "\u0394\u03b5", toString());

    }

    /** Get the function computing the mean obliquity of ecliptic &epsilon;<sub>A</sub>.
     * @return function computing the mean obliquity of ecliptic &epsilon;<sub>A</sub>
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getMeanObliquityOfEclipticFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "\u03b5A", toString());

    }

    /** Get the function computing a possible correction to the Equation of the Equinoxes.
     * @return function computing a possible correction to the Equation of the Equinoxes
     * @exception OrekitException if table cannot be loaded
     */
    public NutationFunction getEquationOfEquinoxesCorrectionFunction() throws OrekitException {
        // default implementation triggers an error
        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER,
                                  "\u0394eqe", toString());

    }

    /** Load a series development model.
     * @param name file name of the series development
     * @param polyFactor multiplicative factor to use for polynomial coefficients
     * @param nonPolyFactor multiplicative factor to use for non-ploynomial coefficients
     * @return series development model
     * @exception OrekitException if table cannot be loaded
     */
    private static NutationFunction loadPoissonSeries(final String name,
                                                      final double polyFactor,
                                                      final double nonPolyFactor)
        throws OrekitException {

        // get the table data
        final InputStream stream = IERSConventions.class.getResourceAsStream(name);

        return new PoissonSeries(stream, name, polyFactor, nonPolyFactor);

    }

    /** Load fundamental nutation arguments.
     * @param name file name of the fundamental arguments expressions
     * @return fundamental nutation arguments
     * @exception OrekitException if table cannot be loaded
     */
    private static FundamentalNutationArguments loadArguments(final String name)
        throws OrekitException {

        // get the table data
        final InputStream stream = IERSConventions.class.getResourceAsStream(name);

        return new FundamentalNutationArguments(stream, name);

    }

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
        private final AbsoluteDate eraReference;

        /** Constant term of Capitaine's Earth Rotation Angle model. */
        private final double era0;

        /** Rate term of Capitaine's Earth Rotation Angle model.
         * (radians per day, main part) */
        private final double era1A;

        /** Rate term of Capitaine's Earth Rotation Angle model.
         * (radians per day, fractional part) */
        private final double era1B;

        /** Total rate term of Capitaine's Earth Rotation Angle model. */
        private final double era1AB;

        /** UT1 time scale. */
        private final TimeScale ut1;

        /** Simple constructor.
         * @param conventions IERS conventions to which this function applies
         * @exception OrekitException if UT1 time scale cannot be retrieved
         */
        public StellarAngleCapitaine(final IERSConventions conventions) throws OrekitException {

            // constants for Capitaine's Earth Rotation Angle model
            eraReference =
                    new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, TimeScalesFactory.getTAI());
            era0   = MathUtils.TWO_PI * 0.7790572732640;
            era1A  = MathUtils.TWO_PI / Constants.JULIAN_DAY;
            era1B  = era1A * 0.00273781191135448;

            // store the toal rate to avoid computing the same addition over and over
            era1AB = era1A + era1B;

            ut1 = TimeScalesFactory.getUT1(conventions);

        }

        /** {@inheritDoc} */
        @Override
        public DerivativeStructure value(final AbsoluteDate date) {

            // split the date offset as a full number of days plus a smaller part
            final int secondsInDay = 86400;
            final double dt  = date.durationFrom(eraReference);
            final long days  = ((long) dt) / secondsInDay;
            final double dtA = secondsInDay * days;
            final double dtB = (dt - dtA) + ut1.offsetFromTAI(date);

            return new DerivativeStructure(1, 1,
                                           era0 + era1A * dtB + era1B * (dtA + dtB),
                                           era1AB);

        }

    }

}
