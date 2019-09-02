/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.geometry.fov;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;

/** Class representing a spacecraft sensor Field Of View with elliptical shape.
 * <p>The field of view is defined by a line-of-sight direction, a primary axis
 * direction, and two half-aperture angles.</p>
 * @author Luc Maisonobe
 * @since 10.1
 */
public class EllipticalFieldOfView extends SmoothFieldOfView {

    /** FOV half aperture angle for spreading along X (i.e. rotation around +Y). */
    private final double halfApertureAlongX;

    /** FOV half aperture angle for spreading along Y (i.e. rotation around -X). */
    private final double halfApertureAlongY;

    /** Convention used to define ellipse shape. */
    private final EllipticalConstraint constraint;

    /** Build a new instance.
     * <p>
     * Using a suitable rotation, an elliptical Field Of View can be oriented such
     * that the ellipse center is along the Z<sub>ell</sub> axis, one of its principal
     * axes is in the (X<sub>ell</sub>, Z<sub>ell</sub>) plane and the other principal
     * axis is in the (Y<sub>ell</sub>, Z<sub>ell</sub>) plane. Beware that the ellipse
     * principal axis that spreads along the Y<sub>ell</sub> direction corresponds to a
     * rotation around -X<sub>ell</sub> axis and that the ellipse principal axis that
     * spreads along the X<sub>ell</sub> direction corresponds to a rotation around
     * +Y<sub>ell</sub> axis. The naming convention used here is that the angles are
     * named after the spreading axis.
     * </p>
     * @param center direction of the FOV center (i.e. Z<sub>ell</sub>),
     * in spacecraft frame
     * @param primaryMeridian vector defining the (+X<sub>ell</sub>, Z<sub>ell</sub>)
     * half-plane (it is allowed to have {@code primaryMeridian} not orthogonal to
     * {@code center} as orthogonality will be fixed internally)
     * @param halfApertureAlongX FOV half aperture angle defining the ellipse spreading
     * along X<sub>ell</sub> (i.e. it corresponds to a rotation around +Y<sub>ell</sub>)
     * @param halfApertureAlongY FOV half aperture angle defining the ellipse spreading
     * along Y<sub>ell</sub> (i.e. it corresponds to a rotation around -X<sub>ell</sub>)
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     * @param constraint convention used to define ellipse shape
     */
    public EllipticalFieldOfView(final Vector3D center, final Vector3D primaryMeridian,
                                 final double halfApertureAlongX, final double halfApertureAlongY,
                                 final double margin, final EllipticalConstraint constraint) {

        super(center, primaryMeridian, margin);
        this.halfApertureAlongX = halfApertureAlongX;
        this.halfApertureAlongY = halfApertureAlongY;
        this.constraint         = constraint;

    }

    /** get the FOV half aperture angle for spreading along X<sub>ell</sub> (i.e. rotation around +Y<sub>ell</sub>).
     * @return FOV half aperture angle for spreading along X<sub>ell</sub> (i.e. rotation around +Y<sub>ell</sub>
     */
    public double getHalfApertureAlongX() {
        return halfApertureAlongX;
    }

    /** get the FOV half aperture angle for spreading along Y<sub>ell</sub> (i.e. rotation around -X<sub>ell</sub>).
     * @return FOV half aperture angle for spreading along Y<sub>ell</sub> (i.e. rotation around -X<sub>ell</sub>)
     */
    public double getHalfApertureAlongY() {
        return halfApertureAlongY;
    }

    /** Get the convention used to define ellipse shape.
     * @return convention used to define ellipse shape
     */
    public EllipticalConstraint getConstraint() {
        return constraint;
    }

