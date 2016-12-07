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
 * This class uses finite differences to compute derivatives and the steps for
 * finite differences are initialized in the {@link
 * #CunninghamAttractionModel(Frame, UnnormalizedSphericalHarmonicsProvider,
 * double) constructor}.
 * </p>
 *
 * @see HolmesFeatherstoneAttractionModel
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */

public class CunninghamAttractionModel extends AbstractForceModel implements TideSystemProvider {

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

    /** Central attraction coefficient. */
    private double mu;

    /** Rotating body. */
    private final Frame bodyFrame;

    /** Helper class computing acceleration derivatives. */
    private Jacobianizer jacobianizer;

   /** Creates a new instance.
   * @param centralBodyFrame rotating body frame
   * @param provider provider for spherical harmonics
   * @param hPosition step used for finite difference computation
   * @since 6.0
   */
    public CunninghamAttractionModel(final Frame centralBodyFrame,
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
                    CunninghamAttractionModel.this.mu = driver.getValue();
                }
            });
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        };

        this.provider     = provider;
        this.mu           = provider.getMu();
        this.bodyFrame    = centralBodyFrame;
        this.jacobianizer = new Jacobianizer(this, mu, hPosition);

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
        final UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
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
        // get the position in body frame
        final FieldAbsoluteDate<T> date = s.getDate();
        final Field<T> field = date.getField();
        final T zero = field.getZero();
        final UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics harmonics = provider.onDate(date.toAbsoluteDate());
        final Transform fromBodyFrame = bodyFrame.getTransformTo(s.getFrame(), date.toAbsoluteDate());
        final Transform toBodyFrame   = fromBodyFrame.getInverse();
        final FieldVector3D<T> relative = toBodyFrame.transformPosition(s.getPVCoordinates().getPosition());

        final T x = relative.getX();
        final T y = relative.getY();
        final T z = relative.getZ();

        final T x2 = x.multiply(x);
        final T y2 = y.multiply(y);
        final T z2 = z.multiply(z);
        final T r2 = x2.add(y2).add(z2);
        final T r = r2.sqrt();
        final double equatorialRadius = provider.getAe();
        if (r.getReal() <= equatorialRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
        }

        // define some intermediate variables
        final T onR2 = r2.reciprocal();
        final T onR3 = onR2.divide(r);
        final T rEqOnR2  = r2.reciprocal().multiply(equatorialRadius);
        final T rEqOnR4  = rEqOnR2.divide(r2);
        final T rEq2OnR2 = rEqOnR2.multiply(equatorialRadius);

        T cmx   = x.negate().multiply(rEqOnR2);
        T cmy   = y.negate().multiply(rEqOnR2);
        T cmz   = z.negate().multiply(rEqOnR2);

        final T dx   = cmx.multiply(-2);
        final T dy   = cmy.multiply(-2);
        final T dz   = cmz.multiply(-2);

        // intermediate variables gradients
        // since dcy/dx = dcx/dy, dcz/dx = dcx/dz and dcz/dy = dcy/dz,
        // we reuse the existing variables

        T dcmxdx =  x2.subtract(y2).subtract(z2).multiply(rEqOnR4);
        T dcmxdy =  dx.multiply(y).multiply(onR2);
        T dcmxdz =  dx.multiply(z).multiply(onR2);
        T dcmydy =  y2.subtract(x2).subtract(z2).multiply(rEqOnR4);
        T dcmydz =  dy.multiply(z).multiply(onR2);
        T dcmzdz =  z2.subtract(x2).subtract(y2).multiply(rEqOnR4);

        final T ddxdx = dcmxdx.multiply(-2);
        final T ddxdy = dcmxdy.multiply(-2);
        final T ddxdz = dcmxdz.multiply(-2);
        final T ddydy = dcmydy.multiply(-2);
        final T ddydz = dcmydz.multiply(-2);
        final T ddzdz = dcmzdz.multiply(-2);

        final T donr2dx = dx.negate().multiply(rEqOnR2);
        final T donr2dy = dy.negate().multiply(rEqOnR2);
        final T donr2dz = dz.negate().multiply(rEqOnR2);

        // potential coefficients (4 per matrix)
        T vrn  = zero;
        T vin  = zero;
        T vrd  = r.multiply(equatorialRadius).reciprocal();
        T vid  = zero;
        T vrn1 = zero;
        T vin1 = zero;
        T vrn2 = zero;
        T vin2 = zero;

        // gradient coefficients (4 per matrix)
        T gradXVrn  = zero;
        T gradXVin  = zero;
        T gradXVrd  = x.negate().multiply(onR3).divide(equatorialRadius);
        T gradXVid  = zero;
        T gradXVrn1 = zero;
        T gradXVin1 = zero;
        T gradXVrn2 = zero;
        T gradXVin2 = zero;

        T gradYVrn  = zero;
        T gradYVin  = zero;
        T gradYVrd  = y.negate().multiply(onR3).divide(equatorialRadius);
        T gradYVid  = zero;
        T gradYVrn1 = zero;
        T gradYVin1 = zero;
        T gradYVrn2 = zero;
        T gradYVin2 = zero;

        T gradZVrn  = zero;
        T gradZVin  = zero;
        T gradZVrd  = z.negate().multiply(onR3).divide(equatorialRadius);
        T gradZVid  = zero;
        T gradZVrn1 = zero;
        T gradZVin1 = zero;
        T gradZVrn2 = zero;
        T gradZVin2 = zero;

        // acceleration coefficients
        T vdX = zero;
        T vdY = zero;
        T vdZ = zero;

        // start calculating
        for (int m = 0; m <= provider.getMaxOrder(); m++) {

            T cx = cmx;
            T cy = cmy;
            T cz = cmz;

            T dcxdx = dcmxdx;
            T dcxdy = dcmxdy;
            T dcxdz = dcmxdz;
            T dcydy = dcmydy;
            T dcydz = dcmydz;
            T dczdz = dcmzdz;

            for (int n = m; n <= provider.getMaxDegree(); n++) {

                if (n == m) {
                    // calculate the first element of the next column

                    vrn      = vrd.multiply(equatorialRadius);
                    vin      = vid.multiply(equatorialRadius);

                    gradXVrn = gradXVrd.multiply(equatorialRadius);
                    gradXVin = gradXVid.multiply(equatorialRadius);
                    gradYVrn = gradYVrd.multiply(equatorialRadius);
                    gradYVin = gradYVid.multiply(equatorialRadius);
                    gradZVrn = gradZVrd.multiply(equatorialRadius);
                    gradZVin = gradZVid.multiply(equatorialRadius);

                    final T tmpGradXVrd = cx.add(dx).multiply(gradXVrd).subtract(cy.add(dy).multiply(gradXVid)).add(dcxdx.add(ddxdx).multiply(vrd))
                                    .subtract(dcxdy.add(ddxdy).multiply(vid));
                    gradXVid = cy.add(dy).multiply(gradXVrd).add(cx.add(dx).multiply(gradXVid)).add(dcxdy.add(ddxdy).multiply(vrd))
                                    .add(dcxdx.add(ddxdx).multiply(vid));
                    gradXVrd = tmpGradXVrd;

                    final T tmpGradYVrd = cx.add(dx).multiply(gradYVrd).subtract(cy.add(dy).multiply(gradYVid)).add(dcxdy.add(ddxdy).multiply(vrd))
                                    .subtract(dcydy.add(ddydy).multiply(vid));
                    gradYVid = cy.add(dy).multiply(gradYVrd).add(cx.add(dx).multiply(gradYVid)).add(dcydy.add(ddydy).multiply(vrd))
                                    .add(dcxdy.add(ddxdy).multiply(vid));
                    gradYVrd = tmpGradYVrd;

                    final T tmpGradZVrd = cx.add(dx).multiply(gradZVrd).subtract(cy.add(dy).multiply(gradZVid)).add(dcxdz.add(ddxdz).multiply(vrd))
                                    .subtract(dcydz.add(ddydz).multiply(vid));
                    gradZVid = cy.add(dy).multiply(gradZVrd).add(cx.add(dx).multiply(gradZVid)).add(dcydz.add(ddydz).multiply(vrd))
                                    .add(dcxdz.add(ddxdz).multiply(vid));
                    gradZVrd = tmpGradZVrd;

                    final T tmpVrd = cx.add(dx).multiply(vrd).subtract(cy.add(dy).multiply(vid));
                    vid = cy.add(dy).multiply(vrd).add(cx.add(dx).multiply(vid));
                    vrd = tmpVrd;

                } else if (n == m + 1) {
                    // calculate the second element of the column
                    vrn = cz.multiply(vrn1);
                    vin = cz.multiply(vin1);

                    gradXVrn = cz.multiply(gradXVrn1).add(dcxdz.multiply(vrn1));
                    gradXVin = cz.multiply(gradXVin1).add(dcxdz.multiply(vin1));

                    gradYVrn = cz.multiply(gradYVrn1).add(dcydz.multiply(vrn1));
                    gradYVin = cz.multiply(gradYVin1).add(dcydz.multiply(vin1));

                    gradZVrn = cz.multiply(gradZVrn1).add(dczdz.multiply(vrn1));
                    gradZVin = cz.multiply(gradZVin1).add(dczdz.multiply(vin1));

                } else {
                    // calculate the other elements of the column
                    final double inv   = 1.0 / (n - m);
                    final double coeff = n + m - 1.0;

                    vrn = cz.multiply(vrn1).subtract(rEq2OnR2.multiply(coeff).multiply(vrn2)).multiply(inv);
                    vin = cz.multiply(vin1).subtract(rEq2OnR2.multiply(coeff).multiply(vin2)).multiply(inv);

                    gradXVrn = cz.multiply(gradXVrn1).subtract(rEq2OnR2.multiply(coeff).multiply(gradXVrn2)).add(dcxdz.multiply(vrn1)).subtract(donr2dx.multiply(coeff).multiply(vrn2)).multiply(inv);
                    gradXVin = cz.multiply(gradXVin1).subtract(rEq2OnR2.multiply(coeff).multiply(gradXVin2)).add(dcxdz.multiply(vin1)).subtract(donr2dx.multiply(coeff).multiply(vin2)).multiply(inv);
                    gradYVrn = cz.multiply(gradYVrn1).subtract(rEq2OnR2.multiply(coeff).multiply(gradYVrn2)).add(dcydz.multiply(vrn1)).subtract(donr2dy.multiply(coeff).multiply(vrn2)).multiply(inv);
                    gradYVin = cz.multiply(gradYVin1).subtract(rEq2OnR2.multiply(coeff).multiply(gradYVin2)).add(dcydz.multiply(vin1)).subtract(donr2dy.multiply(coeff).multiply(vin2)).multiply(inv);
                    gradZVrn = cz.multiply(gradZVrn1).subtract(rEq2OnR2.multiply(coeff).multiply(gradZVrn2)).add(dczdz.multiply(vrn1)).subtract(donr2dz.multiply(coeff).multiply(vrn2)).multiply(inv);
                    gradZVin = cz.multiply(gradZVin1).subtract(rEq2OnR2.multiply(coeff).multiply(gradZVin2)).add(dczdz.multiply(vin1)).subtract(donr2dz.multiply(coeff).multiply(vin2)).multiply(inv);
                }

                // increment variables
                cx = cx.add(dx);
                cy = cy.add(dy);
                cz = cz.add(dz);

                dcxdx = dcxdx.add(ddxdx);
                dcxdy = dcxdy.add(ddxdy);
                dcxdz = dcxdz.add(ddxdz);
                dcydy = dcydy.add(ddydy);
                dcydz = dcydz.add(ddydz);
                dczdz = dczdz.add(ddzdz);

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
                    vdX = vdX.add(gradXVrn.multiply(cnm).add(gradXVin.multiply(snm)));
                    vdY = vdY.add(gradYVrn.multiply(cnm).add(gradYVin.multiply(snm)));
                    vdZ = vdZ.add(gradZVrn.multiply(cnm).add(gradZVin.multiply(snm)));
                }

            }

            // increment variables
            cmx = cmx.add(dx);
            cmy = cmy.add(dy);
            cmz = cmz.add(dz);

            dcmxdx = dcmxdx.add(ddxdx);
            dcmxdy = dcmxdy.add(ddxdy);
            dcmxdz = dcmxdz.add(ddxdz);
            dcmydy = dcmydy.add(ddydy);
            dcmydz = dcmydz.add(ddydz);
            dcmzdz = dcmzdz.add(ddzdz);

        }

        // compute acceleration in inertial frame
        final FieldVector3D<T> acceleration =
            fromBodyFrame.transformVector(new FieldVector3D<T>(vdX.multiply(mu), vdY.multiply(mu), vdZ.multiply(mu)));
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

}
