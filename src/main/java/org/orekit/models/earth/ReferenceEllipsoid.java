/* Contributed in the public domain.
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
package org.orekit.models.earth;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.utils.Constants;

/**
 * A Reference Ellipsoid for use in geodesy. The ellipsoid defines an
 * ellipsoidal potential called the normal potential, and its gradient, normal
 * gravity.
 *
 * <p> These parameters are needed to define the normal potential:
 *
 *
 * <ul> <li>a, semi-major axis</li>
 *
 * <li>f, flattening</li>
 *
 * <li>GM, the gravitational parameter</li>
 *
 * <li>&omega;, the spin rate</li> </ul>
 *
 * <p> References:
 *
 * <ol> <li>Martin Losch, Verena Seufer. How to Compute Geoid Undulations (Geoid
 * Height Relative to a Given Reference Ellipsoid) from Spherical Harmonic
 * Coefficients for Satellite Altimetry Applications. , 2003. <a
 * href="mitgcm.org/~mlosch/geoidcookbook.pdf" >mitgcm.org/~mlosch/geoidcookbook.pdf</a></li>
 *
 * <li>Weikko A. Heiskanen, Helmut Moritz. Physical Geodesy. W. H. Freeman and
 * Company, 1967. (especially sections 2.13 and equation 2-144)</li>
 *
 * <li>Department of Defense World Geodetic System 1984. 2000. NIMA TR 8350.2
 * Third Edition, Amendment 1.</li> </ol>
 *
 * @author Evan Ward
 */
public class ReferenceEllipsoid extends OneAxisEllipsoid implements EarthShape {

    /** uid is date of last modification. */
    private static final long serialVersionUID = 20150311L;

    /** the gravitational parameter of the ellipsoid, in m<sup>3</sup>/s<sup>2</sup>. */
    private final double GM;
    /** the rotation rate of the ellipsoid, in rad/s. */
    private final double spin;

    /**
     * Creates a new geodetic Reference Ellipsoid from four defining
     * parameters.
     *
     * @param ae        Equatorial radius, in m
     * @param f         flattening of the ellipsoid.
     * @param bodyFrame the frame to attach to the ellipsoid. The origin is at
     *                  the center of mass, the z axis is the minor axis.
     * @param GM        gravitational parameter, in m<sup>3</sup>/s<sup>2</sup>
     * @param spin      &omega; in rad/s
     */
    public ReferenceEllipsoid(final double ae,
                              final double f,
                              final Frame bodyFrame,
                              final double GM,
                              final double spin) {
        super(ae, f, bodyFrame);
        this.GM = GM;
        this.spin = spin;
    }

    /**
     * Gets the gravitational parameter that is part of the definition of the
     * reference ellipsoid.
     *
     * @return GM in m<sup>3</sup>/s<sup>2</sup>
     */
    public double getGM() {
        return this.GM;
    }

    /**
     * Gets the rotation of the ellipsoid about its axis.
     *
     * @return &omega; in rad/s
     */
    public double getSpin() {
        return this.spin;
    }

    /**
     * Get the radius of this ellipsoid at the poles.
     *
     * @return the polar radius, in meters
     * @see #getEquatorialRadius()
     */
    public double getPolarRadius() {
        // use the definition of flattening: f = (a-b)/a
        final double a = this.getEquatorialRadius();
        final double f = this.getFlattening();
        return a - f * a;
    }

