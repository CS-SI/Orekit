/* Copyright 2002-2016 CS Systèmes d'Informatiupj
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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.TideSystemProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.Jacobianizer;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

/** This class represents the gravitational field of a celestial body.
 * <p>The algorithm implemented in this class has been designed by
 * Andrzej Droziner (Institute of Mathematical Machines, Warsaw) in
 * his 1976 paper: <em>An algorithm for recurrent calculation of gravitational
 * acceleration</em> (artificial satellites, Vol. 12, No 2, June 1977).</p>
 * <p>
 * Note that this class can often not be used for high degrees (say
 * above 90) as most modern gravity fields are provided as normalized
 * coefficients and the un-normalization process to convert these
 * coefficients underflows at degree and order 89. This class also
 * does not provide analytical partial derivatives (it uses finite differences
 * to compute them) and is much slower than {@link HolmesFeatherstoneAttractionModel}
 * (even when no derivatives are computed). For all these reasons,
 * it is recommended to use the {@link HolmesFeatherstoneAttractionModel
 * Holmes-Featherstone model} rather than this class.
 * </p>
 * <p>
 * This class uses finite differences to compute derivatives and the steps for
 * finite differences are initialized in the {@link
 * #DrozinerAttractionModel(Frame, UnnormalizedSphericalHarmonicsProvider,
 * double) constructor}
 * </p>
 *
 * @see HolmesFeatherstoneAttractionModel
 *
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */

public class DrozinerAttractionModel extends AbstractForceModel implements TideSystemProvider {

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
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Central body attraction coefficient (m³/s²). */
    private double mu;

    /** Rotating body. */
    private final Frame centralBodyFrame;

    /** Helper class computing acceleration derivatives. */
    private Jacobianizer jacobianizer;

