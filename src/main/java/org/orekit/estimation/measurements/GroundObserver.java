/* Copyright 2022-2026 Romain Serra
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.estimation.measurements;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

/** Interface for ground-based observers, for which atmospheric corrections may be applied.
 *
 * @author Romain Serra
 * @since 14.0
 */
public interface GroundObserver extends Observer {

    /**
     * Get the parent shape of the ground observer.
     * @return shape
     */
    BodyShape getParentShape();

    /**
     * Get the offset geodetic point at the given date.
     * @param date date
     * @return geodetic point
     */
    GeodeticPoint getOffsetGeodeticPoint(AbsoluteDate date);

    /**
     * Get the offset geodetic point at the given date (Field).
     * @param date date
     * @return geodetic point
     * @param <T> type of the field elements
     */
    <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> getOffsetGeodeticPoint(FieldAbsoluteDate<T> date);

    /**
     * Get the tracking coordinates (elevation, azimuth and altitude) of a satellite at the given state.
     * @param state spacecraft state
     * @return tracking coordinates
     */
    default TrackingCoordinates getTrackingCoordinates(final SpacecraftState state) {
        final TopocentricFrame topocentricFrame = getTopocentricFrame(state.getDate());
        return topocentricFrame.getTrackingCoordinates(state.getPosition(), state.getFrame(), state.getDate());
    }

    /**
     * Get the tracking coordinates (elevation, azimuth and altitude) of a satellite at the given state (Field).
     * @param state spacecraft state
     * @return tracking coordinates
     * @param <T> type of the field elements
     */
    default <T extends CalculusFieldElement<T>> FieldTrackingCoordinates<T> getTrackingCoordinates(final FieldSpacecraftState<T> state) {
        final TopocentricFrame topocentricFrame = getTopocentricFrame(state.getDate().toAbsoluteDate());
        return topocentricFrame.getTrackingCoordinates(state.getPosition(), state.getFrame(), state.getDate());
    }

    private TopocentricFrame getTopocentricFrame(final AbsoluteDate date) {
        return new TopocentricFrame(getParentShape(), getOffsetGeodeticPoint(date), "offset");
    }
}
