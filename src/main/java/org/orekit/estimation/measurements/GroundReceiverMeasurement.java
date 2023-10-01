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
package org.orekit.estimation.measurements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Base class modeling a measurement where receiver is a ground station.
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 12.0
 * @param <T> type of the measurement
 */
public abstract class GroundReceiverMeasurement<T extends GroundReceiverMeasurement<T>> extends AbstractMeasurement<T> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    public GroundReceiverMeasurement(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                                     final double observed, final double sigma, final double baseWeight,
                                     final ObservableSatellite satellite) {
        super(date, observed, sigma, baseWeight, Collections.singletonList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getClockDriftDriver());
        addParameterDriver(station.getEastOffsetDriver());
        addParameterDriver(station.getNorthOffsetDriver());
        addParameterDriver(station.getZenithOffsetDriver());
        addParameterDriver(station.getPrimeMeridianOffsetDriver());
        addParameterDriver(station.getPrimeMeridianDriftDriver());
        addParameterDriver(station.getPolarOffsetXDriver());
        addParameterDriver(station.getPolarDriftXDriver());
        addParameterDriver(station.getPolarOffsetYDriver());
        addParameterDriver(station.getPolarDriftYDriver());
        if (!twoWay) {
            // for one way measurements, the satellite clock offset affects the measurement
            addParameterDriver(satellite.getClockOffsetDriver());
            addParameterDriver(satellite.getClockDriftDriver());
        }
        this.station = station;
        this.twoway  = twoWay;
    }

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    public GroundReceiverMeasurement(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                                     final double[] observed, final double[] sigma, final double[] baseWeight,
                                     final ObservableSatellite satellite) {
        super(date, observed, sigma, baseWeight, Collections.singletonList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getClockDriftDriver());
        addParameterDriver(station.getEastOffsetDriver());
        addParameterDriver(station.getNorthOffsetDriver());
        addParameterDriver(station.getZenithOffsetDriver());
        addParameterDriver(station.getPrimeMeridianOffsetDriver());
        addParameterDriver(station.getPrimeMeridianDriftDriver());
        addParameterDriver(station.getPolarOffsetXDriver());
        addParameterDriver(station.getPolarDriftXDriver());
        addParameterDriver(station.getPolarOffsetYDriver());
        addParameterDriver(station.getPolarDriftYDriver());
        if (!twoWay) {
            // for one way measurements, the satellite clock offset affects the measurement
            addParameterDriver(satellite.getClockOffsetDriver());
            addParameterDriver(satellite.getClockDriftDriver());
        }
        this.station = station;
        this.twoway  = twoWay;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** Check if the instance represents a two-way measurement.
     * @return true if the instance represents a two-way measurement
     */
    public boolean isTwoWay() {
        return twoway;
    }

    /** Compute common estimation parameters.
     * @param state orbital state at measurement date
     * @return common parameters
     */
    protected GroundReceiverCommonParametersWithoutDerivatives computeCommonParametersWithout(final SpacecraftState state) {

        // Coordinates of the spacecraft
        final TimeStampedPVCoordinates pva = state.getPVCoordinates();

        // transform between station and inertial frame
        final Transform offsetToInertialDownlink =
                        getStation().getOffsetToInertial(state.getFrame(), getDate(), false);
        final AbsoluteDate downlinkDate = offsetToInertialDownlink.getDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedPVCoordinates origin = new TimeStampedPVCoordinates(downlinkDate,
                                                                             Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
        final TimeStampedPVCoordinates stationDownlink = offsetToInertialDownlink.transformPVCoordinates(origin);

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final double tauD = signalTimeOfFlight(pva, stationDownlink.getPosition(), downlinkDate);

        // Transit state & Transit state (re)computed with gradients
        final double          delta        = downlinkDate.durationFrom(state.getDate());
        final double          deltaMTauD   = delta - tauD;
        final SpacecraftState transitState = state.shiftedBy(deltaMTauD);

        return new GroundReceiverCommonParametersWithoutDerivatives(state,
                                                                    offsetToInertialDownlink,
                                                                    stationDownlink,
                                                                    tauD,
                                                                    transitState,
                                                                    transitState.getPVCoordinates());

    }

    /** Compute common estimation parameters.
     * @param state orbital state at measurement date
     * @return common parameters
     */
    protected GroundReceiverCommonParametersWithDerivatives computeCommonParametersWithDerivatives(final SpacecraftState state) {
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    indices.put(span.getData(), nbParams++);
                }
            }
        }
        final FieldVector3D<Gradient> zero = FieldVector3D.getZero(GradientField.getField(nbParams));

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pva = getCoordinates(state, 0, nbParams);

        // transform between station and inertial frame, expressed as a gradient
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<Gradient> offsetToInertialDownlink =
                        getStation().getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> downlinkDate = offsetToInertialDownlink.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<Gradient> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDate,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final Gradient tauD = signalTimeOfFlight(pva, stationDownlink.getPosition(), downlinkDate);

        // Transit state & Transit state (re)computed with gradients
        final Gradient        delta        = downlinkDate.durationFrom(state.getDate());
        final Gradient        deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState transitState = state.shiftedBy(deltaMTauD.getValue());
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = pva.shiftedBy(deltaMTauD);

        return new GroundReceiverCommonParametersWithDerivatives(state,
                                                                 indices,
                                                                 offsetToInertialDownlink,
                                                                 stationDownlink,
                                                                 tauD,
                                                                 transitState,
                                                                 transitPV);

    }

    /**
     * Get the station position for a given frame.
     * @param frame inertial frame for station position
     * @return the station position in the given inertial frame
     * @since 12.0
     */
    public Vector3D getGroundStationPosition(final Frame frame) {
        return station.getBaseFrame().getPosition(getDate(), frame);
    }

    /**
     * Get the station coordinates for a given frame.
     * @param frame inertial frame for station position
     * @return the station coordinates in the given inertial frame
     * @since 12.0
     */
    public PVCoordinates getGroundStationCoordinates(final Frame frame) {
        return station.getBaseFrame().getPVCoordinates(getDate(), frame);
    }

}
