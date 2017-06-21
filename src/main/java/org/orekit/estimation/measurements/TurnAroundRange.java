/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

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
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     */
    public TurnAroundRange(final GroundStation masterStation, final GroundStation slaveStation,
                           final AbsoluteDate date, final double turnAroundRange,
                           final double sigma, final double baseWeight)
        throws OrekitException {
        super(date, turnAroundRange, sigma, baseWeight,
              masterStation.getEastOffsetDriver(),
              masterStation.getNorthOffsetDriver(),
              masterStation.getZenithOffsetDriver(),
              masterStation.getPrimeMeridianOffsetDriver(),
              masterStation.getPrimeMeridianDriftDriver(),
              masterStation.getPolarOffsetXDriver(),
              masterStation.getPolarDriftXDriver(),
              masterStation.getPolarOffsetYDriver(),
              masterStation.getPolarDriftYDriver(),
              slaveStation.getEastOffsetDriver(),
              slaveStation.getNorthOffsetDriver(),
              slaveStation.getZenithOffsetDriver(),
              slaveStation.getPrimeMeridianOffsetDriver(),
              slaveStation.getPrimeMeridianDriftDriver(),
              slaveStation.getPolarOffsetXDriver(),
              slaveStation.getPolarDriftXDriver(),
              slaveStation.getPolarOffsetYDriver(),
              slaveStation.getPolarDriftYDriver());
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
                                                                          final SpacecraftState state)
        throws OrekitException {

        /* Turn around range derivatives are computed with respect to:
         * - Spacecraft state in inertial frame
         * - Master station position in master station's offset frame
         * - Slave station position in slave station's offset frame
         * -------
         *
         * Parameters:
         *  - 0..2  - Px, Py, Pz      : Position of the spacecraft in inertial frame
         *  - 3..5  - Vx, Vy, Vz      : Velocity of the spacecraft in inertial frame
         *  - 6..8  - QMTx, QMTy, QMTz: Position of the master station in station's offset topocentric frame
         *  - 9..11 - QSTx, QSTy, QSTz: Position of the slave station in station's offset topocentric frame
         */
        int nbParams = 6;
        final int primeMeridianOffsetIndex;
        if (masterStation.getPrimeMeridianOffsetDriver().isSelected() ||
            slaveStation.getPrimeMeridianOffsetDriver().isSelected()) {
            primeMeridianOffsetIndex = nbParams++;
        } else {
            primeMeridianOffsetIndex = -1;
        }
        final int primeMeridianDriftIndex;
        if (masterStation.getPrimeMeridianDriftDriver().isSelected() ||
            slaveStation.getPrimeMeridianDriftDriver().isSelected()) {
            primeMeridianDriftIndex = nbParams++;
        } else {
            primeMeridianDriftIndex = -1;
        }
        final int polarOffsetXIndex;
        if (masterStation.getPolarOffsetXDriver().isSelected() ||
            slaveStation.getPolarOffsetXDriver().isSelected()) {
            polarOffsetXIndex = nbParams++;
        } else {
            polarOffsetXIndex = -1;
        }
        final int polarDriftXIndex;
        if (masterStation.getPolarDriftXDriver().isSelected() ||
            slaveStation.getPolarDriftXDriver().isSelected()) {
            polarDriftXIndex = nbParams++;
        } else {
            polarDriftXIndex = -1;
        }
        final int polarOffsetYIndex;
        if (masterStation.getPolarOffsetYDriver().isSelected() ||
            slaveStation.getPolarOffsetYDriver().isSelected()) {
            polarOffsetYIndex = nbParams++;
        } else {
            polarOffsetYIndex = -1;
        }
        final int polarDriftYIndex;
        if (masterStation.getPolarDriftYDriver().isSelected() ||
            slaveStation.getPolarDriftYDriver().isSelected()) {
            polarDriftYIndex = nbParams++;
        } else {
            polarDriftYIndex = -1;
        }
        final int masterEastOffsetIndex;
        if (masterStation.getEastOffsetDriver().isSelected()) {
            masterEastOffsetIndex = nbParams++;
        } else {
            masterEastOffsetIndex = -1;
        }
        final int masterNorthOffsetIndex;
        if (masterStation.getNorthOffsetDriver().isSelected()) {
            masterNorthOffsetIndex = nbParams++;
        } else {
            masterNorthOffsetIndex = -1;
        }
        final int masterZenithOffsetIndex;
        if (masterStation.getZenithOffsetDriver().isSelected()) {
            masterZenithOffsetIndex = nbParams++;
        } else {
            masterZenithOffsetIndex = -1;
        }
        final int slaveEastOffsetIndex;
        if (slaveStation.getEastOffsetDriver().isSelected()) {
            slaveEastOffsetIndex = nbParams++;
        } else {
            slaveEastOffsetIndex = -1;
        }
        final int slaveNorthOffsetIndex;
        if (slaveStation.getNorthOffsetDriver().isSelected()) {
            slaveNorthOffsetIndex = nbParams++;
        } else {
            slaveNorthOffsetIndex = -1;
        }
        final int slaveZenithOffsetIndex;
        if (slaveStation.getZenithOffsetDriver().isSelected()) {
            slaveZenithOffsetIndex = nbParams++;
        } else {
            slaveZenithOffsetIndex = -1;
        }
        final DSFactory                          factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure>         field   = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero    = FieldVector3D.getZero(field);

        // PV coordinates of the spacecraft at time t'
        final PVCoordinates statePV = state.getPVCoordinates();

        // Position of the spacecraft expressed as a derivative structure
        // The components of the position are the 3 first derivative parameters
        final Vector3D stateP = statePV.getPosition();
        final FieldVector3D<DerivativeStructure> pDS =
                        new FieldVector3D<>(factory.variable(0, stateP.getX()),
                                            factory.variable(1, stateP.getY()),
                                            factory.variable(2, stateP.getZ()));

        // Velocity of the spacecraft expressed as a derivative structure
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D stateV = statePV.getVelocity();
        final FieldVector3D<DerivativeStructure> vDS =
                        new FieldVector3D<>(factory.variable(3, stateV.getX()),
                                            factory.variable(4, stateV.getY()),
                                            factory.variable(5, stateV.getZ()));

        // Acceleration of the spacecraft
        // The components of the acceleration are not derivative parameters
        final Vector3D stateA = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<DerivativeStructure> aDS =
                        new FieldVector3D<>(factory.constant(stateA.getX()),
                                            factory.constant(stateA.getY()),
                                            factory.constant(stateA.getZ()));

        // Place the derivative structures in a time-stamped PV
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS =
                        new TimeStampedFieldPVCoordinates<>(state.getDate(), pDS, vDS, aDS);

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
        final AbsoluteDate measurementDate = getDate();
        final FieldAbsoluteDate<DerivativeStructure> measurementDateDS = new FieldAbsoluteDate<>(field, measurementDate);
        final double delta = measurementDate.durationFrom(state.getDate());

        // transform between master station topocentric frame (east-north-zenith) and inertial frame expressed as DerivativeStructures
        // The components of master station's position in offset frame are the 3 third derivative parameters
        final FieldTransform<DerivativeStructure> masterToInert =
                        masterStation.getOffsetToInertial(state.getFrame(), measurementDateDS, factory,
                                                          primeMeridianOffsetIndex, primeMeridianDriftIndex,
                                                          polarOffsetXIndex, polarDriftXIndex,
                                                          polarOffsetYIndex, polarDriftYIndex,
                                                          masterEastOffsetIndex, masterNorthOffsetIndex,
                                                          masterZenithOffsetIndex);

        // Master station PV in inertial frame at measurement date
        final FieldVector3D<DerivativeStructure> QMaster = masterToInert.transformPosition(zero);

        // Compute propagation times
        final DerivativeStructure masterTauD = masterStation.signalTimeOfFlight(pvaDS, QMaster, measurementDateDS);

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
                        slaveStation.getOffsetToInertial(state.getFrame(), approxReboundDate, factory,
                                                         primeMeridianOffsetIndex, primeMeridianDriftIndex,
                                                         polarOffsetXIndex, polarDriftXIndex,
                                                         polarOffsetYIndex, polarDriftYIndex,
                                                         slaveEastOffsetIndex, slaveNorthOffsetIndex,
                                                         slaveZenithOffsetIndex);

        // Slave station PV in inertial frame at approximate rebound date on slave station
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlaveApprox =
                        slaveToInertApprox.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxReboundDate,
                                                                                                      zero, zero, zero));

        // Uplink time of flight from slave station to transit state of leg2
        final DerivativeStructure slaveTauU =
                        slaveStation.signalTimeOfFlight(QSlaveApprox,
                                                        transitStateLeg2PV.getPosition(),
                                                        transitStateLeg2PV.getDate());

        // Total time of flight for leg 2
        final DerivativeStructure tauLeg2 = masterTauD.add(slaveTauU);

        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of rebound of the signal to slave station
        final FieldAbsoluteDate<DerivativeStructure> reboundDateDS = measurementDateDS.shiftedBy(tauLeg2.negate());
        final FieldTransform<DerivativeStructure> slaveToInert =
                        slaveStation.getOffsetToInertial(state.getFrame(), reboundDateDS, factory,
                                                         primeMeridianOffsetIndex, primeMeridianDriftIndex,
                                                         polarOffsetXIndex, polarDriftXIndex,
                                                         polarOffsetYIndex, polarDriftYIndex,
                                                         slaveEastOffsetIndex, slaveNorthOffsetIndex,
                                                         slaveZenithOffsetIndex);

        // Slave station PV in inertial frame at rebound date on slave station
        final FieldVector3D<DerivativeStructure> QSlave = slaveToInert.transformPosition(zero);

        // Downlink time of flight from transitStateLeg1 to slave station at rebound date
        final DerivativeStructure slaveTauD =
                        slaveStation.signalTimeOfFlight(transitStateLeg2PV,
                                                        QSlave,
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
                        masterStation.getOffsetToInertial(state.getFrame(), approxEmissionDate, factory,
                                                          primeMeridianOffsetIndex, primeMeridianDriftIndex,
                                                          polarOffsetXIndex, polarDriftXIndex,
                                                          polarOffsetYIndex, polarDriftYIndex,
                                                          masterEastOffsetIndex, masterNorthOffsetIndex,
                                                          masterZenithOffsetIndex);

        // Master station PV in inertial frame at approximate emission date
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QMasterApprox =
                        masterToInertApprox.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(approxEmissionDate,
                                                                                                       zero, zero, zero));

        // Uplink time of flight from master station to transit state of leg1
        final DerivativeStructure masterTauU =
                        masterStation.signalTimeOfFlight(QMasterApprox,
                                                         transitStateLeg1PV.getPosition(),
                                                         transitStateLeg1PV.getDate());

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
                                                   transitStateLeg2.shiftedBy(-slaveTauU.getValue()));

        // Turn-around range value = Total time of flight for the 2 legs divided by 2 and multiplied by c
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final DerivativeStructure turnAroundRange = (tauLeg2.add(tauLeg1)).multiply(cOver2);
        estimated.setEstimatedValue(turnAroundRange.getValue());

        // Turn-around range partial derivatives with respect to state
        final double[] derivatives = turnAroundRange.getAllDerivatives();
        estimated.setStateDerivatives(Arrays.copyOfRange(derivatives, 1, 7));

        // set partial derivatives with respect to parameters
        setDerivatives(estimated, masterStation.getPrimeMeridianOffsetDriver(), primeMeridianOffsetIndex, derivatives);
        setDerivatives(estimated, masterStation.getPrimeMeridianDriftDriver(),  primeMeridianDriftIndex,  derivatives);
        setDerivatives(estimated, masterStation.getPolarOffsetXDriver(),        polarOffsetXIndex,        derivatives);
        setDerivatives(estimated, masterStation.getPolarDriftXDriver(),         polarDriftXIndex,         derivatives);
        setDerivatives(estimated, masterStation.getPolarOffsetYDriver(),        polarOffsetYIndex,        derivatives);
        setDerivatives(estimated, masterStation.getPolarDriftYDriver(),         polarDriftYIndex,         derivatives);
        setDerivatives(estimated, masterStation.getEastOffsetDriver(),          masterEastOffsetIndex,    derivatives);
        setDerivatives(estimated, masterStation.getNorthOffsetDriver(),         masterNorthOffsetIndex,   derivatives);
        setDerivatives(estimated, masterStation.getZenithOffsetDriver(),        masterZenithOffsetIndex,  derivatives);
        setDerivatives(estimated, slaveStation.getEastOffsetDriver(),           slaveEastOffsetIndex,     derivatives);
        setDerivatives(estimated, slaveStation.getNorthOffsetDriver(),          slaveNorthOffsetIndex,    derivatives);
        setDerivatives(estimated, slaveStation.getZenithOffsetDriver(),         slaveZenithOffsetIndex,   derivatives);

        return estimated;

    }

    /** Set derivatives with resptect to parameters.
     * @param estimated estimated measurement
     * @param driver parameter driver
     * @param index index of the parameter in the set of
     * free parameters in derivatives computations (negative if not used)
     * @param derivatives derivatives (beware element at index 0 is the value, not a derivative)
     */
    private void setDerivatives(final EstimatedMeasurement<TurnAroundRange> estimated,
                                final ParameterDriver driver, final int index,
                                final double[] derivatives) {
        if (index >= 0) {
            estimated.setParameterDerivatives(driver, derivatives[index + 1]);
        }
    }

}
