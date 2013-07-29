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

import org.apache.commons.math3.util.FastMath;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.NutationFunction;
import org.orekit.data.PoissonSeries;
import org.orekit.data.PolynomialNutation;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
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
        public FundamentalNutationArguments getNutationArguments() throws OrekitException {
            return loadArguments(IERS_BASE + "1996/nutation-arguments.txt");
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

    /** Check if {@link #getXFunction()}, {@link #getYFunction()} and {@link
     * #getSXY2XFunction()} methods are supported.
     * @return true if all methods are supported, false if calling any of them would
     * trigger an exception
     */
    public boolean nonRotatingOriginSupported() {
        // by default, we consider we do not support non-rotatin origin functions
        return false;
    }

    /** Get the fundamental nutation arguments.
     * @return fundamental nutation arguments
     * @exception OrekitException if fundamental nutation arguments cannot be loaded
     */
    public abstract FundamentalNutationArguments getNutationArguments() throws OrekitException;

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

}
