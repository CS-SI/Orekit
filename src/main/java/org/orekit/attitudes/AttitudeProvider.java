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
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectorsProvider;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/** This interface represents an attitude provider model set.
 * <p>An attitude provider provides a way to compute an {@link Attitude Attitude}
 * from an date and position-velocity local provider.</p>
 * @author V&eacute;ronique Pommier-Maurussane
 */
public interface AttitudeProvider extends EventDetectorsProvider, AttitudeRotationModel {

    /** Compute the attitude corresponding to an orbital state.
     * @param pvProv local position-velocity provider around current date
     * @param date current date
     * @param frame reference frame from which attitude is computed
     * @return attitude on the specified date and position-velocity state
     */
    Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame);

    /** Compute the attitude corresponding to an orbital state.
     * @param pvProv local position-velocity provider around current date
     * @param date current date
     * @param frame reference frame from which attitude is computed
     * @param <T> type of the field elements
     * @return attitude on the specified date and position-velocity state
     * @since 9.0
     */
    <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv,
                                                                     FieldAbsoluteDate<T> date,
                                                                     Frame frame);

    /** Compute the attitude-related rotation corresponding to an orbital state.
     * @param pvProv local position-velocity provider around current date
     * @param date current date
     * @param frame reference frame from which attitude is computed
     * @return attitude-related rotation on the specified date and position-velocity state
     * @since 12.0
     */
    default Rotation getAttitudeRotation(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
        return getAttitude(pvProv, date, frame).getRotation();
    }

    /** Compute the attitude-related rotation corresponding to an orbital state.
     * @param pvProv local position-velocity provider around current date
     * @param date current date
     * @param frame reference frame from which attitude is computed
     * @param <T> type of the field elements
     * @return rotation on the specified date and position-velocity state
     * @since 12.0
     */
    default <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(FieldPVCoordinatesProvider<T> pvProv,
                                                                                     FieldAbsoluteDate<T> date,
                                                                                     Frame frame) {
        return getAttitude(pvProv, date, frame).getRotation();
    }

    /** {@inheritDoc}
     * The default implementation is independent of the input parameters as by default there is no driver.
     * Users wanting to use them must override this.
     * @since 13.0
     */
    @Override
    default Rotation getAttitudeRotation(final SpacecraftState state, final double[] parameters) {
        return getAttitudeRotation(state.isOrbitDefined() ? state.getOrbit() : state.getAbsPVA(), state.getDate(),
                state.getFrame());
    }

    /** {@inheritDoc}
     * The default implementation is independent of the input parameters as by default there is no driver.
     * Users wanting to use them must override this.
     * @since 13.0
     */
    @Override
    default <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldSpacecraftState<T> state,
                                                                                     final T[] parameters) {
        return getAttitudeRotation(state.isOrbitDefined() ? state.getOrbit() : state.getAbsPVA(), state.getDate(),
                state.getFrame());
    }

    /** {@inheritDoc}
     * @since 13.0
     */
    @Override
    default Stream<EventDetector> getEventDetectors() {
        return getEventDetectors(getParametersDrivers());
    }

    /** {@inheritDoc}
     * @since 13.0
     */
    @Override
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return getFieldEventDetectors(field, getParametersDrivers());
    }

    /** {@inheritDoc}
     * @since 13.0
     */
    @Override
    default List<ParameterDriver> getParametersDrivers() {
        return new ArrayList<>();
    }
}
