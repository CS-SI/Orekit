/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.bodies;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.frames.Frame;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Model of a 2D ellipse in 3D space.
 * <p>
 * These ellipses are mainly created as plane sections of general 3D ellipsoids,
 * but can be used for other purposes.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * @see Ellipsoid#getPlaneSection(FieldVector3D, FieldVector3D)
 * @param <T> the type of the field elements
 * @since 12.0
 * @author Luc Maisonobe
 */
public class FieldEllipse<T extends CalculusFieldElement<T>> {

    /** Convergence limit. */
    private static final double ANGULAR_THRESHOLD = 1.0e-12;

    /** Center of the 2D ellipse. */
    private final FieldVector3D<T> center;

    /** Unit vector along the major axis. */
    private final FieldVector3D<T> u;

    /** Unit vector along the minor axis. */
    private final FieldVector3D<T> v;

    /** Semi major axis. */
    private final T a;

    /** Semi minor axis. */
    private final T b;

    /** Frame in which the ellipse is defined. */
    private final Frame frame;

    /** Semi major axis radius power 2. */
    private final T a2;

    /** Semi minor axis power 2. */
    private final T b2;

    /** Eccentricity power 2. */
    private final T e2;

    /** 1 minus flatness. */
    private final T g;

    /** g * g. */
    private final T g2;

    /** Evolute factor along major axis. */
    private final T evoluteFactorX;

    /** Evolute factor along minor axis. */
    private final T evoluteFactorY;

    /** Simple constructor.
     * @param center center of the 2D ellipse
     * @param u unit vector along the major axis
     * @param v unit vector along the minor axis
     * @param a semi major axis
     * @param b semi minor axis
     * @param frame frame in which the ellipse is defined
     */
    public FieldEllipse(final FieldVector3D<T> center, final FieldVector3D<T> u,
                        final FieldVector3D<T> v, final T a, final T b,
                        final Frame frame) {
        this.center = center;
        this.u      = u;
        this.v      = v;
        this.a      = a;
        this.b      = b;
        this.frame  = frame;
        this.a2     = a.multiply(a);
        this.g      = b.divide(a);
        this.g2     = g.multiply(g);
        this.e2     = g2.negate().add(1);
        this.b2     = b.multiply(b);
        this.evoluteFactorX = a2.subtract(b2).divide(a2.multiply(a2));
        this.evoluteFactorY = b2.subtract(a2).divide(b2.multiply(b2));
    }

    /** Get the center of the 2D ellipse.
     * @return center of the 2D ellipse
     */
    public FieldVector3D<T> getCenter() {
        return center;
    }

    /** Get the unit vector along the major axis.
     * @return unit vector along the major axis
     */
    public FieldVector3D<T> getU() {
        return u;
    }

    /** Get the unit vector along the minor axis.
     * @return unit vector along the minor axis
     */
    public FieldVector3D<T> getV() {
        return v;
    }

    /** Get the semi major axis.
     * @return semi major axis
     */
    public T getA() {
        return a;
    }

