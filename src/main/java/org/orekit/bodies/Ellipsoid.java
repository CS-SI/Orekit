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
package org.orekit.bodies;

import java.io.Serializable;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;

/**
 * Modeling of a general three-axes ellipsoid.
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

    /** Check if a point is inside the ellipsoid.
     * @param point point to check, in the ellipsoid frame
     * @return true if the point is inside the ellipsoid
     * (or exactly on ellipsoid surface)
     * @param <T> the type of the field elements
     * @since 12.0
     */
    public <T extends CalculusFieldElement<T>> boolean isInside(final FieldVector3D<T> point) {
        final T scaledX = point.getX().divide(a);
        final T scaledY = point.getY().divide(b);
        final T scaledZ = point.getZ().divide(c);
        final T d2      = scaledX.multiply(scaledX).add(scaledY.multiply(scaledY)).add(scaledZ.multiply(scaledZ));
        return d2.subtract(1.0).getReal() <= 0.0;
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

    /** Compute the 2D ellipse at the intersection of the 3D ellipsoid and a plane.
     * @param planePoint point belonging to the plane, in the ellipsoid frame
     * @param planeNormal normal of the plane, in the ellipsoid frame
     * @return plane section or null if there are no intersections
     * @exception MathRuntimeException if the norm of planeNormal is null
     * @param <T> the type of the field elements
     * @since 12.0
     */
    public <T extends CalculusFieldElement<T>> FieldEllipse<T> getPlaneSection(final FieldVector3D<T> planePoint, final FieldVector3D<T> planeNormal)
        throws MathRuntimeException {

        final T zero = planePoint.getX().getField().getZero();
        final T one  = planePoint.getX().getField().getOne();

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
        final FieldVector3D<T> u     = planeNormal.orthogonal();
        final FieldVector3D<T> v     = FieldVector3D.crossProduct(planeNormal, u).normalize();
        final T xUOa    = u.getX().divide(a);
        final T yUOb    = u.getY().divide(b);
        final T zUOc    = u.getZ().divide(c);
        final T xVOa    = v.getX().divide(a);
        final T yVOb    = v.getY().divide(b);
        final T zVOc    = v.getZ().divide(c);
        final T xPOa    = planePoint.getX().divide(a);
        final T yPOb    = planePoint.getY().divide(b);
        final T zPOc    = planePoint.getZ().divide(c);
        final T alpha   = xUOa.multiply(xUOa).add(yUOb.multiply(yUOb)).add(zUOc.multiply(zUOc));
        final T beta    = xVOa.multiply(xVOa).add(yVOb.multiply(yVOb)).add(zVOc.multiply(zVOc));
        final T gamma   = alpha.linearCombination(xUOa, xVOa, yUOb, yVOb, zUOc, zVOc);
        final T delta   = alpha.linearCombination(xPOa, xUOa, yPOb, yUOb, zPOc, zUOc);
        final T epsilon = alpha.linearCombination(xPOa, xVOa, yPOb, yVOb, zPOc, zVOc);
        final T zeta    = alpha.linearCombination(xPOa, xPOa, yPOb, yPOb, zPOc, zPOc, one, one.negate());

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
        final T tanTheta;
        if (FastMath.abs(gamma.getReal()) < Precision.SAFE_MIN) {
            tanTheta = zero;
        } else {
            final T bMA = beta.subtract(alpha);
            tanTheta = (bMA.getReal() >= 0) ?
                       gamma.multiply(-2).divide(bMA.add(FastMath.sqrt(bMA.multiply(bMA).add(gamma.multiply(gamma).multiply(4))))) :
                       gamma.multiply(-2).divide(bMA.subtract(FastMath.sqrt(bMA.multiply(bMA).add(gamma.multiply(gamma).multiply(4)))));
        }
        final T tan2   = tanTheta.multiply(tanTheta);
        final T cos2   = tan2.add(1).reciprocal();
        final T sin2   = tan2.multiply(cos2);
        final T cosSin = tanTheta.multiply(cos2);
        final T cos    = FastMath.sqrt(cos2);
        final T sin    = tanTheta.multiply(cos);

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
        final T invDenom = gamma.linearCombination(gamma, gamma,   alpha.negate(), beta).reciprocal();
        final T tauC     = gamma.linearCombination(beta,  delta,   gamma.negate(), epsilon).multiply(invDenom);
        final T nuC      = gamma.linearCombination(alpha, epsilon, gamma.negate(), delta).multiply(invDenom);

        // compute l and m
        // expanding the general equation, we get: A λ² + B μ² + C λμ + D λ + E μ + F = 0
        // with A = α cos²θ + β sin²θ + 2 γ cosθ sinθ
        //      B = α sin²θ + β cos²θ - 2 γ cosθ sinθ
        //      F = α τC² + β υC² + 2 γ τC υC + 2 δ τC + 2 ε υC + ζ
        // hence we compute directly l = √(-F/A) and m = √(-F/B)
        final T twogcs = gamma.multiply(cosSin).multiply(2);
        final T bigA   = alpha.multiply(cos2).add(beta.multiply(sin2)).add(twogcs);
        final T bigB   = alpha.multiply(sin2).add(beta.multiply(cos2)).subtract(twogcs);
        final T bigFN  = alpha.multiply(tauC).add(gamma.multiply(nuC).add(delta).multiply(2)).multiply(tauC).
                         add(beta.multiply(nuC).add(epsilon.multiply(2)).multiply(nuC)).
                         add(zeta).
                         negate();
        final T l      = FastMath.sqrt(bigFN.divide(bigA));
        final T m      = FastMath.sqrt(bigFN.divide(bigB));
        if (l.add(m).isNaN()) {
            // the plane does not intersect the ellipsoid
            return null;
        }

        if (l.subtract(m).getReal() > 0) {
            return new FieldEllipse<>(new FieldVector3D<>(tauC.getField().getOne(), planePoint, tauC, u, nuC, v),
                                      new FieldVector3D<>(cos,          u, sin, v),
                                      new FieldVector3D<>(sin.negate(), u, cos, v),
                                      l, m, frame);
        } else {
            return new FieldEllipse<>(new FieldVector3D<>(tauC.getField().getOne(), planePoint, tauC, u, nuC, v),
                                      new FieldVector3D<>(sin, u, cos.negate(), v),
                                      new FieldVector3D<>(cos, u, sin,          v),
                                      m, l, frame);
        }

    }

    /** Find a point on ellipsoid limb, as seen by an external observer.
     * @param observer observer position in ellipsoid frame
     * @param outside point outside ellipsoid in ellipsoid frame, defining the phase around limb
     * @return point on ellipsoid limb
     * @exception MathRuntimeException if ellipsoid center, observer and outside
     * points are aligned
     * @since 7.1
     */
    public Vector3D pointOnLimb(final Vector3D observer, final Vector3D outside)
        throws MathRuntimeException {

        // There is no limb if we are inside the ellipsoid
        if (isInside(observer)) {
            throw new OrekitException(OrekitMessages.POINT_INSIDE_ELLIPSOID);
        }
        // Cut the ellipsoid, to find an elliptical plane section
        final Vector3D normal  = Vector3D.crossProduct(observer, outside);
        final Ellipse  section = getPlaneSection(Vector3D.ZERO, normal);

        // the point on limb is tangential to the ellipse
        // if T(xt, yt) is an ellipse point at which the tangent is drawn
        // if O(xo, yo) is a point outside of the ellipse belonging to the tangent at T,
        // then the two following equations holds:
        // (1) a² yt²   + b² xt²   = a² b²  (T belongs to the ellipse)
        // (2) a² yt yo + b² xt xo = a² b²  (TP is tangent to the ellipse)
        // using the second equation to eliminate yt from the first equation, we get
        // b² (a² - xt xo)² + a² xt² yo² = a⁴ yo²
        // (3) (a² yo² + b² xo²) xt² - 2 a² b² xo xt + a⁴ (b² - yo²) = 0
        // which can easily be solved for xt

        // To avoid numerical errors, the x and y coordinates in the ellipse plane are normalized using:
        // x' = x / a and y' = y / b
        //
        // This gives:
        // (1) y't² + x't² = 1
        // (2) y't y'o + x't x'o = 1
        //
        // And finally:
        // (3) (x'o² + y'o²) x't² - 2 x't x'o + 1 - y'o² = 0
        //
        // Solving for x't, we get the reduced discriminant:
        // delta' = beta'² - alpha' * gamma'
        //
        // With:
        // beta' = x'o
        // alpha' = x'o² + y'o²
        // gamma' = 1 - y'o²
        //
        // Simplifying to  cancel a term of x'o².
        // delta' = y'o² (x'o² + y'o² - 1) = y'o² (alpha' - 1)
        //
        // After solving for xt1, xt2 using (3) the values are substituted into (2) to
        // compute yt1, yt2. Then terms of x'o may be canceled from the expressions for
        // yt1 and yt2. Additionally a point discontinuity is removed at y'o=0 from both
        // yt1 and yt2.
        //
        // y't1 = (y'o - x'o d) / (x'o² + y'o²)
        // y't2 = (x'o y'o + d) / (x + sqrt(delta'))
        //
        // where:
        // d = sign(y'o) sqrt(alpha' - 1)

        // Get the point in ellipse plane frame (2D)
        final Vector2D observer2D = section.toPlane(observer);

        // Normalize and compute intermediary terms
        final double ap = section.getA();
        final double bp = section.getB();
        final double xpo = observer2D.getX() / ap;
        final double ypo = observer2D.getY() / bp;
        final double xpo2 = xpo * xpo;
        final double ypo2 = ypo * ypo;
        final double   alphap      = ypo2 + xpo2;
        final double   gammap      = 1. - ypo2;

        // Compute the roots
        // We know there are two solutions as we already checked the point is outside ellipsoid
        final double sqrt = FastMath.sqrt(alphap - 1);
        final double sqrtp = FastMath.abs(ypo) * sqrt;
        final double sqrtSigned = FastMath.copySign(sqrt, ypo);

        // Compute the roots (ordered by value)
        final double   xpt1;
        final double   xpt2;
        final double   ypt1;
        final double   ypt2;
        if (xpo > 0) {
            final double s = xpo + sqrtp;
            // xpt1 = (beta' + sqrt(delta')) / alpha' (with beta' = x'o)
            xpt1 = s / alphap;
            // x't2 = gamma' / (beta' + sqrt(delta')) since x't1 * x't2 = gamma' / alpha'
            xpt2 = gammap / s;
            // Get the corresponding values of y't
            ypt1 = (ypo - xpo * sqrtSigned) / alphap;
            ypt2 = (xpo * ypo + sqrtSigned) / s;
        } else {
            final double s = xpo - sqrtp;
            // x't1 and x't2 are reverted compared to previous solution
            xpt1 = gammap / s;
            xpt2 = s / alphap;
            // Get the corresponding values of y't
            ypt2 = (ypo + xpo * sqrtSigned) / alphap;
            ypt1 = (xpo * ypo - sqrtSigned) / s;
        }

        // De-normalize and express the two solutions in 3D
        final Vector3D tp1 = section.toSpace(new Vector2D(ap * xpt1, bp * ypt1));
        final Vector3D tp2 = section.toSpace(new Vector2D(ap * xpt2, bp * ypt2));

        // Return the limb point in the direction of the outside point
        return Vector3D.distance(tp1, outside) <= Vector3D.distance(tp2, outside) ? tp1 : tp2;

    }

    /** Find a point on ellipsoid limb, as seen by an external observer.
     * @param observer observer position in ellipsoid frame
     * @param outside point outside ellipsoid in ellipsoid frame, defining the phase around limb
     * @return point on ellipsoid limb
     * @exception MathRuntimeException if ellipsoid center, observer and outside
     * points are aligned
     * @param <T> the type of the field elements
     * @since 12.0
     */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> pointOnLimb(final FieldVector3D<T> observer, final FieldVector3D<T> outside)
        throws MathRuntimeException {

        // There is no limb if we are inside the ellipsoid
        if (isInside(observer)) {
            throw new OrekitException(OrekitMessages.POINT_INSIDE_ELLIPSOID);
        }
        // Cut the ellipsoid, to find an elliptical plane section
        final FieldVector3D<T> normal  = FieldVector3D.crossProduct(observer, outside);
        final FieldEllipse<T>  section = getPlaneSection(FieldVector3D.getZero(observer.getX().getField()), normal);

        // the point on limb is tangential to the ellipse
        // if T(xt, yt) is an ellipse point at which the tangent is drawn
        // if O(xo, yo) is a point outside of the ellipse belonging to the tangent at T,
        // then the two following equations holds:
        // (1) a² yt²   + b² xt²   = a² b²  (T belongs to the ellipse)
        // (2) a² yt yo + b² xt xo = a² b²  (TP is tangent to the ellipse)
        // using the second equation to eliminate yt from the first equation, we get
        // b² (a² - xt xo)² + a² xt² yo² = a⁴ yo²
        // (3) (a² yo² + b² xo²) xt² - 2 a² b² xo xt + a⁴ (b² - yo²) = 0
        // which can easily be solved for xt

        // To avoid numerical errors, the x and y coordinates in the ellipse plane are normalized using:
        // x' = x / a and y' = y / b
        //
        // This gives:
        // (1) y't² + x't² = 1
        // (2) y't y'o + x't x'o = 1
        //
        // And finally:
        // (3) (x'o² + y'o²) x't² - 2 x't x'o + 1 - y'o² = 0
        //
        // Solving for x't, we get the reduced discriminant:
        // delta' = beta'² - alpha' * gamma'
        //
        // With:
        // beta' = x'o
        // alpha' = x'o² + y'o²
        // gamma' = 1 - y'o²
        //
        // Simplifying to  cancel a term of x'o².
        // delta' = y'o² (x'o² + y'o² - 1) = y'o² (alpha' - 1)
        //
        // After solving for xt1, xt2 using (3) the values are substituted into (2) to
        // compute yt1, yt2. Then terms of x'o may be canceled from the expressions for
        // yt1 and yt2. Additionally a point discontinuity is removed at y'o=0 from both
        // yt1 and yt2.
        //
        // y't1 = (y'o - x'o d) / (x'o² + y'o²)
        // y't2 = (x'o y'o + d) / (x + sqrt(delta'))
        //
        // where:
        // d = sign(y'o) sqrt(alpha' - 1)

        // Get the point in ellipse plane frame (2D)
        final FieldVector2D<T> observer2D = section.toPlane(observer);

        // Normalize and compute intermediary terms
        final T ap     = section.getA();
        final T bp     = section.getB();
        final T xpo    = observer2D.getX().divide(ap);
        final T ypo    = observer2D.getY().divide(bp);
        final T xpo2   = xpo.multiply(xpo);
        final T ypo2   = ypo.multiply(ypo);
        final T alphap = ypo2.add(xpo2);
        final T gammap = ypo2.negate().add(1);

        // Compute the roots
        // We know there are two solutions as we already checked the point is outside ellipsoid
        final T sqrt = FastMath.sqrt(alphap.subtract(1));
        final T sqrtp = FastMath.abs(ypo).multiply(sqrt);
        final T sqrtSigned = FastMath.copySign(sqrt, ypo);

        // Compute the roots (ordered by value)
        final T   xpt1;
        final T   xpt2;
        final T   ypt1;
        final T   ypt2;
        if (xpo.getReal() > 0) {
            final T s = xpo.add(sqrtp);
            // xpt1 = (beta' + sqrt(delta')) / alpha' (with beta' = x'o)
            xpt1 = s.divide(alphap);
            // x't2 = gamma' / (beta' + sqrt(delta')) since x't1 * x't2 = gamma' / alpha'
            xpt2 = gammap.divide(s);
            // Get the corresponding values of y't
            ypt1 = ypo.subtract(xpo.multiply(sqrtSigned)).divide(alphap);
            ypt2 = xpo.multiply(ypo).add(sqrtSigned).divide(s);
        } else {
            final T s = xpo.subtract(sqrtp);
            // x't1 and x't2 are reverted compared to previous solution
            xpt1 = gammap.divide(s);
            xpt2 = s.divide(alphap);
            // Get the corresponding values of y't
            ypt2 = ypo.add(xpo.multiply(sqrtSigned)).divide(alphap);
            ypt1 = xpo.multiply(ypo).subtract(sqrtSigned).divide(s);
        }

        // De-normalize and express the two solutions in 3D
        final FieldVector3D<T> tp1 = section.toSpace(new FieldVector2D<>(ap.multiply(xpt1), bp.multiply(ypt1)));
        final FieldVector3D<T> tp2 = section.toSpace(new FieldVector2D<>(ap.multiply(xpt2), bp.multiply(ypt2)));

        // Return the limb point in the direction of the outside point
        return FieldVector3D.distance(tp1, outside).subtract(FieldVector3D.distance(tp2, outside)).getReal() <= 0 ? tp1 : tp2;

    }

}
