/* Copyright 2002-2012 CS Systèmes d'Information
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
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Solar radiation pressure contribution to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  The solar radiation pressure acceleration is computed as follows:<br>
 *  &gamma; = (1/2 C<sub>R</sub> A / m) * (p<sub>ref</sub> * d<sup>2</sup><sub>ref</sub>) *
 *  (r<sub>sat</sub> - R<sub>sun</sub>) / |r<sub>sat</sub> - R<sub>sun</sub>|<sup>3</sup>
 *  </p>
 *
 *  @author Pascal Parraud
 */
public class DSSTSolarRadiationPressure extends AbstractDSSTGaussianContribution {

    /** Number of points for quadrature (from 2 to 96). */
    private static final int    NB_POINTS   = 48;

    /** Epsilon for quadratic resolution. */
    private static final double EPSILON     = 1e-3;

    /** A value smaller than ALMOST_ZERO is considered to be zero (0.0). */
    private static final double ALMOST_ZERO = FastMath.ulp(1.0);

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Flux on satellite:
     * kRef = 0.5 * C<sub>R</sub> * Area * P<sub>Ref</sub> * D<sub>Ref</sub><sup>2</sup>.
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
     * @param equatorialRadius central body equatrial radius (for shadow computation)
     */
    public DSSTSolarRadiationPressure(final double cr, final double area,
                                      final PVCoordinatesProvider sun,
                                      final double equatorialRadius) {
        this(149597870000.0, 4.56e-6, cr, area, sun, equatorialRadius);
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
        super(NB_POINTS);
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
        // Compute the coefficients of the quartic equation in cos(L) 3.5-(2)
        final double h2 = h * h;
        final double k2 = k * k;
        final double m  = ae / (a * B);
        final double m2 = m * m;
        final double m4 = m2 * m2;
        final Vector3D sunDir = sun.getPVCoordinates(state.getDate(), state.getFrame()).getPosition().normalize();
        final double alfa = sunDir.dotProduct(f);
        final double beta = sunDir.dotProduct(f);
        final double bet2 = beta * beta;
        final double bb = alfa * beta + m2 * h * k;
        final double b2 = bb * bb;
        final double cc = alfa * alfa - bet2 + m2 * (k2 - h2);
        final double dd = 1. - bet2 - m2 * (1. + h2);
        final double[] a = new double[5];
        a[0] = 4. * b2 + cc * cc;
        a[1] = 8. * bb * m2 * h + 4. * cc * m2 * k;
        a[2] = -4. * b2 + 4. * m4 * h2 - 2. * cc * dd + 4. * m4 * k2;
        a[3] = -8. * bb * m2 * h - 4. * dd * m2 * k;
        a[4] = -4. * m4 * h2 + dd * dd;
        // Compute the real roots of the quartic equation 3.5-2
        final double[] cosL = new double[4];
        final int nbRoots = realQuarticRoots(a, cosL);
        if (nbRoots > 0) {
            // Check for consistency
            boolean entryFound = false;
            boolean exitFound = false;
            // Test the roots
            for (int i = 0; i < nbRoots; i++) {
                double cL;
                // Eliminate the extraneous roots : first check if |root| < 1 + eps
                if (FastMath.abs(cosL[i]) < 1 + EPSILON) {
                    // Check boundaries :
                    if (FastMath.abs(cosL[i] + 1) < EPSILON) {
                        cL = -1;
                    } else if (FastMath.abs(cosL[i] - 1) < EPSILON) {
                        cL = +1;
                    } else {
                        cL = cosL[i];
                    }
                    final double absL = FastMath.acos(cL);
                    // Check both angles: L and -L
                    for (int j = 1; j >= -1; j -= 2) {
                        final double L = j * absL;
                        final double sL = FastMath.sin(L);
                        final double t1 = 1. + k * cL + h * sL;
                        final double t2 = alfa * cL + beta * sL;
                        final double S = 1. - m2 * t1 * t1 - t2 * t2;
                        // The solution must satisfy 3.5-1 and 3.5-3
                        if (t2 < 0. && FastMath.abs(S) < EPSILON) {
                            // Compute the derivative dS/dL
                            final double dSdL = -2. * (m2 * t1 * (h * cL + k * sL) + t2 * (beta * cL - alfa * sL));
                            if (dSdL > 0.) {
                                // Exit from shadow: 3.5-4
                                exitFound = true;
                                ll[0] = L;
                            } else {
                                // Entry into shadow: 3.5-5
                                entryFound = true;
                                ll[1] = L;
                            }
                        }
                    }
                }
            }
            // Must be an entry and an exit or none
            if (!(entryFound == exitFound)) {
                // TODO problem with exception : entry or exit found but not both ! In this case,
                // consider there is no eclipse...
                // throw new OrekitException(OrekitMessages.DSST_SPR_SHADOW_INCONSISTENT,
                // entryFound, exitFound);
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
        return ll;
    }

    /** Get the central body equatorial radius.
     *  @return central body equatorial radius (m)
     */
    public final double getEquatorialRadius() {
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
        if (a[0] == 0.0) {
            final double[] aa = new double[a.length - 1];
            System.arraycopy(a, 1, aa, 0, aa.length);
            return realCubicRoots(aa, y);
        }

        final double a0 = a[0];
        double a1 = a[1];
        double a2 = a[2];
        double a3 = a[3];
        double a4 = a[4];
        /* Set the leading coefficient to 1.0 */
        if (a0 != 1.0) {
            a1 /= a0;
            a2 /= a0;
            a3 /= a0;
            a4 /= a0;
        }

        /* Compute the cubic resolvant */
        final double a12 = a1 * a1;
        final double p = -0.375 * a12 + a2;
        final double q = 0.125 * a12 * a1 - 0.5 * a1 * a2 + a3;
        final double r = -0.01171875 * a12 * a12 + 0.0625 * a12 * a2 - 0.25 * a1 * a3 + a4;

        final double[] y3 = new double[3];
        final int i3 = realCubicRoots(new double[] {
            1.0, -0.5 * p, -r, 0.5 * r * p - 0.125 * q * q
        }, y3);

        if (i3 == 0) {
            return 0;
        }
        final double z = y3[0];

        double d1 = 2.0 * z - p;
        if (d1 < 0.0) {
            if (d1 > -ALMOST_ZERO) {
                d1 = 0.0;
            } else {
                return 0;
            }
        }
        double d2;
        if (d1 < ALMOST_ZERO) {
            d2 = z * z - r;
            if (d2 < 0.0) {
                return 0;
            }
            d2 = FastMath.sqrt(d2);
        } else {
            d1 = FastMath.sqrt(d1);
            d2 = 0.5 * q / d1;
        }

        /* Set up useful values for the quadratic factors */
        final double q1 = d1 * d1;
        final double q2 = -0.25 * a1;

        int i4 = 0;
        /* Solve the first quadratic */
        double p1 = q1 - 4.0 * (z - d2);
        if (p1 == 0) {
            y[i4++] = -0.5 * d1 - q2;
        } else if (p1 > 0) {
            p1 = FastMath.sqrt(p1);
            y[i4++] = -0.5 * (d1 + p1) + q2;
            y[i4++] = -0.5 * (d1 - p1) + q2;
        }

        /* Solve the second quadratic */
        double p2 = q1 - 4.0 * (z + d2);
        if (p2 == 0) {
            y[i4++] = 0.5 * d1 - q2;
        } else if (p2 > 0) {
            p2 = FastMath.sqrt(p2);
            y[i4++] = 0.5 * (d1 + p2) + q2;
            y[i4++] = 0.5 * (d1 - p2) + q2;
        }

        return i4;
    }

    /**
     * Compute the real roots of a cubic equation.
     *
     * <pre>
     * a[0] * x<sup>3</sup> + a[1] * x<sup>2</sup> + a[2] * x + a[3] = 0.
     * </pre>
     *
     * @param a the 4 coefficients
     * @param y the real roots
     * @return the number of real roots
     */
    private int realCubicRoots(final double[] a, final double[] y) {
        /* Treat the degenerate cubic as quadratic */
        if (a[0] == 0.0) {
            final double[] aa = new double[a.length - 1];
            System.arraycopy(a, 1, aa, 0, aa.length);
            return realQuadraticRoots(aa, y);
        }

        final double a0 = a[0];
        double a1 = a[1];
        double a2 = a[2];
        double a3 = a[3];
        /* Make sure the cubic has a leading coefficient of 1.0 */
        if (a0 != 1.0) {
            a1 /= a0;
            a2 /= a0;
            a3 /= a0;
        }

        final double a12 = a1 * a1;
        final double q = (a12 - 3.0 * a2) / 9.0;
        final double q3 = q * q * q;
        final double r = (a1 * (a12 - 4.5 * a2) + 13.5 * a3) / 27.0;
        final double r2 = r * r;
        final double d = q3 - r2;
        final double a1o3 = a1 / 3.0;

        if (d >= 0.0) {
            /* 3 real roots. */
            final double roq3 = r / FastMath.sqrt(q3);
            final double teta = FastMath.acos(roq3) / 3.0;
            final double sqrq = -2.0 * FastMath.sqrt(q);

            y[0] = sqrq * FastMath.cos(teta) - a1o3;
            y[1] = sqrq * FastMath.cos(teta + FastMath.PI * 2. / 3.) - a1o3;
            y[2] = sqrq * FastMath.cos(teta + FastMath.PI * 4. / 3.) - a1o3;

            return 3;

        } else {
            /* 1 real root. */
            final double tmp = FastMath.cbrt(FastMath.sqrt(-d) + FastMath.abs(r));
            if (r < 0) {
                y[0] = (tmp + q / tmp) - a1o3;
            } else {
                y[0] = -(tmp + q / tmp) - a1o3;
            }

            return 1;
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
     * @param y the real roots
     * @return the number of real roots
     */
    private int realQuadraticRoots(final double[] a, final double[] y) {
        final double aa = a[0];
        final double bb = -a[1];
        final double cc = a[2];

        if (aa == 0.0) {
            if (bb == 0.0) {
                return 0;
            }
            y[0] = cc / bb;
            return 1;
        }

        final double d = bb * bb - 4.0 * aa * cc;

        /* Treat values of d around 0 as 0. */
        if (FastMath.abs(d) < ALMOST_ZERO) {
            y[0] = 0.5 * bb / aa;
            return 1;
        } else {
            if (d < 0.0) {
                return 0;
            }
        }

        final double oo2a = 0.5 / aa;
        final double sqrd = FastMath.sqrt(d);

        y[0] = (bb + sqrd) * oo2a;
        y[1] = (bb - sqrd) * oo2a;

        return 2;
    }

}