    /** Get the semi minor axis.
     * @return semi minor axis
     */
    public T getB() {
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
    public FieldVector3D<T> pointAt(final T theta) {
        final FieldSinCos<T> scTheta = FastMath.sinCos(theta);
        return toSpace(new FieldVector2D<>(a.multiply(scTheta.cos()), b.multiply(scTheta.sin())));
    }

    /** Create a point from its ellipse-relative coordinates.
     * @param p point defined with respect to ellipse
     * @return point defined with respect to 3D frame
     * @see #toPlane(FieldVector3D)
     */
    public FieldVector3D<T> toSpace(final FieldVector2D<T> p) {
        return new FieldVector3D<T>(a.getField().getOne(), center, p.getX(), u, p.getY(), v);
    }

    /** Project a point to the ellipse plane.
     * @param p point defined with respect to 3D frame
     * @return point defined with respect to ellipse
     * @see #toSpace(FieldVector2D)
     */
    public FieldVector2D<T> toPlane(final FieldVector3D<T> p) {
        final FieldVector3D<T> delta = p.subtract(center);
        return new FieldVector2D<>(FieldVector3D.dotProduct(delta, u), FieldVector3D.dotProduct(delta, v));
    }

    /** Find the closest ellipse point.
     * @param p point in the ellipse plane to project on the ellipse itself
     * @return closest point belonging to 2D meridian ellipse
     */
    public FieldVector2D<T> projectToEllipse(final FieldVector2D<T> p) {

        final T x = FastMath.abs(p.getX());
        final T y = p.getY();

        if (x.getReal() <= ANGULAR_THRESHOLD * FastMath.abs(y.getReal())) {
            // the point is almost on the minor axis, approximate the ellipse with
            // the osculating circle whose center is at evolute cusp along minor axis
            final T osculatingRadius = a2.divide(b);
            final T evoluteCuspZ     = FastMath.copySign(a.multiply(e2).divide(g), y.negate());
            final T deltaZ           = y.subtract(evoluteCuspZ);
            final T ratio            = osculatingRadius.divide(FastMath.hypot(deltaZ, x));
            return new FieldVector2D<>(FastMath.copySign(ratio.multiply(x), p.getX()),
                                       evoluteCuspZ.add(ratio.multiply(deltaZ)));
        }

        if (FastMath.abs(y.getReal()) <= ANGULAR_THRESHOLD * x.getReal()) {
            // the point is almost on the major axis

            final T osculatingRadius = b2.divide(a);
            final T evoluteCuspR     = a.multiply(e2);
            final T deltaR           = x.subtract(evoluteCuspR);
            if (deltaR.getReal() >= 0) {
                // the point is outside of the ellipse evolute, approximate the ellipse
                // with the osculating circle whose center is at evolute cusp along major axis
                final T ratio = osculatingRadius.divide(FastMath.hypot(y, deltaR));
                return new FieldVector2D<>(FastMath.copySign(evoluteCuspR.add(ratio.multiply(deltaR)), p.getX()),
                                           ratio.multiply(y));
            }

            // the point is on the part of the major axis within ellipse evolute
            // we can compute the closest ellipse point analytically
            final T rEllipse = x.divide(e2);
            return new FieldVector2D<>(FastMath.copySign(rEllipse, p.getX()),
                                       FastMath.copySign(g.multiply(FastMath.sqrt(a2.subtract(rEllipse.multiply(rEllipse)))), y));

        } else {

            // initial point at evolute cusp along major axis
            T omegaX = a.multiply(e2);
            T omegaY = a.getField().getZero();

            T projectedX  = x;
            T projectedY  = y;
            double deltaX = Double.POSITIVE_INFINITY;
            double deltaY = Double.POSITIVE_INFINITY;
            int count = 0;
            final double threshold = ANGULAR_THRESHOLD * ANGULAR_THRESHOLD * a2.getReal();
            while ((deltaX * deltaX + deltaY * deltaY) > threshold && count++ < 100) { // this loop usually converges in 3 iterations

                // find point at the intersection of ellipse and line going from query point to evolute point
                final T dx         = x.subtract(omegaX);
                final T dy         = y.subtract(omegaY);
                final T alpha      = b2.multiply(dx).multiply(dx).add(a2.multiply(dy).multiply(dy));
                final T betaPrime  = b2.multiply(omegaX).multiply(dx).add(a2.multiply(omegaY).multiply(dy));
                final T gamma      = b2.multiply(omegaX).multiply(omegaX).add(a2.multiply(omegaY).multiply(omegaY)).subtract(a2.multiply(b2));
                final T deltaPrime = a.linearCombination(betaPrime, betaPrime, alpha.negate(), gamma);
                final T ratio      = (betaPrime.getReal() <= 0) ?
                                          FastMath.sqrt(deltaPrime).subtract(betaPrime).divide(alpha) :
                                          gamma.negate().divide(FastMath.sqrt(deltaPrime).add(betaPrime));
                final T previousX  = projectedX;
                final T previousY  = projectedY;
                projectedX = omegaX.add(ratio.multiply(dx));
                projectedY = omegaY.add(ratio.multiply(dy));

                // find new evolute point
                omegaX     = evoluteFactorX.multiply(projectedX).multiply(projectedX).multiply(projectedX);
                omegaY     = evoluteFactorY.multiply(projectedY).multiply(projectedY).multiply(projectedY);

                // compute convergence parameters
                deltaX     = projectedX.subtract(previousX).getReal();
                deltaY     = projectedY.subtract(previousY).getReal();

            }
            return new FieldVector2D<>(FastMath.copySign(projectedX, p.getX()), projectedY);
        }
    }

    /** Project position-velocity-acceleration on an ellipse.
     * @param pv position-velocity-acceleration to project, in the reference frame
     * @return projected position-velocity-acceleration
     */
    public TimeStampedFieldPVCoordinates<T> projectToEllipse(final TimeStampedFieldPVCoordinates<T> pv) {

        // find the closest point in the meridian plane
        final FieldVector2D<T>p2D = toPlane(pv.getPosition());
        final FieldVector2D<T>e2D = projectToEllipse(p2D);

        // tangent to the ellipse
        final T fx = a2.negate().multiply(e2D.getY());
        final T fy = b2.multiply(e2D.getX());
        final T f2 = fx.multiply(fx).add(fy.multiply(fy));
        final T f  = FastMath.sqrt(f2);
        final FieldVector2D<T>tangent = new FieldVector2D<>(fx.divide(f), fy.divide(f));

        // normal to the ellipse (towards interior)
        final FieldVector2D<T>normal = new FieldVector2D<>(tangent.getY().negate(), tangent.getX());

        // center of curvature
        final T x2     = e2D.getX().multiply(e2D.getX());
        final T y2     = e2D.getY().multiply(e2D.getY());
        final T eX     = evoluteFactorX.multiply(x2);
        final T eY     = evoluteFactorY.multiply(y2);
        final T omegaX = eX.multiply(e2D.getX());
        final T omegaY = eY.multiply(e2D.getY());

        // velocity projection ratio
        final T rho                = FastMath.hypot(e2D.getX().subtract(omegaX), e2D.getY().subtract(omegaY));
        final T d                  = FastMath.hypot(p2D.getX().subtract(omegaX), p2D.getY().subtract(omegaY));
        final T projectionRatio    = rho.divide(d);

        // tangential velocity
        final FieldVector2D<T>pDot2D     = new FieldVector2D<>(FieldVector3D.dotProduct(pv.getVelocity(), u),
                                                              FieldVector3D.dotProduct(pv.getVelocity(), v));
        final T   pDotTangent            = pDot2D.dotProduct(tangent);
        final T   pDotNormal             = pDot2D.dotProduct(normal);
        final T   eDotTangent            = projectionRatio.multiply(pDotTangent);
        final FieldVector2D<T>eDot2D     = new FieldVector2D<>(eDotTangent, tangent);
        final FieldVector2D<T>tangentDot = new FieldVector2D<>(a2.multiply(b2).
                                                               multiply(e2D.getX().multiply(eDot2D.getY()).
                                                                        subtract(e2D.getY().multiply(eDot2D.getX()))).
                                                               divide(f2),
                                                               normal);

        // velocity of the center of curvature in the meridian plane
        final T omegaXDot          = eX.multiply(eDotTangent).multiply(tangent.getX()).multiply(3);
        final T omegaYDot          = eY.multiply(eDotTangent).multiply(tangent.getY()).multiply(3);

        // derivative of the projection ratio
        final T voz                = omegaXDot.multiply(tangent.getY()).subtract(omegaYDot.multiply(tangent.getX()));
        final T vsz                = pDotNormal.negate();
        final T projectionRatioDot = rho.subtract(d).multiply(voz).subtract(rho.multiply(vsz)).divide(d.multiply(d));

        // acceleration
        final FieldVector2D<T>pDotDot2D  = new FieldVector2D<>(FieldVector3D.dotProduct(pv.getAcceleration(), u),
                                                               FieldVector3D.dotProduct(pv.getAcceleration(), v));
        final T   pDotDotTangent         = pDotDot2D.dotProduct(tangent);
        final T   pDotTangentDot         = pDot2D.dotProduct(tangentDot);
        final T   eDotDotTangent         = projectionRatio.multiply(pDotDotTangent.add(pDotTangentDot)).
                                           add(projectionRatioDot.multiply(pDotTangent));
        final FieldVector2D<T>eDotDot2D = new FieldVector2D<>(eDotDotTangent, tangent, eDotTangent, tangentDot);

        // back to 3D
        final FieldVector3D<T> e3D       = toSpace(e2D);
        final FieldVector3D<T> eDot3D    = new FieldVector3D<T>(eDot2D.getX(),    u, eDot2D.getY(),    v);
        final FieldVector3D<T> eDotDot3D = new FieldVector3D<T>(eDotDot2D.getX(), u, eDotDot2D.getY(), v);

        return new TimeStampedFieldPVCoordinates<>(pv.getDate(), e3D, eDot3D, eDotDot3D);

    }

    /** Find the center of curvature (point on the evolute) at the nadir of a point.
     * @param point point in the ellipse plane
     * @return center of curvature of the ellipse directly at point nadir
     */
    public FieldVector2D<T> getCenterOfCurvature(final FieldVector2D<T> point) {
        final FieldVector2D<T>projected = projectToEllipse(point);
        return new FieldVector2D<>(evoluteFactorX.multiply(projected.getX()).multiply(projected.getX()).multiply(projected.getX()),
                                   evoluteFactorY.multiply(projected.getY()).multiply(projected.getY()).multiply(projected.getY()));
    }

}
