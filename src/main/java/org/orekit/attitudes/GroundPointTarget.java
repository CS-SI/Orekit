/* Copyright 2002-2024 Luc Maisonobe
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.HashMap;
import java.util.Map;

/**
 * Ground point target for {@link AlignedAndConstrained}.
 * @author Luc Maisonobe
 * @since 12.2
 */
public class GroundPointTarget implements TargetProvider
{

    /** Location of the target in Earth frame. */
    private final PVCoordinates location;

    /** Cached field-based locations. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldPVCoordinates<?>>
        cachedLocations;

    /** Simple constructor.
     * @param location location of the target in Earth frame
     */
    public GroundPointTarget(final Vector3D location)
    {
        this.location        = new PVCoordinates(location, Vector3D.ZERO, Vector3D.ZERO);
        this.cachedLocations = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<UnivariateDerivative2> getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                                                                   final OneAxisEllipsoid earth,
                                                                   final TimeStampedPVCoordinates pv,
                                                                   final Frame frame) {
        final Transform earthToInert = earth.getFrame().getTransformTo(frame, pv.getDate());
        return new PVCoordinates(pv, earthToInert.transformPVCoordinates(location)).
               toUnivariateDerivative2Vector().
               normalize();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>>
    getTargetDirection(final ExtendedPVCoordinatesProvider sun,
                       final OneAxisEllipsoid earth,
                       final TimeStampedFieldPVCoordinates<T> pv,
                       final Frame frame) {
        final Field<T> field = pv.getDate().getField();

        // get the target location for specified field
        @SuppressWarnings("unchecked")
        final FieldPVCoordinates<T> l =
            (FieldPVCoordinates<T>) cachedLocations.computeIfAbsent(field, f -> {
                final T zero = field.getZero();
                return new FieldPVCoordinates<>(new FieldVector3D<>(zero.newInstance(location.getPosition().getX()),
                                                                    zero.newInstance(location.getPosition().getY()),
                                                                    zero.newInstance(location.getPosition().getZ())),
                                                FieldVector3D.getZero(field),
                                                FieldVector3D.getZero(field));
            });

        final FieldTransform<T> earthToInert = earth.getFrame().getTransformTo(frame, pv.getDate());
        return new FieldPVCoordinates<>(pv, earthToInert.transformPVCoordinates(l)).
               toUnivariateDerivative2Vector().
               normalize();

    }

}
