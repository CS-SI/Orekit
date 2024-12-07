/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.PVCoordinatesProvider;

import java.util.List;
import java.util.stream.Stream;

/** This interface represents an attitude provider that modifies/wraps another underlying provider.
 * @author Luc Maisonobe
 * @since 5.1
 */
public interface AttitudeProviderModifier extends AttitudeProvider {

    /** Get the underlying attitude provider.
     * @return underlying attitude provider
     */
    AttitudeProvider getUnderlyingAttitudeProvider();

    /** {@inheritDoc} */
    @Override
    default Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        return getUnderlyingAttitudeProvider().getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    default <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                             final FieldAbsoluteDate<T> date,
                                                                             final Frame frame) {
        return getUnderlyingAttitudeProvider().getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    default Stream<EventDetector> getEventDetectors(final List<ParameterDriver> parameterDrivers) {
        return getUnderlyingAttitudeProvider().getEventDetectors(parameterDrivers);
    }

    /** {@inheritDoc} */
    @Override
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field,
                                                                                                     final List<ParameterDriver> parameterDrivers) {
        return getUnderlyingAttitudeProvider().getFieldEventDetectors(field, parameterDrivers);
    }

    /** {@inheritDoc} */
    @Override
    default List<ParameterDriver> getParametersDrivers() {
        return getUnderlyingAttitudeProvider().getParametersDrivers();
    }

    /**
     * Wrap the input provider with a new one always returning attitudes with zero rotation rate and acceleration.
     * It is not physically sound, but remains useful for performance when a full, physical attitude with time derivatives is not needed.
     * @param attitudeProvider provider to wrap
     * @return wrapping provider
     * @since 12.1
     */
    static AttitudeProviderModifier getFrozenAttitudeProvider(final AttitudeProvider attitudeProvider) {
        return new AttitudeProviderModifier() {
            @Override
            public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
                final Rotation rotation = getAttitudeRotation(pvProv, date, frame);
                final AngularCoordinates angularCoordinates = new AngularCoordinates(rotation, Vector3D.ZERO);
                return new Attitude(date, frame, angularCoordinates);
            }

            @Override
            public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
                return attitudeProvider.getAttitudeRotation(pvProv, date, frame);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv, final FieldAbsoluteDate<T> date, final Frame frame) {
                final FieldRotation<T> rotation = getAttitudeRotation(pvProv, date, frame);
                final FieldAngularCoordinates<T> angularCoordinates = new FieldAngularCoordinates<>(rotation, FieldVector3D.getZero(date.getField()));
                return new FieldAttitude<>(date, frame, angularCoordinates);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                            final FieldAbsoluteDate<T> date,
                                                                                            final Frame frame) {
                return attitudeProvider.getAttitudeRotation(pvProv, date, frame);
            }

            @Override
            public AttitudeProvider getUnderlyingAttitudeProvider() {
                return attitudeProvider;
            }
        };
    }
}
