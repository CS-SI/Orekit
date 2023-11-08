/* Copyright 2023 Luc Maisonobe
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
package org.orekit.attitudes;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.DenseOutputModel;
import org.hipparchus.ode.FieldDenseOutputModel;
import org.hipparchus.ode.FieldExpandableODE;
import org.hipparchus.ode.FieldODEState;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.ode.ODEState;
import org.hipparchus.ode.OrdinaryDifferentialEquation;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.special.elliptic.jacobi.CopolarN;
import org.hipparchus.special.elliptic.jacobi.FieldCopolarN;
import org.hipparchus.special.elliptic.jacobi.FieldJacobiElliptic;
import org.hipparchus.special.elliptic.jacobi.JacobiElliptic;
import org.hipparchus.special.elliptic.jacobi.JacobiEllipticBuilder;
import org.hipparchus.special.elliptic.legendre.LegendreEllipticIntegral;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;


/** This class handles torque-free motion of a general (non-symmetrical) body.
 * <p>
 * This attitude model is analytical, it can be called at any arbitrary date
 * before or after the date of the initial attitude. Despite being an analytical
 * model, it is <em>not</em> an approximation. It provides the attitude exactly
 * in O(1) time.
 * </p>
 * <p>
 * The equations are based on Landau and Lifchitz Course of Theoretical Physics,
 * Mechanics vol 1, chapter 37. Some adaptations have been made to Landau and
 * Lifchitz equations:
 * </p>
 * <ul>
 *   <li>inertia can be in any order</li>
 *   <li>initial conditions can be arbitrary</li>
 *   <li>signs of several equations have been fixed to work for all initial conditions</li>
 *   <li>equations have been rewritten to work in all octants</li>
 *   <li>the φ angle model is based on a precomputed quadrature over one period computed
 *   at construction (the Landau and Lifchitz equations 37.17 to 37.20 seem to be wrong)</li>
 * </ul>
 * <p>
 * The precomputed quadrature is performed numerically, but as it is performed only once at
 * construction and the full integrated model over one period is saved, it can be applied
 * analytically later on for any number of periods, hence we consider this attitude mode
 * to be analytical.
 * </p>
 * @author Luc Maisonobe
 * @author Lucas Girodet
 * @since 12.0
 */
public class TorqueFree implements AttitudeProvider {

    /** Initial attitude. */
    private final Attitude initialAttitude;

    /** Spacecraft inertia. */
    private final Inertia inertia;

    /** Regular model for primitive double arguments. */
    private final DoubleModel doubleModel;

    /** Cached field-based models. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldModel<? extends CalculusFieldElement<?>>> cachedModels;

    /** Simple constructor.
     * @param initialAttitude initial attitude
     * @param inertia spacecraft inertia
     */
    public TorqueFree(final Attitude initialAttitude, final Inertia inertia) {

        this.initialAttitude = initialAttitude;
        this.inertia         = inertia;

        // prepare the regular model
        this.doubleModel  = new DoubleModel();

        // set an empty cache for field-based models that will be lazily build
        this.cachedModels = new HashMap<>();

    }

    /** Get the initial attitude.
     * @return initial attitude
     */
    public Attitude getInitialAttitude() {
        return initialAttitude;
    }