    /** {@inheritDoc} */
    @Override
    public double rawOffsetFromBoundary(final Vector3D lineOfSight) {
        final Vector3D normalized = lineOfSight.normalize();
        return constraint.rawOffsetFromBoundary(halfApertureAlongX, halfApertureAlongY,
                                                Vector3D.dotProduct(normalized, getX()),
                                                Vector3D.dotProduct(normalized, getY()),
                                                Vector3D.dotProduct(normalized, getZ()));
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D directionAt(final double angle) {
        final Vector3D d = constraint.directionAt(halfApertureAlongX, halfApertureAlongY, angle);
        return new Vector3D(d.getX(), getX(), d.getY(), getY(), d.getZ(), getZ());
    }

    /** Enumerate for ellipse shape definition.
     * <p>
     * There are several ways to define an elliptical shape on the unit sphere,
     * one based on angular coordinates and one based on Cartesian coordinates.
     * </p>
     * <p>
     * Without loss of generality, one can assume that with a suitable rotation
     * the ellipse center is along the Z<sub>ell</sub> axis and the ellipse principal axes
     * are along the X and Y axes. We can define angular coordinates \((\alpha,
     * \beta)\) as dihedra angles around the +Y<sub>ell</sub> and -X<sub>ell</sub>
     * axes respectively to specify points on the unit sphere. The corresponding
     * Cartesian coordinates will be
     * \[P_{\alpha,\beta}\left(\begin{gather*}
     *   \frac{\sin\alpha\cos\beta}{\sqrt{1-\sin^2\alpha\sin^2\beta}}\\
     *   \frac{\cos\alpha\sin\beta}{\sqrt{1-\sin^2\alpha\sin^2\beta}}\\
     *   \frac{\cos\alpha\cos\beta}{\sqrt{1-\sin^2\alpha\sin^2\beta}}
     * \end{gather*}\right)\]
     * which shows that angle \(\beta=0\) corresponds to the (X<sub>ell</sub>, Z<sub>ell</sub>)
     * plane and that angle \(\alpha=0\) corresponds to the (Y<sub>ell</sub>, Z<sub>ell</sub>)
     * plane. Note that at least one of the angles must be different from \(\pm\frac{\pi}{2}\),
     * which means that the expression above is singular for points in the (X<sub>ell</sub>,
     * Y<sub>ell</sub>) plane.
     * </p>
     * <p>
     * The first way to define an elliptical shape on the unit sphere is to write
     * the ellipticity constraint using directly the angular coordinates:
     * \[\left(\frac{\alpha}{\lambda}\right)^2 +
     *   \left(\frac{\beta}{\mu}\right)^2 = 1\]
     * </p>
     * <p>
     * The second way to define an elliptical shape on the unit sphere is to define
     * first a regular planar ellipse drawn on a plane \(z = c\) and then to project
     * it onto the sphere using a central projection:
     * \[\left\{\begin{align*}
     * \left(\frac{x}{a}\right)^2 + \left(\frac{y}{b}\right)^2 &amp;= \left(\frac{z}{c}\right)^2\\
     * x^2 + y^2 + z^2 &amp;= 1
     * \end{align*}\right.\]
     * </p>
     * <p>
     * If we define two ellipses, one using each convention, choosing the defining constants
     * such that \(a=c\tan\lambda\) and \(b=c\tan\mu\) (the plane height \(c\) can be chosen
     * arbitrarily to any strictly positive value, \(c=1\) being the simplest choice) then
     * the two ellipses will be tangent to each other at the end of their principal axes. The
     * ellipses will however <em>not</em> match for intermediate points. As an example the
     * ellipses defined by a 40° half aperture angle along the X<sub>ell</sub> axis and a 10°
     * half aperture angle along the Y<sub>ell</sub> axis are as far as 0.6° from each other
     * at some intermediate points. This means that defining a proper ellipse is done by
     * specifying both the convention and the angular extensions along principal axes.
     * </p>
     * <p>
     * If the ellipse shape result from a diaphragm, for example a sensor looking through
     * a circular aperture but on a tilted angle rather than directly on axis, then the
     * {@link #CARTESIAN} convention should be used. If the ellipse shape result rather
     * from some physical property like an angle dependent sensitivity for two sensors
     * arranged orthogonally to each other in a focal plane with different gains and a
     * cutoff threshold, then the {@link #ANGULAR} convention should be used.
     * </p>
     * @since 10.1
     */
    public enum EllipticalConstraint {

        /** Constant for ellipse shape defined in angular coordinates. */
        ANGULAR() {

            /** {@inheritDoc} */
            @Override
            protected Vector3D directionAt(final double lambda, final double mu, final double theta) {
                final SinCos sct = FastMath.sinCos(theta);
                final SinCos sca = FastMath.sinCos(lambda * sct.cos());
                final SinCos scb = FastMath.sinCos(mu     * sct.sin());
                final double d   = 1.0 / FastMath.sqrt(1 - sca.sin() * sca.sin() * scb.sin() * scb.sin());
                return new Vector3D(sca.sin() * scb.cos() / d,
                                    sca.cos() * scb.sin() / d,
                                    sca.cos() * scb.cos() / d);
            }

            /** {@inheritDoc} */
            @Override
            protected double rawOffsetFromBoundary(final double lambda, final double mu,
                                                   final double x, final double y, final double z) {
                final double rA = FastMath.atan2( x, z) / lambda;
                final double rB = FastMath.atan2( y, z) / mu;
                return rA * rA + rB * rB - 1.0;
            }

        },

        /** Constant for ellipse shape defined in Cartesian coordinates. */
        CARTESIAN() {

            /** {@inheritDoc} */
            @Override
            protected Vector3D directionAt(final double lambda, final double mu, final double theta) {
                final SinCos sct = FastMath.sinCos(theta);
                final double a   = FastMath.tan(lambda);
                final double b   = FastMath.tan(mu);
                return new Vector3D(a * sct.cos(), b * sct.sin(), 1.0).normalize();
            }

            /** {@inheritDoc} */
            @Override
            protected double rawOffsetFromBoundary(final double lambda, final double mu,
                                                   final double x, final double y, final double z) {
                final double rX  = x / FastMath.tan(lambda);
                final double rY  = y / FastMath.tan(mu);
                final double rZ  = z;
                return rX * rX + rY * rY - rZ * rZ;
            }

        };

        /** Compute a point on the ellipse.
         * @param lambda half angular aperture along the X<sub>ell</sub> axis (i.e. rotation around +Y<sub>ell</sub>)
         * @param mu half angular aperture along the Y<sub>ell</sub> axis (i.e. rotation around -X<sub>ell</sub>)
         * @param theta phase angle
         * @return point on the ellipse in ellipse canonical frame
         */
        protected abstract Vector3D directionAt(double lambda, double mu, double theta);

        /** Get the raw offset of target direction with respect to the ellipse.
         * @param lambda half angular aperture along the X<sub>ell</sub> axis (i.e. rotation around +Y<sub>ell</sub>)
         * @param mu half angular aperture along the Y<sub>ell</sub> axis (i.e. rotation around -X<sub>ell</sub>)
         * @param x target direction coordinate in canonical ellipse frame
         * @param y target direction coordinate in canonical ellipse frame
         * @param z target direction coordinate in canonical ellipse frame
         * @return an offset negative if the target is within the ellipse
         * and positive if it is outside of the ellipse
         */
        protected abstract double rawOffsetFromBoundary(double lambda, double mu,
                                                        double x, double y, double z);

    }

}
