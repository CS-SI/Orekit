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
package org.orekit.forces.gravity;


import java.io.Serializable;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.SphericalCoordinates;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.TideSystemProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

/** This class represents the gravitational field of a celestial body.
 * <p>
 * The algorithm implemented in this class has been designed by S. A. Holmes
 * and W. E. Featherstone from Department of Spatial Sciences, Curtin University
 * of Technology, Perth, Australia. It is described in their 2002 paper: <a
 * href="http://cct.gfy.ku.dk/publ_others/ccta1870.pdf">A unified approach to
 * the Clenshaw summation and the recursive computation of very high degree and
 * order normalised associated Legendre functions</a> (Journal of Geodesy (2002)
 * 76: 279–299).
 * </p>
 * <p>
 * This model directly uses normalized coefficients and stable recursion algorithms
 * so it is more suited to high degree gravity fields than the classical {@link
 * CunninghamAttractionModel Cunningham} or {@link DrozinerAttractionModel Droziner}
 * models which use un-normalized coefficients.
 * </p>
 * <p>
 * Among the different algorithms presented in Holmes and Featherstone paper, this
 * class implements the <em>modified forward row method</em>. All recursion coefficients
 * are precomputed and stored for greater performance. This caching was suggested in the
 * paper but not used due to the large memory requirements. Since 2002, even low end
 * computers and mobile devices do have sufficient memory so this caching has become
 * feasible nowadays.
 * <p>
 * @author Luc Maisonobe
 * @since 6.0
 */

public class HolmesFeatherstoneAttractionModel extends AbstractForceModel implements TideSystemProvider {

    /** Exponent scaling to avoid floating point overflow.
     * <p>The paper uses 10^280, we prefer a power of two to preserve accuracy thanks to
     * {@link FastMath#scalb(double, int)}, so we use 2^930 which has the same order of magnitude.
     */
    private static final int SCALING = 930;

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Drivers for force model parameters. */
    private final ParameterDriver[] parametersDrivers;

    /** Provider for the spherical harmonics. */
    private final NormalizedSphericalHarmonicsProvider provider;

    /** Central attraction coefficient. */
    private double mu;

    /** Rotating body. */
    private final Frame bodyFrame;

    /** Recursion coefficients g<sub>n,m</sub>/√j. */
    private final double[] gnmOj;

    /** Recursion coefficients h<sub>n,m</sub>/√j. */
    private final double[] hnmOj;

    /** Recursion coefficients e<sub>n,m</sub>. */
    private final double[] enm;

    /** Scaled sectorial Pbar<sub>m,m</sub>/u<sup>m</sup> &times; 2<sup>-SCALING</sup>. */
    private final double[] sectorial;