    /** Get the spacecraft inertia.
     * @return spacecraft inertia
     */
    public Inertia getInertia() {
        return inertia;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {

        // attitude from model
        final Attitude attitude =
                        new Attitude(initialAttitude.getReferenceFrame(), doubleModel.evaluate(date));

        // fix frame
        return attitude.withReferenceFrame(frame);

    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                            final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {

        // get the model for specified field
        @SuppressWarnings("unchecked")
        FieldModel<T> fm = (FieldModel<T>) cachedModels.get(date.getField());
        if (fm == null) {
            // create a model for this field
            fm = new FieldModel<>(date.getField());
            cachedModels.put(date.getField(), fm);
        }

        // attitude from model
        final FieldAttitude<T> attitude =
                        new FieldAttitude<>(initialAttitude.getReferenceFrame(), fm.evaluate(date));

        // fix frame
        return attitude.withReferenceFrame(frame);

    }

    /** Torque-free model. */
    private class DoubleModel {

        /** Inertia sorted to get a motion about axis 3. */
        private final Inertia sortedInertia;

        /** State scaling factor. */
        private final double o1Scale;

        /** State scaling factor. */
        private final double o2Scale;

        /** State scaling factor. */
        private final double o3Scale;

        /** Jacobi elliptic function. */
        private final JacobiElliptic jacobi;

        /** Time scaling factor. */
        private final double tScale;

        /** Time reference for rotation rate. */
        private final AbsoluteDate tRef;

        /** Offset rotation  between initial inertial frame and the frame with moment vector and Z axis aligned. */
        private final Rotation inertToAligned;

        /** Rotation to switch to the converted axes frame. */
        private final Rotation sortedToBody;

        /** Period of rotation rate. */
        private final double period;

        /** Slope of the linear part of the phi model. */
        private final double phiSlope;

        /** DenseOutputModel of phi. */
        private final DenseOutputModel phiQuadratureModel;

        /** Integral part of quadrature model over one period. */
        private final double integOnePeriod;

        /** Simple constructor.
         */
        DoubleModel() {

            // build inertia tensor
            final double   i1  = inertia.getInertiaAxis1().getI();
            final Vector3D a1  = inertia.getInertiaAxis1().getA();
            final double   i2  = inertia.getInertiaAxis2().getI();
            final Vector3D a2  = inertia.getInertiaAxis2().getA();
            final double   i3  = inertia.getInertiaAxis3().getI();
            final Vector3D a3  = inertia.getInertiaAxis3().getA();
            final Vector3D n1  = a1.normalize();
            final Vector3D n2  = a2.normalize();
            final Vector3D n3  = Vector3D.dotProduct(Vector3D.crossProduct(a1, a2), a3) > 0 ?
                                  a3.normalize() : a3.normalize().negate();

            final Vector3D omega0 = initialAttitude.getSpin();
            final Vector3D m0     = new Vector3D(i1 * Vector3D.dotProduct(omega0, n1), n1,
                                                 i2 * Vector3D.dotProduct(omega0, n2), n2,
                                                 i3 * Vector3D.dotProduct(omega0, n3), n3);

            // sort axes in increasing moments of inertia order
            Inertia tmpInertia = new Inertia(new InertiaAxis(i1, n1), new InertiaAxis(i2, n2), new InertiaAxis(i3, n3));
            if (tmpInertia.getInertiaAxis1().getI() > tmpInertia.getInertiaAxis2().getI()) {
                tmpInertia = tmpInertia.swap12();
            }
            if (tmpInertia.getInertiaAxis2().getI() > tmpInertia.getInertiaAxis3().getI()) {
                tmpInertia = tmpInertia.swap23();
            }
            if (tmpInertia.getInertiaAxis1().getI() > tmpInertia.getInertiaAxis2().getI()) {
                tmpInertia = tmpInertia.swap12();
            }

            // in order to simplify implementation, we want the motion to be about axis 3
            // which is either the minimum or the maximum inertia axis
            final double  o1                 = Vector3D.dotProduct(omega0, n1);
            final double  o2                 = Vector3D.dotProduct(omega0, n2);
            final double  o3                 = Vector3D.dotProduct(omega0, n3);
            final double  o12                = o1 * o1;
            final double  o22                = o2 * o2;
            final double  o32                = o3 * o3;
            final double   twoE              = i1 * o12 + i2 * o22 + i3 * o32;
            final double   m2                = i1 * i1 * o12 + i2 * i2 * o22 + i3 * i3 * o32;
            final double   separatrixInertia = (twoE == 0) ? 0.0 : m2 / twoE;
            final boolean  clockwise;
            if (separatrixInertia < tmpInertia.getInertiaAxis2().getI()) {
                // motion is about minimum inertia axis
                // we swap axes to put them in decreasing moments order
                // motion will be clockwise about axis 3
                clockwise = true;
                tmpInertia   = tmpInertia.swap13();
            } else {
                // motion is about maximum inertia axis
                // we keep axes in increasing moments order
                // motion will be counter-clockwise about axis 3
                clockwise = false;
            }
            sortedInertia = tmpInertia;

            final double i1C = tmpInertia.getInertiaAxis1().getI();
            final double i2C = tmpInertia.getInertiaAxis2().getI();
            final double i3C = tmpInertia.getInertiaAxis3().getI();
            final double i32 = i3C - i2C;
            final double i31 = i3C - i1C;
            final double i21 = i2C - i1C;

            // convert initial conditions to Euler angles such the M is aligned with Z in sorted computation frame
            sortedToBody   = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                          tmpInertia.getInertiaAxis1().getA(), tmpInertia.getInertiaAxis2().getA());
            final Vector3D omega0Sorted = sortedToBody.applyInverseTo(omega0);
            final Vector3D m0Sorted     = sortedToBody.applyInverseTo(m0);
            final double   phi0         = 0; // this angle can be set arbitrarily, so 0 is a fair value (Eq. 37.13 - 37.14)
            final double   theta0       = FastMath.acos(m0Sorted.getZ() / m0Sorted.getNorm());
            final double   psi0         = FastMath.atan2(m0Sorted.getX(), m0Sorted.getY()); // it is really atan2(x, y), not atan2(y, x) as usual!

            // compute offset rotation between inertial frame aligned with momentum and regular inertial frame
            final Rotation alignedToSorted0 = new Rotation(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                           phi0, theta0, psi0);
            inertToAligned = alignedToSorted0.
                             applyInverseTo(sortedToBody.applyInverseTo(initialAttitude.getRotation()));

            // Ω is always o1Scale * cn((t-tref) * tScale), o2Scale * sn((t-tref) * tScale), o3Scale * dn((t-tref) * tScale)
            tScale  = FastMath.copySign(FastMath.sqrt(i32 * (m2 - twoE * i1C) / (i1C * i2C * i3C)),
                                        clockwise ? -omega0Sorted.getZ() : omega0Sorted.getZ());
            o1Scale = FastMath.sqrt((twoE * i3C - m2) / (i1C * i31));
            o2Scale = FastMath.sqrt((twoE * i3C - m2) / (i2C * i32));
            o3Scale = FastMath.copySign(FastMath.sqrt((m2 - twoE * i1C) / (i3C * i31)), omega0Sorted.getZ());

            final double k2 = (twoE == 0) ? 0.0 : i21 * (twoE * i3C - m2) / (i32 * (m2 - twoE * i1C));
            jacobi = JacobiEllipticBuilder.build(k2);
            period = 4 * LegendreEllipticIntegral.bigK(k2) / tScale;

            final double dtRef;
            if (o1Scale == 0) {
                // special case where twoE * i3C = m2, then o2Scale is also zero
                // motion is exactly along one axis
                dtRef = 0;
            } else {
                if (FastMath.abs(omega0Sorted.getX()) >= FastMath.abs(omega0Sorted.getY())) {
                    if (omega0Sorted.getX() >= 0) {
                        // omega is roughly towards +I
                        dtRef = -jacobi.arcsn(omega0Sorted.getY() / o2Scale) / tScale;
                    } else {
                        // omega is roughly towards -I
                        dtRef = jacobi.arcsn(omega0Sorted.getY() / o2Scale) / tScale - 0.5 * period;
                    }
                } else {
                    if (omega0Sorted.getY() >= 0) {
                        // omega is roughly towards +J
                        dtRef = -jacobi.arccn(omega0Sorted.getX() / o1Scale) / tScale;
                    } else {
                        // omega is roughly towards -J
                        dtRef = jacobi.arccn(omega0Sorted.getX() / o1Scale) / tScale;
                    }
                }
            }
            tRef = initialAttitude.getDate().shiftedBy(dtRef);

            phiSlope           = FastMath.sqrt(m2) / i3C;
            phiQuadratureModel = computePhiQuadratureModel(dtRef);
            integOnePeriod     = phiQuadratureModel.getInterpolatedState(phiQuadratureModel.getFinalTime()).getPrimaryState()[0];

        }

        /** Compute the model for φ angle.
         * @param dtRef start time
         * @return model for φ angle
         */
        private DenseOutputModel computePhiQuadratureModel(final double dtRef) {

            final double i1C = sortedInertia.getInertiaAxis1().getI();
            final double i2C = sortedInertia.getInertiaAxis2().getI();
            final double i3C = sortedInertia.getInertiaAxis3().getI();

            final double i32 = i3C - i2C;
            final double i31 = i3C - i1C;
            final double i21 = i2C - i1C;

            // coefficients for φ model
            final double b = phiSlope * i32 * i31;
            final double c = i1C * i32;
            final double d = i3C * i21;

            // integrate the quadrature phi term over one period
            final DormandPrince853Integrator integ = new DormandPrince853Integrator(1.0e-6 * period, 1.0e-2 * period,
                                                                                    phiSlope * period * 1.0e-13, 1.0e-13);
            final DenseOutputModel model = new DenseOutputModel();
            integ.addStepHandler(model);

            integ.integrate(new OrdinaryDifferentialEquation() {

                /** {@inheritDoc} */
                @Override
                public int getDimension() {
                    return 1;
                }

                /** {@inheritDoc} */
                @Override
                public double[] computeDerivatives(final double t, final double[] y) {
                    final double sn = jacobi.valuesN((t - dtRef) * tScale).sn();
                    return new double[] {
                        b / (c + d * sn * sn)
                    };
                }

            }, new ODEState(0, new double[1]), period);

            return model;

        }

        /** Evaluate torque-free motion model.
         * @param date evaluation date
         * @return body orientation at date
         */
        public TimeStampedAngularCoordinates evaluate(final AbsoluteDate date) {

            // angular velocity
            final CopolarN valuesN     = jacobi.valuesN(date.durationFrom(tRef) * tScale);
            final Vector3D omegaSorted = new Vector3D(o1Scale * valuesN.cn(), o2Scale * valuesN.sn(), o3Scale * valuesN.dn());
            final Vector3D omegaBody   = sortedToBody.applyTo(omegaSorted);

            // acceleration
            final Vector3D accelerationSorted = new Vector3D(o1Scale * tScale *                  valuesN.cn() * valuesN.dn(),
                                                             o2Scale * tScale *                 -valuesN.sn() * valuesN.dn(),
                                                             o3Scale * tScale * -jacobi.getM() * valuesN.sn() * valuesN.cn());
            final Vector3D accelerationBody   = sortedToBody.applyTo(accelerationSorted);

            // first Euler angles are directly linked to angular velocity
            final double   dt          = date.durationFrom(initialAttitude.getDate());
            final double   psi         = FastMath.atan2(sortedInertia.getInertiaAxis1().getI() * omegaSorted.getX(),
                                                        sortedInertia.getInertiaAxis2().getI() * omegaSorted.getY());
            final double   theta       = FastMath.acos(omegaSorted.getZ() / phiSlope);
            final double   phiLinear   = phiSlope * dt;

            // third Euler angle results from a quadrature
            final int    nbPeriods     = (int) FastMath.floor(dt / period);
            final double tStartInteg   = nbPeriods * period;
            final double integPartial  = phiQuadratureModel.getInterpolatedState(dt - tStartInteg).getPrimaryState()[0];
            final double phiQuadrature = nbPeriods * integOnePeriod + integPartial;
            final double phi           = phiLinear + phiQuadrature;

            // rotation between computation frame (aligned with momentum) and sorted computation frame
            // (it is simply the angles equations provided by Landau & Lifchitz)
            final Rotation alignedToSorted = new Rotation(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                          phi, theta, psi);

            // combine with offset rotation to get back from regular inertial frame to body frame
            final Rotation inertToBody = sortedToBody.applyTo(alignedToSorted.applyTo(inertToAligned));

            return new TimeStampedAngularCoordinates(date, inertToBody, omegaBody, accelerationBody);
        }

    }

    /** Torque-free model.
     * @param <T> type of the field elements
     */
    private class FieldModel <T extends CalculusFieldElement<T>> {

        /** Inertia sorted to get a motion about axis 3. */
        private final FieldInertia<T> sortedInertia;

        /** State scaling factor. */
        private final T o1Scale;

        /** State scaling factor. */
        private final T o2Scale;

        /** State scaling factor. */
        private final T o3Scale;

        /** Jacobi elliptic function. */
        private final FieldJacobiElliptic<T> jacobi;

        /** Time scaling factor. */
        private final T tScale;

        /** Time reference for rotation rate. */
        private final FieldAbsoluteDate<T> tRef;

        /** Offset rotation  between initial inertial frame and the frame with moment vector and Z axis aligned. */
        private final FieldRotation<T> inertToAligned;

        /** Rotation to switch to the converted axes frame. */
        private final FieldRotation<T> sortedToBody;

        /** Period of rotation rate. */
        private final T period;

        /** Slope of the linear part of the phi model. */
        private final T phiSlope;

        /** DenseOutputModel of phi. */
        private final FieldDenseOutputModel<T> phiQuadratureModel;

        /** Integral part of quadrature model over one period. */
        private final T integOnePeriod;

        /** Simple constructor.
         * @param field field to which elements belong
         */
        FieldModel(final Field<T> field) {

            final double   i1  = inertia.getInertiaAxis1().getI();
            final Vector3D a1  = inertia.getInertiaAxis1().getA();
            final double   i2  = inertia.getInertiaAxis2().getI();
            final Vector3D a2  = inertia.getInertiaAxis2().getA();
            final double   i3  = inertia.getInertiaAxis3().getI();
            final Vector3D a3  = inertia.getInertiaAxis3().getA();

            final T                zero = field.getZero();
            final T                fI1  = zero.newInstance(i1);
            final FieldVector3D<T> fA1  = new FieldVector3D<>(field, a1);
            final T                fI2  = zero.newInstance(i2);
            final FieldVector3D<T> fA2  = new FieldVector3D<>(field, a2);
            final T                fI3  = zero.newInstance(i3);
            final FieldVector3D<T> fA3  = new FieldVector3D<>(field, a3);

            // build inertia tensor
            final FieldVector3D<T> n1  = fA1.normalize();
            final FieldVector3D<T> n2  = fA2.normalize();
            final FieldVector3D<T> n3  = Vector3D.dotProduct(Vector3D.crossProduct(a1, a2), a3) > 0 ?
                                         fA3.normalize() : fA3.normalize().negate();

            final FieldVector3D<T> omega0 = new FieldVector3D<>(field, initialAttitude.getSpin());
            final FieldVector3D<T> m0 = new FieldVector3D<>(fI1.multiply(FieldVector3D.dotProduct(omega0, n1)), n1,
                                                            fI2.multiply(FieldVector3D.dotProduct(omega0, n2)), n2,
                                                            fI3.multiply(FieldVector3D.dotProduct(omega0, n3)), n3);

            // sort axes in increasing moments of inertia order
            FieldInertia<T> tmpInertia = new FieldInertia<>(new FieldInertiaAxis<>(fI1, n1),
                                                            new FieldInertiaAxis<>(fI2, n2),
                                                            new FieldInertiaAxis<>(fI3, n3));
            if (tmpInertia.getInertiaAxis1().getI().subtract(tmpInertia.getInertiaAxis2().getI()).getReal() > 0) {
                tmpInertia = tmpInertia.swap12();
            }
            if (tmpInertia.getInertiaAxis2().getI().subtract(tmpInertia.getInertiaAxis3().getI()).getReal() > 0) {
                tmpInertia = tmpInertia.swap23();
            }
            if (tmpInertia.getInertiaAxis1().getI().subtract(tmpInertia.getInertiaAxis2().getI()).getReal() > 0) {
                tmpInertia = tmpInertia.swap12();
            }

            // in order to simplify implementation, we want the motion to be about axis 3
            // which is either the minimum or the maximum inertia axis
            final T       o1                = FieldVector3D.dotProduct(omega0, n1);
            final T       o2                = FieldVector3D.dotProduct(omega0, n2);
            final T       o3                = FieldVector3D.dotProduct(omega0, n3);
            final T       o12               = o1.multiply(o1);
            final T       o22               = o2.multiply(o2);
            final T       o32               = o3.multiply(o3);
            final T       twoE              = fI1.multiply(o12).add(fI2.multiply(o22)).add(fI3.multiply(o32));
            final T       m2                = fI1.multiply(fI1).multiply(o12).add(fI2.multiply(fI2).multiply(o22)).add(fI3.multiply(fI3).multiply(o32));
            final T       separatrixInertia = (twoE.isZero()) ? zero : m2.divide(twoE);
            final boolean clockwise;
            if (separatrixInertia.subtract(tmpInertia.getInertiaAxis2().getI()).getReal() < 0) {
                // motion is about minimum inertia axis
                // we swap axes to put them in decreasing moments order
                // motion will be clockwise about axis 3
                clockwise  = true;
                tmpInertia = tmpInertia.swap13();
            } else {
                // motion is about maximum inertia axis
                // we keep axes in increasing moments order
                // motion will be counter-clockwise about axis 3
                clockwise = false;
            }
            sortedInertia = tmpInertia;

            final T i1C = tmpInertia.getInertiaAxis1().getI();
            final T i2C = tmpInertia.getInertiaAxis2().getI();
            final T i3C = tmpInertia.getInertiaAxis3().getI();
            final T i32 = i3C.subtract(i2C);
            final T i31 = i3C.subtract(i1C);
            final T i21 = i2C.subtract(i1C);

            // convert initial conditions to Euler angles such the M is aligned with Z in sorted computation frame
            sortedToBody   = new FieldRotation<>(FieldVector3D.getPlusI(field),
                                                 FieldVector3D.getPlusJ(field),
                                                 tmpInertia.getInertiaAxis1().getA(),
                                                 tmpInertia.getInertiaAxis2().getA());
            final FieldVector3D<T> omega0Sorted = sortedToBody.applyInverseTo(omega0);
            final FieldVector3D<T> m0Sorted     = sortedToBody.applyInverseTo(m0);
            final T                phi0         = zero; // this angle can be set arbitrarily, so 0 is a fair value (Eq. 37.13 - 37.14)
            final T                theta0       = FastMath.acos(m0Sorted.getZ().divide(m0Sorted.getNorm()));
            final T                psi0         = FastMath.atan2(m0Sorted.getX(), m0Sorted.getY()); // it is really atan2(x, y), not atan2(y, x) as usual!

            // compute offset rotation between inertial frame aligned with momentum and regular inertial frame
            final FieldRotation<T> alignedToSorted0 = new FieldRotation<>(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                                          phi0, theta0, psi0);
            inertToAligned = alignedToSorted0.
                             applyInverseTo(sortedToBody.applyInverseTo(new FieldRotation<>(field, initialAttitude.getRotation())));

            // Ω is always o1Scale * cn((t-tref) * tScale), o2Scale * sn((t-tref) * tScale), o3Scale * dn((t-tref) * tScale)
            tScale  = FastMath.copySign(FastMath.sqrt(i32.multiply(m2.subtract(twoE.multiply(i1C))).divide(i1C.multiply(i2C).multiply(i3C))),
                                        clockwise ? omega0Sorted.getZ().negate() : omega0Sorted.getZ());
            o1Scale = FastMath.sqrt(twoE.multiply(i3C).subtract(m2).divide(i1C.multiply(i31)));
            o2Scale = FastMath.sqrt(twoE.multiply(i3C).subtract(m2).divide(i2C.multiply(i32)));
            o3Scale = FastMath.copySign(FastMath.sqrt(m2.subtract(twoE.multiply(i1C)).divide(i3C.multiply(i31))),
                                        omega0Sorted.getZ());

            final T k2 = (twoE.isZero()) ?
                         zero :
                         i21.multiply(twoE.multiply(i3C).subtract(m2)).
                         divide(i32.multiply(m2.subtract(twoE.multiply(i1C))));
            jacobi = JacobiEllipticBuilder.build(k2);
            period = LegendreEllipticIntegral.bigK(k2).multiply(4).divide(tScale);

            final T dtRef;
            if (o1Scale.isZero()) {
                // special case where twoE * i3C = m2, then o2Scale is also zero
                // motion is exactly along one axis
                dtRef = zero;
            } else {
                if (FastMath.abs(omega0Sorted.getX()).subtract(FastMath.abs(omega0Sorted.getY())).getReal() >= 0) {
                    if (omega0Sorted.getX().getReal() >= 0) {
                        // omega is roughly towards +I
                        dtRef = jacobi.arcsn(omega0Sorted.getY().divide(o2Scale)).divide(tScale).negate();
                    } else {
                        // omega is roughly towards -I
                        dtRef = jacobi.arcsn(omega0Sorted.getY().divide(o2Scale)).divide(tScale).subtract(period.multiply(0.5));
                    }
                } else {
                    if (omega0Sorted.getY().getReal() >= 0) {
                        // omega is roughly towards +J
                        dtRef = jacobi.arccn(omega0Sorted.getX().divide(o1Scale)).divide(tScale).negate();
                    } else {
                        // omega is roughly towards -J
                        dtRef = jacobi.arccn(omega0Sorted.getX().divide(o1Scale)).divide(tScale);
                    }
                }
            }
            tRef = new FieldAbsoluteDate<>(field, initialAttitude.getDate()).shiftedBy(dtRef);

            phiSlope           = FastMath.sqrt(m2).divide(i3C);
            phiQuadratureModel = computePhiQuadratureModel(dtRef);
            integOnePeriod     = phiQuadratureModel.getInterpolatedState(phiQuadratureModel.getFinalTime()).getPrimaryState()[0];

        }

        /** Compute the model for φ angle.
         * @param dtRef start time
         * @return model for φ angle
         */
        private FieldDenseOutputModel<T> computePhiQuadratureModel(final T dtRef) {

            final T zero = dtRef.getField().getZero();

            final T i1C = sortedInertia.getInertiaAxis1().getI();
            final T i2C = sortedInertia.getInertiaAxis2().getI();
            final T i3C = sortedInertia.getInertiaAxis3().getI();

            final T i32 = i3C.subtract(i2C);
            final T i31 = i3C.subtract(i1C);
            final T i21 = i2C.subtract(i1C);

            // coefficients for φ model
            final T b = phiSlope.multiply(i32).multiply(i31);
            final T c = i1C.multiply(i32);
            final T d = i3C.multiply(i21);

            // integrate the quadrature phi term on one period
            final DormandPrince853FieldIntegrator<T> integ = new DormandPrince853FieldIntegrator<>(dtRef.getField(),
                                                                                                   1.0e-6 * period.getReal(),
                                                                                                   1.0e-2 * period.getReal(),
                                                                                                   phiSlope.getReal() * period.getReal() * 1.0e-13,
                                                                                                   1.0e-13);
            final FieldDenseOutputModel<T> model = new FieldDenseOutputModel<>();
            integ.addStepHandler(model);

            integ.integrate(new FieldExpandableODE<T>(new FieldOrdinaryDifferentialEquation<T>() {

                /** {@inheritDoc} */
                @Override
                public int getDimension() {
                    return 1;
                }

                /** {@inheritDoc} */
                @Override
                public T[] computeDerivatives(final T t, final T[] y) {
                    final T sn = jacobi.valuesN(t.subtract(dtRef).multiply(tScale)).sn();
                    final T[] yDot = MathArrays.buildArray(dtRef.getField(), 1);
                    yDot[0] = b.divide(c.add(d.multiply(sn).multiply(sn)));
                    return yDot;
                }

            }), new FieldODEState<T>(zero, MathArrays.buildArray(dtRef.getField(), 1)), period);

            return model;

        }

        /** Evaluate torque-free motion model.
         * @param date evaluation date
         * @return body orientation at date
         */
        public TimeStampedFieldAngularCoordinates<T> evaluate(final FieldAbsoluteDate<T> date) {

            // angular velocity
            final FieldCopolarN<T> valuesN     = jacobi.valuesN(date.durationFrom(tRef).multiply(tScale));
            final FieldVector3D<T> omegaSorted = new FieldVector3D<>(valuesN.cn().multiply(o1Scale),
                                                                     valuesN.sn().multiply(o2Scale),
                                                                     valuesN.dn().multiply(o3Scale));
            final FieldVector3D<T> omegaBody   = sortedToBody.applyTo(omegaSorted);

            // acceleration
            final FieldVector3D<T> accelerationSorted =
                            new FieldVector3D<>(o1Scale.multiply(tScale).multiply(valuesN.cn()).multiply(valuesN.dn()),
                                                o2Scale.multiply(tScale).multiply(valuesN.sn().negate()).multiply(valuesN.dn()),
                                                o3Scale.multiply(tScale).multiply(jacobi.getM().negate()).multiply(valuesN.sn()).multiply(valuesN.cn()));
            final FieldVector3D<T> accelerationBody   = sortedToBody.applyTo(accelerationSorted);

            // first Euler angles are directly linked to angular velocity
            final T   dt          = date.durationFrom(initialAttitude.getDate());
            final T   psi         = FastMath.atan2(sortedInertia.getInertiaAxis1().getI().multiply(omegaSorted.getX()),
                                                   sortedInertia.getInertiaAxis2().getI().multiply(omegaSorted.getY()));
            final T   theta       = FastMath.acos(omegaSorted.getZ().divide(phiSlope));
            final T   phiLinear   = dt.multiply(phiSlope);

            // third Euler angle results from a quadrature
            final int nbPeriods   = (int) FastMath.floor(dt.divide(period)).getReal();
            final T tStartInteg   = period.multiply(nbPeriods);
            final T integPartial  = phiQuadratureModel.getInterpolatedState(dt.subtract(tStartInteg)).getPrimaryState()[0];
            final T phiQuadrature = integOnePeriod.multiply(nbPeriods).add(integPartial);
            final T phi           = phiLinear.add(phiQuadrature);

            // rotation between computation frame (aligned with momentum) and sorted computation frame
            // (it is simply the angles equations provided by Landau & Lifchitz)
            final FieldRotation<T> alignedToSorted = new FieldRotation<>(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                                         phi, theta, psi);

            // combine with offset rotation to get back from regular inertial frame to body frame
            final FieldRotation<T> inertToBody = sortedToBody.applyTo(alignedToSorted.applyTo(inertToAligned));

            return new TimeStampedFieldAngularCoordinates<>(date, inertToBody, omegaBody, accelerationBody);

        }

    }

}
