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
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a turn-around range measurement using a master ground station and a slave ground station.
 * <p>
 * The measurement is considered to be a signal:
 * - Emitted from the master ground station
 * - Reflected on the spacecraft
 * - Reflected on the slave ground station
 * - Reflected on the spacecraft again
 * - Received on the master ground station
 * Its value is the elapsed time between emission and reception
 * divided by 2c were c is the speed of light.
 * The motion of the stations and the spacecraft
 * during the signal flight time are taken into account.
 * The date of the measurement corresponds to the
 * reception on ground of the reflected signal.
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 *
 * @since 9.0
 */
public class TurnAroundRange extends AbstractMeasurement<TurnAroundRange> {

    /** Master ground station from which measurement is performed. */
    private final GroundStation masterStation;

    /** Slave ground station reflecting the signal. */
    private final GroundStation slaveStation;

    /** Simple constructor.
     * @param masterStation ground station from which measurement is performed
     * @param slaveStation ground station reflecting the signal
     * @param date date of the measurement
     * @param turnAroundRange observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public TurnAroundRange(final GroundStation masterStation, final GroundStation slaveStation,
                           final AbsoluteDate date, final double turnAroundRange,
                           final double sigma, final double baseWeight,
                           final ObservableSatellite satellite) {
        super(date, turnAroundRange, sigma, baseWeight, Arrays.asList(satellite));
        addParameterDriver(masterStation.getClockOffsetDriver());
        addParameterDriver(masterStation.getEastOffsetDriver());
        addParameterDriver(masterStation.getNorthOffsetDriver());
        addParameterDriver(masterStation.getZenithOffsetDriver());
        addParameterDriver(masterStation.getPrimeMeridianOffsetDriver());
        addParameterDriver(masterStation.getPrimeMeridianDriftDriver());
        addParameterDriver(masterStation.getPolarOffsetXDriver());
        addParameterDriver(masterStation.getPolarDriftXDriver());
        addParameterDriver(masterStation.getPolarOffsetYDriver());
        addParameterDriver(masterStation.getPolarDriftYDriver());
        // the slave station clock is not used at all, we ignore the corresponding parameter driver
        addParameterDriver(slaveStation.getEastOffsetDriver());
        addParameterDriver(slaveStation.getNorthOffsetDriver());
        addParameterDriver(slaveStation.getZenithOffsetDriver());
        addParameterDriver(slaveStation.getPrimeMeridianOffsetDriver());
        addParameterDriver(slaveStation.getPrimeMeridianDriftDriver());
        addParameterDriver(slaveStation.getPolarOffsetXDriver());
        addParameterDriver(slaveStation.getPolarDriftXDriver());
        addParameterDriver(slaveStation.getPolarOffsetYDriver());
        addParameterDriver(slaveStation.getPolarDriftYDriver());
        this.masterStation = masterStation;
        this.slaveStation = slaveStation;
    }

    /** Get the master ground station from which measurement is performed.
     * @return master ground station from which measurement is performed
     */
    public GroundStation getMasterStation() {
        return masterStation;
    }

