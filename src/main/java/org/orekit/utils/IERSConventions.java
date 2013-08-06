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
        public boolean nutationSupported() {
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
        public NutationFunction<double[]> getPrecessionFunction() throws OrekitException {

            // set up the conventional polynomials
            final PolynomialNutation zetaA =
                    new PolynomialNutation(0.0,
                                           2306.2181 * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.30188   * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.017998  * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation thetaA =
                    new PolynomialNutation(0.0,
                                           2004.3109 * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.42665  * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.041833 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation zA =
                    new PolynomialNutation(0.0,
                                           2306.2181 * Constants.ARC_SECONDS_TO_RADIANS,
                                           1.09468   * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.018203  * Constants.ARC_SECONDS_TO_RADIANS);

            return new NutationFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        zetaA.value(elements), thetaA.value(elements), zA.value(elements)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction<double[]> getNutationFunction() throws OrekitException {

            final PoissonSeries psiSeries =
                    loadPoissonSeries(IERS_BASE + "1996/tab5.1-psi.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);
            final PoissonSeries epsilonSeries =
                    loadPoissonSeries(IERS_BASE + "1996/tab5.1-epsilon.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);
            final PolynomialNutation moePolynomial =
                    new PolynomialNutation(84381.448    * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.8150   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.00059  * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.001813 * Constants.ARC_SECONDS_TO_RADIANS);
            final IAU1994ResolutionC7 eqeCorrectionFunction = new IAU1994ResolutionC7();

            return new NutationFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        psiSeries.value(elements), epsilonSeries.value(elements),
                        moePolynomial.value(elements), eqeCorrectionFunction.value(elements)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction<double[]> getXYSpXY2Function() throws OrekitException {

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
            final double sinEps0   = FastMath.sin(getEpsilon0());

            final PoissonSeries xSum =
                    loadPoissonSeries(IERS_BASE + "1996/tab5.4-x.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);

            // Y = -0.00013″ - 22.40992″t² + 0.001836″t³ + 0.0011130″t⁴
            //     + Σ [(Bi + Bi' t) cos(ARGUMENT) + Bi'' t sin(ARGUMENT)]
            //    - 0.00231″t² cos Ω − 0.00014″t² cos 2(F - D + Ω)
            final PolynomialNutation yPolynomial =
                    new PolynomialNutation(-0.00013,
                                           0.0,
                                           -22.40992 * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.001836  * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0011130 * Constants.ARC_SECONDS_TO_RADIANS);

            final double fYCosOm    = -0.00231 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fYCos2FDOm = -0.00014 * Constants.ARC_SECONDS_TO_RADIANS;

            final PoissonSeries ySum =
                    loadPoissonSeries(IERS_BASE + "1996/tab5.4-y.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-4);

            // s = -XY/2 + 0.00385″t - 0.07259″t³ - 0.00264″ sin Ω - 0.00006″ sin 2Ω
            //     + 0.00074″t² sin Ω + 0.00006″t² sin 2(F - D + Ω)
            final double fST          =  0.00385 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST3         = -0.07259 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSSinOm      = -0.00264 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fSSin2Om     = -0.00006 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST2SinOm    =  0.00074 * Constants.ARC_SECONDS_TO_RADIANS;
            final double fST2Sin2FDOm =  0.00006 * Constants.ARC_SECONDS_TO_RADIANS;

            return new NutationFunction<double[]>() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {

                    final double omega     = elements.getOmega();
                    final double f         = elements.getF();
                    final double d         = elements.getD();
                    final double t         = elements.getTC();

                    final double cosOmega  = FastMath.cos(omega);
                    final double sinOmega  = FastMath.sin(omega);
                    final double sin2Omega = FastMath.sin(2 * omega);
                    final double cos2FDOm  = FastMath.cos(2 * (f - d + omega));
                    final double sin2FDOm  = FastMath.sin(2 * (f - d + omega));

                    final double x = xPolynomial.value(elements) + sinEps0 * xSum.value(elements) +
                            t * t * (fXCosOm * cosOmega + fXSinOm * sinOmega + fXSin2FDOm * cos2FDOm);
                    final double y = yPolynomial.value(elements) + ySum.value(elements) +
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
        public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException {
            return new StellarAngleCapitaine(this);
        }

    },

    /** Constant for IERS 2003 conventions. */
    IERS_2003 {

        /** {@inheritDoc} */
        @Override
        public boolean nutationSupported() {
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
        public NutationFunction<double[]> getNutationFunction() throws OrekitException {

            final PoissonSeries psiSeries =
                    loadPoissonSeries(IERS_BASE + "2003/tab5.3-psi.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3);
            final PoissonSeries epsilonSeries =
                    loadPoissonSeries(IERS_BASE + "2003/tab5.3-epsilon.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-3);

            // value from chapter 5, equation 32, page 45
            final PolynomialNutation moePolynomial =
                    new PolynomialNutation(84381.448    * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.84024  * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.00059  * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.001813 * Constants.ARC_SECONDS_TO_RADIANS);

            final IAU1994ResolutionC7 eqeCorrectionFunction = new IAU1994ResolutionC7();

            return new NutationFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        psiSeries.value(elements), epsilonSeries.value(elements),
                        moePolynomial.value(elements), eqeCorrectionFunction.value(elements)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction<double[]> getXYSpXY2Function() throws OrekitException {

            // load the Poisson series
            final PoissonSeries xSeries = loadPoissonSeries(IERS_BASE + "2003/tab5.2a.txt",
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
            final PoissonSeries ySeries = loadPoissonSeries(IERS_BASE + "2003/tab5.2b.txt",
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
            final PoissonSeries sSeries = loadPoissonSeries(IERS_BASE + "2003/tab5.2c.txt",
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);

            // create a function evaluating the series
            return new NutationFunction<double[]>() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        xSeries.value(elements), ySeries.value(elements), sSeries.value(elements)
                    };
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException {
            return new StellarAngleCapitaine(this);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction<double[]> getPrecessionFunction() throws OrekitException {

            // set up the conventional polynomials
            // the following values are from equation 33 in IERS 2003 conventions
            final PolynomialNutation zetaA =
                    new PolynomialNutation(2.5976176    * Constants.ARC_SECONDS_TO_RADIANS,
                                           2306.0809506 * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.3019015    * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0179663    * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.0000327   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.0000002   * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation thetaA =
                    new PolynomialNutation(0.0,
                                           2004.1917476 * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.4269353   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.0418251   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.0000601   * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.0000001   * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation zA =
                    new PolynomialNutation(-2.5976176   * Constants.ARC_SECONDS_TO_RADIANS,
                                           2306.0803226 * Constants.ARC_SECONDS_TO_RADIANS,
                                           1.0947790    * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0182273    * Constants.ARC_SECONDS_TO_RADIANS,
                                           0.0000470    * Constants.ARC_SECONDS_TO_RADIANS,
                                           -0.0000003   * Constants.ARC_SECONDS_TO_RADIANS);

            return new NutationFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        zetaA.value(elements), thetaA.value(elements), zA.value(elements)
                    };
                }
            };

        }

    },

    /** Constant for IERS 2010 conventions. */
    IERS_2010 {

        /** {@inheritDoc} */
        @Override
        public boolean nutationSupported() {
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
        public NutationFunction<double[]> getNutationFunction() throws OrekitException {

            final PoissonSeries psiSeries =
                    loadPoissonSeries(IERS_BASE + "2010/tab5.3a.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
            final PoissonSeries epsilonSeries =
                    loadPoissonSeries(IERS_BASE + "2010/tab5.3b.txt",
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                      Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);

            // value from section 5.6.4, page 64 for epsilon0
            // and page 65 equation 5.40 for the other terms
            final PolynomialNutation moePolynomial =
                    new PolynomialNutation(84381.406        * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.836769     * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0001831    * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.00200340   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.000000576  * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0000000434 * Constants.ARC_SECONDS_TO_RADIANS);

            final IAU1994ResolutionC7 eqeCorrectionFunction = new IAU1994ResolutionC7();

            return new NutationFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        psiSeries.value(elements), epsilonSeries.value(elements),
                        moePolynomial.value(elements), eqeCorrectionFunction.value(elements)
                    };
                }
            };

        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction<double[]> getXYSpXY2Function() throws OrekitException {

            // load the Poisson series
            final PoissonSeries xSeries = loadPoissonSeries(IERS_BASE + "2010/tab5.2a.txt",
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
            final PoissonSeries ySeries = loadPoissonSeries(IERS_BASE + "2010/tab5.2b.txt",
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
            final PoissonSeries sSeries = loadPoissonSeries(IERS_BASE + "2010/tab5.2d.txt",
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                                                            Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);

            // create a function evaluating the series
            return new NutationFunction<double[]>() {

                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        xSeries.value(elements), ySeries.value(elements), sSeries.value(elements)
                    };
                }

            };

        }

        /** {@inheritDoc} */
        @Override
        public TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException {
            return new StellarAngleCapitaine(this);
        }

        /** {@inheritDoc} */
        @Override
        public NutationFunction<double[]> getPrecessionFunction() throws OrekitException {

            // set up the conventional polynomials
            // the following values are from equation 5.40 in IERS 2010 conventions
            final PolynomialNutation gammaBar =
                    new PolynomialNutation(   -0.052928     * Constants.ARC_SECONDS_TO_RADIANS,
                                              10.556378     * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.4932044    * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.00031238   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.000002788  * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.0000000260 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation phiBar =
                    new PolynomialNutation(84381.412819     * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.811016     * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.0511268    * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.00053289   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.000000440  * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0000000176 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation psiBar =
                    new PolynomialNutation(   -0.041775     * Constants.ARC_SECONDS_TO_RADIANS,
                                            5038.481484     * Constants.ARC_SECONDS_TO_RADIANS,
                                               1.5584175    * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.00018522   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.000026452  * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0000000148 * Constants.ARC_SECONDS_TO_RADIANS);
            final PolynomialNutation epsilonBar =
                    new PolynomialNutation(84381.406        * Constants.ARC_SECONDS_TO_RADIANS,
                                             -46.836769     * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0001831    * Constants.ARC_SECONDS_TO_RADIANS,
                                               0.00200340   * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.000000576  * Constants.ARC_SECONDS_TO_RADIANS,
                                              -0.0000000434 * Constants.ARC_SECONDS_TO_RADIANS);

            return new NutationFunction<double[]>() {
                /** {@inheritDoc} */
                @Override
                public double[] value(final BodiesElements elements) {
                    return new double[] {
                        gammaBar.value(elements), phiBar.value(elements),
                        psiBar.value(elements), epsilonBar.value(elements)
                    };
                }
            };

        }

    };

    /** IERS conventions resources base directory. */
    private static final String IERS_BASE = "/assets/org/orekit/IERS-conventions/";

    /** Check if {@link #getNutationInLongitudeFunction()}, {@link
     * #getNutationInObliquityFunction()}, {@link #getEquationOfEquinoxesCorrectionFunction()}
     * and {@link #getMeanObliquityOfEclipticFunction()} methods are supported.
     * @return true if all methods are supported, false if calling any of them would
     * trigger an exception
     */
    public boolean nutationSupported() {
        // by default, we consider we do not support nutation functions
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

    /** Get the function computing the Celestial Intermediate Pole and Celestial Intermediate Origin components.
     * <p>
     * The returned function computes the two X, Y commponents of CIP and the S+XY/2 component of the non-rotating CIO.
     * </p>
     * @return function computing the Celestial Intermediate Pole and Celestial Intermediate Origin components
     * @exception OrekitException if table cannot be loaded
     */
    public abstract NutationFunction<double[]> getXYSpXY2Function() throws OrekitException;

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
    public abstract TimeFunction<DerivativeStructure> getEarthOrientationAngleFunction() throws OrekitException;

    /** Get the function computing the precession angles.
     * <p>
     * The function returned computes either the three classical angles
     * &zeta;<sub>A</sub> (around Z axis), &theta;<sub>A</sub> (around Y axis)
     * and z<sub>A</sub> (around Z axis) or the four Fukushima-Williams angles
     * &gamma; (around Z axis), &phi; (around X axis), &psi; (around Z axis) and
     * &epsilon; (around X axis). The caller should check the number of components
     * in the returned array to identify which rotations set is used.
     * </p>
     * @return function computing the precession angle
     * @exception OrekitException if table cannot be loaded
     */
    public abstract NutationFunction<double[]> getPrecessionFunction() throws OrekitException;

    /** Get the function computing the nutation angles.
     * <p>
     * The function returned computes the two classical angles &Delta;&Psi; and &Delta;&epsilon;,
     * the mean obliquity of ecliptic &epsilon;<sub>A</sub>, and the correction to the
     * equation of equinoxes introduced since 1997-02-27 by IAU 1994 resolution C7 (the correction
     * is forced to 0 before this date)
     * </p>
     * @return function computing the nutation in longitude &Delta;&Psi; and &Delta;&epsilon;
     * @exception OrekitException if table cannot be loaded
     */
    public abstract NutationFunction<double[]> getNutationFunction() throws OrekitException;

    /** Load a series development model.
     * @param name file name of the series development
     * @param polyFactor multiplicative factor to use for polynomial coefficients
     * @param nonPolyFactor multiplicative factor to use for non-ploynomial coefficients
     * @return series development model
     * @exception OrekitException if table cannot be loaded
     */
    private static PoissonSeries loadPoissonSeries(final String name,
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

    /** Correction to equation of equinoxes.
     * <p>IAU 1994 resolution C7 added two terms to the equation of equinoxes
     * taking effect since 1997-02-27 for continuity
     * </p>
     */
    private static class IAU1994ResolutionC7 {

        /** First Moon correction term for the Equation of the Equinoxes. */
        private final double eqe1 =     0.00264  * Constants.ARC_SECONDS_TO_RADIANS;

        /** Second Moon correction term for the Equation of the Equinoxes. */
        private final double eqe2 =     0.000063 * Constants.ARC_SECONDS_TO_RADIANS;

        /** Start date for applying Moon corrections to the equation of the equinoxes.
         * This date corresponds to 1997-02-27T00:00:00 UTC, hence the 30s offset from TAI.
         */
        private final AbsoluteDate newEQEModelStart =
            new AbsoluteDate(1997, 2, 27, 0, 0, 30, TimeScalesFactory.getTAI());

        /** Evaluate the correction.
         * @param elements bodies elements for nutation
         * @return correction value (0 before 1997-02-27)
         */
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
