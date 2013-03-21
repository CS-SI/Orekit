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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Solar radiation pressure contribution to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  The solar radiation pressure acceleration is computed as follows:<br>
 *  &gamma; = (C<sub>R</sub> A / m) * (p<sub>ref</sub> * d<sup>2</sup><sub>ref</sub>) *
 *  (r<sub>sat</sub> - R<sub>sun</sub>) / |r<sub>sat</sub> - R<sub>sun</sub>|<sup>3</sup>
 *  </p>
 *
 *  @author Pascal Parraud
 */
public class DSSTSolarRadiationPressure extends AbstractGaussianContribution {

    /** Threshold for the choice of the Gauss quadrature order. */
    private static final double GAUSS_THRESHOLD = 1.0e-15;

    /** Threshold for shadow equation. */
    private static final double S_ZERO = 1.0e-6;

    /** Reference distance for the solar radiation pressure (m). */
    private static final double D_REF = 149597870000.0;

    /** Reference solar radiation pressure at D_REF (N/m<sup>2</sup>). */
    private static final double P_REF = 4.56e-6;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Flux on satellite:
     * kRef = C<sub>R</sub> * Area * P<sub>Ref</sub> * D<sub>Ref</sub><sup>2</sup>.
     */
    private final double                kRef;

    /** Central Body radius. */
    private final double                ae;

    /** satellite radiation pressure coefficient (assuming total specular reflection). */
    private final double                cr;

