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
 * Leland E. Cunningham (Lockheed Missiles and Space Company, Sunnyvale
 * and Astronomy Department University of California, Berkeley) in
 * his 1969 paper: <em>On the computation of the spherical harmonic
 * terms needed during the numerical integration of the orbital motion
 * of an artificial satellite</em> (Celestial Mechanics 2, 1970).</p>
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
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */

public class CunninghamAttractionModel
    extends AbstractParameterizable implements ForceModel, TideSystemProvider {

    /** Provider for the spherical harmonics. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Central attraction coefficient. */
    private double mu;

    /** Rotating body. */
    private final Frame bodyFrame;

    /** Helper class computing acceleration derivatives. */
    private Jacobianizer jacobianizer;

   /** Creates a new instance.
   * @param centralBodyFrame rotating body frame
   * @param provider provider for spherical harmonics
   * @since 6.0
   */
    public CunninghamAttractionModel(final Frame centralBodyFrame,
                                     final UnnormalizedSphericalHarmonicsProvider provider) {
        super(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);

        this.provider     = provider;
        this.mu           = provider.getMu();
        this.bodyFrame    = centralBodyFrame;
        this.jacobianizer = null;

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

        // get the position in body frame
        final AbsoluteDate date = s.getDate();
        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        final Transform fromBodyFrame = bodyFrame.getTransformTo(s.getFrame(), date);
        final Transform toBodyFrame   = fromBodyFrame.getInverse();
        final Vector3D relative = toBodyFrame.transformPosition(s.getPVCoordinates().getPosition());

        final double x = relative.getX();
        final double y = relative.getY();
        final double z = relative.getZ();

        final double x2 = x * x;
        final double y2 = y * y;
        final double z2 = z * z;
        final double r2 = x2 + y2 + z2;
        final double r = FastMath.sqrt(r2);
        final double equatorialRadius = provider.getAe();
        if (r <= equatorialRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
        }

        // define some intermediate variables
        final double onR2 = 1 / r2;
        final double onR3 = onR2 / r;
        final double rEqOnR2  = equatorialRadius / r2;
        final double rEqOnR4  = rEqOnR2 / r2;
        final double rEq2OnR2 = equatorialRadius * rEqOnR2;

        double cmx   = -x * rEqOnR2;
        double cmy   = -y * rEqOnR2;
        double cmz   = -z * rEqOnR2;

        final double dx   = -2 * cmx;
        final double dy   = -2 * cmy;
        final double dz   = -2 * cmz;

        // intermediate variables gradients
        // since dcy/dx = dcx/dy, dcz/dx = dcx/dz and dcz/dy = dcy/dz,
        // we reuse the existing variables

        double dcmxdx = (x2 - y2 - z2) * rEqOnR4;
        double dcmxdy =  dx * y * onR2;
        double dcmxdz =  dx * z * onR2;
        double dcmydy = (y2 - x2 - z2) * rEqOnR4;
        double dcmydz =  dy * z * onR2;
        double dcmzdz = (z2 - x2 - y2) * rEqOnR4;

        final double ddxdx = -2 * dcmxdx;
        final double ddxdy = -2 * dcmxdy;
        final double ddxdz = -2 * dcmxdz;
        final double ddydy = -2 * dcmydy;
        final double ddydz = -2 * dcmydz;
        final double ddzdz = -2 * dcmzdz;

        final double donr2dx = -dx * rEqOnR2;
        final double donr2dy = -dy * rEqOnR2;
        final double donr2dz = -dz * rEqOnR2;

        // potential coefficients (4 per matrix)
        double vrn  = 0.0;
        double vin  = 0.0;
        double vrd  = 1.0 / (equatorialRadius * r);
        double vid  = 0.0;
        double vrn1 = 0.0;
        double vin1 = 0.0;
        double vrn2 = 0.0;
        double vin2 = 0.0;

        // gradient coefficients (4 per matrix)
        double gradXVrn  = 0.0;
        double gradXVin  = 0.0;
        double gradXVrd  = -x * onR3 / equatorialRadius;
        double gradXVid  = 0.0;
        double gradXVrn1 = 0.0;
        double gradXVin1 = 0.0;
        double gradXVrn2 = 0.0;
        double gradXVin2 = 0.0;

        double gradYVrn  = 0.0;
        double gradYVin  = 0.0;
        double gradYVrd  = -y * onR3 / equatorialRadius;
        double gradYVid  = 0.0;
        double gradYVrn1 = 0.0;
        double gradYVin1 = 0.0;
        double gradYVrn2 = 0.0;
        double gradYVin2 = 0.0;

        double gradZVrn  = 0.0;
        double gradZVin  = 0.0;
        double gradZVrd  = -z * onR3 / equatorialRadius;
        double gradZVid  = 0.0;
        double gradZVrn1 = 0.0;
        double gradZVin1 = 0.0;
        double gradZVrn2 = 0.0;
        double gradZVin2 = 0.0;

        // acceleration coefficients
        double vdX = 0.0;
        double vdY = 0.0;
        double vdZ = 0.0;

        // start calculating
        for (int m = 0; m <= provider.getMaxOrder(); m++) {

            double cx = cmx;
            double cy = cmy;
            double cz = cmz;

            double dcxdx = dcmxdx;
            double dcxdy = dcmxdy;
            double dcxdz = dcmxdz;
            double dcydy = dcmydy;
            double dcydz = dcmydz;
            double dczdz = dcmzdz;

            for (int n = m; n <= provider.getMaxDegree(); n++) {

                if (n == m) {
                    // calculate the first element of the next column

                    vrn      = equatorialRadius * vrd;
                    vin      = equatorialRadius * vid;

                    gradXVrn = equatorialRadius * gradXVrd;
                    gradXVin = equatorialRadius * gradXVid;
                    gradYVrn = equatorialRadius * gradYVrd;
                    gradYVin = equatorialRadius * gradYVid;
                    gradZVrn = equatorialRadius * gradZVrd;
                    gradZVin = equatorialRadius * gradZVid;

                    final double tmpGradXVrd = (cx + dx) * gradXVrd - (cy + dy) * gradXVid + (dcxdx + ddxdx) * vrd - (dcxdy + ddxdy) * vid;
                    gradXVid = (cy + dy) * gradXVrd + (cx + dx) * gradXVid + (dcxdy + ddxdy) * vrd + (dcxdx + ddxdx) * vid;
                    gradXVrd = tmpGradXVrd;

                    final double tmpGradYVrd = (cx + dx) * gradYVrd - (cy + dy) * gradYVid + (dcxdy + ddxdy) * vrd - (dcydy + ddydy) * vid;
                    gradYVid = (cy + dy) * gradYVrd + (cx + dx) * gradYVid + (dcydy + ddydy) * vrd + (dcxdy + ddxdy) * vid;
                    gradYVrd = tmpGradYVrd;

                    final double tmpGradZVrd = (cx + dx) * gradZVrd - (cy + dy) * gradZVid + (dcxdz + ddxdz) * vrd - (dcydz + ddydz) * vid;
                    gradZVid = (cy + dy) * gradZVrd + (cx + dx) * gradZVid + (dcydz + ddydz) * vrd + (dcxdz + ddxdz) * vid;
                    gradZVrd = tmpGradZVrd;

                    final double tmpVrd = (cx + dx) * vrd - (cy + dy) * vid;
                    vid = (cy + dy) * vrd + (cx + dx) * vid;
                    vrd = tmpVrd;

                } else if (n == m + 1) {
                    // calculate the second element of the column
                    vrn = cz * vrn1;
                    vin = cz * vin1;

                    gradXVrn = cz * gradXVrn1 + dcxdz * vrn1;
                    gradXVin = cz * gradXVin1 + dcxdz * vin1;

                    gradYVrn = cz * gradYVrn1 + dcydz * vrn1;
                    gradYVin = cz * gradYVin1 + dcydz * vin1;

                    gradZVrn = cz * gradZVrn1 + dczdz * vrn1;
                    gradZVin = cz * gradZVin1 + dczdz * vin1;

                } else {
                    // calculate the other elements of the column
                    final double inv   = 1.0 / (n - m);
                    final double coeff = n + m - 1.0;

                    vrn = (cz * vrn1 - coeff * rEq2OnR2 * vrn2) * inv;
                    vin = (cz * vin1 - coeff * rEq2OnR2 * vin2) * inv;

                    gradXVrn = (cz * gradXVrn1 - coeff * rEq2OnR2 * gradXVrn2 + dcxdz * vrn1 - coeff * donr2dx * vrn2) * inv;
                    gradXVin = (cz * gradXVin1 - coeff * rEq2OnR2 * gradXVin2 + dcxdz * vin1 - coeff * donr2dx * vin2) * inv;
                    gradYVrn = (cz * gradYVrn1 - coeff * rEq2OnR2 * gradYVrn2 + dcydz * vrn1 - coeff * donr2dy * vrn2) * inv;
                    gradYVin = (cz * gradYVin1 - coeff * rEq2OnR2 * gradYVin2 + dcydz * vin1 - coeff * donr2dy * vin2) * inv;
                    gradZVrn = (cz * gradZVrn1 - coeff * rEq2OnR2 * gradZVrn2 + dczdz * vrn1 - coeff * donr2dz * vrn2) * inv;
                    gradZVin = (cz * gradZVin1 - coeff * rEq2OnR2 * gradZVin2 + dczdz * vin1 - coeff * donr2dz * vin2) * inv;
                }

                // increment variables
                cx += dx;
                cy += dy;
                cz += dz;

                dcxdx += ddxdx;
                dcxdy += ddxdy;
                dcxdz += ddxdz;
                dcydy += ddydy;
                dcydz += ddydz;
                dczdz += ddzdz;

                vrn2 = vrn1;
                vin2 = vin1;
                gradXVrn2 = gradXVrn1;
                gradXVin2 = gradXVin1;
                gradYVrn2 = gradYVrn1;
                gradYVin2 = gradYVin1;
                gradZVrn2 = gradZVrn1;
                gradZVin2 = gradZVin1;

                vrn1 = vrn;
                vin1 = vin;
                gradXVrn1 = gradXVrn;
                gradXVin1 = gradXVin;
                gradYVrn1 = gradYVrn;
                gradYVin1 = gradYVin;
                gradZVrn1 = gradZVrn;
                gradZVin1 = gradZVin;

                // compute the acceleration due to the Cnm and Snm coefficients
                // ignoring the central attraction
                if (n > 0) {
                    final double cnm = harmonics.getUnnormalizedCnm(n, m);
                    final double snm = harmonics.getUnnormalizedSnm(n, m);
                    vdX += cnm * gradXVrn + snm * gradXVin;
                    vdY += cnm * gradYVrn + snm * gradYVin;
                    vdZ += cnm * gradZVrn + snm * gradZVin;
                }

            }

            // increment variables
            cmx += dx;
            cmy += dy;
            cmz += dz;

            dcmxdx += ddxdx;
            dcmxdy += ddxdy;
            dcmxdz += ddxdz;
            dcmydy += ddydy;
            dcmydz += ddydz;
            dcmzdz += ddzdz;

        }

        // compute acceleration in inertial frame
        final Vector3D acceleration =
            fromBodyFrame.transformVector(new Vector3D(mu * vdX, mu * vdY, mu * vdZ));
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());

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
