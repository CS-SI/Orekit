/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;

/** This class contains methods to compute propagated coordinates with the SGP4 model.
 * <p>
 * The user should not bother in this class since it is handled internaly by the
 * {@link TLEPropagator}.
 * </p>
 * <p>This implementation is largely inspired from the paper and source code <a
 * href="http://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack
 * Report #3</a> and is fully compliant with its results and tests cases.</p>
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 */
public class SGP4 extends TLEPropagator {

    /** If perige is less than 220 km, some calculus are avoided. */
    private boolean lessThan220;

    /** (1 + eta * cos(M0))³. */
    private double delM0;

    // CHECKSTYLE: stop JavadocVariable check
    private double d2;
    private double d3;
    private double d4;
    private double t3cof;
    private double t4cof;
    private double t5cof;
    private double sinM0;
    private double omgcof;
    private double xmcof;
    private double c5;
    // CHECKSTYLE: resume JavadocVariable check

    /** Constructor for a unique initial TLE.
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @exception OrekitException if some specific error occurs
     */
    public SGP4(final TLE initialTLE, final AttitudeProvider attitudeProvider,
                       final double mass) throws OrekitException {
        super(initialTLE, attitudeProvider, mass);
    }

    /** Initialization proper to each propagator (SGP or SDP).
     */
    protected void sxpInitialize() {

        // For perigee less than 220 kilometers, the equations are truncated to
        // linear variation in sqrt a and quadratic variation in mean anomaly.
        // Also, the c3 term, the delta omega term, and the delta m term are dropped.
        lessThan220 = perige < 220;
        if (!lessThan220) {
            final double c1sq = c1 * c1;
            delM0 = 1.0 + eta * FastMath.cos(tle.getMeanAnomaly());
            delM0 *= delM0 * delM0;
            d2 = 4 * a0dp * tsi * c1sq;
            final double temp = d2 * tsi * c1 / 3.0;
            d3 = (17 * a0dp + s4) * temp;
            d4 = 0.5 * temp * a0dp * tsi * (221 * a0dp + 31 * s4) * c1;
            t3cof = d2 + 2 * c1sq;
            t4cof = 0.25 * (3 * d3 + c1 * (12 * d2 + 10 * c1sq));
            t5cof = 0.2 * (3 * d4 + 12 * c1 * d3 + 6 * d2 * d2 + 15 * c1sq * (2 * d2 + c1sq));
            sinM0 = FastMath.sin(tle.getMeanAnomaly());
            if (tle.getE() < 1e-4) {
                omgcof = 0.;
                xmcof = 0.;
            } else  {
                final double c3 = coef * tsi * TLEConstants.A3OVK2 * xn0dp *
                                  TLEConstants.NORMALIZED_EQUATORIAL_RADIUS * sini0 / tle.getE();
                xmcof = -TLEConstants.TWO_THIRD * coef * tle.getBStar() *
                        TLEConstants.NORMALIZED_EQUATORIAL_RADIUS / eeta;
                omgcof = tle.getBStar() * c3 * FastMath.cos(tle.getPerigeeArgument());
            }
        }

        c5 = 2 * coef1 * a0dp * beta02 * (1 + 2.75 * (etasq + eeta) + eeta * etasq);
        // initialized
    }

    /** Propagation proper to each propagator (SGP or SDP).
     * @param tSince the offset from initial epoch (min)
     */
    protected void sxpPropagate(final double tSince) {

        // Update for secular gravity and atmospheric drag.
        final double xmdf = tle.getMeanAnomaly() + xmdot * tSince;
        final double omgadf = tle.getPerigeeArgument() + omgdot * tSince;
        final double xn0ddf = tle.getRaan() + xnodot * tSince;
        omega = omgadf;
        double xmp = xmdf;
        final double tsq = tSince * tSince;
        xnode = xn0ddf + xnodcf * tsq;
        double tempa = 1 - c1 * tSince;
        double tempe = tle.getBStar() * c4 * tSince;
        double templ = t2cof * tsq;

        if (!lessThan220) {
            final double delomg = omgcof * tSince;
            double delm = 1. + eta * FastMath.cos(xmdf);
            delm = xmcof * (delm * delm * delm - delM0);
            final double temp = delomg + delm;
            xmp = xmdf + temp;
            omega = omgadf - temp;
            final double tcube = tsq * tSince;
            final double tfour = tSince * tcube;
            tempa = tempa - d2 * tsq - d3 * tcube - d4 * tfour;
            tempe = tempe + tle.getBStar() * c5 * (FastMath.sin(xmp) - sinM0);
            templ = templ + t3cof * tcube + tfour * (t4cof + tSince * t5cof);
        }

        a = a0dp * tempa * tempa;
        e = tle.getE() - tempe;

        // A highly arbitrary lower limit on e,  of 1e-6:
        if (e < 1e-6) {
            e = 1e-6;
        }

        xl = xmp + omega + xnode + xn0dp * templ;

        i = tle.getI();

    }

}
