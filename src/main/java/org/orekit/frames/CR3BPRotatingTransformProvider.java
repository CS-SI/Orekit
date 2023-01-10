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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Transform provider for the rotating frame of the CR3BP System.
 * @author Vincent Mouraux
 * @since 10.2
 */
class CR3BPRotatingTransformProvider implements TransformProvider {

    /** Serializable UID.*/
    private static final long serialVersionUID = 20190519L;

    /** Frame for results. Always defined as primaryBody's inertially oriented frame.*/
    private final Frame frame;

    /** Celestial body with smaller mass, m2.*/
    private final CelestialBody secondaryBody;

    /** Mass ratio of the system.*/
    private final double mu;


    /** Simple constructor.
     * @param mu System mass ratio
     * @param primaryBody Primary body.
     * @param secondaryBody Secondary body.
     */
    CR3BPRotatingTransformProvider(final double mu, final CelestialBody primaryBody, final CelestialBody secondaryBody) {
        this.secondaryBody = secondaryBody;
        this.frame = primaryBody.getInertiallyOrientedFrame();
        this.mu = mu;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final FieldPVCoordinates<UnivariateDerivative2> pv21        = secondaryBody.getPVCoordinates(date, frame).toUnivariateDerivative2PV();
        final Field<UnivariateDerivative2>              field       = pv21.getPosition().getX().getField();
        final FieldVector3D<UnivariateDerivative2>      translation = FieldVector3D.getPlusI(field).scalarMultiply(pv21.getPosition().getNorm().multiply(mu)).negate();

        final FieldRotation<UnivariateDerivative2> rotation = new FieldRotation<>(pv21.getPosition(), pv21.getMomentum(),
                                                                                  FieldVector3D.getPlusI(field),
                                                                                  FieldVector3D.getPlusK(field));

        final UnivariateDerivative2[] rotationRates = rotation.getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        final Vector3D rotationRate = new Vector3D(rotationRates[0].getPartialDerivative(1),   rotationRates[1].getPartialDerivative(1),   rotationRates[2].getPartialDerivative(1));
        final Vector3D rotationAcc  = new Vector3D(rotationRates[0].getPartialDerivative(2),   rotationRates[1].getPartialDerivative(2),   rotationRates[2].getPartialDerivative(2));
        final Vector3D velocity     = new Vector3D(translation.getX().getPartialDerivative(1), translation.getY().getPartialDerivative(1), translation.getZ().getPartialDerivative(1));
        final Vector3D acceleration = new Vector3D(translation.getX().getPartialDerivative(2), translation.getY().getPartialDerivative(2), translation.getZ().getPartialDerivative(2));

        final Transform transform1 = new Transform(date, translation.toVector3D(), velocity, acceleration);
        final Transform transform2 = new Transform(date, rotation.toRotation(), rotationRate, rotationAcc);
        return new Transform(date, transform2, transform1);
    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        final TimeStampedPVCoordinates pv = secondaryBody.getPVCoordinates(date, frame);
        final Vector3D translation = Vector3D.PLUS_I
                .scalarMultiply(pv.getPosition().getNorm() * mu).negate();

        final Rotation rotation = new Rotation(
                pv.getPosition(), pv.getMomentum(),
                Vector3D.PLUS_I, Vector3D.PLUS_K);

        final StaticTransform transform1 = StaticTransform.of(date, translation);
        final StaticTransform transform2 = StaticTransform.of(date, rotation);
        return StaticTransform.compose(date, transform2, transform1);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21 = secondaryBody.getPVCoordinates(date, frame);
        final Field<T>              field = pv21.getPosition().getX().getField();

        final FieldVector3D<T> translationField = FieldVector3D.getPlusI(field).scalarMultiply(pv21.getPosition().getNorm().multiply(mu)).negate();
        final FieldRotation<T> rotationField = new FieldRotation<>(pv21.getPosition(), pv21.getMomentum(),
                                                                   FieldVector3D.getPlusI(field),
                                                                   FieldVector3D.getPlusK(field));

        final FieldPVCoordinates<FieldUnivariateDerivative2<T>> pv21FDS        = secondaryBody.getPVCoordinates(date, frame).toUnivariateDerivative2PV();
        final Field<FieldUnivariateDerivative2<T>>              fieldUD        = pv21FDS.getPosition().getX().getField();
        final FieldVector3D<FieldUnivariateDerivative2<T>>      translationFDS = FieldVector3D.getPlusI(fieldUD).scalarMultiply(pv21FDS.getPosition().getNorm().multiply(mu)).negate();

        final FieldRotation<FieldUnivariateDerivative2<T>> rotationFDS = new FieldRotation<>(pv21FDS.getPosition(), pv21FDS.getMomentum(),
                                                                                             FieldVector3D.getPlusI(fieldUD),
                                                                                             FieldVector3D.getPlusK(fieldUD));
        final FieldUnivariateDerivative2<T>[] rotationRates = rotationFDS.getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        final FieldVector3D<T> rotationRate = new FieldVector3D<>(rotationRates[0].getPartialDerivative(1),      rotationRates[1].getPartialDerivative(1),      rotationRates[2].getPartialDerivative(1));
        final FieldVector3D<T> rotationAcc  = new FieldVector3D<>(rotationRates[0].getPartialDerivative(2),      rotationRates[1].getPartialDerivative(2),      rotationRates[2].getPartialDerivative(2));
        final FieldVector3D<T> velocity     = new FieldVector3D<>(translationFDS.getX().getPartialDerivative(1), translationFDS.getY().getPartialDerivative(1), translationFDS.getZ().getPartialDerivative(1));
        final FieldVector3D<T> acceleration = new FieldVector3D<>(translationFDS.getX().getPartialDerivative(2), translationFDS.getY().getPartialDerivative(2), translationFDS.getZ().getPartialDerivative(2));

        final FieldTransform<T> transform1 = new FieldTransform<>(date, translationField, velocity, acceleration);
        final FieldTransform<T> transform2 = new FieldTransform<>(date, rotationField, rotationRate, rotationAcc);
        return new FieldTransform<>(date, transform2, transform1);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final TimeStampedFieldPVCoordinates<T> pv = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<T> translation = FieldVector3D.getPlusI(field).
                scalarMultiply(pv.getPosition().getNorm().multiply(mu)).negate();

        final FieldRotation<T> rotation = new FieldRotation<>(
                pv.getPosition(), pv.getMomentum(),
                FieldVector3D.getPlusI(field), FieldVector3D.getMinusK(field));

        final FieldStaticTransform<T> transform1 = FieldStaticTransform.of(date, translation);
        final FieldStaticTransform<T> transform2 = FieldStaticTransform.of(date, rotation);
        return FieldStaticTransform.compose(date, transform2, transform1);
    }

}