    /** Get the slave ground station reflecting the signal.
     * @return slave ground station reflecting the signal
     */
    public GroundStation getSlaveStation() {
        return slaveStation;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<TurnAroundRange> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                          final SpacecraftState[] states) {

        final SpacecraftState state = states[0];

        // Turn around range derivatives are computed with respect to:
        // - Spacecraft state in inertial frame
        // - Master station parameters
        // - Slave station parameters
        // --------------------------
        //
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - stations' parameters (clock offset, station offsets, pole, prime meridian...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            // we have to check for duplicate keys because master and slave station share
            // pole and prime meridian parameters names that must be considered
            // as one set only (they are combined together by the estimation engine)
            if (driver.isSelected() && !indices.containsKey(driver.getName())) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final DSFactory                          factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure>         field   = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero    = FieldVector3D.getZero(field);

        // Place the derivative structures in a time-stamped PV
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS = getCoordinates(state, 0, factory);

        // The path of the signal is divided in two legs.
        // Leg1: Emission from master station to satellite in masterTauU seconds
        //     + Reflection from satellite to slave station in slaveTauD seconds
        // Leg2: Reflection from slave station to satellite in slaveTauU seconds
        //     + Reflection from satellite to master station in masterTaudD seconds
        // The measurement is considered to be time stamped at reception on ground
        // by the master station. All times are therefore computed as backward offsets
        // with respect to this reception time.
        //
        // Two intermediate spacecraft states are defined:
        //  - transitStateLeg2: State of the satellite when it bounced back the signal
        //                      from slave station to master station during the 2nd leg
        //  - transitStateLeg1: State of the satellite when it bounced back the signal
        //                      from master station to slave station during the 1st leg

        // Compute propagation time for the 2nd leg of the signal path
        // --

        // Time difference between t (date of the measurement) and t' (date tagged in spacecraft state)
        // (if state has already been set up to pre-compensate propagation delay,
        // we will have delta = masterTauD + slaveTauU)
        final double delta = getDate().durationFrom(state.getDate());

        // transform between master station topocentric frame (east-north-zenith) and inertial frame expressed as DerivativeStructures
        final FieldTransform<DerivativeStructure> masterToInert =
                        masterStation.getOffsetToInertial(state.getFrame(), getDate(), factory, indices);
        final FieldAbsoluteDate<DerivativeStructure> measurementDateDS =
                        masterToInert.getFieldDate();

        // Master station PV in inertial frame at measurement date
        final TimeStampedFieldPVCoordinates<DerivativeStructure> masterArrival =
                        masterToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(measurementDateDS,
                                                                                                 zero, zero, zero));

        // Compute propagation times
        final DerivativeStructure masterTauD = signalTimeOfFlight(pvaDS, masterArrival.getPosition(), measurementDateDS);

        // Elapsed time between state date t' and signal arrival to the transit state of the 2nd leg
        final DerivativeStructure dtLeg2 = masterTauD.negate().add(delta);

        // Transit state where the satellite reflected the signal from slave to master station
        final SpacecraftState transitStateLeg2 = state.shiftedBy(dtLeg2.getValue());

        // Transit state pv of leg2 (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateLeg2PV = pvaDS.shiftedBy(dtLeg2);

        // transform between slave station topocentric frame (east-north-zenith) and inertial frame expressed as DerivativeStructures
        // The components of slave station's position in offset frame are the 3 last derivative parameters
        final FieldAbsoluteDate<DerivativeStructure> approxReboundDate = measurementDateDS.shiftedBy(-delta);
        final FieldTransform<DerivativeStructure> slaveToInertApprox =
                        slaveStation.getOffsetToInertial(state.getFrame(), approxReboundDate, factory, indices);

