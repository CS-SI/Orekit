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
package org.orekit.forces.radiation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;

/**
 * Abstract class for light flux models.
 * Via the definition of the lighting ratio and the unocculted flux vector, derives the final value.
 *
 * @author Romain Serra
 * @see LightFluxModel
 * @since 12.1
 */
public abstract class AbstractLightFluxModel implements LightFluxModel {

    /** Direction provider for the occulted body e.g. the Sun. */
    private final ExtendedPVCoordinatesProvider occultedBody;

    /**
     * Constructor.
     * @param occultedBody position provider for light source
     */
    protected AbstractLightFluxModel(final ExtendedPVCoordinatesProvider occultedBody) {
        this.occultedBody = occultedBody;
    }

    /**
     * Getter for the occulted body's position provider.
     * @return occulted body
     */
    public ExtendedPVCoordinatesProvider getOccultedBody() {
        return occultedBody;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getLightFluxVector(final SpacecraftState state) {
        final Vector3D position = state.getPosition();
        final Vector3D lightSourcePosition = getOccultedBodyPosition(state.getDate(), state.getFrame());
        final double lightingRatio = getLightingRatio(position, lightSourcePosition);
        if (lightingRatio != 0.) {
            final Vector3D relativePosition = position.subtract(lightSourcePosition);
            final Vector3D unoccultedFlux = getUnoccultedFluxVector(relativePosition);
            return unoccultedFlux.scalarMultiply(lightingRatio);
        } else {
            return Vector3D.ZERO;
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getLightFluxVector(final FieldSpacecraftState<T> state) {
        final FieldVector3D<T> position = state.getPosition();
        final FieldVector3D<T> lightSourcePosition = getOccultedBodyPosition(state.getDate(), state.getFrame());
        final T lightingRatio = getLightingRatio(position, lightSourcePosition);
        final FieldVector3D<T> relativePosition = position.subtract(lightSourcePosition);
        final FieldVector3D<T> unoccultedFlux = getUnoccultedFluxVector(relativePosition);
        return unoccultedFlux.scalarMultiply(lightingRatio);
    }

    /**
     * Method computing the occulted body's position at a given date and frame.
     * @param date date
     * @param frame frame
     * @return position
     */
    protected Vector3D getOccultedBodyPosition(final AbsoluteDate date, final Frame frame) {
        return occultedBody.getPosition(date, frame);
    }

    /**
     * Method computing the occulted body's position at a given date and frame. Field version.
     * @param date date
     * @param frame frame
     * @param <T> field type
     * @return position
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getOccultedBodyPosition(final FieldAbsoluteDate<T> date,
                                                                                           final Frame frame) {
        return occultedBody.getPosition(date, frame);
    }

    /** Get the light flux vector, not considering any shadowing effect.
     * @param relativePosition relative position w.r.t. light source
     * @return unocculted flux
     */
    protected abstract Vector3D getUnoccultedFluxVector(Vector3D relativePosition);

    /** Get the light flux vector, not considering any shadowing effect. Field version.
     * @param relativePosition relative position w.r.t. light source
     * @param <T> field type
     * @return unocculted flux
     */
    protected abstract <T extends CalculusFieldElement<T>> FieldVector3D<T> getUnoccultedFluxVector(FieldVector3D<T> relativePosition);

    /** Get the lighting ratio ([0-1]).
     * @param position object's position
     * @param occultedBodyPosition occulted body position in same frame
     * @return lighting ratio
     */
    protected abstract double getLightingRatio(Vector3D position, Vector3D occultedBodyPosition);

    /** Get the lighting ratio ([0-1]). Field version.
     * @param position object's position
     * @param occultedBodyPosition occulted body position in same frame
     * @param <T> field type
     * @return lighting ratio
     */
    protected abstract <T extends CalculusFieldElement<T>> T getLightingRatio(FieldVector3D<T> position,
                                                                              FieldVector3D<T> occultedBodyPosition);

}
