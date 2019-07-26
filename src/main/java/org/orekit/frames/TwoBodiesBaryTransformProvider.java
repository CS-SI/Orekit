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

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/** Transform provider for the inertial Barycentered frame.
 * @author Vincent Mouraux
 */
class TwoBodiesBaryTransformProvider implements TransformProvider {

    /** Serializable UID.*/
    private static final long serialVersionUID = 20190726L;

    /** Frame for results. Always defined as primaryBody's inertially oriented frame.*/
    private final Frame frame;

    /** Celestial body with bigger mass, m1. */
    private final CelestialBody primaryBody;

    /** Celestial body with smaller mass, m2. */
    private final CelestialBody secondaryBody;


    /** Simple constructor.
     * @param primaryBody Primary body.
     * @param secondaryBody Secondary body.
     */
    TwoBodiesBaryTransformProvider(final CelestialBody primaryBody, final CelestialBody secondaryBody) {
        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;
        this.frame = primaryBody.getInertiallyOrientedFrame();
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final FieldPVCoordinates<DerivativeStructure> pv21        = secondaryBody.getPVCoordinates(date, frame).toDerivativeStructurePV(2);
        final FieldVector3D<DerivativeStructure>      translation = getBary(pv21.getPosition()).negate();
        final Vector3D velocity = new Vector3D(translation.getX().getPartialDerivative(1), translation.getY().getPartialDerivative(1), translation.getZ().getPartialDerivative(1));
        final Vector3D acceleration =  new Vector3D(translation.getX().getPartialDerivative(2), translation.getY().getPartialDerivative(2), translation.getZ().getPartialDerivative(2));
        return new Transform(date, translation.toVector3D(), velocity, acceleration);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<T>      translation = getBary(pv21.getPosition()).negate();
        final FieldPVCoordinates<FieldDerivativeStructure<T>> pv21f = pv21.toDerivativeStructurePV(1);
        final FieldVector3D<T> velocity = new FieldVector3D<>(pv21f.getPosition().getX().getPartialDerivative(1), pv21f.getPosition().getY().getPartialDerivative(1), pv21f.getPosition().getZ().getPartialDerivative(1));
        final FieldVector3D<T> acceleration =  new FieldVector3D<>(pv21f.getPosition().getX().getPartialDerivative(2), pv21f.getPosition().getY().getPartialDerivative(2), pv21f.getPosition().getZ().getPartialDerivative(2));
        return new FieldTransform<>(date, translation, velocity, acceleration);
    }


    /** Compute the coordinates of the barycenter.
     * @param <T> type of the field elements
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the barycenter given in frame: primaryBody.getInertiallyOrientedFrame()
     */
    private <T extends RealFieldElement<T>> FieldVector3D<T>
        getBary(final FieldVector3D<T> primaryToSecondary) {
        // Barycenter point is built
        final double massRatio = secondaryBody.getGM() / (primaryBody.getGM() + secondaryBody.getGM());
        final T barycenter = primaryToSecondary.getNorm().multiply(massRatio);
        final FieldVector3D<T> normalized = primaryToSecondary.normalize();
        return new FieldVector3D<>(barycenter, normalized);

    }
}