        // Slave station PV in inertial frame at approximate rebound date on slave station
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlaveApprox =
                        slaveToInertApprox.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxReboundDate,
                                                                                                      zero, zero, zero));

        // Uplink time of flight from slave station to transit state of leg2
        final DerivativeStructure slaveTauU = signalTimeOfFlight(QSlaveApprox,
                                                                 transitStateLeg2PV.getPosition(),
                                                                 transitStateLeg2PV.getDate());

        // Total time of flight for leg 2
        final DerivativeStructure tauLeg2 = masterTauD.add(slaveTauU);

        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of rebound of the signal to slave station
        final FieldAbsoluteDate<DerivativeStructure> reboundDateDS = measurementDateDS.shiftedBy(tauLeg2.negate());
        final FieldTransform<DerivativeStructure> slaveToInert =
                        slaveStation.getOffsetToInertial(state.getFrame(), reboundDateDS, factory, indices);

        // Slave station PV in inertial frame at rebound date on slave station
        final TimeStampedFieldPVCoordinates<DerivativeStructure> slaveRebound =
                        slaveToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(reboundDateDS,
                                                                                                FieldPVCoordinates.getZero(field)));

        // Downlink time of flight from transitStateLeg1 to slave station at rebound date
        final DerivativeStructure slaveTauD = signalTimeOfFlight(transitStateLeg2PV,
                                                                 slaveRebound.getPosition(),
                                                                 reboundDateDS);


        // Elapsed time between state date t' and signal arrival to the transit state of the 1st leg
        final DerivativeStructure dtLeg1 = dtLeg2.subtract(slaveTauU).subtract(slaveTauD);

        // Transit state pv of leg2 (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateLeg1PV = pvaDS.shiftedBy(dtLeg1);

        // transform between master station topocentric frame (east-north-zenith) and inertial frame expressed as DerivativeStructures
        // The components of master station's position in offset frame are the 3 third derivative parameters
        final FieldAbsoluteDate<DerivativeStructure> approxEmissionDate =
                        measurementDateDS.shiftedBy(-2 * (slaveTauU.getValue() + masterTauD.getValue()));
        final FieldTransform<DerivativeStructure> masterToInertApprox =
                        masterStation.getOffsetToInertial(state.getFrame(), approxEmissionDate, factory, indices);

        // Master station PV in inertial frame at approximate emission date
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QMasterApprox =
                        masterToInertApprox.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxEmissionDate,
                                                                                                       zero, zero, zero));

        // Uplink time of flight from master station to transit state of leg1
        final DerivativeStructure masterTauU = signalTimeOfFlight(QMasterApprox,
                                                                  transitStateLeg1PV.getPosition(),
                                                                  transitStateLeg1PV.getDate());

        // Master station PV in inertial frame at exact emission date
        final AbsoluteDate emissionDate = transitStateLeg1PV.getDate().toAbsoluteDate().shiftedBy(-masterTauU.getValue());
        final TimeStampedPVCoordinates masterDeparture =
                        masterToInertApprox.shiftedBy(emissionDate.durationFrom(masterToInertApprox.getDate())).
                        transformPVCoordinates(new TimeStampedPVCoordinates(emissionDate, PVCoordinates.ZERO)).
                        toTimeStampedPVCoordinates();

        // Total time of flight for leg 1
        final DerivativeStructure tauLeg1 = slaveTauD.add(masterTauU);


        // --
        // Evaluate the turn-around range value and its derivatives
        // --------------------------------------------------------

        // The state we use to define the estimated measurement is a middle ground between the two transit states
        // This is done to avoid calling "SpacecraftState.shiftedBy" function on long duration
        // Thus we define the state at the date t" = date of rebound of the signal at the slave station
        // Or t" = t -masterTauD -slaveTauU
        // The iterative process in the estimation ensures that, after several iterations, the date stamped in the
        // state S in input of this function will be close to t"
        // Therefore we will shift state S by:
        //  - +slaveTauU to get transitStateLeg2
        //  - -slaveTauD to get transitStateLeg1
        final EstimatedMeasurement<TurnAroundRange> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       transitStateLeg2.shiftedBy(-slaveTauU.getValue())
                                                   },
                                                   new TimeStampedPVCoordinates[] {
                                                       masterDeparture,
                                                       transitStateLeg1PV.toTimeStampedPVCoordinates(),
                                                       slaveRebound.toTimeStampedPVCoordinates(),
                                                       transitStateLeg2.getPVCoordinates(),
                                                       masterArrival.toTimeStampedPVCoordinates()
                                                   });

        // Turn-around range value = Total time of flight for the 2 legs divided by 2 and multiplied by c
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final DerivativeStructure turnAroundRange = (tauLeg2.add(tauLeg1)).multiply(cOver2);
        estimated.setEstimatedValue(turnAroundRange.getValue());

        // Turn-around range partial derivatives with respect to state
        final double[] derivatives = turnAroundRange.getAllDerivatives();
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
