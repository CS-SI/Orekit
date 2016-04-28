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
package org.orekit.bodies;

import java.io.Serializable;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
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

        final double x = FastMath.abs(p.getX());
        final double y = p.getY();

        if (x <= ANGULAR_THRESHOLD * FastMath.abs(y)) {
            // the point is almost on the minor axis, approximate the ellipse with
            // the osculating circle whose center is at evolute cusp along minor axis
            final double osculatingRadius = a2 / b;
            final double evoluteCuspZ     = FastMath.copySign(a * e2 / g, -y);
            final double deltaZ           = y - evoluteCuspZ;
            final double ratio            = osculatingRadius / FastMath.hypot(deltaZ, x);
            return new Vector2D(FastMath.copySign(ratio * x, p.getX()),
                                evoluteCuspZ + ratio * deltaZ);
        }

        if (FastMath.abs(y) <= ANGULAR_THRESHOLD * x) {
            // the point is almost on the major axis

            final double osculatingRadius = b2 / a;
            final double evoluteCuspR     = a * e2;
            final double deltaR           = x - evoluteCuspR;
            if (deltaR >= 0) {
                // the point is outside of the ellipse evolute, approximate the ellipse
                // with the osculating circle whose center is at evolute cusp along major axis
                final double ratio = osculatingRadius / FastMath.hypot(y, deltaR);
                return new Vector2D(FastMath.copySign(evoluteCuspR + ratio * deltaR, p.getX()),
                                    ratio * y);
            }

            // the point is on the part of the major axis within ellipse evolute
            // we can compute the closest ellipse point analytically
            final double rEllipse = x / e2;
            return new Vector2D(FastMath.copySign(rEllipse, p.getX()),
                                FastMath.copySign(g * FastMath.sqrt(a2 - rEllipse * rEllipse), y));

        } else {
            final double k = FastMath.hypot(x / a, y / b);
            double projectedX = x / k;
            double projectedY = y / k;
            double deltaX = Double.POSITIVE_INFINITY;
            double deltaY = Double.POSITIVE_INFINITY;
            int count = 0;
            final double threshold = ANGULAR_THRESHOLD * ANGULAR_THRESHOLD * a2;
            while ((deltaX * deltaX + deltaY * deltaY) > threshold && count++ < 100) { // this loop usually converges in 3 iterations
                final double omegaX     = evoluteFactorX * projectedX * projectedX * projectedX;
                final double omegaY     = evoluteFactorY * projectedY * projectedY * projectedY;
                final double dx         = x - omegaX;
                final double dy         = y - omegaY;
                final double alpha      = b2 * dx * dx + a2 * dy * dy;
                final double beta       = b2 * omegaX * dx + a2 * omegaY * dy;
                final double gamma      = b2 * omegaX * omegaX + a2 * omegaY * omegaY - a2 * b2;
                final double deltaPrime = MathArrays.linearCombination(beta, beta, -alpha, gamma);
                final double ratio      = (beta <= 0) ?
                                          (FastMath.sqrt(deltaPrime) - beta) / alpha :
                                          -gamma / (FastMath.sqrt(deltaPrime) + beta);
                final double previousX  = projectedX;
                final double previousY  = projectedY;
                projectedX = omegaX + ratio * dx;
                projectedY = omegaY + ratio * dy;
                deltaX     = projectedX - previousX;
                deltaY     = projectedY - previousY;
            }
            return new Vector2D(FastMath.copySign(projectedX, p.getX()), projectedY);
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

    /** Find the center of curvature (point on the evolute) at the nadir of a point.
     * @param point point in the ellipse plane
     * @return center of curvature of the ellipse directly at point nadir
     * @since 7.1
     */
    public Vector2D getCenterOfCurvature(final Vector2D point) {
        final Vector2D projected = projectToEllipse(point);
        return new Vector2D(evoluteFactorX * projected.getX() * projected.getX() * projected.getX(),
                            evoluteFactorY * projected.getY() * projected.getY() * projected.getY());
    }

}
