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

import java.util.Collections;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.TideSystemProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.Jacobianizer;
import org.orekit.propagation.numerical.ParameterConfiguration;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;

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
 * As this class uses finite differences to compute derivatives, the steps for
 * finite differences <strong>must</strong> be initialized by calling {@link
 * #setSteps(double, double)} prior to use derivatives, otherwise an exception
 * will be thrown by {@link #accelerationDerivatives(AbsoluteDate, Frame, FieldVector3D,
 * FieldVector3D, FieldRotation, DerivativeStructure)} and by {@link
 * #accelerationDerivatives(SpacecraftState, String)}.
 * </p>
 *
 * @see HolmesFeatherstoneAttractionModel
 *
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */

public class DrozinerAttractionModel
    extends AbstractParameterizable implements ForceModel, TideSystemProvider {

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
   * @since 6.0
   */
    public DrozinerAttractionModel(final Frame centralBodyFrame,
                                   final UnnormalizedSphericalHarmonicsProvider provider) {
        super(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);

        this.provider         = provider;
        this.mu               = provider.getMu();
        this.centralBodyFrame = centralBodyFrame;
        this.jacobianizer     = null;

    }

    /** Set the step for finite differences with respect to spacecraft position.
     * @param hPosition step used for finite difference computation
     * with respect to spacecraft position (m)
     * @param hMu step used for finite difference computation
     * with respect to central attraction coefficient (m³/s²)
     */
    public void setSteps(final double hPosition, final double hMu) {
        final ParameterConfiguration muConfig =
                new ParameterConfiguration(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT, hMu);
        jacobianizer = new Jacobianizer(this, mu, Collections.singletonList(muConfig), hPosition);
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
        if (jacobianizer == null) {
            throw new OrekitException(OrekitMessages.STEPS_NOT_INITIALIZED_FOR_FINITE_DIFFERENCES);
        }
        return jacobianizer.accelerationDerivatives(date, frame, position, velocity, rotation, mass);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {
        if (jacobianizer == null) {
            throw new OrekitException(OrekitMessages.STEPS_NOT_INITIALIZED_FOR_FINITE_DIFFERENCES);
        }
        return jacobianizer.accelerationDerivatives(s, paramName);
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        return mu;
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        mu = value;
    }

}
