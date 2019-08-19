/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.frames;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/** Transform provider for the rotating frame of the CR3BP System.
 * @author Vincent Mouraux
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
        final FieldPVCoordinates<DerivativeStructure> pv21        = secondaryBody.getPVCoordinates(date, frame).toDerivativeStructurePV(2);
        Field<DerivativeStructure> field = pv21.getPosition().getX().getField();
        final FieldVector3D<DerivativeStructure>     translation = FieldVector3D.getPlusI(field).scalarMultiply(pv21.getPosition().getNorm().multiply(mu)).negate();

        final FieldRotation<DerivativeStructure> rotation = new FieldRotation<>(pv21.getPosition(), pv21.getMomentum(),
                        FieldVector3D.getPlusI(field),
                        FieldVector3D.getPlusK(field));
        final DerivativeStructure[] rotationRates = rotation.getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        final Vector3D rotationRate = new Vector3D(rotationRates[0].getPartialDerivative(1), rotationRates[1].getPartialDerivative(1), rotationRates[2].getPartialDerivative(1));
        final Vector3D rotationAcc = new Vector3D(rotationRates[0].getPartialDerivative(2), rotationRates[1].getPartialDerivative(2), rotationRates[2].getPartialDerivative(2));
        final Vector3D velocity = new Vector3D(translation.getX().getPartialDerivative(1), translation.getY().getPartialDerivative(1), translation.getZ().getPartialDerivative(1));
        final Vector3D acceleration =  new Vector3D(translation.getX().getPartialDerivative(2), translation.getY().getPartialDerivative(2), translation.getZ().getPartialDerivative(2));

        final Transform transform1 = new Transform(date, translation.toVector3D(), velocity, acceleration);
        final Transform transform2 = new Transform(date, rotation.toRotation(), rotationRate, rotationAcc);
        return new Transform(date, transform2, transform1);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final Field<T>              field       = pv21.getPosition().getX().getField();
        final FieldVector3D<T>      translation = FieldVector3D.getPlusI(field).scalarMultiply(pv21.getPosition().getNorm().multiply(mu)).negate();
        final FieldRotation<T>      rotation    = new FieldRotation<>(pv21.getPosition(), pv21.getMomentum(),
                        FieldVector3D.getPlusI(field),
                        FieldVector3D.getPlusK(field));
        //        final FieldPVCoordinates<FieldDerivativeStructure<T>> pv21f = pv21.toDerivativeStructurePV(1);
        //        final Field<FieldDerivativeStructure<T>> fieldf =
        //            pv21f.getPosition().getX().getField();

        //        final FieldRotation<FieldDerivativeStructure<T>> FieldRotation =
        //            new FieldRotation<>(pv21f.getPosition(), pv21f.getMomentum(),
        //                                FieldVector3D.getPlusI(fieldf),
        //                                FieldVector3D.getPlusK(fieldf));

        //        final FieldDerivativeStructure<T>[] rotationRates = FieldRotation.getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        //        final FieldVector3D<T> rotationRate = new FieldVector3D<>(rotationRates[0].getPartialDerivative(1), rotationRates[1].getPartialDerivative(1), rotationRates[2].getPartialDerivative(1));
        //        final FieldVector3D<T> rotationAcc = new FieldVector3D<>(rotationRates[0].getPartialDerivative(2), rotationRates[1].getPartialDerivative(2), rotationRates[2].getPartialDerivative(2));
        //        final FieldVector3D<T> velocity = new FieldVector3D<>(pv21f.getPosition().getX().getPartialDerivative(1), pv21f.getPosition().getY().getPartialDerivative(1), pv21f.getPosition().getZ().getPartialDerivative(1));
        //        final FieldVector3D<T> acceleration =  new FieldVector3D<>(pv21f.getPosition().getX().getPartialDerivative(2), pv21f.getPosition().getY().getPartialDerivative(2), pv21f.getPosition().getZ().getPartialDerivative(2));
        return new FieldTransform<T>(date,
                        new FieldTransform<>(date, translation),
                        new FieldTransform<>(date, rotation));
    }
}
