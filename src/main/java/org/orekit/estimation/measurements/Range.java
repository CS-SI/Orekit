/* Copyright 2002-2020 CS Group
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a range measurement from a ground station.
 * <p>
 * For one-way measurements, a signal is emitted by the satellite
 * and received by the ground station. The measurement value is the
 * elapsed time between emission and reception multiplied by c where
 * c is the speed of light.
 * </p>
 * <p>
 * For two-way measurements, the measurement is considered to be a signal
 * emitted from a ground station, reflected on spacecraft, and received
 * on the same ground station. Its value is the elapsed time between
 * emission and reception multiplied by c/2 where c is the speed of light.
 * </p>
 * <p>
 * The motion of both the station and the spacecraft during the signal
 * flight time are taken into account. The date of the measurement
 * corresponds to the reception on ground of the emitted or reflected signal.
 * </p>
 * <p>
 * The clock offsets of both the ground station and the satellite are taken
 * into account. These offsets correspond to the values that must be subtracted
 * from station (resp. satellite) reading of time to compute the real physical
 * date. These offsets have two effects:
 * </p>
 * <ul>
 *   <li>as measurement date is evaluated at reception time, the real physical date
 *   of the measurement is the observed date to which the receiving ground station
 *   clock offset is subtracted</li>
 *   <li>as range is evaluated using the total signal time of flight, for one-way
 *   measurements the observed range is the real physical signal time of flight to
 *   which (Δtg - Δts) ⨉ c is added, where Δtg (resp. Δts) is the clock offset for the
 *   receiving ground station (resp. emitting satellite). A similar effect exists in
 *   two-way measurements but it is computed as (Δtg - Δtg) ⨉ c / 2 as the same ground
 *   station clock is used for initial emission and final reception and therefore it evaluates
 *   to zero.</li>
 * </ul>
 * <p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 8.0
 */
public class Range extends AbstractMeasurement<Range> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public Range(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight,
                 final ObservableSatellite satellite) {
        super(date, range, sigma, baseWeight, Arrays.asList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
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
        }
        this.station = station;
        this.twoway = twoWay;
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

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Range> theoreticalEvaluation(final int iteration,
                                                                final int evaluation,
                                                                final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Range derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final DSFactory                          factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure>         field   = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero    = FieldVector3D.getZero(field);

        // Coordinates of the spacecraft expressed as a derivative structure
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS = getCoordinates(state, 0, factory);

        // transform between station and inertial frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final FieldTransform<DerivativeStructure> offsetToInertialDownlink =
                        station.getOffsetToInertial(state.getFrame(), getDate(), factory, indices);
        final FieldAbsoluteDate<DerivativeStructure> downlinkDateDS = offsetToInertialDownlink.getFieldDate();

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final DerivativeStructure tauD = signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

        // Transit state & Transit state (re)computed with derivative structures
        final DerivativeStructure   delta        = downlinkDateDS.durationFrom(state.getDate());
        final DerivativeStructure   deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(deltaMTauD.getValue());
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateDS = pvaDS.shiftedBy(deltaMTauD);

        // prepare the evaluation
        final EstimatedMeasurement<Range> estimated;
        final DerivativeStructure range;

        if (twoway) {

            // Station at transit state date (derivatives of tauD taken into account)
            final TimeStampedFieldPVCoordinates<DerivativeStructure> stationAtTransitDate =
                            stationDownlink.shiftedBy(tauD.negate());
            // Uplink delay
            final DerivativeStructure tauU =
                            signalTimeOfFlight(stationAtTransitDate, transitStateDS.getPosition(), transitStateDS.getDate());
            final TimeStampedFieldPVCoordinates<DerivativeStructure> stationUplink =
                            stationDownlink.shiftedBy(-tauD.getValue() - tauU.getValue());

            // Prepare the evaluation
            estimated = new EstimatedMeasurement<Range>(this, iteration, evaluation,
                                                            new SpacecraftState[] {
                                                                transitState
                                                            }, new TimeStampedPVCoordinates[] {
                                                                stationUplink.toTimeStampedPVCoordinates(),
                                                                transitStateDS.toTimeStampedPVCoordinates(),
                                                                stationDownlink.toTimeStampedPVCoordinates()
                                                            });

            // Range value
            final double              cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
            final DerivativeStructure tau    = tauD.add(tauU);
            range                            = tau.multiply(cOver2);

        } else {

            estimated = new EstimatedMeasurement<Range>(this, iteration, evaluation,
                            new SpacecraftState[] {
                                transitState
                            }, new TimeStampedPVCoordinates[] {
                                transitStateDS.toTimeStampedPVCoordinates(),
                                stationDownlink.toTimeStampedPVCoordinates()
                            });

            // Clock offsets
            final ObservableSatellite satellite = getSatellites().get(0);
            final DerivativeStructure dts       = satellite.getClockOffsetDriver().getValue(factory, indices);
            final DerivativeStructure dtg       = station.getClockOffsetDriver().getValue(factory, indices);

            // Range value
            range = tauD.add(dtg).subtract(dts).multiply(Constants.SPEED_OF_LIGHT);

        }

        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to state
        final double[] derivatives = range.getAllDerivatives();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 1, 7));

        // set partial derivatives with respect to parameters
        // (beware element at index 0 is the value, not a derivative)
        for (final ParameterDriver driver : getParametersDrivers()) {
            final Integer index = indices.get(driver.getName());
            if (index != null) {
                estimated.setParameterDerivatives(driver, derivatives[index + 1]);
            }
        }

        return estimated;

    }

}
