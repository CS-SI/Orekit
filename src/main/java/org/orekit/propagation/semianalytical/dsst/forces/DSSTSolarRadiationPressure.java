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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/** Solar radiation pressure contribution to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  The solar radiation pressure acceleration is computed through the
 *  acceleration model of
 *  {@link org.orekit.forces.radiation.SolarRadiationPressure SolarRadiationPressure}.
 *  </p>
 *
 *  @author Pascal Parraud
 */
public class DSSTSolarRadiationPressure extends AbstractGaussianContribution {

    /** Reference distance for the solar radiation pressure (m). */
    private static final double D_REF = 149597870000.0;

    /** Reference solar radiation pressure at D_REF (N/m²). */
    private static final double P_REF = 4.56e-6;

    /** Threshold for the choice of the Gauss quadrature order. */
    private static final double GAUSS_THRESHOLD = 1.0e-15;

    /** Threshold for shadow equation. */
    private static final double S_ZERO = 1.0e-6;

    /** Prefix for the coefficient keys. */
    private static final String PREFIX = "DSST-SRP-";

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Central Body radius. */
    private final double                ae;

    /** Spacecraft model for radiation acceleration computation. */
    private final RadiationSensitive spacecraft;

    /**
     * Simple constructor with default reference values and spherical spacecraft.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m²</li>
     * </ul>
     * <p>
     * The spacecraft has a spherical shape.
     * </p>
     *
     * @param cr satellite radiation pressure coefficient (assuming total specular reflection)
     * @param area cross sectional area of satellite
     * @param sun Sun model
     * @param centralBody central body (for shadow computation)
     * @param mu central attraction coefficient
     * @since 12.0
     */
    public DSSTSolarRadiationPressure(final double cr, final double area,
                                      final ExtendedPVCoordinatesProvider sun,
                                      final OneAxisEllipsoid centralBody,
                                      final double mu) {
        this(D_REF, P_REF, cr, area, sun, centralBody, mu);
    }

    /**
     * Simple constructor with default reference values, but custom spacecraft.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m²</li>
     * </ul>
     *
     * @param sun Sun model
     * @param centralBody central body (for shadow computation)
     * @param spacecraft spacecraft model
     * @param mu central attraction coefficient
     * @since 12.0
     */
    public DSSTSolarRadiationPressure(final ExtendedPVCoordinatesProvider sun,
                                      final OneAxisEllipsoid centralBody,
                                      final RadiationSensitive spacecraft,
                                      final double mu) {
        this(D_REF, P_REF, sun, centralBody, spacecraft, mu);
    }

    /**
     * Constructor with customizable reference values but spherical spacecraft.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m² is linked
     * to solar flux SF in W/m² using formula pRef = SF/c where c is the speed of light
     * (299792458 m/s). So at 1UA a 1367 W/m² solar flux is a 4.56 10<sup>-6</sup>
     * N/m² solar radiation pressure.
     * </p>
     *
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m²)
     * @param cr satellite radiation pressure coefficient (assuming total specular reflection)
     * @param area cross sectional area of satellite
     * @param sun Sun model
     * @param centralBody central body (for shadow computation)
     * @param mu central attraction coefficient
     * @since 12.0
     */
    public DSSTSolarRadiationPressure(final double dRef, final double pRef,
                                      final double cr, final double area,
                                      final ExtendedPVCoordinatesProvider sun,
                                      final OneAxisEllipsoid centralBody,
                                      final double mu) {

        // cR being the DSST SRP coef and assuming a spherical spacecraft,
        // the conversion is:
        // cR = 1 + (1 - kA) * (1 - kR) * 4 / 9
        // with kA arbitrary sets to 0
        this(dRef, pRef, sun, centralBody, new IsotropicRadiationSingleCoefficient(area, cr), mu);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m² is linked
     * to solar flux SF in W/m² using formula pRef = SF/c where c is the speed of light
     * (299792458 m/s). So at 1UA a 1367 W/m² solar flux is a 4.56 10<sup>-6</sup>
     * N/m² solar radiation pressure.
     * </p>
     *
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m²)
     * @param sun Sun model
     * @param centralBody central body shape model (for umbra/penumbra computation)
     * @param spacecraft spacecraft model
     * @param mu central attraction coefficient
     * @since 12.0
     */
    public DSSTSolarRadiationPressure(final double dRef, final double pRef,
                                      final ExtendedPVCoordinatesProvider sun,
                                      final OneAxisEllipsoid centralBody,
                                      final RadiationSensitive spacecraft,
                                      final double mu) {

        //Call to the constructor from superclass using the numerical SRP model as ForceModel
        super(PREFIX, GAUSS_THRESHOLD,
              new SolarRadiationPressure(dRef, pRef, sun, centralBody, spacecraft), mu);

        this.sun  = sun;
        this.ae   = centralBody.getEquatorialRadius();
        this.spacecraft = spacecraft;
    }

