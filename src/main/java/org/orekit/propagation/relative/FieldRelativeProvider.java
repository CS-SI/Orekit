/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldAdditionalDataProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Interface for Field Relative provider.
 * <p>
 * A field relative provider is an interface extending FieldAdditionalDataProvider to provide the relative state of a
 * chaser S/C in regard to a target S/C as a FieldAdditionalData. The target S/C is propagated with the "main"
 * propagator where the additional data is added.
 * </p>
 *
 * @param <T> Any scalar field.
 * @author Romain Cuvillon
 * @since 14.0
 */
public interface FieldRelativeProvider<T extends CalculusFieldElement<T>> extends FieldAdditionalDataProvider<T[], T> {

    /**
     * Returns the LOF type used by the provider.
     * @return the LOF used by the provider.
     */
    LOF getLof();

    /**
     * Get the initial TimeStampedPVCoordinates of the chaser in the Local Orbital Frame of the target.
     *
     * @return initial TimeStampedPVCoordinates of the chaser in target's LOF
     */
    TimeStampedFieldPVCoordinates<T> getInitialChaserPVTLof();

    /**
     * Set the initial TimeStampedPVCoordinates of the chaser in the Local Orbital Frame of the target.
     *
     * @param initialChaserPVTLof initial TimeStampedPVCoordinates of the chaser in target's LOF
     */
    void setInitialChaserPVTLof(TimeStampedFieldPVCoordinates<T> initialChaserPVTLof);

    /**
     * Extracts the chaser's PVT in the target LOF from the given target {@link FieldSpacecraftState}.
     *
     * @param targetState target FieldSpacecraftState
     * @return chaser's TimeStampedPVCoordinates in target's LOF
     */
    TimeStampedFieldPVCoordinates<T> extractChaserPVT(FieldSpacecraftState<T> targetState);

    /**
     * Extracts the chaser's PVT from the given target {@link FieldSpacecraftState} and converts it to the desired
     * output frame.
     *
     * @param targetState target FieldSpacecraftState
     * @param outputFrame desired frame in which to extract chaser's TimeStampedPVCoordinates
     * @return TimeStampedPVCoordinates in desired frame
     */
    default TimeStampedFieldPVCoordinates<T> extractChaserPVT(final FieldSpacecraftState<T> targetState,
                                                              final Frame outputFrame) {

        // Extract chaser PVT in target's LOF
        final TimeStampedFieldPVCoordinates<T> chaserPVTLOF = extractChaserPVT(targetState);

        // Transform PVT from target's LOF to reference inertial frame
        final FieldTransform<T> lofToInertial =
                        getLof().transformFromInertial(targetState.getDate(), targetState.getPVCoordinates())
                                .getInverse();
        final TimeStampedFieldPVCoordinates<T> pvInertial = lofToInertial.transformPVCoordinates(chaserPVTLOF);

        // Transform PVT from reference inertial frame to desired output frame.
        final FieldTransform<T> inertialToOutputFrame =
                        targetState.getFrame().getTransformTo(outputFrame, targetState.getDate());
        return inertialToOutputFrame.transformPVCoordinates(pvInertial);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Get the chaser state relative to a target, in target's QSW LOF.
     * </p>
     *
     * @param spacecraftState spacecraft state to which additional data should correspond
     * @return chaser cartesian state in target's QSW Local Orbital Frame
     */
    @Override
    default T[] getAdditionalData(final FieldSpacecraftState<T> spacecraftState) {

        // Compute PVT of chaser at S/C date
        final TimeStampedFieldPVCoordinates<T> pvt = extractChaserPVT(spacecraftState);

        // Transform to T[] to comply with AdditionalDataProvider interface.
        final T[] array = MathArrays.buildArray(spacecraftState.getAdditionalDataValues().getField(), 6);
        array[0] = pvt.getPosition().getX();
        array[1] = pvt.getPosition().getY();
        array[2] = pvt.getPosition().getZ();
        array[3] = pvt.getVelocity().getX();
        array[4] = pvt.getVelocity().getY();
        array[5] = pvt.getVelocity().getZ();

        return array;
    }

    /**
     * Get the target Orbit.
     *
     * @return orbit of the target
     */
    FieldOrbit<T> getTargetOrbit();

    /**
     * Set the target orbit.
     *
     * @param targetOrbit orbit of the target
     */
    void setTargetOrbit(FieldOrbit<T> targetOrbit);

    /**
     * CW doesn't use true anomaly, so default is a no-op Set the true anomaly of the target.
     *
     * @param trueAnomaly true anomaly of the target
     */
    default void setTargetTrueAnomaly(final T trueAnomaly) {
    }
}
