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
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Provider for target vector.
 * @author Luc Maisonobe
 * @since 12.2
 */
public interface TargetProvider
{
    /**
     * Create a new provider with direction flipped w.r.t. original.
     * @param targetProvider original provider
     * @return provider with flipped directions
     * @since 13.0
     */
    static TargetProvider negate(final TargetProvider targetProvider) {
        return new TargetProvider() {
            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                                                                  final OneAxisEllipsoid earth,
                                                                                                                                  final TimeStampedFieldPVCoordinates<T> pv,
                                                                                                                                  final Frame frame) {
                return targetProvider.getDerivative2TargetDirection(sun, earth, pv, frame).negate();
            }

            @Override
            public FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(final ExtendedPositionProvider sun,
                                                                                      final OneAxisEllipsoid earth,
                                                                                      final TimeStampedPVCoordinates pv,
                                                                                      final Frame frame) {
                return targetProvider.getDerivative2TargetDirection(sun, earth, pv, frame).negate();
            }

            @Override
            public Vector3D getTargetDirection(final ExtendedPositionProvider sun, final OneAxisEllipsoid earth,
                                               final TimeStampedPVCoordinates pv, final Frame frame) {
                return targetProvider.getTargetDirection(sun, earth, pv, frame).negate();
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(final ExtendedPositionProvider sun,
                                                                                           final OneAxisEllipsoid earth,
                                                                                           final TimeStampedFieldPVCoordinates<T> pv,
                                                                                           final Frame frame) {
                return targetProvider.getTargetDirection(sun, earth, pv, frame).negate();
            }
        };
    }

    /**
     * Get a target vector.
     * @param sun   Sun model
     * @param earth Earth model
     * @param pv    spacecraft position and velocity
     * @param frame inertial frame
     * @return target direction in the spacecraft state frame, with second order time derivative
     */
    default FieldVector3D<UnivariateDerivative2> getDerivative2TargetDirection(ExtendedPositionProvider sun,
                                                                               OneAxisEllipsoid earth,
                                                                               TimeStampedPVCoordinates pv,
                                                                               Frame frame) {
        final FieldPVCoordinates<UnivariateDerivative2> ud2PV = pv.toUnivariateDerivative2PV();
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final UnivariateDerivative2 dt = new UnivariateDerivative2(0., 1., 0.);
        final FieldAbsoluteDate<UnivariateDerivative2> ud2Date = new FieldAbsoluteDate<>(field, pv.getDate()).shiftedBy(dt);
        return getTargetDirection(sun, earth, new TimeStampedFieldPVCoordinates<>(ud2Date, ud2PV), frame);
    }

    /**
     * Get a target vector.
     * @param sun   Sun model
     * @param earth Earth model
     * @param pv    spacecraft position and velocity
     * @param frame inertial frame
     * @return target direction in the spacecraft state frame
     */
    default Vector3D getTargetDirection(ExtendedPositionProvider sun, OneAxisEllipsoid earth,
                                        TimeStampedPVCoordinates pv, Frame frame) {
        return getDerivative2TargetDirection(sun, earth, pv, frame).toVector3D();
    }

    /**
     * Get a target vector.
     * @param <T>   type of the field element
     * @param sun   Sun model
     * @param earth Earth model
     * @param pv    spacecraft position and velocity
     * @param frame inertial frame
     * @return target direction in the spacecraft state frame, with second order time derivative
     */
    default <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>> getDerivative2TargetDirection(ExtendedPositionProvider sun,
                                                                                                                           OneAxisEllipsoid earth,
                                                                                                                           TimeStampedFieldPVCoordinates<T> pv,
                                                                                                                           Frame frame) {
        final FieldPVCoordinates<FieldUnivariateDerivative2<T>> ud2PV = pv.toUnivariateDerivative2PV();
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> ud2Date = pv.getDate().toFUD2Field();
        return getTargetDirection(sun, earth, new TimeStampedFieldPVCoordinates<>(ud2Date, ud2PV), frame);
    }

    /**
     * Get a target vector.
     * @param <T>   type of the field element
     * @param sun   Sun model
     * @param earth Earth model
     * @param pv    spacecraft position and velocity
     * @param frame inertial frame
     * @return target direction in the spacecraft state frame
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetDirection(ExtendedPositionProvider sun,
                                                                            OneAxisEllipsoid earth,
                                                                            TimeStampedFieldPVCoordinates<T> pv,
                                                                            Frame frame);
}
