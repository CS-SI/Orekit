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

import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.PoissonSeries;
import org.orekit.errors.OrekitException;


/** Supported IERS conventions.
 * @since 6.0
 * @author Luc Maisonobe
 */
public enum IERSConventions {

    /** Constant for IERS 1996 conventions. */
    IERS_1996 {

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments() throws OrekitException {
            return loadArguments(IERS_BASE + "1996/nutation-arguments.txt");
        }

        /** {@inheritDoc} */
        public PoissonSeries getXSeries() throws OrekitException {
            return loadModel(IERS_BASE + "1996/tab5.4x.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        public PoissonSeries getYSeries() throws OrekitException {
            return loadModel(IERS_BASE + "1996/tab5.4y.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        public PoissonSeries getSXY2XSeries() throws OrekitException {
            return loadModel(IERS_BASE + "1996/s-equation.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

    },

    /** Constant for IERS 2003 conventions. */
    IERS_2003 {

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments() throws OrekitException {
            return loadArguments(IERS_BASE + "2003/nutation-arguments.txt");
        }

        /** {@inheritDoc} */
        public PoissonSeries getXSeries() throws OrekitException {
            return loadModel(IERS_BASE + "2003/tab5.2a.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        public PoissonSeries getYSeries() throws OrekitException {
            return loadModel(IERS_BASE + "2003/tab5.2b.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        public PoissonSeries getSXY2XSeries() throws OrekitException {
            return loadModel(IERS_BASE + "2003/tab5.2c.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

    },

    /** Constant for IERS 2010 conventions. */
    IERS_2010 {

        /** {@inheritDoc} */
        public FundamentalNutationArguments getNutationArguments() throws OrekitException {
            return loadArguments(IERS_BASE + "2010/nutation-arguments.txt");
        }

        /** {@inheritDoc} */
        public PoissonSeries getXSeries() throws OrekitException {
            return loadModel(IERS_BASE + "2010/tab5.2a.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        public PoissonSeries getYSeries() throws OrekitException {
            return loadModel(IERS_BASE + "2010/tab5.2b.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

        /** {@inheritDoc} */
        public PoissonSeries getSXY2XSeries() throws OrekitException {
            return loadModel(IERS_BASE + "2010/tab5.2d.txt",
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6,
                             Constants.ARC_SECONDS_TO_RADIANS * 1.0e-6);
        }

    };

    /** IERS conventions resources base directory. */
    private static final String IERS_BASE = "/assets/org/orekit/IERS-conventions/";

    /** Get the fundamental nutation arguments.
     * @return fundamental nutation arguments
     * @exception OrekitException if fundamental nutation arguments cannot be loaded
     */
    public abstract FundamentalNutationArguments getNutationArguments() throws OrekitException;

    /** Get the {@link PoissonSeries Poisson series} for the X pole component.
     * @return {@link PoissonSeries Poisson series} for the X pole component
     * @exception OrekitException if table cannot be loaded
     */
    public abstract PoissonSeries getXSeries() throws OrekitException;

    /** Get the {@link PoissonSeries Poisson series} for the Y pole component.
     * @return {@link PoissonSeries Poisson series} for the Y pole component
     * @exception OrekitException if table cannot be loaded
     */
    public abstract PoissonSeries getYSeries() throws OrekitException;

    /** Get the {@link PoissonSeries Poisson series} for the S + XY/2 pole component.
     * @return {@link PoissonSeries Poisson series} for the S + XY/2 pole component
     * @exception OrekitException if table cannot be loaded
     */
    public abstract PoissonSeries getSXY2XSeries() throws OrekitException;

    /** Load a series development model.
     * @param name file name of the series development
     * @param polyFactor multiplicative factor to use for polynomial coefficients
     * @param nonPolyFactor multiplicative factor to use for non-ploynomial coefficients
     * @return series development model
     * @exception OrekitException if table cannot be loaded
     */
    protected static PoissonSeries loadModel(final String name,
                                             final double polyFactor, final double nonPolyFactor)
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
    protected static FundamentalNutationArguments loadArguments(final String name)
        throws OrekitException {

        // get the table data
        final InputStream stream = IERSConventions.class.getResourceAsStream(name);

        return new FundamentalNutationArguments(stream, name);

    }

}