    /** Creates a new instance.
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @since 6.0
     */
    public HolmesFeatherstoneAttractionModel(final Frame centralBodyFrame,
                                             final NormalizedSphericalHarmonicsProvider provider) {

        this.parametersDrivers = new ParameterDriver[1];
        try {
            parametersDrivers[0] = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                       provider.getMu(), MU_SCALE, 0.0, Double.POSITIVE_INFINITY);
            parametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    HolmesFeatherstoneAttractionModel.this.mu = driver.getValue();
                }
            });
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        };

        this.provider  = provider;
        this.mu        = provider.getMu();
        this.bodyFrame = centralBodyFrame;

        // the pre-computed arrays hold coefficients from triangular arrays in a single
        // storing neither diagonal elements (n = m) nor the non-diagonal element n=1, m=0
        final int degree = provider.getMaxDegree();
        final int size = FastMath.max(0, degree * (degree + 1) / 2 - 1);
        gnmOj = new double[size];
        hnmOj = new double[size];
        enm   = new double[size];

        // pre-compute the recursion coefficients corresponding to equations 19 and 22
        // from Holmes and Featherstone paper
        // for cache efficiency, elements are stored in the same order they will be used
        // later on, i.e. from rightmost column to leftmost column
        int index = 0;
        for (int m = degree; m >= 0; --m) {
            final int j = (m == 0) ? 2 : 1;
            for (int n = FastMath.max(2, m + 1); n <= degree; ++n) {
                final double f = (n - m) * (n + m + 1);
                gnmOj[index] = 2 * (m + 1) / FastMath.sqrt(j * f);
                hnmOj[index] = FastMath.sqrt((n + m + 2) * (n - m - 1) / (j * f));
                enm[index]   = FastMath.sqrt(f / j);
                ++index;
            }
        }

        // scaled sectorial terms corresponding to equation 28 in Holmes and Featherstone paper
        sectorial    = new double[degree + 1];
        sectorial[0] = FastMath.scalb(1.0, -SCALING);
        sectorial[1] = FastMath.sqrt(3) * sectorial[0];
        for (int m = 2; m < sectorial.length; ++m) {
            sectorial[m] = FastMath.sqrt((2 * m + 1) / (2.0 * m)) * sectorial[m - 1];
        }

    }

    /** {@inheritDoc} */
    public TideSystem getTideSystem() {
        return provider.getTideSystem();
    }

    /** Compute the value of the gravity field.
     * @param date current date
     * @param position position at which gravity field is desired in body frame
     * @return value of the gravity field (central and non-central parts summed together)
     * @exception OrekitException if position cannot be converted to central body frame
     */
    public double value(final AbsoluteDate date, final Vector3D position)
        throws OrekitException {
        return mu / position.getNorm() + nonCentralPart(date, position);
    }

    /** Compute the non-central part of the gravity field.
     * @param date current date
     * @param position position at which gravity field is desired in body frame
     * @return value of the non-central part of the gravity field
     * @exception OrekitException if position cannot be converted to central body frame
     */
    public double nonCentralPart(final AbsoluteDate date, final Vector3D position)
        throws OrekitException {

        final int degree = provider.getMaxDegree();
        final int order  = provider.getMaxOrder();
        final NormalizedSphericalHarmonics harmonics = provider.onDate(date);

        // allocate the columns for recursion
        double[] pnm0Plus2 = new double[degree + 1];
        double[] pnm0Plus1 = new double[degree + 1];
        double[] pnm0      = new double[degree + 1];

        // compute polar coordinates
        final double x   = position.getX();
        final double y   = position.getY();
        final double z   = position.getZ();
        final double x2  = x * x;
        final double y2  = y * y;
        final double z2  = z * z;
        final double r2  = x2 + y2 + z2;
        final double r   = FastMath.sqrt (r2);
        final double rho = FastMath.sqrt(x2 + y2);
        final double t   = z / r;   // cos(theta), where theta is the polar angle
        final double u   = rho / r; // sin(theta), where theta is the polar angle
        final double tOu = z / rho;

        // compute distance powers
        final double[] aOrN = createDistancePowersArray(provider.getAe() / r);

        // compute longitude cosines/sines
        final double[][] cosSinLambda = createCosSinArrays(position.getX() / rho, position.getY() / rho);

        // outer summation over order
        int    index = 0;
        double value = 0;
        for (int m = degree; m >= 0; --m) {

            // compute tesseral terms without derivatives
            index = computeTesseral(m, degree, index, t, u, tOu,
                                    pnm0Plus2, pnm0Plus1, null, pnm0, null, null);

            if (m <= order) {
                // compute contribution of current order to field (equation 5 of the paper)

                // inner summation over degree, for fixed order
                double sumDegreeS        = 0;
                double sumDegreeC        = 0;
                for (int n = FastMath.max(2, m); n <= degree; ++n) {
                    sumDegreeS += pnm0[n] * aOrN[n] * harmonics.getNormalizedSnm(n, m);
                    sumDegreeC += pnm0[n] * aOrN[n] * harmonics.getNormalizedCnm(n, m);
                }

                // contribution to outer summation over order
                value = value * u + cosSinLambda[1][m] * sumDegreeS + cosSinLambda[0][m] * sumDegreeC;

            }

            // rotate the recursion arrays
            final double[] tmp = pnm0Plus2;
            pnm0Plus2 = pnm0Plus1;
            pnm0Plus1 = pnm0;
            pnm0      = tmp;

        }

        // scale back
        value = FastMath.scalb(value, SCALING);

        // apply the global mu/r factor
        return mu * value / r;

    }

    /** Compute the gradient of the non-central part of the gravity field.
     * @param date current date
     * @param position position at which gravity field is desired in body frame
     * @return gradient of the non-central part of the gravity field
     * @exception OrekitException if position cannot be converted to central body frame
     */
    public double[] gradient(final AbsoluteDate date, final Vector3D position)
        throws OrekitException {

        final int degree = provider.getMaxDegree();
        final int order  = provider.getMaxOrder();
        final NormalizedSphericalHarmonics harmonics = provider.onDate(date);

        // allocate the columns for recursion
        double[] pnm0Plus2  = new double[degree + 1];
        double[] pnm0Plus1  = new double[degree + 1];
        double[] pnm0       = new double[degree + 1];
        final double[] pnm1 = new double[degree + 1];

        // compute polar coordinates
        final double x    = position.getX();
        final double y    = position.getY();
        final double z    = position.getZ();
        final double x2   = x * x;
        final double y2   = y * y;
        final double z2   = z * z;
        final double r2   = x2 + y2 + z2;
        final double r    = FastMath.sqrt (r2);
        final double rho2 = x2 + y2;
        final double rho  = FastMath.sqrt(rho2);
        final double t    = z / r;   // cos(theta), where theta is the polar angle
        final double u    = rho / r; // sin(theta), where theta is the polar angle
        final double tOu  = z / rho;

        // compute distance powers
        final double[] aOrN = createDistancePowersArray(provider.getAe() / r);

        // compute longitude cosines/sines
        final double[][] cosSinLambda = createCosSinArrays(position.getX() / rho, position.getY() / rho);

        // outer summation over order
        int    index = 0;
        double value = 0;
        final double[] gradient = new double[3];
        for (int m = degree; m >= 0; --m) {

            // compute tesseral terms with derivatives
            index = computeTesseral(m, degree, index, t, u, tOu,
                                    pnm0Plus2, pnm0Plus1, null, pnm0, pnm1, null);

            if (m <= order) {
                // compute contribution of current order to field (equation 5 of the paper)

                // inner summation over degree, for fixed order
                double sumDegreeS        = 0;
                double sumDegreeC        = 0;
                double dSumDegreeSdR     = 0;
                double dSumDegreeCdR     = 0;
                double dSumDegreeSdTheta = 0;
                double dSumDegreeCdTheta = 0;
                for (int n = FastMath.max(2, m); n <= degree; ++n) {
                    final double qSnm  = aOrN[n] * harmonics.getNormalizedSnm(n, m);
                    final double qCnm  = aOrN[n] * harmonics.getNormalizedCnm(n, m);
                    final double nOr   = n / r;
                    final double s0    = pnm0[n] * qSnm;
                    final double c0    = pnm0[n] * qCnm;
                    final double s1    = pnm1[n] * qSnm;
                    final double c1    = pnm1[n] * qCnm;
                    sumDegreeS        += s0;
                    sumDegreeC        += c0;
                    dSumDegreeSdR     -= nOr * s0;
                    dSumDegreeCdR     -= nOr * c0;
                    dSumDegreeSdTheta += s1;
                    dSumDegreeCdTheta += c1;
                }

                // contribution to outer summation over order
                // beware that we need to order gradient using the mathematical conventions
                // compliant with the SphericalCoordinates class, so our lambda is its theta
                // (and hence at index 1) and our theta is its phi (and hence at index 2)
                final double sML = cosSinLambda[1][m];
                final double cML = cosSinLambda[0][m];
                value            = value       * u + sML * sumDegreeS        + cML * sumDegreeC;
                gradient[0]      = gradient[0] * u + sML * dSumDegreeSdR     + cML * dSumDegreeCdR;
                gradient[1]      = gradient[1] * u + m * (cML * sumDegreeS - sML * sumDegreeC);
                gradient[2]      = gradient[2] * u + sML * dSumDegreeSdTheta + cML * dSumDegreeCdTheta;

            }

            // rotate the recursion arrays
            final double[] tmp = pnm0Plus2;
            pnm0Plus2 = pnm0Plus1;
            pnm0Plus1 = pnm0;
            pnm0      = tmp;

        }

        // scale back
        value       = FastMath.scalb(value,       SCALING);
        gradient[0] = FastMath.scalb(gradient[0], SCALING);
        gradient[1] = FastMath.scalb(gradient[1], SCALING);
        gradient[2] = FastMath.scalb(gradient[2], SCALING);

        // apply the global mu/r factor
        final double muOr = mu / r;
        value            *= muOr;
        gradient[0]       = muOr * gradient[0] - value / r;
        gradient[1]      *= muOr;
        gradient[2]      *= muOr;

        // convert gradient from spherical to Cartesian
        return new SphericalCoordinates(position).toCartesianGradient(gradient);

    }

    /** Compute the gradient of the non-central part of the gravity field.
     * @param date current date
     * @param position position at which gravity field is desired in body frame
     * @param <T> type of field used
     * @return gradient of the non-central part of the gravity field
     * @exception OrekitException if position cannot be converted to central body frame
     */
    public <T extends RealFieldElement<T>> T[] gradient(final FieldAbsoluteDate<T> date, final FieldVector3D<T> position)
        throws OrekitException {

        final int degree = provider.getMaxDegree();
        final int order  = provider.getMaxOrder();
        final NormalizedSphericalHarmonics harmonics = provider.onDate(date.toAbsoluteDate());
        final T zero = date.getField().getZero();
        // allocate the columns for recursion
        T[] pnm0Plus2  = MathArrays.buildArray(date.getField(), degree + 1);
        T[] pnm0Plus1  = MathArrays.buildArray(date.getField(), degree + 1);
        T[] pnm0       = MathArrays.buildArray(date.getField(), degree + 1);
        final T[] pnm1 = MathArrays.buildArray(date.getField(), degree + 1);

        // compute polar coordinates
        final T x    = position.getX();
        final T y    = position.getY();
        final T z    = position.getZ();
        final T x2   = x.multiply(x);
        final T y2   = y.multiply(y);
        final T z2   = z.multiply(z);
        final T r2   = x2.add(y2).add(z2);
        final T r    = r2.sqrt();
        final T rho2 = x2.add(y2);
        final T rho  = rho2.sqrt();
        final T t    = z.divide(r);   // cos(theta), where theta is the polar angle
        final T u    = rho.divide(r); // sin(theta), where theta is the polar angle
        final T tOu  = z.divide(rho);

        // compute distance powers
        final T[] aOrN = createDistancePowersArray(r.reciprocal().multiply(provider.getAe()));

        // compute longitude cosines/sines
        final T[][] cosSinLambda = createCosSinArrays(rho.reciprocal().multiply(position.getX()), rho.reciprocal().multiply(position.getY()));
        // outer summation over order
        int    index = 0;
        T value = zero;
        final T[] gradient = MathArrays.buildArray(zero.getField(), 3);
        for (int m = degree; m >= 0; --m) {

            // compute tesseral terms with derivatives
            index = computeTesseral(m, degree, index, t, u, tOu,
                                    pnm0Plus2, pnm0Plus1, null, pnm0, pnm1, null);
            if (m <= order) {
                // compute contribution of current order to field (equation 5 of the paper)

                // inner summation over degree, for fixed order
                T sumDegreeS        = zero;
                T sumDegreeC        = zero;
                T dSumDegreeSdR     = zero;
                T dSumDegreeCdR     = zero;
                T dSumDegreeSdTheta = zero;
                T dSumDegreeCdTheta = zero;
                for (int n = FastMath.max(2, m); n <= degree; ++n) {
                    final T qSnm  = aOrN[n].multiply(harmonics.getNormalizedSnm(n, m));
                    final T qCnm  = aOrN[n].multiply(harmonics.getNormalizedCnm(n, m));
                    final T nOr   = r.reciprocal().multiply(n);
                    final T s0    = pnm0[n].multiply(qSnm);
                    final T c0    = pnm0[n].multiply(qCnm);
                    final T s1    = pnm1[n].multiply(qSnm);
                    final T c1    = pnm1[n].multiply(qCnm);
                    sumDegreeS        = sumDegreeS       .add(s0);
                    sumDegreeC        = sumDegreeC       .add(c0);
                    dSumDegreeSdR     = dSumDegreeSdR    .subtract(nOr.multiply(s0));
                    dSumDegreeCdR     = dSumDegreeCdR    .subtract(nOr.multiply(c0));
                    dSumDegreeSdTheta = dSumDegreeSdTheta.add(s1);
                    dSumDegreeCdTheta = dSumDegreeCdTheta.add(c1);
                }

                // contribution to outer summation over order
                // beware that we need to order gradient using the mathematical conventions
                // compliant with the SphericalCoordinates class, so our lambda is its theta
                // (and hence at index 1) and our theta is its phi (and hence at index 2)
                final T sML = cosSinLambda[1][m];
                final T cML = cosSinLambda[0][m];
                value            = value      .multiply(u).add(sML.multiply(sumDegreeS   )).add(cML.multiply(sumDegreeC));
                gradient[0]      = gradient[0].multiply(u).add(sML.multiply(dSumDegreeSdR)).add(cML.multiply(dSumDegreeCdR));
                gradient[1]      = gradient[1].multiply(u).add(cML.multiply(sumDegreeS).subtract(sML.multiply(sumDegreeC)).multiply(m));
                gradient[2]      = gradient[2].multiply(u).add(sML.multiply(dSumDegreeSdTheta)).add(cML.multiply(dSumDegreeCdTheta));
            }
            // rotate the recursion arrays
            final T[] tmp = pnm0Plus2;
            pnm0Plus2 = pnm0Plus1;
            pnm0Plus1 = pnm0;
            pnm0      = tmp;

        }
        // scale back
        value       = value.scalb(SCALING);
        gradient[0] = gradient[0].scalb(SCALING);
        gradient[1] = gradient[1].scalb(SCALING);
        gradient[2] = gradient[2].scalb(SCALING);

        // apply the global mu/r factor
        final T muOr = r.reciprocal().multiply(mu);
        value            = value.multiply(muOr);
        gradient[0]       = muOr.multiply(gradient[0]).subtract(value.divide(r));
        gradient[1]      = gradient[1].multiply(muOr);
        gradient[2]      = gradient[2].multiply(muOr);

        // convert gradient from spherical to Cartesian
        // Cartesian coordinates
        // remaining spherical coordinates
        final T rPos     = position.getNorm();
        // intermediate variables
        final T xPos    = position.getX();
        final T yPos    = position.getY();
        final T zPos    = position.getZ();
        final T rho2Pos = x.multiply(x).add(y.multiply(y));
        final T rhoPos  = rho2.sqrt();
        final T r2Pos   = rho2.add(z.multiply(z));

        final T[][] jacobianPos = MathArrays.buildArray(zero.getField(), 3, 3);

        // row representing the gradient of r
        jacobianPos[0][0] = xPos.divide(rPos);
        jacobianPos[0][1] = yPos.divide(rPos);
        jacobianPos[0][2] = zPos.divide(rPos);

        // row representing the gradient of theta
        jacobianPos[1][0] =  yPos.negate().divide(rho2Pos);
        jacobianPos[1][1] =  xPos.divide(rho2Pos);
        // jacobian[1][2] is already set to 0 at allocation time

        // row representing the gradient of phi
        jacobianPos[2][0] = xPos.multiply(zPos).divide(rhoPos.multiply(r2Pos));
        jacobianPos[2][1] = yPos.multiply(zPos).divide(rhoPos.multiply(r2Pos));
        jacobianPos[2][2] = rhoPos.negate().divide(r2Pos);
        final T[] cartGradPos = MathArrays.buildArray(zero.getField(), 3);
        cartGradPos[0] = gradient[0].multiply(jacobianPos[0][0]).add(gradient[1].multiply(jacobianPos[1][0])).add(gradient[2].multiply(jacobianPos[2][0]));
        cartGradPos[1] = gradient[0].multiply(jacobianPos[0][1]).add(gradient[1].multiply(jacobianPos[1][1])).add(gradient[2].multiply(jacobianPos[2][1]));
        cartGradPos[2] = gradient[0].multiply(jacobianPos[0][2])                                      .add(gradient[2].multiply(jacobianPos[2][2]));
        return cartGradPos;

    }

    /** Compute both the gradient and the hessian of the non-central part of the gravity field.
     * @param date current date
     * @param position position at which gravity field is desired in body frame
     * @return gradient and hessian of the non-central part of the gravity field
     * @exception OrekitException if position cannot be converted to central body frame
     */
    public GradientHessian gradientHessian(final AbsoluteDate date, final Vector3D position)
        throws OrekitException {

        final int degree = provider.getMaxDegree();
        final int order  = provider.getMaxOrder();
        final NormalizedSphericalHarmonics harmonics = provider.onDate(date);

        // allocate the columns for recursion
        double[] pnm0Plus2  = new double[degree + 1];
        double[] pnm0Plus1  = new double[degree + 1];
        double[] pnm0       = new double[degree + 1];
        double[] pnm1Plus1  = new double[degree + 1];
        double[] pnm1       = new double[degree + 1];
        final double[] pnm2 = new double[degree + 1];

        // compute polar coordinates
        final double x    = position.getX();
        final double y    = position.getY();
        final double z    = position.getZ();
        final double x2   = x * x;
        final double y2   = y * y;
        final double z2   = z * z;
        final double r2   = x2 + y2 + z2;
        final double r    = FastMath.sqrt (r2);
        final double rho2 = x2 + y2;
        final double rho  = FastMath.sqrt(rho2);
        final double t    = z / r;   // cos(theta), where theta is the polar angle
        final double u    = rho / r; // sin(theta), where theta is the polar angle
        final double tOu  = z / rho;

        // compute distance powers
        final double[] aOrN = createDistancePowersArray(provider.getAe() / r);

        // compute longitude cosines/sines
        final double[][] cosSinLambda = createCosSinArrays(position.getX() / rho, position.getY() / rho);

        // outer summation over order
        int    index = 0;
        double value = 0;
        final double[]   gradient = new double[3];
        final double[][] hessian  = new double[3][3];
        for (int m = degree; m >= 0; --m) {

            // compute tesseral terms
            index = computeTesseral(m, degree, index, t, u, tOu,
                                    pnm0Plus2, pnm0Plus1, pnm1Plus1, pnm0, pnm1, pnm2);

            if (m <= order) {
                // compute contribution of current order to field (equation 5 of the paper)

                // inner summation over degree, for fixed order
                double sumDegreeS               = 0;
                double sumDegreeC               = 0;
                double dSumDegreeSdR            = 0;
                double dSumDegreeCdR            = 0;
                double dSumDegreeSdTheta        = 0;
                double dSumDegreeCdTheta        = 0;
                double d2SumDegreeSdRdR         = 0;
                double d2SumDegreeSdRdTheta     = 0;
                double d2SumDegreeSdThetadTheta = 0;
                double d2SumDegreeCdRdR         = 0;
                double d2SumDegreeCdRdTheta     = 0;
                double d2SumDegreeCdThetadTheta = 0;
                for (int n = FastMath.max(2, m); n <= degree; ++n) {
                    final double qSnm         = aOrN[n] * harmonics.getNormalizedSnm(n, m);
                    final double qCnm         = aOrN[n] * harmonics.getNormalizedCnm(n, m);
                    final double nOr          = n / r;
                    final double nnP1Or2      = nOr * (n + 1) / r;
                    final double s0           = pnm0[n] * qSnm;
                    final double c0           = pnm0[n] * qCnm;
                    final double s1           = pnm1[n] * qSnm;
                    final double c1           = pnm1[n] * qCnm;
                    final double s2           = pnm2[n] * qSnm;
                    final double c2           = pnm2[n] * qCnm;
                    sumDegreeS               += s0;
                    sumDegreeC               += c0;
                    dSumDegreeSdR            -= nOr * s0;
                    dSumDegreeCdR            -= nOr * c0;
                    dSumDegreeSdTheta        += s1;
                    dSumDegreeCdTheta        += c1;
                    d2SumDegreeSdRdR         += nnP1Or2 * s0;
                    d2SumDegreeSdRdTheta     -= nOr * s1;
                    d2SumDegreeSdThetadTheta += s2;
                    d2SumDegreeCdRdR         += nnP1Or2 * c0;
                    d2SumDegreeCdRdTheta     -= nOr * c1;
                    d2SumDegreeCdThetadTheta += c2;
                }

                // contribution to outer summation over order
                final double sML = cosSinLambda[1][m];
                final double cML = cosSinLambda[0][m];
                value            = value         * u + sML * sumDegreeS + cML * sumDegreeC;
                gradient[0]      = gradient[0]   * u + sML * dSumDegreeSdR + cML * dSumDegreeCdR;
                gradient[1]      = gradient[1]   * u + m * (cML * sumDegreeS - sML * sumDegreeC);
                gradient[2]      = gradient[2]   * u + sML * dSumDegreeSdTheta + cML * dSumDegreeCdTheta;
                hessian[0][0]    = hessian[0][0] * u + sML * d2SumDegreeSdRdR + cML * d2SumDegreeCdRdR;
                hessian[1][0]    = hessian[1][0] * u + m * (cML * dSumDegreeSdR - sML * dSumDegreeCdR);
                hessian[2][0]    = hessian[2][0] * u + sML * d2SumDegreeSdRdTheta + cML * d2SumDegreeCdRdTheta;
                hessian[1][1]    = hessian[1][1] * u - m * m * (sML * sumDegreeS + cML * sumDegreeC);
                hessian[2][1]    = hessian[2][1] * u + m * (cML * dSumDegreeSdTheta - sML * dSumDegreeCdTheta);
                hessian[2][2]    = hessian[2][2] * u + sML * d2SumDegreeSdThetadTheta + cML * d2SumDegreeCdThetadTheta;

            }

            // rotate the recursion arrays
            final double[] tmp0 = pnm0Plus2;
            pnm0Plus2 = pnm0Plus1;
            pnm0Plus1 = pnm0;
            pnm0      = tmp0;
            final double[] tmp1 = pnm1Plus1;
            pnm1Plus1 = pnm1;
            pnm1      = tmp1;

        }

        // scale back
        value = FastMath.scalb(value, SCALING);
        for (int i = 0; i < 3; ++i) {
            gradient[i] = FastMath.scalb(gradient[i], SCALING);
            for (int j = 0; j <= i; ++j) {
                hessian[i][j] = FastMath.scalb(hessian[i][j], SCALING);
            }
        }


        // apply the global mu/r factor
        final double muOr = mu / r;
        value         *= muOr;
        gradient[0]    = muOr * gradient[0] - value / r;
        gradient[1]   *= muOr;
        gradient[2]   *= muOr;
        hessian[0][0]  = muOr * hessian[0][0] - 2 * gradient[0] / r;
        hessian[1][0]  = muOr * hessian[1][0] -     gradient[1] / r;
        hessian[2][0]  = muOr * hessian[2][0] -     gradient[2] / r;
        hessian[1][1] *= muOr;
        hessian[2][1] *= muOr;
        hessian[2][2] *= muOr;

        // convert gradient and Hessian from spherical to Cartesian
        final SphericalCoordinates sc = new SphericalCoordinates(position);
        return new GradientHessian(sc.toCartesianGradient(gradient),
                                   sc.toCartesianHessian(hessian, gradient));


    }

    /** Container for gradient and Hessian. */
    public static class GradientHessian implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20130219L;

        /** Gradient. */
        private final double[] gradient;

        /** Hessian. */
        private final double[][] hessian;

        /** Simple constructor.
         * <p>
         * A reference to the arrays is stored, they are <strong>not</strong> cloned.
         * </p>
         * @param gradient gradient
         * @param hessian hessian
         */
        public GradientHessian(final double[] gradient, final double[][] hessian) {
            this.gradient = gradient;
            this.hessian  = hessian;
        }

        /** Get a reference to the gradient.
         * @return gradient (a reference to the internal array is returned)
         */
        public double[] getGradient() {
            return gradient;
        }

        /** Get a reference to the Hessian.
         * @return Hessian (a reference to the internal array is returned)
         */
        public double[][] getHessian() {
            return hessian;
        }

    }

    /** Compute a/r powers array.
     * @param aOr a/r
     * @return array containing (a/r)<sup>n</sup>
     */
    private double[] createDistancePowersArray(final double aOr) {

        // initialize array
        final double[] aOrN = new double[provider.getMaxDegree() + 1];
        aOrN[0] = 1;
        aOrN[1] = aOr;

        // fill up array
        for (int n = 2; n < aOrN.length; ++n) {
            final int p = n / 2;
            final int q = n - p;
            aOrN[n] = aOrN[p] * aOrN[q];
        }

        return aOrN;

    }
    /** Compute a/r powers array.
     * @param aOr a/r
     * @param <T> type of field used
     * @return array containing (a/r)<sup>n</sup>
     */
    private <T extends RealFieldElement<T>> T[] createDistancePowersArray(final T aOr) {

        // initialize array
        final T[] aOrN = MathArrays.buildArray(aOr.getField(), provider.getMaxDegree() + 1);
        aOrN[0] = aOr.getField().getOne();
        aOrN[1] = aOr;

        // fill up array
        for (int n = 2; n < aOrN.length; ++n) {
            final int p = n / 2;
            final int q = n - p;
            aOrN[n] = aOrN[p].multiply(aOrN[q]);
        }

        return aOrN;

    }

    /** Compute longitude cosines and sines.
     * @param cosLambda cos(λ)
     * @param sinLambda sin(λ)
     * @return array containing cos(m &times; λ) in row 0
     * and sin(m &times; λ) in row 1
     */
    private double[][] createCosSinArrays(final double cosLambda, final double sinLambda) {

        // initialize arrays
        final double[][] cosSin = new double[2][provider.getMaxOrder() + 1];
        cosSin[0][0] = 1;
        cosSin[1][0] = 0;
        if (provider.getMaxOrder() > 0) {
            cosSin[0][1] = cosLambda;
            cosSin[1][1] = sinLambda;

            // fill up array
            for (int m = 2; m < cosSin[0].length; ++m) {

                // m * lambda is split as p * lambda + q * lambda, trying to avoid
                // p or q being much larger than the other. This reduces the number of
                // intermediate results reused to compute each value, and hence should limit
                // as much as possible roundoff error accumulation
                // (this does not change the number of floating point operations)
                final int p = m / 2;
                final int q = m - p;

                cosSin[0][m] = cosSin[0][p] * cosSin[0][q] - cosSin[1][p] * cosSin[1][q];
                cosSin[1][m] = cosSin[1][p] * cosSin[0][q] + cosSin[0][p] * cosSin[1][q];
            }
        }

        return cosSin;

    }

    /** Compute longitude cosines and sines.
     * @param cosLambda cos(λ)
     * @param sinLambda sin(λ)
     * @param <T> type of field used
     * @return array containing cos(m &times; λ) in row 0
     * and sin(m &times; λ) in row 1
     */
    private <T extends RealFieldElement<T>> T[][] createCosSinArrays(final T cosLambda, final T sinLambda) {

        final T one = cosLambda.getField().getOne();
        final T zero = cosLambda.getField().getZero();
        // initialize arrays
        final T[][] cosSin = MathArrays.buildArray(one.getField(), 2, provider.getMaxOrder() + 1);
        cosSin[0][0] = one;
        cosSin[1][0] = zero;
        if (provider.getMaxOrder() > 0) {
            cosSin[0][1] = cosLambda;
            cosSin[1][1] = sinLambda;

            // fill up array
            for (int m = 2; m < cosSin[0].length; ++m) {

                // m * lambda is split as p * lambda + q * lambda, trying to avoid
                // p or q being much larger than the other. This reduces the number of
                // intermediate results reused to compute each value, and hence should limit
                // as much as possible roundoff error accumulation
                // (this does not change the number of floating point operations)
                final int p = m / 2;
                final int q = m - p;

                cosSin[0][m] = cosSin[0][p].multiply(cosSin[0][q]).subtract(cosSin[1][p].multiply(cosSin[1][q]));
                cosSin[1][m] = cosSin[1][p].multiply(cosSin[0][q]).add(cosSin[0][p].multiply(cosSin[1][q]));

            }
        }

        return cosSin;

    }

    /** Compute one order of tesseral terms.
     * <p>
     * This corresponds to equations 27 and 30 of the paper.
     * </p>
     * @param m current order
     * @param degree max degree
     * @param index index in the flattened array
     * @param t cos(θ), where θ is the polar angle
     * @param u sin(θ), where θ is the polar angle
     * @param tOu t/u
     * @param pnm0Plus2 array containing scaled P<sub>n,m+2</sub>/u<sup>m+2</sup>
     * @param pnm0Plus1 array containing scaled P<sub>n,m+1</sub>/u<sup>m+1</sup>
     * @param pnm1Plus1 array containing scaled dP<sub>n,m+1</sub>/u<sup>m+1</sup>
     * (may be null if second derivatives are not needed)
     * @param pnm0 array to fill with scaled P<sub>n,m</sub>/u<sup>m</sup>
     * @param pnm1 array to fill with scaled dP<sub>n,m</sub>/u<sup>m</sup>
     * (may be null if first derivatives are not needed)
     * @param pnm2 array to fill with scaled d²P<sub>n,m</sub>/u<sup>m</sup>
     * (may be null if second derivatives are not needed)
     * @return new value for index
     */
    private int computeTesseral(final int m, final int degree, final int index,
                                final double t, final double u, final double tOu,
                                final double[] pnm0Plus2, final double[] pnm0Plus1, final double[] pnm1Plus1,
                                final double[] pnm0, final double[] pnm1, final double[] pnm2) {

        final double u2 = u * u;

        // initialize recursion from sectorial terms
        int n = FastMath.max(2, m);
        if (n == m) {
            pnm0[n] = sectorial[n];
            ++n;
        }

        // compute tesseral values
        int localIndex = index;
        while (n <= degree) {

            // value (equation 27 of the paper)
            pnm0[n] = gnmOj[localIndex] * t * pnm0Plus1[n] - hnmOj[localIndex] * u2 * pnm0Plus2[n];

            ++localIndex;
            ++n;

        }

        if (pnm1 != null) {

            // initialize recursion from sectorial terms
            n = FastMath.max(2, m);
            if (n == m) {
                pnm1[n] = m * tOu * pnm0[n];
                ++n;
            }

            // compute tesseral values and derivatives with respect to polar angle
            localIndex = index;
            while (n <= degree) {

                // first derivative (equation 30 of the paper)
                pnm1[n] = m * tOu * pnm0[n] - enm[localIndex] * u * pnm0Plus1[n];

                ++localIndex;
                ++n;

            }

            if (pnm2 != null) {

                // initialize recursion from sectorial terms
                n = FastMath.max(2, m);
                if (n == m) {
                    pnm2[n] = m * (tOu * pnm1[n] - pnm0[n] / u2);
                    ++n;
                }

                // compute tesseral values and derivatives with respect to polar angle
                localIndex = index;
                while (n <= degree) {

                    // second derivative (differential of equation 30 with respect to theta)
                    pnm2[n] = m * (tOu * pnm1[n] - pnm0[n] / u2) - enm[localIndex] * u * pnm1Plus1[n];

                    ++localIndex;
                    ++n;

                }

            }

        }

        return localIndex;

    }

    /** Compute one order of tesseral terms.
     * <p>
     * This corresponds to equations 27 and 30 of the paper.
     * </p>
     * @param m current order
     * @param degree max degree
     * @param index index in the flattened array
     * @param t cos(θ), where θ is the polar angle
     * @param u sin(θ), where θ is the polar angle
     * @param tOu t/u
     * @param pnm0Plus2 array containing scaled P<sub>n,m+2</sub>/u<sup>m+2</sup>
     * @param pnm0Plus1 array containing scaled P<sub>n,m+1</sub>/u<sup>m+1</sup>
     * @param pnm1Plus1 array containing scaled dP<sub>n,m+1</sub>/u<sup>m+1</sup>
     * (may be null if second derivatives are not needed)
     * @param pnm0 array to fill with scaled P<sub>n,m</sub>/u<sup>m</sup>
     * @param pnm1 array to fill with scaled dP<sub>n,m</sub>/u<sup>m</sup>
     * (may be null if first derivatives are not needed)
     * @param pnm2 array to fill with scaled d²P<sub>n,m</sub>/u<sup>m</sup>
     * (may be null if second derivatives are not needed)
     * @param <T> instance of field element
     * @return new value for index
     */
    private <T extends RealFieldElement<T>> int computeTesseral(final int m, final int degree, final int index,
                                final T t, final T u, final T tOu,
                                final T[] pnm0Plus2, final T[] pnm0Plus1, final T[] pnm1Plus1,
                                final T[] pnm0, final T[] pnm1, final T[] pnm2) {

        final T u2 = u.multiply(u);
        final T zero = u.getField().getZero();
        // initialize recursion from sectorial terms
        int n = FastMath.max(2, m);
        if (n == m) {
            pnm0[n] = zero.add(sectorial[n]);
            ++n;
        }

        // compute tesseral values
        int localIndex = index;
        while (n <= degree) {

            // value (equation 27 of the paper)
            pnm0[n] = t.multiply(gnmOj[localIndex]).multiply(pnm0Plus1[n]).subtract(u2.multiply(pnm0Plus2[n]).multiply(hnmOj[localIndex]));
            ++localIndex;
            ++n;

        }
        if (pnm1 != null) {

            // initialize recursion from sectorial terms
            n = FastMath.max(2, m);
            if (n == m) {
                pnm1[n] = tOu.multiply(m).multiply(pnm0[n]);
                ++n;
            }

            // compute tesseral values and derivatives with respect to polar angle
            localIndex = index;
            while (n <= degree) {

                // first derivative (equation 30 of the paper)
                pnm1[n] = tOu.multiply(m).multiply(pnm0[n]).subtract(u.multiply(enm[localIndex]).multiply(pnm0Plus1[n]));

                ++localIndex;
                ++n;

            }

            if (pnm2 != null) {

                // initialize recursion from sectorial terms
                n = FastMath.max(2, m);
                if (n == m) {
                    pnm2[n] =   tOu.multiply(pnm1[n]).subtract(pnm0[n].divide(u2)).multiply(m);
                    ++n;
                }

                // compute tesseral values and derivatives with respect to polar angle
                localIndex = index;
                while (n <= degree) {

                    // second derivative (differential of equation 30 with respect to theta)
                    pnm2[n] = tOu.multiply(pnm1[n]).subtract(pnm0[n].divide(u2)).multiply(m).subtract(u.multiply(pnm1Plus1[n]).multiply(enm[localIndex]));
                    ++localIndex;
                    ++n;

                }

            }

        }
        return localIndex;

    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        // get the position in body frame
        final AbsoluteDate date       = s.getDate();
        final Transform fromBodyFrame = bodyFrame.getTransformTo(s.getFrame(), date);
        final Transform toBodyFrame   = fromBodyFrame.getInverse();
        final Vector3D position       = toBodyFrame.transformPosition(s.getPVCoordinates().getPosition());

        // gradient of the non-central part of the gravity field
        final Vector3D gInertial = fromBodyFrame.transformVector(new Vector3D(gradient(date, position)));
        adder.addXYZAcceleration(gInertial.getX(), gInertial.getY(), gInertial.getZ());
    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> void addContribution(final FieldSpacecraftState<T> s, final FieldTimeDerivativesEquations<T> adder)
        throws OrekitException {

        // get the position in body frame
        final FieldAbsoluteDate<T> date       = s.getDate();
        final Transform fromBodyFrame = bodyFrame.getTransformTo(s.getFrame(), date.toAbsoluteDate());
        final Transform toBodyFrame   = fromBodyFrame.getInverse();
        final FieldVector3D<T> position       = toBodyFrame.transformPosition(s.getPVCoordinates().getPosition());

        // gradient of the non-central part of the gravity field
        final FieldVector3D<T> gInertial = fromBodyFrame.transformVector(new FieldVector3D<T>(gradient(date, position)));
        adder.addXYZAcceleration(gInertial.getX(), gInertial.getY(), gInertial.getZ());
    }

    /** {@inheritDoc} */
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    @Override
    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position, final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass)
        throws OrekitException {

        // get the position in body frame
        final Transform fromBodyFrame = bodyFrame.getTransformTo(frame, date);
        final Transform toBodyFrame   = fromBodyFrame.getInverse();
        final Vector3D positionBody   = toBodyFrame.transformPosition(position.toVector3D());

        // compute gradient and Hessian
        final GradientHessian gh   = gradientHessian(date, positionBody);

        // gradient of the non-central part of the gravity field
        final double[] gInertial = fromBodyFrame.transformVector(new Vector3D(gh.getGradient())).toArray();

        // Hessian of the non-central part of the gravity field
        final RealMatrix hBody     = new Array2DRowRealMatrix(gh.getHessian(), false);
        final RealMatrix rot       = new Array2DRowRealMatrix(toBodyFrame.getRotation().getMatrix());
        final RealMatrix hInertial = rot.transpose().multiply(hBody).multiply(rot);

        // distribute all partial derivatives in a compact acceleration vector
        final int parameters       = mass.getFreeParameters();
        final int order            = mass.getOrder();
        final double[] derivatives = new double[1 + parameters];
        final DerivativeStructure[] accDer = new DerivativeStructure[3];
        for (int i = 0; i < 3; ++i) {

            // first element is value of acceleration (i.e. gradient of field)
            derivatives[0] = gInertial[i];

            // next three elements are one row of the Jacobian of acceleration (i.e. Hessian of field)
            derivatives[1] = hInertial.getEntry(i, 0);
            derivatives[2] = hInertial.getEntry(i, 1);
            derivatives[3] = hInertial.getEntry(i, 2);

            // next elements (three or four depending on mass being used or not) are left as 0

            accDer[i] = new DerivativeStructure(parameters, order, derivatives);

        }

        return new FieldVector3D<DerivativeStructure>(accDer);

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException, IllegalArgumentException {

        complainIfNotSupported(paramName);

        // get the position in body frame
        final AbsoluteDate date       = s.getDate();
        final Transform fromBodyFrame = bodyFrame.getTransformTo(s.getFrame(), date);
        final Transform toBodyFrame   = fromBodyFrame.getInverse();
        final Vector3D position       = toBodyFrame.transformPosition(s.getPVCoordinates().getPosition());

        // gradient of the non-central part of the gravity field
        final Vector3D gInertial = fromBodyFrame.transformVector(new Vector3D(gradient(date, position)));

        return new FieldVector3D<DerivativeStructure>(new DerivativeStructure(1, 1, gInertial.getX(), gInertial.getX() / mu),
                                                      new DerivativeStructure(1, 1, gInertial.getY(), gInertial.getY() / mu),
                                                      new DerivativeStructure(1, 1, gInertial.getZ(), gInertial.getZ() / mu));

    }

    /** {@inheritDoc} */
    public ParameterDriver[] getParametersDrivers() {
        return parametersDrivers.clone();
    }

}