    /** Get spacecraft shape.
     * @return the spacecraft shape.
     */
    public RadiationSensitive getSpacecraft() {
        return spacecraft;
    }

    /** {@inheritDoc} */
    protected List<ParameterDriver> getParametersDriversWithoutMu() {
        return spacecraft.getRadiationParametersDrivers();
    }

    /** {@inheritDoc} */
    protected double[] getLLimits(final SpacecraftState state, final AuxiliaryElements auxiliaryElements) {

        // Default bounds without shadow [-PI, PI]
        final double[] ll = {-FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0),
                             FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0)};

        // Direction cosines of the Sun in the equinoctial frame
        final Vector3D sunDir = sun.getPosition(state.getDate(), state.getFrame()).normalize();
        final double alpha = sunDir.dotProduct(auxiliaryElements.getVectorF());
        final double beta  = sunDir.dotProduct(auxiliaryElements.getVectorG());
        final double gamma = sunDir.dotProduct(auxiliaryElements.getVectorW());

        // Compute limits only if the perigee is close enough from the central body to be in the shadow
        if (FastMath.abs(gamma * auxiliaryElements.getSma() * (1. - auxiliaryElements.getEcc())) < ae) {

            // Compute the coefficients of the quartic equation in cos(L) 3.5-(2)
            final double bet2 = beta * beta;
            final double h2 = auxiliaryElements.getH() * auxiliaryElements.getH();
            final double k2 = auxiliaryElements.getK() * auxiliaryElements.getK();
            final double m  = ae / (auxiliaryElements.getSma() * auxiliaryElements.getB());
            final double m2 = m * m;
            final double m4 = m2 * m2;
            final double bb = alpha * beta + m2 * auxiliaryElements.getH() * auxiliaryElements.getK();
            final double b2 = bb * bb;
            final double cc = alpha * alpha - bet2 + m2 * (k2 - h2);
            final double dd = 1. - bet2 - m2 * (1. + h2);
            final double[] a = new double[5];
            a[0] = 4. * b2 + cc * cc;
            a[1] = 8. * bb * m2 * auxiliaryElements.getH() + 4. * cc * m2 * auxiliaryElements.getK();
            a[2] = -4. * b2 + 4. * m4 * h2 - 2. * cc * dd + 4. * m4 * k2;
            a[3] = -8. * bb * m2 * auxiliaryElements.getH() - 4. * dd * m2 * auxiliaryElements.getK();
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
                            final double range = 1. + auxiliaryElements.getK() * cosL + auxiliaryElements.getH() * sinL;
                            final double S  = 1. - m2 * range * range - cPhi * cPhi;
                            // Is the shadow equation 3.5-1 satisfied ?
                            if (FastMath.abs(S) < S_ZERO) {
                                // Is this the entry or exit angle ?
                                final double dSdL = m2 * range * (auxiliaryElements.getK() * sinL - auxiliaryElements.getH() * cosL) + cPhi * (alpha * sinL - beta * cosL);
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

    /** {@inheritDoc} */
    protected <T extends CalculusFieldElement<T>> T[] getLLimits(final FieldSpacecraftState<T> state,
                                                             final FieldAuxiliaryElements<T> auxiliaryElements) {

        final Field<T> field = state.getDate().getField();
        final T zero = field.getZero();
        final T one  = field.getOne();
        final T pi   = one.getPi();

        // Default bounds without shadow [-PI, PI]
        final T[] ll = MathArrays.buildArray(field, 2);
        ll[0] = MathUtils.normalizeAngle(state.getLv(), zero).subtract(pi);
        ll[1] = MathUtils.normalizeAngle(state.getLv(), zero).add(pi);

        // Direction cosines of the Sun in the equinoctial frame
        final FieldVector3D<T> sunDir = sun.getPosition(state.getDate(), state.getFrame()).normalize();
        final T alpha = sunDir.dotProduct(auxiliaryElements.getVectorF());
        final T beta  = sunDir.dotProduct(auxiliaryElements.getVectorG());
        final T gamma = sunDir.dotProduct(auxiliaryElements.getVectorW());

        // Compute limits only if the perigee is close enough from the central body to be in the shadow
        if (FastMath.abs(gamma.multiply(auxiliaryElements.getSma()).multiply(auxiliaryElements.getEcc().negate().add(one))).getReal() < ae) {

            // Compute the coefficients of the quartic equation in cos(L) 3.5-(2)
            final T bet2 = beta.multiply(beta);
            final T h2 = auxiliaryElements.getH().multiply(auxiliaryElements.getH());
            final T k2 = auxiliaryElements.getK().multiply(auxiliaryElements.getK());
            final T m  = (auxiliaryElements.getSma().multiply(auxiliaryElements.getB())).divide(ae).reciprocal();
            final T m2 = m.multiply(m);
            final T m4 = m2.multiply(m2);
            final T bb = alpha.multiply(beta).add(m2.multiply(auxiliaryElements.getH()).multiply(auxiliaryElements.getK()));
            final T b2 = bb.multiply(bb);
            final T cc = alpha.multiply(alpha).subtract(bet2).add(m2.multiply(k2.subtract(h2)));
            final T dd = bet2.add(m2.multiply(h2.add(1.))).negate().add(one);
            final T[] a = MathArrays.buildArray(field, 5);
            a[0] = b2.multiply(4.).add(cc.multiply(cc));
            a[1] = bb.multiply(8.).multiply(m2).multiply(auxiliaryElements.getH()).add(cc.multiply(4.).multiply(m2).multiply(auxiliaryElements.getK()));
            a[2] = m4.multiply(h2).multiply(4.).subtract(cc.multiply(dd).multiply(2.)).add(m4.multiply(k2).multiply(4.)).subtract(b2.multiply(4.));
            a[3] = auxiliaryElements.getH().multiply(m2).multiply(bb).multiply(8.).add(auxiliaryElements.getK().multiply(m2).multiply(dd).multiply(4.)).negate();
            a[4] = dd.multiply(dd).subtract(m4.multiply(h2).multiply(4.));
            // Compute the real roots of the quartic equation 3.5-2
            final T[] roots = MathArrays.buildArray(field, 4);
            final int nbRoots = realQuarticRoots(a, roots, field);
            if (nbRoots > 0) {
                // Check for consistency
                boolean entryFound = false;
                boolean exitFound  = false;
                // Eliminate spurious roots
                for (int i = 0; i < nbRoots; i++) {
                    final T cosL = roots[i];
                    final T sL = FastMath.sqrt((cosL.negate().add(one)).multiply(cosL.add(one)));
                    // Check both angles: L and -L
                    for (int j = -1; j <= 1; j += 2) {
                        final T sinL = sL.multiply(j);
                        final T cPhi = cosL.multiply(alpha).add(sinL.multiply(beta));
                        // Is the angle on the shadow side of the central body (eq. 3.5-3) ?
                        if (cPhi.getReal() < 0.) {
                            final T range = cosL.multiply(auxiliaryElements.getK()).add(sinL.multiply(auxiliaryElements.getH())).add(one);
                            final T S  = (range.multiply(range).multiply(m2).add(cPhi.multiply(cPhi))).negate().add(1.);
                            // Is the shadow equation 3.5-1 satisfied ?
                            if (FastMath.abs(S).getReal() < S_ZERO) {
                                // Is this the entry or exit angle ?
                                final T dSdL = m2.multiply(range).multiply(auxiliaryElements.getK().multiply(sinL).subtract(auxiliaryElements.getH().multiply(cosL))).add(cPhi.multiply(alpha.multiply(sinL).subtract(beta.multiply(cosL))));
                                if (dSdL.getReal() > 0.) {
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
                    ll[0] = pi.negate();
                    ll[1] = pi;
                }
                // Quadrature between L at exit and L at entry so Lexit must be lower than Lentry
                if (ll[0].getReal() > ll[1].getReal()) {
                    // Keep the angles between [-2PI, 2PI]
                    if (ll[1].getReal() < 0.) {
                        ll[1] = ll[1].add(pi.multiply(2.0));
                    } else {
                        ll[0] = ll[0].subtract(pi.multiply(2.0));
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

    /**
     * Compute the real roots of a quartic equation.
     *
     * <pre>
     * a[0] * x⁴ + a[1] * x³ + a[2] * x² + a[3] * x + a[4] = 0.
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
     * Compute the real roots of a quartic equation.
     *
     * <pre>
     * a[0] * x⁴ + a[1] * x³ + a[2] * x² + a[3] * x + a[4] = 0.
     * </pre>
     * @param <T> the type of the field elements
     *
     * @param a the 5 coefficients
     * @param y the real roots
     * @param field field of elements
     * @return the number of real roots
     */
    private <T extends CalculusFieldElement<T>> int realQuarticRoots(final T[] a, final T[] y,
                                                                 final Field<T> field) {

        final T zero = field.getZero();

        // Treat the degenerate quartic as cubic
        if (Precision.equals(a[0].getReal(), 0.)) {
            final T[] aa = MathArrays.buildArray(field, a.length - 1);
            System.arraycopy(a, 1, aa, 0, aa.length);
            return realCubicRoots(aa, y, field);
        }

        // Transform coefficients
        final T b  = a[1].divide(a[0]);
        final T c  = a[2].divide(a[0]);
        final T d  = a[3].divide(a[0]);
        final T e  = a[4].divide(a[0]);
        final T bh = b.multiply(0.5);

        // Solve resolvant cubic
        final T[] z3 = MathArrays.buildArray(field, 3);
        final T[] i = MathArrays.buildArray(field, 4);
        i[0] = zero.add(1.0);
        i[1] = c.negate();
        i[2] = b.multiply(d).subtract(e.multiply(4.0));
        i[3] = e.multiply(c.multiply(4.).subtract(b.multiply(b))).subtract(d.multiply(d));
        final int i3 = realCubicRoots(i, z3, field);
        if (i3 == 0) {
            return 0;
        }

        // Largest real root of resolvant cubic
        final T z = z3[0];

        // Compute auxiliary quantities
        final T zh = z.multiply(0.5);
        final T p  = FastMath.max(z.add(bh.multiply(bh)).subtract(c), zero);
        final T q  = FastMath.max(zh.multiply(zh).subtract(e), zero);
        final T r  = bh.multiply(z).subtract(d);
        final T pp = FastMath.sqrt(p);
        final T qq = FastMath.copySign(FastMath.sqrt(q), r);

        // Solve quadratic factors of quartic equation
        final T[] y1 = MathArrays.buildArray(field, 2);
        final T[] n = MathArrays.buildArray(field, 3);
        n[0] = zero.add(1.0);
        n[1] = bh.subtract(pp);
        n[2] = zh.subtract(qq);
        final int n1 = realQuadraticRoots(n, y1);
        final T[] y2 = MathArrays.buildArray(field, 2);
        final T[] nn = MathArrays.buildArray(field, 3);
        nn[0] = zero.add(1.0);
        nn[1] = bh.add(pp);
        nn[2] = zh.add(qq);
        final int n2 = realQuadraticRoots(nn, y2);

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
     * a[0] * x³ + a[1] * x² + a[2] * x + a[3] = 0.
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
            final double cbrtAl = FastMath.cbrt(alpha);
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
            final double cbrtQ = FastMath.cbrt(q);
            final double root1 = b + 2. * cbrtQ;
            final double root2 = b - cbrtQ;
            if (q < 0.) {
                y[0] = root2;
                y[1] = root2;
                y[2] = root1;
            } else {
                y[0] = root1;
                y[1] = root2;
                y[2] = root2;
            }

            return 3;
        }
    }

    /**
     * Compute the real roots of a cubic equation.
     *
     * <pre>
     * a[0] * x³ + a[1] * x² + a[2] * x + a[3] = 0.
     * </pre>
     *
     * @param <T> the type of the field elements
     * @param a the 4 coefficients
     * @param y the real roots sorted in descending order
     * @param field field of elements
     * @return the number of real roots
     */
    private <T extends CalculusFieldElement<T>> int realCubicRoots(final T[] a, final T[] y,
                                                               final Field<T> field) {

        if (Precision.equals(a[0].getReal(), 0.)) {
            // Treat the degenerate cubic as quadratic
            final T[] aa = MathArrays.buildArray(field, a.length - 1);
            System.arraycopy(a, 1, aa, 0, aa.length);
            return realQuadraticRoots(aa, y);
        }

        // Transform coefficients
        final T b  =  a[1].divide(a[0].multiply(3.)).negate();
        final T c  =  a[2].divide(a[0]);
        final T d  =  a[3].divide(a[0]);
        final T b2 =  b.multiply(b);
        final T p  =  b2.subtract(c.divide(3.));
        final T q  =  b.multiply(b2.subtract(c.multiply(0.5))).subtract(d.multiply(0.5));

        // Compute discriminant
        final T disc = p.multiply(p).multiply(p).subtract(q.multiply(q));

        if (disc.getReal() < 0.) {
            // One real root
            final T alpha  = FastMath.copySign(FastMath.sqrt(disc.negate()), q).add(q);
            final T cbrtAl = FastMath.cbrt(alpha);
            final T cbrtBe = p.divide(cbrtAl);

            if (p .getReal() < 0.) {
                y[0] = q.divide(cbrtAl.multiply(cbrtAl).add(cbrtBe.multiply(cbrtBe)).subtract(p)).multiply(2.).add(b);
            } else if (p.getReal() > 0.) {
                y[0] = b.add(cbrtAl).add(cbrtBe);
            } else {
                y[0] = b.add(cbrtAl);
            }

            return 1;

        } else if (disc.getReal() > 0.) {
            // Three distinct real roots
            final T phi = FastMath.atan2(FastMath.sqrt(disc), q).divide(3.);
            final T sqP = FastMath.sqrt(p).multiply(2.);

            y[0] = b.add(sqP.multiply(FastMath.cos(phi)));
            y[1] = b.subtract(sqP.multiply(FastMath.cos(phi.add(b.getPi().divide(3.)))));
            y[2] = b.subtract(sqP.multiply(FastMath.cos(phi.negate().add(b.getPi().divide(3.)))));

            return 3;

        } else {
            // One distinct and two equals real roots
            final T cbrtQ = FastMath.cbrt(q);
            final T root1 = b.add(cbrtQ.multiply(2.0));
            final T root2 = b.subtract(cbrtQ);
            if (q.getReal() < 0.) {
                y[0] = root2;
                y[1] = root2;
                y[2] = root1;
            } else {
                y[0] = root1;
                y[1] = root2;
                y[2] = root2;
            }

            return 3;
        }
    }

    /**
     * Compute the real roots of a quadratic equation.
     *
     * <pre>
     * a[0] * x² + a[1] * x + a[2] = 0.
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

    /**
     * Compute the real roots of a quadratic equation.
     *
     * <pre>
     * a[0] * x² + a[1] * x + a[2] = 0.
     * </pre>
     *
     * @param <T> the type of the field elements
     * @param a the 3 coefficients
     * @param y the real roots sorted in descending order
     * @return the number of real roots
     */
    private <T extends CalculusFieldElement<T>> int realQuadraticRoots(final T[] a, final T[] y) {

        if (Precision.equals(a[0].getReal(), 0.)) {
            // Degenerate quadratic
            if (Precision.equals(a[1].getReal(), 0.)) {
                // Degenerate linear equation: no real roots
                return 0;
            }
            // Linear equation: one real root
            y[0] = a[2].divide(a[1]).negate();
            return 1;
        }

        // Transform coefficients
        final T b = a[1].divide(a[0]).multiply(0.5).negate();
        final T c =  a[2].divide(a[0]);

        // Compute discriminant
        final T d =  b.multiply(b).subtract(c);

        if (d.getReal() < 0.) {
            // No real roots
            return 0;
        } else if (d.getReal() > 0.) {
            // Two distinct real roots
            final T y0 = b.add(FastMath.copySign(FastMath.sqrt(d), b));
            final T y1 = c.divide(y0);
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
