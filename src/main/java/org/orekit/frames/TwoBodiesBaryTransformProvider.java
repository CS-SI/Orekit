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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/** Transform provider for the inertial Barycentered frame.
 * @author Vincent Mouraux
 * @since 10.2
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
        final PVCoordinates pv21 = secondaryBody.getPVCoordinates(date, frame);
        final double massRatio = secondaryBody.getGM() / (primaryBody.getGM() + secondaryBody.getGM());
        final Vector3D translation = pv21.getPosition().scalarMultiply(massRatio).negate();
        return new Transform(date, translation);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final double massRatio = secondaryBody.getGM() / (primaryBody.getGM() + secondaryBody.getGM());
        final FieldVector3D<T> translation = pv21.getPosition().scalarMultiply(massRatio).negate();
        return new FieldTransform<>(date, translation);
    }
}