    /** Cross sectional area of satellite. */
    private final double                area;

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     *
     * @param cr satellite radiation pressure coefficient (assuming total specular reflection)
     * @param area cross sectionnal area of satellite
     * @param sun Sun model
     * @param equatorialRadius central body equatorial radius (for shadow computation)
     */
    public DSSTSolarRadiationPressure(final double cr, final double area,
                                      final PVCoordinatesProvider sun,
                                      final double equatorialRadius) {
        this(D_REF, P_REF, cr, area, sun, equatorialRadius);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked
     * to solar flux SF in W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light
     * (299792458 m/s). So at 1UA a 1367 W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup>
     * N/m<sup>2</sup> solar radiation pressure.
     * </p>
     *
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param cr satellite radiation pressure coefficient (assuming total specular reflection)
     * @param area cross sectionnal area of satellite
     * @param sun Sun model
     * @param equatorialRadius central body equatrial radius (for shadow computation)
     */
    public DSSTSolarRadiationPressure(final double dRef, final double pRef,
                                      final double cr, final double area,
                                      final PVCoordinatesProvider sun,
                                      final double equatorialRadius) {
        super(GAUSS_THRESHOLD);
        this.kRef = pRef * dRef * dRef * cr * area;
        this.area = area;
        this.cr   = cr;
        this.sun  = sun;
        this.ae   = equatorialRadius;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** {@inheritDoc} */
    protected Vector3D getAcceleration(final SpacecraftState state,
                                       final Vector3D position, final Vector3D velocity)
        throws OrekitException {

        final Vector3D sunSat = getSunSatVector(state, position);
        final double R = sunSat.getNorm();
        final double R3 = R * R * R;
        final double T = kRef / state.getMass();
        // raw radiation pressure
        return new Vector3D(T / R3, sunSat);
    }

    /** {@inheritDoc} */
    protected double[] getLLimits(final SpacecraftState state) throws OrekitException {
        // Default bounds without shadow [-PI, PI]
        final double[] ll = {-FastMath.PI, FastMath.PI};

        // Direction cosines of the Sun in the equinoctial frame
        final Vector3D sunDir = sun.getPVCoordinates(state.getDate(), state.getFrame()).getPosition().normalize();
        final double alpha = sunDir.dotProduct(f);
        final double beta  = sunDir.dotProduct(g);
        final double gamma = sunDir.dotProduct(w);

        // Compute limits only if the perigee is close enough from the central body to be in the shadow
        if (FastMath.abs(gamma * a * (1. - ecc)) < ae) {

            // Compute the coefficients of the quartic equation in cos(L) 3.5-(2)
            final double bet2 = beta * beta;
            final double h2 = h * h;
            final double k2 = k * k;
            final double m  = ae / (a * B);
            final double m2 = m * m;
            final double m4 = m2 * m2;
            final double bb = alpha * beta + m2 * h * k;
            final double b2 = bb * bb;
            final double cc = alpha * alpha - bet2 + m2 * (k2 - h2);
            final double dd = 1. - bet2 - m2 * (1. + h2);
            final double[] a = new double[5];
            a[0] = 4. * b2 + cc * cc;
            a[1] = 8. * bb * m2 * h + 4. * cc * m2 * k;
            a[2] = -4. * b2 + 4. * m4 * h2 - 2. * cc * dd + 4. * m4 * k2;
            a[3] = -8. * bb * m2 * h - 4. * dd * m2 * k;
            a[4] = -4. * m4 * h2 + dd * dd;
            // Compute the real roots of the quartic equation 3.5-2
            final double[] roots = new double[4];
            final int nbRoots = realQuarticRoots(a, roots);
            if (nbRoots > 0) {
                // Check for consistency
                boolean entryFound = false;
                boolean exitFound  = false;
                // Eliminate spurious roots
                for (int i = 0; i < nbRoots; i++) {
                    final double cosL = roots[i];
                    final double sL = FastMath.sqrt((1. - cosL) * (1. + cosL));
                    // Check both angles: L and -L
                    for (int j = -1; j <= 1; j += 2) {
                        final double sinL = j * sL;
                        final double cPhi = alpha * cosL + beta * sinL;
                        // Is the angle on the shadow side of the central body (eq. 3.5-3) ?
                        if (cPhi < 0.) {
                            final double range = 1. + k * cosL + h * sinL;
                            final double S  = 1. - m2 * range * range - cPhi * cPhi;
                            // Is the shadow equation 3.5-1 satisfied ?
                            if (FastMath.abs(S) < S_ZERO) {
                                // Is this the entry or exit angle ?
                                final double dSdL = m2 * range * (k * sinL - h * cosL) + cPhi * (alpha * sinL - beta * cosL);
                                if (dSdL > 0.) {
                                    // Exit from shadow: 3.5-4
                                    exitFound = true;
                                    ll[0] = FastMath.atan2(sinL, cosL);
                                } else {
                                    // Entry into shadow: 3.5-5
                                    entryFound = true;
                                    ll[1] = FastMath.atan2(sinL, cosL);
                                }
                            }
                        }
                    }
                }
                // Must be one entry and one exit or none
                if (!(entryFound == exitFound)) {
                    // entry or exit found but not both ! In this case, consider there is no eclipse...
                    ll[0] = -FastMath.PI;
                    ll[1] = FastMath.PI;
                }
                // Quadrature between L at exit and L at entry so Lexit must be lower than Lentry
                if (ll[0] > ll[1]) {
                    // Keep the angles between [-2PI, 2PI]
                    if (ll[1] < 0.) {
                        ll[1] += 2. * FastMath.PI;
                    } else {
                        ll[0] -= 2. * FastMath.PI;
                    }
                }
            }
        }
        return ll;
    }

    /** Get the central body equatorial radius.
     *  @return central body equatorial radius (m)
     */
    public double getEquatorialRadius() {
        return ae;
    }

    /** Get the satellite radiation pressure coefficient (assuming total specular reflection).
     *  @return satellite radiation pressure coefficient
     */
    public double getCr() {
        return cr;
    }

    /** Get the cross sectional area of satellite.
     *  @return cross sectional section (m<sup>2</sup>
     */
    public double getArea() {
        return area;
    }

    /**
     * Compute Sun-sat vector in SpacecraftState frame.
     * @param state current spacecraft state
     * @param position spacecraft position
     * @return Sun-sat vector in SpacecraftState frame
     * @exception OrekitException if sun position cannot be computed
     */
    private Vector3D getSunSatVector(final SpacecraftState state,
                                     final Vector3D position) throws OrekitException {
        final PVCoordinates sunPV = sun.getPVCoordinates(state.getDate(), state.getFrame());
        return position.subtract(sunPV.getPosition());
    }

    /**
     * Compute the real roots of a quartic equation.
     *
     * <pre>
     * a[0] * x<sup>4</sup> + a[1] * x<sup>3</sup> + a[2] * x<sup>2</sup> + a[3] * x + a[4] = 0.
     * </pre>
     *
     * @param a the 5 coefficients
     * @param y the real roots
     * @return the number of real roots
     */
    private int realQuarticRoots(final double[] a, final double[] y) {
        /* Treat the degenerate quartic as cubic */
        if (Precision.equals(a[0], 0.)) {
            final double[] aa = new double[a.length - 1];
            System.arraycopy(a, 1, aa, 0, aa.length);
            return realCubicRoots(aa, y);
        }

        // Transform coefficients
        final double b  = a[1] / a[0];
        final double c  = a[2] / a[0];
        final double d  = a[3] / a[0];
        final double e  = a[4] / a[0];
        final double bh = b * 0.5;

        // Solve resolvant cubic
        final double[] z3 = new double[3];
        final int i3 = realCubicRoots(new double[] {1.0, -c, b * d - 4. * e, e * (4. * c - b * b) - d * d}, z3);
        if (i3 == 0) {
            return 0;
        }

        // Largest real root of resolvant cubic
        final double z = z3[0];

        // Compute auxiliary quantities
        final double zh = z * 0.5;
        final double p  = FastMath.max(z + bh * bh - c, 0.);
        final double q  = FastMath.max(zh * zh - e, 0.);
        final double r  = bh * z - d;
        final double pp = FastMath.sqrt(p);
        final double qq = FastMath.copySign(FastMath.sqrt(q), r);

        // Solve quadratic factors of quartic equation
        final double[] y1 = new double[2];
        final int n1 = realQuadraticRoots(new double[] {1.0, bh - pp, zh - qq}, y1);
        final double[] y2 = new double[2];
        final int n2 = realQuadraticRoots(new double[] {1.0, bh + pp, zh + qq}, y2);

        if (n1 == 2) {
            if (n2 == 2) {
                y[0] = y1[0];
                y[1] = y1[1];
                y[2] = y2[0];
                y[3] = y2[1];
                return 4;
            } else {
                y[0] = y1[0];
                y[1] = y1[1];
                return 2;
            }
        } else {
            if (n2 == 2) {
                y[0] = y2[0];
                y[1] = y2[1];
                return 2;
            } else {
                return 0;
            }
        }
    }

    /**
     * Compute the real roots of a cubic equation.
     *
     * <pre>
     * a[0] * x<sup>3</sup> + a[1] * x<sup>2</sup> + a[2] * x + a[3] = 0.
     * </pre>
     *
     * @param a the 4 coefficients
     * @param y the real roots sorted in descending order
     * @return the number of real roots
     */
    private int realCubicRoots(final double[] a, final double[] y) {
        if (Precision.equals(a[0], 0.)) {
            // Treat the degenerate cubic as quadratic
            final double[] aa = new double[a.length - 1];
            System.arraycopy(a, 1, aa, 0, aa.length);
            return realQuadraticRoots(aa, y);
        }

        // Transform coefficients
        final double b  = -a[1] / (3. * a[0]);
        final double c  =  a[2] / a[0];
        final double d  =  a[3] / a[0];
        final double b2 =  b * b;
        final double p  =  b2 - c / 3.;
        final double q  =  b * (b2 - c * 0.5) - d * 0.5;

        // Compute discriminant
        final double disc = p * p * p - q * q;

        if (disc < 0.) {
            // One real root
            final double alpha  = q + FastMath.copySign(FastMath.sqrt(-disc), q);
            final double cbrtAl = FastMath.copySign(FastMath.pow(FastMath.abs(alpha), 1. / 3.), alpha);
            final double cbrtBe = p / cbrtAl;

            if (p < 0.) {
                y[0] = b + 2. * q / (cbrtAl * cbrtAl + cbrtBe * cbrtBe - p);
            } else if (p > 0.) {
                y[0] = b + cbrtAl + cbrtBe;
            } else {
                y[0] = b + cbrtAl;
            }

            return 1;

        } else if (disc > 0.) {
            // Three distinct real roots
            final double phi = FastMath.atan2(FastMath.sqrt(disc), q) / 3.;
            final double sqP = 2.0 * FastMath.sqrt(p);

            y[0] = b + sqP * FastMath.cos(phi);
            y[1] = b - sqP * FastMath.cos(FastMath.PI / 3. + phi);
            y[2] = b - sqP * FastMath.cos(FastMath.PI / 3. - phi);

            return 3;

        } else {
            // One distinct and two equals real roots
            final double cbrtQ = FastMath.copySign(FastMath.pow(FastMath.abs(q), 1. / 3.), q);
            if (q < 0.) {
                y[0] = b - cbrtQ;
                y[1] = b - cbrtQ;
                y[2] = b + 2. * cbrtQ;
            } else {
                y[0] = b + 2. * cbrtQ;
                y[1] = b - cbrtQ;
                y[2] = b - cbrtQ;
            }

            return 3;
        }
    }

    /**
     * Compute the real roots of a quadratic equation.
     *
     * <pre>
     * a[0] * x<sup>2</sup> + a[1] * x + a[2] = 0.
     * </pre>
     *
     * @param a the 3 coefficients
     * @param y the real roots sorted in descending order
     * @return the number of real roots
     */
    private int realQuadraticRoots(final double[] a, final double[] y) {
        if (Precision.equals(a[0], 0.)) {
            // Degenerate quadratic
            if (Precision.equals(a[1], 0.)) {
                // Degenerate linear equation: no real roots
                return 0;
            }
            // Linear equation: one real root
            y[0] = -a[2] / a[1];
            return 1;
        }

        // Transform coefficients
        final double b = -0.5 * a[1] / a[0];
        final double c =  a[2] / a[0];

        // Compute discriminant
        final double d =  b * b - c;

        if (d < 0.) {
            // No real roots
            return 0;
        } else if (d > 0.) {
            // Two distinct real roots
            final double y0 = b + FastMath.copySign(FastMath.sqrt(d), b);
            final double y1 = c / y0;
            y[0] = FastMath.max(y0, y1);
            y[1] = FastMath.min(y0, y1);
            return 2;
        } else {
            // Discriminant is zero: two equal real roots
            y[0] = b;
            y[1] = b;
            return 2;
        }
    }

}
