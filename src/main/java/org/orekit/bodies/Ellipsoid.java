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

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;

/**
 * Modeling of a general three-axes ellipsoid. <p>
 * @since 7.0
 * @author Luc Maisonobe
 */
public class Ellipsoid implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140924L;

    /** Frame at the ellipsoid center, aligned with principal axes. */
    private final Frame frame;

    /** First semi-axis length. */
    private final double a;

    /** Second semi-axis length. */
    private final double b;

    /** Third semi-axis length. */
    private final double c;

    /** Simple constructor.
     * @param frame at the ellipsoid center, aligned with principal axes
     * @param a first semi-axis length
     * @param b second semi-axis length
     * @param c third semi-axis length
     */
    public Ellipsoid(final Frame frame, final double a, final double b, final double c) {
        this.frame = frame;
        this.a     = a;
        this.b     = b;
        this.c     = c;
    }

    /** Get the length of the first semi-axis.
     * @return length of the first semi-axis (m)
     */
    public double getA() {
        return a;
    }

    /** Get the length of the second semi-axis.
     * @return length of the second semi-axis (m)
     */
    public double getB() {
        return b;
    }

    /** Get the length of the third semi-axis.
     * @return length of the third semi-axis (m)
     */
    public double getC() {
        return c;
    }

    /** Get the ellipsoid central frame.
     * @return ellipsoid central frame
     */
    public Frame getFrame() {
        return frame;
    }

    /** Check if a point is inside the ellipsoid.
     * @param point point to check, in the ellipsoid frame
     * @return true if the point is inside the ellipsoid
     * (or exactly on ellipsoid surface)
     * @since 7.1
     */
    public boolean isInside(final Vector3D point) {
        final double scaledX = point.getX() / a;
        final double scaledY = point.getY() / b;
        final double scaledZ = point.getZ() / c;
        return scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ <= 1.0;
    }

    /** Compute the 2D ellipse at the intersection of the 3D ellipsoid and a plane.
     * @param planePoint point belonging to the plane, in the ellipsoid frame
     * @param planeNormal normal of the plane, in the ellipsoid frame
     * @return plane section or null if there are no intersections
     * @exception MathRuntimeException if the norm of planeNormal is null
     */
    public Ellipse getPlaneSection(final Vector3D planePoint, final Vector3D planeNormal)
        throws MathRuntimeException {

        // we define the points Q in the plane using two free variables τ and υ as:
        // Q = P + τ u + υ v
        // where u and v are two unit vectors belonging to the plane
        // Q belongs to the 3D ellipsoid so:
        // (xQ / a)² + (yQ / b)² + (zQ / c)² = 1
        // combining both equations, we get:
        //   (xP² + 2 xP (τ xU + υ xV) + (τ xU + υ xV)²) / a²
        // + (yP² + 2 yP (τ yU + υ yV) + (τ yU + υ yV)²) / b²
        // + (zP² + 2 zP (τ zU + υ zV) + (τ zU + υ zV)²) / c²
        // = 1
        // which can be rewritten:
        // α τ² + β υ² + 2 γ τυ + 2 δ τ + 2 ε υ + ζ = 0
        // with
        // α =  xU²  / a² +  yU²  / b² +  zU²  / c² > 0
        // β =  xV²  / a² +  yV²  / b² +  zV²  / c² > 0
        // γ = xU xV / a² + yU yV / b² + zU zV / c²
        // δ = xP xU / a² + yP yU / b² + zP zU / c²
        // ε = xP xV / a² + yP yV / b² + zP zV / c²
        // ζ =  xP²  / a² +  yP²  / b² +  zP²  / c² - 1
        // this is the equation of a conic (here an ellipse)
        // Of course, we note that if the point P belongs to the ellipsoid
        // then ζ = 0 and the equation holds at point P since τ = 0 and υ = 0
        final Vector3D u     = planeNormal.orthogonal();
        final Vector3D v     = Vector3D.crossProduct(planeNormal, u).normalize();
        final double xUOa    = u.getX() / a;
        final double yUOb    = u.getY() / b;
        final double zUOc    = u.getZ() / c;
        final double xVOa    = v.getX() / a;
        final double yVOb    = v.getY() / b;
        final double zVOc    = v.getZ() / c;
        final double xPOa    = planePoint.getX() / a;
        final double yPOb    = planePoint.getY() / b;
        final double zPOc    = planePoint.getZ() / c;
        final double alpha   = xUOa * xUOa + yUOb * yUOb + zUOc * zUOc;
        final double beta    = xVOa * xVOa + yVOb * yVOb + zVOc * zVOc;
        final double gamma   = MathArrays.linearCombination(xUOa, xVOa, yUOb, yVOb, zUOc, zVOc);
        final double delta   = MathArrays.linearCombination(xPOa, xUOa, yPOb, yUOb, zPOc, zUOc);
        final double epsilon = MathArrays.linearCombination(xPOa, xVOa, yPOb, yVOb, zPOc, zVOc);
        final double zeta    = MathArrays.linearCombination(xPOa, xPOa, yPOb, yPOb, zPOc, zPOc, 1, -1);

        // reduce the general equation α τ² + β υ² + 2 γ τυ + 2 δ τ + 2 ε υ + ζ = 0
        // to canonical form (λ/l)² + (μ/m)² = 1
        // using a coordinates change
        //       τ = τC + λ cosθ - μ sinθ
        //       υ = υC + λ sinθ + μ cosθ
        // or equivalently
        //       λ =   (τ - τC) cosθ + (υ - υC) sinθ
        //       μ = - (τ - τC) sinθ + (υ - υC) cosθ
        // τC and υC are the coordinates of the 2D ellipse center with respect to P
        // 2l and 2m and are the axes lengths (major or minor depending on which one is greatest)
        // θ is the angle of the 2D ellipse axis corresponding to axis with length 2l

        // choose θ in order to cancel the coupling term in λμ
        // expanding the general equation, we get: A λ² + B μ² + C λμ + D λ + E μ + F = 0
        // with C = 2[(β - α) cosθ sinθ + γ (cos²θ - sin²θ)]
        // hence the term is cancelled when θ = arctan(t), with γ t² + (α - β) t - γ = 0
        // As the solutions of the quadratic equation obey t₁t₂ = -1, they correspond to
        // angles θ in quadrature to each other. Selecting one solution or the other simply
        // exchanges the principal axes. As we don't care about which axis we want as the
        // first one, we select an arbitrary solution
        final double tanTheta;
        if (FastMath.abs(gamma) < Precision.SAFE_MIN) {
            tanTheta = 0.0;
        } else {
            final double bMA = beta - alpha;
            tanTheta = (bMA >= 0) ?
                       (-2 * gamma / (bMA + FastMath.sqrt(bMA * bMA + 4 * gamma * gamma))) :
                       (-2 * gamma / (bMA - FastMath.sqrt(bMA * bMA + 4 * gamma * gamma)));
        }
        final double tan2   = tanTheta * tanTheta;
        final double cos2   = 1 / (1 + tan2);
        final double sin2   = tan2 * cos2;
        final double cosSin = tanTheta * cos2;
        final double cos    = FastMath.sqrt(cos2);
        final double sin    = tanTheta * cos;

        // choose τC and υC in order to cancel the linear terms in λ and μ
        // expanding the general equation, we get: A λ² + B μ² + C λμ + D λ + E μ + F = 0
        // with D = 2[ (α τC + γ υC + δ) cosθ + (γ τC + β υC + ε) sinθ]
        //      E = 2[-(α τC + γ υC + δ) sinθ + (γ τC + β υC + ε) cosθ]
        // θ can be eliminated by combining the equations
        // D cosθ - E sinθ = 2[α τC + γ υC + δ]
        // E cosθ + D sinθ = 2[γ τC + β υC + ε]
        // hence the terms D and E are both cancelled (regardless of θ) when
        //     τC = (β δ - γ ε) / (γ² - α β)
        //     υC = (α ε - γ δ) / (γ² - α β)
        final double denom = MathArrays.linearCombination(gamma, gamma,   -alpha, beta);
        final double tauC  = MathArrays.linearCombination(beta,  delta,   -gamma, epsilon) / denom;
        final double nuC   = MathArrays.linearCombination(alpha, epsilon, -gamma, delta)   / denom;

        // compute l and m
        // expanding the general equation, we get: A λ² + B μ² + C λμ + D λ + E μ + F = 0
        // with A = α cos²θ + β sin²θ + 2 γ cosθ sinθ
        //      B = α sin²θ + β cos²θ - 2 γ cosθ sinθ
        //      F = α τC² + β υC² + 2 γ τC υC + 2 δ τC + 2 ε υC + ζ
        // hence we compute directly l = √(-F/A) and m = √(-F/B)
        final double twogcs = 2 * gamma * cosSin;
        final double bigA   = alpha * cos2 + beta * sin2 + twogcs;
        final double bigB   = alpha * sin2 + beta * cos2 - twogcs;
        final double bigF   = (alpha * tauC + 2 * (gamma * nuC + delta)) * tauC +
                              (beta * nuC + 2 * epsilon) * nuC + zeta;
        final double l      = FastMath.sqrt(-bigF / bigA);
        final double m      = FastMath.sqrt(-bigF / bigB);
        if (Double.isNaN(l + m)) {
            // the plane does not intersect the ellipsoid
            return null;
        }

        if (l > m) {
            return new Ellipse(new Vector3D(1, planePoint, tauC, u, nuC, v),
                               new Vector3D( cos, u, sin, v),
                               new Vector3D(-sin, u, cos, v),
                               l, m, frame);
        } else {
            return new Ellipse(new Vector3D(1, planePoint, tauC, u, nuC, v),
                               new Vector3D(sin, u, -cos, v),
                               new Vector3D(cos, u,  sin, v),
                               m, l, frame);
        }

    }

    /** Find a point on ellipsoid limb, as seen by an external observer.
     * @param observer observer position in ellipsoid frame
     * @param outside point outside ellipsoid in ellipsoid frame, defining the phase around limb
     * @return point on ellipsoid limb
     * @exception OrekitException if the observer is inside the ellipsoid
     * @exception MathRuntimeException if ellipsoid center, observer and outside
     * points are aligned
     * @since 7.1
     */
    public Vector3D pointOnLimb(final Vector3D observer, final Vector3D outside)
        throws OrekitException, MathRuntimeException {

        // there is no limb if we are inside the ellipsoid
        if (isInside(observer)) {
            throw new OrekitException(OrekitMessages.POINT_INSIDE_ELLIPSOID);
        }
        // cut the ellipsoid, to find an elliptical plane section
        final Vector3D normal  = Vector3D.crossProduct(observer, outside);
        final Ellipse  section = getPlaneSection(Vector3D.ZERO, normal);
        final double   a2      = section.getA() * section.getA();
        final double   b2      = section.getB() * section.getB();

        // the point on limb is tangential to the ellipse
        // if T(xt, yt) is an ellipse point at which the tangent is drawn
        // if O(xo, yo) is a point outside of the ellipse belonging to the tangent at T,
        // then the two following equations holds:
        //  a² yt²   + b² xt²   = a² b²  (T belongs to the ellipse)
        //  a² yt yo + b² xt xo = a² b²  (TP is tangent to the ellipse)
        // using the second equation to eliminate yt from the first equation, we get
        // b² (a² - xt xo)² + a² xt² yo² = a⁴ yo²
        // (a² yo² + b² xo²) xt² - 2 a² b² xo xt + a⁴ (b² - yo²) = 0
        // which can easily be solved for xt
        final Vector2D observer2D = section.toPlane(observer);
        final double   xo         = observer2D.getX();
        final double   yo         = observer2D.getY();
        final double   xo2        = xo * xo;
        final double   yo2        = yo * yo;
        final double   alpha      = a2 * yo2 + b2 * xo2;
        final double   beta       = a2 * b2 * xo;
        final double   gamma      = a2 * a2 * (b2 - yo2);
        // we know there are two solutions as we already checked the point is outside ellipsoid
        final double   sqrt       = FastMath.sqrt(beta * beta - alpha * gamma);
        final double   xt1;
        final double   xt2;
        if (beta > 0) {
            final double s = beta + sqrt;
            xt1 = s / alpha;
            xt2 = gamma / s;
        } else {
            final double s = beta - sqrt;
            xt1 = gamma / s;
            xt2 = s / alpha;
        }

        // we return the limb point in the direction of the outside point
        final Vector3D t1 = section.toSpace(new Vector2D(xt1, b2 * (a2 - xt1 * xo) / (a2 * yo)));
        final Vector3D t2 = section.toSpace(new Vector2D(xt2, b2 * (a2 - xt2 * xo) / (a2 * yo)));
        return Vector3D.distance(t1, outside) <= Vector3D.distance(t2, outside) ? t1 : t2;

    }

}
