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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Base class modeling a measurement where receiver is a satellite.
 * @param <T> type of the measurement
 * @author Luc Maisonobe
 * @since 12.1
 */
public abstract class OnBoardMeasurement<T extends ObservedMeasurement<T>> extends AbstractMeasurement<T> {

    /** Constructor.
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellites satellites related to this measurement
     */
    public OnBoardMeasurement(final AbsoluteDate date, final double observed,
                              final double sigma, final double baseWeight,
                              final List<ObservableSatellite> satellites) {
        // Call to super constructor
        super(date, observed, sigma, baseWeight, satellites);

        // Add parameter drivers
        satellites.forEach(s -> addParameterDriver(s.getClockDriftDriver()));

    }

    /** Compute common estimation parameters.
     * @param local orbital state of local satellite at measurement date
     * @param remote emitting satellite
     * @param dtRemote remote clock offset
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    protected OnBoardCommonParametersWithoutDerivatives computeCommonParametersWithout(final SpacecraftState local,
                                                                                       final PVCoordinatesProvider remote,
                                                                                       final double dtRemote,
                                                                                       final boolean clockOffsetAlreadyApplied) {

        // Coordinates of both satellites
        final TimeStampedPVCoordinates pvaLocal = local.getPVCoordinates();

        // take clock offset into account
        final ParameterDriver localClockDriver = getSatellites().get(0).getClockOffsetDriver();
        final double          dtLocal          = localClockDriver.getValue(getDate());
        final AbsoluteDate    arrivalDate      = clockOffsetAlreadyApplied ?
                                                 getDate() :
                                                 getDate().shiftedBy(-dtLocal);

        // Downlink delay
        final double deltaT = arrivalDate.durationFrom(local);
        final TimeStampedPVCoordinates pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final double tauD = signalTimeOfFlight(remote, arrivalDate, pvaDownlink.getPosition(),
                                               arrivalDate, local.getFrame());

        // Remote satellite at signal emission
        return new OnBoardCommonParametersWithoutDerivatives(local, dtLocal, dtRemote, tauD,
                                                             pvaDownlink,
                                                             remote.getPVCoordinates(arrivalDate.shiftedBy(-tauD),
                                                                                     local.getFrame()));

    }

    /** Compute common estimation parameters.
     * ^param nbSat number of satellites involved in the measurement
     * @param local orbital state of local satellite at measurement date
     * @param remote emitting satellite
     * @param dtRemote remote clock offset
     * @param clockOffsetAlreadyApplied if true, the specified {@code date} is as read
     * by the receiver clock (i.e. clock offset <em>not</em> compensated), if false,
     * the specified {@code date} was already compensated and is a physical absolute date
     * @return common parameters
     */
    protected OnBoardCommonParametersWithDerivatives computeCommonParametersWith(final int nbSat,
                                                                                 final SpacecraftState local,
                                                                                 final PVCoordinatesProvider remote,
                                                                                 final double dtRemote,
                                                                                 final boolean clockOffsetAlreadyApplied) {

        // Range derivatives are computed with respect to spacecraft state in inertial frame
        // Parameters:
        //  - 6k..6k+2 - Position of spacecraft k (counting from 0) in inertial frame
        //  - 6k+3..6k+5 - Velocity of spacecraft k (counting from 0) in inertial frame
        //  - 6k+6..n - measurements parameters (clock offset, etc)
        int nbEstimatedParams = 6 * nbSat;
        final Map<String, Integer> parameterIndices = new HashMap<>();
        for (ParameterDriver measurementDriver : getParametersDrivers()) {
            if (measurementDriver.isSelected()) {
                for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    parameterIndices.put(span.getData(), nbEstimatedParams++);
                }
            }
        }

        // convert the PVCoordinatesProvider to a FieldPVCoordinatesProvider<Gradient>
        final FieldPVCoordinatesProvider<Gradient> gRemote = (date, frame) -> {

            // apply the raw (no derivatives) remote provider
            final AbsoluteDate             dateBase = date.toAbsoluteDate();
            final TimeStampedPVCoordinates pvBase   = remote.getPVCoordinates(dateBase, frame);
            final TimeStampedFieldPVCoordinates<Gradient> pvWithoutDerivatives =
                new TimeStampedFieldPVCoordinates<>(date.getField(), pvBase);

            // add derivatives, using a trick: we shift the date by 0, with derivatives
            final Gradient zeroWithDerivatives = date.durationFrom(dateBase);
            return pvWithoutDerivatives.shiftedBy(zeroWithDerivatives);

        };

        // Coordinates of both satellites
        final TimeStampedFieldPVCoordinates<Gradient>
            pvaLocal  = getCoordinates(local, 0, nbEstimatedParams);

        // take clock offset into account
        final ParameterDriver             localClockDriver = getSatellites().get(0).getClockOffsetDriver();
        final Gradient                    dtLocal          = localClockDriver.getValue(nbEstimatedParams, parameterIndices, getDate());
        final FieldAbsoluteDate<Gradient> arrivalDate = clockOffsetAlreadyApplied ?
                                                        new FieldAbsoluteDate<>(dtLocal.getField(), getDate()) :
                                                        new FieldAbsoluteDate<>(getDate(), dtLocal.negate());

        // Downlink delay
        final Gradient deltaT = arrivalDate.durationFrom(local.getDate());
        final TimeStampedFieldPVCoordinates<Gradient> pvaDownlink = pvaLocal.shiftedBy(deltaT);
        final Gradient tauD = signalTimeOfFlight(gRemote, arrivalDate,
                                                 pvaDownlink.getPosition(), arrivalDate,
                                                 local.getFrame());

        // Remote satellite at signal emission
        return new OnBoardCommonParametersWithDerivatives(local, parameterIndices,
                                                          dtLocal, dtLocal.newInstance(dtRemote), tauD,
                                                          pvaDownlink,
                                                          gRemote.getPVCoordinates(arrivalDate.shiftedBy(tauD.negate()),
                                                                                   local.getFrame()));

    }

}
