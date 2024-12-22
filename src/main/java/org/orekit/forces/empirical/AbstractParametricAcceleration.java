/* Copyright 2022-2024 Romain Serra
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
package org.orekit.forces.empirical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

/**
 * Abstract class for parametric acceleration.
 *
 * @since 13.0
 * @author Romain Serra
 */
public abstract class AbstractParametricAcceleration implements ForceModel {

    /** Direction of the acceleration in defining frame. */
    private final Vector3D direction;

    /** Flag for inertial acceleration direction. */
    private final boolean isInertial;

    /** The attitude to override, if set. */
    private final AttitudeProvider attitudeOverride;

    protected AbstractParametricAcceleration(final Vector3D direction, final boolean isInertial,
                                             final AttitudeProvider attitudeOverride) {
        this.direction = direction;
        this.isInertial = isInertial;
        this.attitudeOverride = attitudeOverride;
    }

    /**
     * Getter for attitude override.
     * @return attitude override
     */
    public AttitudeProvider getAttitudeOverride() {
        return attitudeOverride;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return isInertial;
    }

    /**
     * Computes the acceleration's direction in the propagation frame.
     * @param state state
     * @return direction
     */
    protected Vector3D getAccelerationDirection(final SpacecraftState state) {
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            return direction;
        } else {
            final Rotation rotation;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                rotation = state.getAttitude().getRotation();
            } else {
                // the acceleration direction is defined in a dedicated frame
                rotation = attitudeOverride.getAttitudeRotation(state.isOrbitDefined() ? state.getOrbit() : state.getAbsPVA(),
                        state.getDate(), state.getFrame());
            }
            return rotation.applyInverseTo(direction);
        }
    }

    /**
     * Computes the acceleration's direction in the propagation frame.
     * @param state state
     * @param <T> field type
     * @return direction
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getAccelerationDirection(final FieldSpacecraftState<T> state) {
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            return new FieldVector3D<>(state.getDate().getField(), direction);
        } else {
            final FieldRotation<T> rotation;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                rotation = state.getAttitude().getRotation();
            } else {
                // the acceleration direction is defined in a dedicated frame
                rotation = attitudeOverride.getAttitudeRotation(state.getOrbit(), state.getDate(), state.getFrame());
            }
            return rotation.applyInverseTo(direction);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return attitudeOverride == null ? Stream.of() : attitudeOverride.getEventDetectors();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return attitudeOverride == null ? Stream.of() : attitudeOverride.getFieldEventDetectors(field);
    }
}
