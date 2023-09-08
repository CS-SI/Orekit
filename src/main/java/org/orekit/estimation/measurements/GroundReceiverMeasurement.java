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
import java.util.Optional;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Base class modeling a measurement where receiver is a ground station.
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @author Tommy Fryer
 * @since 12.0
 */
public abstract class GroundReceiverMeasurement<T extends GroundReceiverMeasurement<T>> extends AbstractMeasurement<T> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Enum indicating the time tag specification of a range observation. */
    private final TimeTagSpecificationType timeTagSpecificationType;

    /** Constructor for single value measurement (e.g. range or range-rate).
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @param timeTagSpecificationType specify the time-tag configuration of the provided measurement
     */
    public GroundReceiverMeasurement(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                                     final double observed, final double sigma, final double baseWeight,
                                     final ObservableSatellite satellite,
                                     final TimeTagSpecificationType timeTagSpecificationType) {
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
        this.station                  = station;
        this.twoway                   = twoWay;
        this.timeTagSpecificationType = timeTagSpecificationType;
    }

    /** Constructor for single value measurement with default time-tag (reception time).
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
        this(station, twoWay, date, observed, sigma, baseWeight, satellite, TimeTagSpecificationType.RX);
    }

    /** Constructor for multiple values measurement (e.g. angular measurements).
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @param timeTagSpecificationType specify the time-tag configuration of the provided observation
     * @since 12.0
     */
    public GroundReceiverMeasurement(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                                     final double[] observed, final double[] sigma, final double[] baseWeight,
                                     final ObservableSatellite satellite,
                                     final TimeTagSpecificationType timeTagSpecificationType) {
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
        this.station                  = station;
        this.twoway                   = twoWay;
        this.timeTagSpecificationType = timeTagSpecificationType;
    }

    /** Constructor for multiple values measurement with default time-tag (reception time).
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 12.0
     */
    public GroundReceiverMeasurement(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                                     final double[] observed, final double[] sigma, final double[] baseWeight,
                                     final ObservableSatellite satellite) {
        this(station, twoWay, date, observed, sigma, baseWeight, satellite, TimeTagSpecificationType.RX);
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

    /** Getter for the timeTagSpecificationType.
     *
     * @return the timeTagSpecificationType
     */

    public TimeTagSpecificationType getTimeTagSpecificationType() {
        return timeTagSpecificationType;
    }

    /** Compute common estimation parameters without derivatives.
     * @param state orbital state at measurement date
     * @return common parameters without derivatives
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

    /** Compute common estimation parameters with derivatives.
     * @param state orbital state at measurement date
     * @return common parameters with derivatives
     */
    protected GroundReceiverCommonParametersWithDerivatives computeCommonParametersWithDerivatives(final SpacecraftState state) {

        // Derivatives are computed with respect to spacecraft state in inertial frame and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)

        // Cartesian orbital rbital parameters
        int nbParams = 6;

        // Measurement parameters
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

        // Transform between station and inertial frame at observation date, expressed as a gradient
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<Gradient> offsetToInertialObsDate =
                        getStation().getOffsetToInertial(state.getFrame(), getDate(), nbParams, indices);
        final FieldAbsoluteDate<Gradient> obsDate = offsetToInertialObsDate.getFieldDate();

        // Station position/velocity in inertial frame at observation date
        final TimeStampedFieldPVCoordinates<Gradient> stationObsDate =
                        offsetToInertialObsDate.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(obsDate,
                                        zero, zero, zero));

        // Time difference between observation date and state date
        final Gradient delta = obsDate.durationFrom(state.getDate());

        // Transit state and field PV
        final SpacecraftState transitState;
        final TimeStampedFieldPVCoordinates<Gradient> transitPV;

        // Downlink time and station PV
        final Gradient tauD;
        final TimeStampedFieldPVCoordinates<Gradient> stationDownlink;

        // Uplink time and station PV
        // (optional, used in case of time-tag = transmission date, or for a two-way measurement)
        Optional<Gradient> optionalTauU = Optional.empty();
        Optional<TimeStampedFieldPVCoordinates<Gradient>> optionalStationUplink = Optional.empty();

        // Station position for relative position vector calculation - set to downlink for transmit and transmit
        // receive apparent (TXRX).
        // For transit/bounce time tag specification we use the station at bounce time.
        // For transmit apparent the station at time of transmission is used.
        TimeStampedFieldPVCoordinates<Gradient> stationEstimationDate = stationObsDate;

        if (timeTagSpecificationType == TimeTagSpecificationType.TX || timeTagSpecificationType == TimeTagSpecificationType.TXRX) {

            // Case time-tag = reception or transmit - receive apparent
            // -----

            // Station PV at transmission date
            final TimeStampedFieldPVCoordinates<Gradient> stationUplink = stationObsDate;

            // Uplink delay
            // Observation date = epoch of transmission.
            // Vary position of receiver -> in case of uplink leg, receiver is the satellite
            final Gradient tauU = signalTimeOfFlightFixedEmission(pva, stationUplink.getPosition(), obsDate);

            // Get state and fielded PV at transit
            // (if state has already been set up to pre-compensate propagation delay,
            //  we will have delta = -tauU and transitState will be the same as state)
            final Gradient deltaMTauU = tauU.add(delta);
            transitPV    = pva.shiftedBy(deltaMTauU);
            transitState = state.shiftedBy(deltaMTauU.getValue());

            // Get station at transit - although this is effectively an initial seed for fitting the downlink delay
            final TimeStampedFieldPVCoordinates<Gradient> stationTransit = stationUplink.shiftedBy(tauU);

            // Downlink delay
            // Project time of flight forwards with 0 offset.
            // Vary position of receiver -> in case of downlink leg, receiver is the station
            tauD = signalTimeOfFlightFixedEmission(stationTransit, transitPV.getPosition(), transitPV.getDate());

            // Station PV at reception date
            stationDownlink = stationUplink.shiftedBy(tauU.add(tauD));

            // If observation receive apparent, set station PV at estimation date to downlink
            if (timeTagSpecificationType == TimeTagSpecificationType.TXRX) {
                stationEstimationDate = stationDownlink;
            }

            if (twoway) {
                // If two-way, store station uplink position and uplink delay
                optionalStationUplink = Optional.of(stationUplink);
                optionalTauU          = Optional.of(tauU);
            }

        } else if (timeTagSpecificationType == TimeTagSpecificationType.TRANSIT) {

            // Case time-tag = transit date
            // ----

            transitPV    = pva.shiftedBy(delta);
            transitState = state.shiftedBy(delta.getValue());

            // Downlink delay
            // Vary position of receiver -> in case of downlink leg, receiver is the station
            tauD = signalTimeOfFlightFixedEmission(stationObsDate, transitPV.getPosition(), transitPV.getDate());

            // Station PV at end of downlink leg (reception date)
            stationDownlink = stationObsDate.shiftedBy(tauD);

            // If two-way signal, get uplink delay and station position at transmission date
            if (twoway) {
                // Uplink delay
                // Vary position of emitter -> in case of uplink leg, emitter is the station
                optionalTauU = Optional.of(signalTimeOfFlightFixedReception(stationObsDate, transitPV.getPosition(), transitPV.getDate()));

                // Store uplink delay and station PV at transmission date
                optionalStationUplink = Optional.of(stationObsDate.shiftedBy(optionalTauU.get().negate()));
            }

        } else {

            // Case time-tag = reception date
            // ----

            // Downlink delay
            // Vary position of emitter -> in case of downlink leg, emitter is the satellite
            tauD = signalTimeOfFlightFixedReception(pva, stationObsDate.getPosition(), obsDate);

            // Transit state
            // (if state has already been set up to pre-compensate propagation delay,
            //  we will have delta = tauD and transitState will be the same as state)
            final Gradient deltaMTauD = tauD.negate().add(delta);
            transitState = state.shiftedBy(deltaMTauD.getValue());

            // Transit state (re)computed with gradients
            transitPV = pva.shiftedBy(deltaMTauD);
            stationDownlink = stationObsDate;

            // If two-way signal, get uplink delay and station position at transmission date
            if (twoway) {

                // Station at transit state date (derivatives of tauD taken into account)
                final TimeStampedFieldPVCoordinates<Gradient> stationTransit = stationDownlink.shiftedBy(tauD.negate());

                // Uplink delay
                // Vary position of emitter -> in case of uplink leg, emitter is the station
                optionalTauU = Optional.of(signalTimeOfFlightFixedReception(stationTransit, transitPV.getPosition(), transitPV.getDate()));

                // Station PV at transmission date
                optionalStationUplink = Optional.of(stationDownlink.shiftedBy(optionalTauU.get().negate().subtract(tauD)));
            }
        }

        final FieldTransform<Gradient> offsetToInertialEstimationDate =
                        getStation().getOffsetToInertial(state.getFrame(), stationEstimationDate.getDate(), nbParams, indices);


        if (twoway) {

            // Two-way signal: Build participants list
            final TimeStampedPVCoordinates[] participants =
                            new TimeStampedPVCoordinates[] {
                                optionalStationUplink.get().toTimeStampedPVCoordinates(),
                                transitPV.toTimeStampedPVCoordinates(),
                                stationDownlink.toTimeStampedPVCoordinates()};

            return new GroundReceiverCommonParametersWithDerivatives(state,
                                                                     indices,
                                                                     offsetToInertialEstimationDate,
                                                                     stationEstimationDate,
                                                                     stationDownlink,
                                                                     tauD,
                                                                     optionalStationUplink.get(),
                                                                     optionalTauU.get(),
                                                                     transitState,
                                                                     transitPV,
                                                                     participants);

        } else {

            // One-way signal: Build participants list without uplink
            final TimeStampedPVCoordinates[] participants =
                            new TimeStampedPVCoordinates[] {
                                transitPV.toTimeStampedPVCoordinates(),
                                stationDownlink.toTimeStampedPVCoordinates()};

            return new GroundReceiverCommonParametersWithDerivatives(state,
                                                                     indices,
                                                                     offsetToInertialEstimationDate,
                                                                     stationEstimationDate,
                                                                     stationDownlink,
                                                                     tauD,
                                                                     transitState,
                                                                     transitPV,
                                                                     participants);
        }
    }
}
