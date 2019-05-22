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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

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

    /** barycenter of the system.*/
    private final double barycenter;


    /** Simple constructor.
     * @param distance distance between the two bodies
     * @param barycenter CR3BP Barycenter.
     * @param primaryBody Primary body.
     * @param secondaryBody Secondary body.
     */
    CR3BPRotatingTransformProvider(final double distance, final double barycenter, final CelestialBody primaryBody, final CelestialBody secondaryBody) {
        this.secondaryBody = secondaryBody;
        this.frame = primaryBody.getInertiallyOrientedFrame();
        this.barycenter = barycenter;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final PVCoordinates pv21        = secondaryBody.getPVCoordinates(date, frame);
        final Vector3D      translation = getBary(pv21.getPosition()).negate();
        final Rotation      rotation    = new Rotation(pv21.getPosition(), pv21.getMomentum(),
                                                       Vector3D.PLUS_I, Vector3D.PLUS_K);
        return new Transform(date, new Transform(date, translation), new Transform(date, rotation));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<T>      translation = getBary(pv21.getPosition()).negate();
        final Field<T>              field       = pv21.getPosition().getX().getField();
        final FieldRotation<T>      rotation    = new FieldRotation<>(pv21.getPosition(), pv21.getMomentum(),
                                                                      FieldVector3D.getPlusI(field),
                                                                      FieldVector3D.getPlusK(field));
        return new FieldTransform<T>(date,
                                     new FieldTransform<>(date, translation),
                                     new FieldTransform<>(date, rotation));
    }

    /** Compute the coordinates of the barycenter.
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the barycenter given in frame: primaryBody.getInertiallyOrientedFrame()
     */
    private Vector3D getBary(final Vector3D primaryToSecondary) {
        final Vector3D normalized = primaryToSecondary.normalize();
        return new Vector3D(barycenter, normalized);
    }

    /** Compute the coordinates of the barycenter.
     * @param <T> type of the field elements
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the barycenter given in frame: primaryBody.getInertiallyOrientedFrame()
     */
    private <T extends RealFieldElement<T>> FieldVector3D<T>
        getBary(final FieldVector3D<T> primaryToSecondary) {
        // Barycenter point is built
        final FieldVector3D<T> normalized = primaryToSecondary.normalize();
        return new FieldVector3D<>(barycenter, normalized);

    }
}
