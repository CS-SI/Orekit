/* Copyright 2002-2014 CS Systèmes d'Information
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
package org.orekit.bodies;

import java.io.Serializable;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BracketedUnivariateSolver;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Model of a 2D ellipse in 3D space.
 * <p>
 * These ellipses are mainly created as plane sections of general 3D ellipsoids,
 * but can be used for other purposes.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * @see Ellipsoid#getPlaneSection(Vector3D, Vector3D)
 * @since 7.0
 * @author Luc Maisonobe
 */
public class Ellipse implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140925L;

    /** Convergence limit. */
    private static final double ANGULAR_THRESHOLD = 1.0e-12;

    /** Center of the 2D ellipse. */
    private final Vector3D center;

    /** Unit vector along the major axis. */
    private final Vector3D u;

    /** Unit vector along the minor axis. */
    private final Vector3D v;

    /** Semi major axis. */
    private final double a;

    /** Semi minor axis. */
    private final double b;

    /** Frame in which the ellipse is defined. */
    private final Frame frame;

    /** Semi major axis radius power 2. */
    private final double a2;

    /** Semi minor axis power 2. */
    private final double b2;

    /** Eccentricity power 2. */
    private final double e2;

    /** 1 minus flatness. */
    private final double g;

    /** g * g. */
    private final double g2;

    /** Evolute factor along major axis. */
    private final double evoluteFactorX;

    /** Evolute factor along minor axis. */
    private final double evoluteFactorY;

    /** Simple constructor.
     * @param center center of the 2D ellipse
     * @param u unit vector along the major axis
     * @param v unit vector along the minor axis
     * @param a semi major axis
     * @param b semi minor axis
     * @param frame frame in which the ellipse is defined
     */
    public Ellipse(final Vector3D center, final Vector3D u,
                   final Vector3D v, final double a, final double b,
                   final Frame frame) {
        this.center = center;
        this.u      = u;
        this.v      = v;
        this.a      = a;
        this.b      = b;
        this.frame  = frame;
        this.a2     = a * a;
        this.g      = b / a;
        this.g2     = g * g;
        this.e2     = 1 - g2;
        this.b2     = b * b;
        this.evoluteFactorX = (a2 - b2) / (a2 * a2);
        this.evoluteFactorY = (b2 - a2) / (b2 * b2);
    }

    /** Get the center of the 2D ellipse.
     * @return center of the 2D ellipse
     */
    public Vector3D getCenter() {
        return center;
    }

    /** Get the unit vector along the major axis.
     * @return unit vector along the major axis
     */
    public Vector3D getU() {
        return u;
    }

    /** Get the unit vector along the minor axis.
     * @return unit vector along the minor axis
     */
    public Vector3D getV() {
        return v;
    }

    /** Get the semi major axis.
     * @return semi major axis
     */
    public double getA() {
        return a;
    }

    /** Get the semi minor axis.
     * @return semi minor axis
     */
    public double getB() {
        return b;
    }

    /** Get the defining frame.
     * @return defining frame
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get a point of the 2D ellipse.
     * @param theta angular parameter on the ellipse (really the eccentric anomaly)
     * @return ellipse point at theta, in underlying ellipsoid frame
     */
    public Vector3D pointAt(final double theta) {
        return toSpace(new Vector2D(a * FastMath.cos(theta), b * FastMath.sin(theta)));
    }

    /** Create a point from its ellipse-relative coordinates.
     * @param p point defined with respect to ellipse
     * @return point defined with respect to 3D frame
     * @see #toPlane(Vector3D)
     */
    public Vector3D toSpace(final Vector2D p) {
        return new Vector3D(1, center, p.getX(), u, p.getY(), v);
    }

    /** Project a point to the ellipse plane.
     * @param p point defined with respect to 3D frame
     * @return point defined with respect to ellipse
     * @see #toSpace(Vector2D)
     */
    public Vector2D toPlane(final Vector3D p) {
        final Vector3D delta = p.subtract(center);
        return new Vector2D(Vector3D.dotProduct(delta, u), Vector3D.dotProduct(delta, v));
    }

    /** Find the closest ellipse point.
     * @param p point in the ellipse plane to project on the ellipse itself
     * @return closest point belonging to 2D meridian ellipse
     */
    public Vector2D projectToEllipse(final Vector2D p) {

        if (p.getX() <= ANGULAR_THRESHOLD * FastMath.abs(p.getY())) {
            // the point is almost on the minor axis, approximate the ellipse with
            // the osculating circle whose center is at evolute cusp along minor axis
            final double osculatingRadius = a2 / b;
            final double evoluteCuspZ     = FastMath.copySign(a * e2 / g, -p.getY());
            final double deltaZ           = p.getY() - evoluteCuspZ;
            final double ratio           = osculatingRadius / FastMath.hypot(deltaZ, p.getX());
            return new Vector2D(ratio * p.getX(), evoluteCuspZ + ratio * deltaZ);
        }

        // find ellipse point closest to test point
        if (FastMath.abs(p.getY()) <= ANGULAR_THRESHOLD * p.getX()) {
            // the point is almost on the major axis

            final double osculatingRadius = b2 / a;
            final double evoluteCuspR     = a * e2;
            final double deltaR           = p.getX() - evoluteCuspR;
            if (deltaR >= 0) {
                // the point is outside of the ellipse evolute, approximate the ellipse
                // with the osculating circle whose center is at evolute cusp along major axis
                final double ratio = osculatingRadius / FastMath.hypot(p.getY(), deltaR);
                return new Vector2D(evoluteCuspR + ratio * deltaR, ratio * p.getY());
            }

            // the point is on the part of the major axis within ellipse evolute
            // we can compute the closest ellipse point analytically
            final double rEllipse = p.getX() / e2;
            return new Vector2D(rEllipse, FastMath.copySign(g * FastMath.sqrt(a2 - rEllipse * rEllipse), p.getY()));

        } else {

            final ClosestPointFinder finder = new ClosestPointFinder(p);
            final double rho;
            if (e2 >= ANGULAR_THRESHOLD) {
                // search the nadir point on the major axis,
                // somewhere within the evolute, i.e. between 0 and a * e2
                // we use a slight margin factor 1.1 to make sure we properly bracket
                // the solution even for points very close to major axis
                final BracketedUnivariateSolver<UnivariateFunction> solver =
                        new BracketingNthOrderBrentSolver(ANGULAR_THRESHOLD * b, 5);
                rho = solver.solve(100, finder, 0, 1.1 * a * e2);
            } else {
                // the evolute is almost reduced to the central point,
                // the ellipsoid is almost a sphere
                rho = 0;
            }
            return finder.intersectionPoint(rho);

        }

    }

    /** Project position-velocity-acceleration on an ellipse.
     * @param pv position-velocity-acceleration to project, in the reference frame
     * @return projected position-velocity-acceleration
     */
    public TimeStampedPVCoordinates projectToEllipse(final TimeStampedPVCoordinates pv) {

        // find the closest point in the meridian plane
        final Vector2D p2D = toPlane(pv.getPosition());
        final Vector2D e2D = projectToEllipse(p2D);

        // tangent to the ellipse
        final double fx = -a2 * e2D.getY();
        final double fy =  b2 * e2D.getX();
        final double f2 = fx * fx + fy * fy;
        final double f  = FastMath.sqrt(f2);
        final Vector2D tangent = new Vector2D(fx / f, fy / f);

        // normal to the ellipse (towards interior)
        final Vector2D normal = new Vector2D(-tangent.getY(), tangent.getX());

        // center of curvature
        final double x2     = e2D.getX() * e2D.getX();
        final double y2     = e2D.getY() * e2D.getY();
        final double eX     = evoluteFactorX * x2;
        final double eY     = evoluteFactorY * y2;
        final double omegaX = eX * e2D.getX();
        final double omegaY = eY * e2D.getY();

        // velocity projection ratio
        final double rho                = FastMath.hypot(e2D.getX() - omegaX, e2D.getY() - omegaY);
        final double d                  = FastMath.hypot(p2D.getX() - omegaX, p2D.getY() - omegaY);
        final double projectionRatio    = rho / d;

        // tangential velocity
        final Vector2D pDot2D           = new Vector2D(Vector3D.dotProduct(pv.getVelocity(), u),
                                                       Vector3D.dotProduct(pv.getVelocity(), v));
        final double   pDotTangent      = pDot2D.dotProduct(tangent);
        final double   pDotNormal       = pDot2D.dotProduct(normal);
        final double   eDotTangent      = projectionRatio * pDotTangent;
        final Vector2D eDot2D           = new Vector2D(eDotTangent, tangent);
        final Vector2D tangentDot       = new Vector2D(a2 * b2 * (e2D.getX() * eDot2D.getY() - e2D.getY() * eDot2D.getX()) / f2,
                                                       normal);

        // velocity of the center of curvature in the meridian plane
        final double omegaXDot          = 3 * eX * eDotTangent * tangent.getX();
        final double omegaYDot          = 3 * eY * eDotTangent * tangent.getY();

        // derivative of the projection ratio
        final double voz                = omegaXDot * tangent.getY() - omegaYDot * tangent.getX();
        final double vsz                = -pDotNormal;
        final double projectionRatioDot = ((rho - d) * voz - rho * vsz) / (d * d);

        // acceleration
        final Vector2D pDotDot2D        = new Vector2D(Vector3D.dotProduct(pv.getAcceleration(), u),
                                                       Vector3D.dotProduct(pv.getAcceleration(), v));
        final double   pDotDotTangent   = pDotDot2D.dotProduct(tangent);
        final double   pDotTangentDot   = pDot2D.dotProduct(tangentDot);
        final double   eDotDotTangent   = projectionRatio    * (pDotDotTangent + pDotTangentDot) +
                                          projectionRatioDot * pDotTangent;
        final Vector2D eDotDot2D        = new Vector2D(eDotDotTangent, tangent, eDotTangent, tangentDot);

        // back to 3D
        final Vector3D e3D       = toSpace(e2D);
        final Vector3D eDot3D    = new Vector3D(eDot2D.getX(),    u, eDot2D.getY(),    v);
        final Vector3D eDotDot3D = new Vector3D(eDotDot2D.getX(), u, eDotDot2D.getY(), v);

        return new TimeStampedPVCoordinates(pv.getDate(), e3D, eDot3D, eDotDot3D);

    }

    /** Local class for finding closest point to ellipse.
     * <p>
     * We consider a guessed equatorial point E somewhere along
     * the ellipse major axis, and within the ellipse evolute curve.
     * This point is defined by its coordinates (ρ, 0).
     * </p>
     * <p>
     * A point P belonging to line (E, A) can be computed from a
     * parameter k as follows:
     * </p>
     *
     * <pre>
     *   u = ρ + k * (r - ρ)
     *   v = 0 + k * (z - 0)
     * </pre>
     * <p>
     * For some specific positive value of k, the line (E, A) intersects the
     * ellipse at a point I which lies in the same quadrant as test point A.
     * There is another intersection point with k negative, but this
     * intersection point is not in the same quadrant as test point A.
     * </p>
     * <p>
     * The line joining point I and the center of the corresponding osculating
     * circle (i.e. the normal to the ellipse at point I) crosses major axis at
     * another equatorial point E'. If E and E' are the same points, then the
     * guessed point E is the true nadir. When the point I is close to the major
     * axis, the intersection of the line I with equatorial line is not well
     * defined, but the limit position of point E' can be computed, it is the
     * cusp of the ellipse evolute.
     * </p>
     * <p>
     * This class provides methods to compute I and to compute the offset
     * between E' and E, which allows to find the value of ρ such that I is the
     * closest point of the ellipse to A.
     * </p>
     */
    private class ClosestPointFinder implements UnivariateFunction {

        /** Test point. */
        private final Vector2D p;

        /** Simple constructor.
         * @param p test point
         */
        public ClosestPointFinder(final Vector2D p) {
            this.p = p;
        }

        /** Compute intersection point I.
         * @param rho guessed equatorial point radius
         * @return coordinates of intersection point I
         */
        private Vector2D intersectionPoint(final double rho) {
            final double k = kOnEllipse(rho);
            return new Vector2D(rho + k * (p.getX() - rho), k * p.getY());
        }

        /** Compute parameter k of intersection point I.
         * @param rho guessed equatorial point radius
         * @return value of parameter k such that line point belongs to the ellipse
         */
        private double kOnEllipse(final double rho) {

            // rho defines a point on the ellipse major axis E with coordinates (rho, 0)
            // the fixed test point A has coordinates (r, z)
            // the coordinates (u, v) of point P belonging to line (E, A) can be
            // computed from a parameter k as follows:
            //     u = rho + k * (r - rho)
            //     v = 0 + k * (z - 0)
            // if P also belongs to the ellipse, the following quadratic
            // equation in k holds: alpha * k^2 + 2 * beta * k + gamma = 0
            final double dr = p.getX() - rho;
            final double alpha = b2 * dr * dr + a2 * p.getY() * p.getY();
            final double beta  = b2 * rho * dr;
            final double gamma = b2 * (rho - a) * (rho + a);

            // positive root of the quadratic equation
            final double s = FastMath.sqrt(beta * beta - alpha * gamma);
            return (beta > 0) ? -gamma / (s + beta) : (s - beta) / alpha;

        }

        /** Compute offset between guessed equatorial point and nadir.
         * <p>
         * We consider a guessed equatorial point E somewhere along the ellipse
         * major axis, and within the ellipse evolute curve. The line (E, A)
         * intersects the ellipse at some point I. The line segment starting at
         * point I and going along the interior normal of the ellipse crosses
         * major axis at another equatorial point E'. If E and E' are the same
         * points, then the guessed point E is the true nadir. This method
         * compute the offset between E and E' along major axis.
         * </p>
         * @param rho guessed equatorial point radius (point E is at coordinates
         *        (rho, 0) in the ellipse canonical axes system)
         * @return offset between E and E'
         */
        @Override
        public double value(final double rho) {

            // intersection of line (E, A) with ellipse
            final double k = kOnEllipse(rho);
            final double l = rho + k * (p.getX() - rho);

            // equatorial point E' in the nadir direction of P
            final double rhoPrime = l * e2;

            // offset between guessed point and recovered nadir point
            return rhoPrime - rho;

        }

    }

}