    /**
     * Gets the normal gravity, that is gravity just due to the reference
     * ellipsoid's potential. The normal gravity only depends on latitude
     * because the ellipsoid is axis symmetric.
     *
     * <p> The normal gravity is a vector, having both magnitude and direction.
     * This method only give the magnitude.
     *
     * @param latitude geodetic latitude, in radians. That is the angle between
     *                 the local normal on the ellipsoid and the equatorial
     *                 plane.
     * @return the normal gravity, &gamma;, at the given latitude in
     * m/s<sup>2</sup>. This is the acceleration felt by a mass at rest on the
     * surface of the reference ellipsoid.
     */
    public double getNormalGravity(final double latitude) {
        /*
         * Uses the equations from [2] as compiled in [1]. See Class comment.
         */

        final double a  = this.getEquatorialRadius();
        final double f  = this.getFlattening();

        // define derived constants, move to constructor for more speed
        // semi-minor axis
        final double b = a * (1 - f);
        final double a2 = a * a;
        final double b2 = b * b;
        // linear eccentricity
        final double E = FastMath.sqrt(a2 - b2);
        // first numerical eccentricity
        final double e = E / a;
        // second numerical eccentricity
        final double eprime = E / b;
        // an abbreviation for a common term
        final double m = this.spin * this.spin * a2 * b / this.GM;
        // gravity at equator
        final double ya = this.GM / (a * b) *
                (1 - 3. / 2. * m - 3. / 14. * eprime * m);
        // gravity at the poles
        final double yb = this.GM / a2 * (1 + m + 3. / 7. * eprime * m);
        // another abbreviation for a common term
        final double kappa = (b * yb - a * ya) / (a * ya);

        // calculate normal gravity at the given latitude.
        final double sin  = FastMath.sin(latitude);
        final double sin2 = sin * sin;
        return ya * (1 + kappa * sin2) / FastMath.sqrt(1 - e * e * sin2);
    }

    /**
     * Get the fully normalized coefficient C<sub>2n,0</sub> for the normal
     * gravity potential.
     *
     * @param n index in C<sub>2n,0</sub>, n &gt;= 1.
     * @return normalized C<sub>2n,0</sub> of the ellipsoid
     * @see "Department of Defense World Geodetic System 1984. 2000. NIMA TR
     * 8350.2 Third Edition, Amendment 1."
     * @see "DMA TR 8350.2. 1984."
     */
    public double getC2n0(final int n) {
        // parameter check
        if (n < 1) {
            throw new IllegalArgumentException("Expected n < 1, got n=" + n);
        }

        final double a = this.getEquatorialRadius();
        final double f = this.getFlattening();
        // define derived constants, move to constructor for more speed
        // semi-minor axis
        final double b = a * (1 - f);
        final double a2 = a * a;
        final double b2 = b * b;
        // linear eccentricity
        final double E = FastMath.sqrt(a2 - b2);
        // first numerical eccentricity
        final double e = E / a;
        // an abbreviation for a common term
        final double m = this.spin * this.spin * a2 * b / this.GM;

        /*
         * derive C2 using a linear approximation, good to ~1e-9, eq 2.118 in
         * Heiskanen & Moritz[2]. See comment for ReferenceEllipsoid
         */
        final double J2 = 2. / 3. * f - 1. / 3. * m - 1. / 3. * f * f + 2. / 21. * f * m;
        final double C2 = -J2 / FastMath.sqrt(5);

        // eq 3-62 in chapter 3 of DMA TR 8350.2, calculated by scaling C2,0
        return (((n & 0x1) == 0) ? 3 : -3) * FastMath.pow(e, 2 * n) *
                (1 - n - FastMath.pow(5, 3. / 2.) * n * C2 / (e * e)) /
                ((2 * n + 1) * (2 * n + 3) * FastMath.sqrt(4 * n + 1));
    }

    @Override
    public ReferenceEllipsoid getEllipsoid() {
        return this;
    }

    /**
     * Get the WGS84 ellipsoid, attached to the given body frame.
     *
     * @param bodyFrame the earth centered fixed frame
     * @return a WGS84 reference ellipsoid
     */
    public static ReferenceEllipsoid getWgs84(final Frame bodyFrame) {
        return new ReferenceEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, bodyFrame,
                Constants.WGS84_EARTH_MU,
                Constants.WGS84_EARTH_ANGULAR_VELOCITY);
    }

    /**
     * Get the GRS80 ellipsoid, attached to the given body frame.
     *
     * @param bodyFrame the earth centered fixed frame
     * @return a GRS80 reference ellipsoid
     */
    public static ReferenceEllipsoid getGrs80(final Frame bodyFrame) {
        return new ReferenceEllipsoid(
                Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
                Constants.GRS80_EARTH_FLATTENING,
                bodyFrame,
                Constants.GRS80_EARTH_MU,
                Constants.GRS80_EARTH_ANGULAR_VELOCITY
        );
    }

}