    /** Creates a new instance.
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @param hPosition step used for finite difference computation
     * with respect to spacecraft position (m)
     */
    public DrozinerAttractionModel(final Frame centralBodyFrame,
                                   final UnnormalizedSphericalHarmonicsProvider provider,
                                   final double hPosition) {

        this.parametersDrivers = new ParameterDriver[1];
        try {
            parametersDrivers[0] = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                       provider.getMu(), MU_SCALE, 0.0, Double.POSITIVE_INFINITY);
            parametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    DrozinerAttractionModel.this.mu = driver.getValue();
                }
            });
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        };

        this.provider         = provider;
        this.mu               = provider.getMu();
        this.centralBodyFrame = centralBodyFrame;
        this.jacobianizer     = new Jacobianizer(this, mu, hPosition);

    }

    /** {@inheritDoc} */
    public TideSystem getTideSystem() {
        return provider.getTideSystem();
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {
        // Get the position in body frame
        final AbsoluteDate date = s.getDate();
        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        final Transform bodyToInertial = centralBodyFrame.getTransformTo(s.getFrame(), date);
        final Vector3D posInBody =
            bodyToInertial.getInverse().transformVector(s.getPVCoordinates().getPosition());
        final double xBody = posInBody.getX();
        final double yBody = posInBody.getY();
        final double zBody = posInBody.getZ();

        // Computation of intermediate variables
        final double r12 = xBody * xBody + yBody * yBody;
        final double r1 = FastMath.sqrt(r12);
        if (r1 <= 10e-2) {
            throw new OrekitException(OrekitMessages.POLAR_TRAJECTORY, r1);
        }
        final double r2 = r12 + zBody * zBody;
        final double r  = FastMath.sqrt(r2);
        final double equatorialRadius = provider.getAe();
        if (r <= equatorialRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
        }
        final double r3    = r2  * r;
        final double aeOnr = equatorialRadius / r;
        final double zOnr  = zBody / r;
        final double r1Onr = r1 / r;

        // Definition of the first acceleration terms
        final double mMuOnr3  = -mu / r3;
        final double xDotDotk = xBody * mMuOnr3;
        final double yDotDotk = yBody * mMuOnr3;

        // Zonal part of acceleration
        double sumA = 0.0;
        double sumB = 0.0;
        double bk1 = zOnr;
        double bk0 = aeOnr * (3 * bk1 * bk1 - 1.0);
        double jk = -harmonics.getUnnormalizedCnm(1, 0);

        // first zonal term
        sumA += jk * (2 * aeOnr * bk1 - zOnr * bk0);
        sumB += jk * bk0;

        // other terms
        for (int k = 2; k <= provider.getMaxDegree(); k++) {
            final double bk2 = bk1;
            bk1 = bk0;
            final double p = (1.0 + k) / k;
            bk0 = aeOnr * ((1 + p) * zOnr * bk1 - (k * aeOnr * bk2) / (k - 1));
            final double ak0 = p * aeOnr * bk1 - zOnr * bk0;
            jk = -harmonics.getUnnormalizedCnm(k, 0);
            sumA += jk * ak0;
            sumB += jk * bk0;
        }

        // calculate the acceleration
        final double p = -sumA / (r1Onr * r1Onr);
        double aX = xDotDotk * p;
        double aY = yDotDotk * p;
        double aZ = mu * sumB / r2;


        // Tessereal-sectorial part of acceleration
        if (provider.getMaxOrder() > 0) {
            // latitude and longitude in body frame
            final double cosL = xBody / r1;
            final double sinL = yBody / r1;
            // intermediate variables
            double betaKminus1 = aeOnr;

            double cosjm1L = cosL;
            double sinjm1L = sinL;

            double sinjL = sinL;
            double cosjL = cosL;
            double betaK = 0;
            double Bkj = 0.0;
            double Bkm1j = 3 * betaKminus1 * zOnr * r1Onr;
            double Bkm2j = 0;
            double Bkminus1kminus1 = Bkm1j;

            // first terms
            final double c11 = harmonics.getUnnormalizedCnm(1, 1);
            final double s11 = harmonics.getUnnormalizedSnm(1, 1);
            double Gkj  = c11 * cosL + s11 * sinL;
            double Hkj  = c11 * sinL - s11 * cosL;
            double Akj  = 2 * r1Onr * betaKminus1 - zOnr * Bkminus1kminus1;
            double Dkj  = (Akj + zOnr * Bkminus1kminus1) * 0.5;
            double sum1 = Akj * Gkj;
            double sum2 = Bkminus1kminus1 * Gkj;
            double sum3 = Dkj * Hkj;

            // the other terms
            for (int j = 1; j <= provider.getMaxOrder(); ++j) {

                double innerSum1 = 0.0;
                double innerSum2 = 0.0;
                double innerSum3 = 0.0;

                for (int k = FastMath.max(2, j); k <= provider.getMaxDegree(); ++k) {

                    final double ckj = harmonics.getUnnormalizedCnm(k, j);
                    final double skj = harmonics.getUnnormalizedSnm(k, j);
                    Gkj = ckj * cosjL + skj * sinjL;
                    Hkj = ckj * sinjL - skj * cosjL;

                    if (j <= (k - 2)) {
                        Bkj = aeOnr * (zOnr * Bkm1j * (2.0 * k + 1.0) / (k - j) -
                                aeOnr * Bkm2j * (k + j) / (k - 1 - j));
                        Akj = aeOnr * Bkm1j * (k + 1.0) / (k - j) - zOnr * Bkj;
                    } else if (j == (k - 1)) {
                        betaK =  aeOnr * (2.0 * k - 1.0) * r1Onr * betaKminus1;
                        Bkj = aeOnr * (2.0 * k + 1.0) * zOnr * Bkm1j - betaK;
                        Akj = aeOnr *  (k + 1.0) * Bkm1j - zOnr * Bkj;
                        betaKminus1 = betaK;
                    } else if (j == k) {
                        Bkj = (2 * k + 1) * aeOnr * r1Onr * Bkminus1kminus1;
                        Akj = (k + 1) * r1Onr * betaK - zOnr * Bkj;
                        Bkminus1kminus1 = Bkj;
                    }

                    Dkj =  (Akj + zOnr * Bkj) * j / (k + 1.0);

                    Bkm2j = Bkm1j;
                    Bkm1j = Bkj;

                    innerSum1 += Akj * Gkj;
                    innerSum2 += Bkj * Gkj;
                    innerSum3 += Dkj * Hkj;
                }

                sum1 += innerSum1;
                sum2 += innerSum2;
                sum3 += innerSum3;

                sinjL = sinjm1L * cosL + cosjm1L * sinL;
                cosjL = cosjm1L * cosL - sinjm1L * sinL;
                sinjm1L = sinjL;
                cosjm1L = cosjL;
            }

            // compute the acceleration
            final double r2Onr12 = r2 / (r1 * r1);
            final double p1 = r2Onr12 * xDotDotk;
            final double p2 = r2Onr12 * yDotDotk;
            aX += p1 * sum1 - p2 * sum3;
            aY += p2 * sum1 + p1 * sum3;
            aZ -= mu * sum2 / r2;

        }
        // provide the perturbing acceleration to the derivatives adder in inertial frame
        final Vector3D accInInert =
            bodyToInertial.transformVector(new Vector3D(aX, aY, aZ));
        adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final  Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass)
        throws OrekitException {
        return jacobianizer.accelerationDerivatives(date, frame, position, velocity, rotation, mass);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {
        return jacobianizer.accelerationDerivatives(s, paramName);
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
    public ParameterDriver[] getParametersDrivers() {
        return parametersDrivers.clone();
    }

    @Override
    public <T extends RealFieldElement<T>> void
        addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder)
            throws OrekitException {

     // Get the position in body frame
        final FieldAbsoluteDate<T> date = s.getDate();
        final Field<T> field = date.getField();
        final T zero = field.getZero();
        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date.toAbsoluteDate());
        final Transform bodyToInertial = centralBodyFrame.getTransformTo(s.getFrame(), date.toAbsoluteDate());
        final FieldVector3D<T> posInBody =
            bodyToInertial.getInverse().transformVector(s.getPVCoordinates().getPosition());
        final T xBody = posInBody.getX();
        final T yBody = posInBody.getY();
        final T zBody = posInBody.getZ();

        // Computation of intermediate variables
        final T r12 = xBody.multiply(xBody).add(yBody.multiply(yBody));
        final T r1 = r12.sqrt();
        if (r1.getReal() <= 10e-2) {
            throw new OrekitException(OrekitMessages.POLAR_TRAJECTORY, r1);
        }
        final T r2 = r12.add(zBody.multiply(zBody));
        final T r  = r2.sqrt();
        final double equatorialRadius = provider.getAe();
        if (r.getReal() <= equatorialRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
        }
        final T r3    = r2.multiply(r);
        final T aeOnr = r.reciprocal().multiply(equatorialRadius);
        final T zOnr  = zBody.divide(r);
        final T r1Onr = r1.divide(r);

        // Definition of the first acceleration terms
        final T mMuOnr3  = r3.reciprocal().multiply(-mu);
        final T xDotDotk = xBody.multiply(mMuOnr3);
        final T yDotDotk = yBody.multiply(mMuOnr3);

        // Zonal part of acceleration
        T sumA = zero;
        T sumB = zero;
        T bk1 = zOnr;
        T bk0 = aeOnr.multiply(bk1.multiply(3).multiply(bk1).subtract(1.0));
        double jk = -harmonics.getUnnormalizedCnm(1, 0);

        // first zonal term
        sumA = sumA.add(aeOnr.multiply(2).multiply(bk1).subtract(zOnr.multiply(bk0)).multiply(jk));
        sumB = sumB.add(bk0.multiply(jk));

        // other terms
        for (int k = 2; k <= provider.getMaxDegree(); k++) {
            final T bk2 = bk1;
            bk1 = bk0;
            final double p = (1.0 + k) / k;
            bk0 = aeOnr.multiply(zOnr.multiply(1 + p).multiply(bk1).subtract(aeOnr.multiply(k).multiply(bk2).divide(k - 1)));
            final T ak0 = aeOnr.multiply(p).multiply(bk1).subtract(zOnr.multiply(bk0));
            jk = -harmonics.getUnnormalizedCnm(k, 0);
            sumA = sumA.add(ak0.multiply(jk));
            sumB = sumB.add(bk0.multiply(jk));
        }

        // calculate the acceleration
        final T p = sumA.negate().divide(r1Onr.multiply(r1Onr));
        T aX = xDotDotk.multiply(p);
        T aY = yDotDotk.multiply(p);
        T aZ = sumB.multiply(mu).divide(r2);


        // Tessereal-sectorial part of acceleration
        if (provider.getMaxOrder() > 0) {
            // latitude and longitude in body frame
            final T cosL = xBody.divide(r1);
            final T sinL = yBody.divide(r1);
            // intermediate variables
            T betaKminus1 = aeOnr;

            T cosjm1L = cosL;
            T sinjm1L = sinL;

            T sinjL = sinL;
            T cosjL = cosL;
            T betaK = zero;
            T Bkj = zero;
            T Bkm1j = betaKminus1.multiply(3).multiply(zOnr).multiply(r1Onr);
            T Bkm2j = zero;
            T Bkminus1kminus1 = Bkm1j;

            // first terms
            final double c11 = harmonics.getUnnormalizedCnm(1, 1);
            final double s11 = harmonics.getUnnormalizedSnm(1, 1);
            T Gkj  = cosL.multiply(c11).add(sinL.multiply(s11));
            T Hkj  = sinL.multiply(c11).add(cosL.multiply(s11));
            T Akj  = r1Onr.multiply(2).multiply(betaKminus1).subtract(zOnr.multiply(Bkminus1kminus1));
            T Dkj  = Akj.add(zOnr.multiply(Bkminus1kminus1)).multiply(0.5);
            T sum1 = Akj.multiply(Gkj);
            T sum2 = Bkminus1kminus1.multiply(Gkj);
            T sum3 = Dkj.multiply(Hkj);

            // the other terms
            for (int j = 1; j <= provider.getMaxOrder(); ++j) {

                T innerSum1 = zero;
                T innerSum2 = zero;
                T innerSum3 = zero;

                for (int k = FastMath.max(2, j); k <= provider.getMaxDegree(); ++k) {

                    final double ckj = harmonics.getUnnormalizedCnm(k, j);
                    final double skj = harmonics.getUnnormalizedSnm(k, j);
                    Gkj = cosjL.multiply(ckj).add(sinjL.multiply(skj));
                    Hkj = sinjL.multiply(ckj).subtract(cosjL.multiply(skj));

                    if (j <= (k - 2)) {
                        Bkj = aeOnr.multiply(zOnr.multiply(Bkm1j).multiply((2.0 * k + 1.0) / (k - j)).subtract(
                                aeOnr.multiply(Bkm2j).multiply((k + j) / (k - 1 - j))));
                        Akj = aeOnr.multiply(Bkm1j).multiply((k + 1.0) / (k - j)).subtract(zOnr.multiply(Bkj));
                    } else if (j == (k - 1)) {
                        betaK =  aeOnr.multiply(2.0 * k - 1.0).multiply(r1Onr).multiply(betaKminus1);
                        Bkj = aeOnr.multiply(2.0 * k + 1.0).multiply(zOnr).multiply(Bkm1j).subtract(betaK);
                        Akj = aeOnr.multiply(k + 1.0).multiply(Bkm1j).subtract(zOnr.multiply(Bkj));
                        betaKminus1 = betaK;
                    } else if (j == k) {
                        Bkj = aeOnr.multiply(2 * k + 1).multiply(r1Onr).multiply(Bkminus1kminus1);
                        Akj = r1Onr.multiply(k + 1).multiply(betaK).subtract(zOnr.multiply(Bkj));
                        Bkminus1kminus1 = Bkj;
                    }

                    Dkj =  Akj.add(zOnr.multiply(Bkj)).multiply(j / (k + 1.0));

                    Bkm2j = Bkm1j;
                    Bkm1j = Bkj;

                    innerSum1 = innerSum1.add(Akj.multiply(Gkj));
                    innerSum2 = innerSum2.add(Bkj.multiply(Gkj));
                    innerSum3 = innerSum3.add(Dkj.multiply(Hkj));
                }

                sum1 = sum1.add(innerSum1);
                sum2 = sum2.add(innerSum2);
                sum3 = sum3.add(innerSum3);

                sinjL = sinjm1L.add(cosL).add(cosjm1L.multiply(sinL));
                cosjL = cosjm1L.add(cosL).subtract(sinjm1L.multiply(sinL));
                sinjm1L = sinjL;
                cosjm1L = cosjL;
            }
            // compute the acceleration
            final T r2Onr12 = r2.divide(r1.multiply(r1));
            final T p1 = r2Onr12.multiply(xDotDotk);
            final T p2 = r2Onr12.multiply(yDotDotk);
            aX = aX.add(p1.multiply(sum1).subtract(p2.multiply(sum3)));
            aY = aY.add(p2.multiply(sum1).add(p1.multiply(sum3)));
            aZ = aZ.subtract(sum2.multiply(mu).divide(r2));

        }
        // provide the perturbing acceleration to the derivatives adder in inertial frame
        final FieldVector3D<T> accInInert =
            bodyToInertial.transformVector(new FieldVector3D<T>(aX, aY, aZ));
        adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());
    }

}
