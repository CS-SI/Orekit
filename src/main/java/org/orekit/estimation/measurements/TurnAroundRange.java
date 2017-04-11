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

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.GroundStation.OffsetDerivatives;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
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

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;


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
              slaveStation.getEastOffsetDriver(),
              slaveStation.getNorthOffsetDriver(),
              slaveStation.getZenithOffsetDriver());
        this.masterStation = masterStation;
        this.slaveStation = slaveStation;
        this.factory = new DSFactory(12, 1);
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

    /** Get the DSFactory of this class.
     * @return DSFactory of this class
     */
    protected DSFactory getDSFactory() {
        return factory;
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

        // PV coordinates of the spacecraft at time t'
        final PVCoordinates statePV = state.getPVCoordinates();

        // Position of the spacecraft expressed as a derivative structure
        // The components of the position are the 3 first derivative parameters
        final Vector3D stateP = statePV.getPosition();
        final FieldVector3D<DerivativeStructure> pDS = new FieldVector3D<DerivativeStructure>(
                        factory.variable(0, stateP.getX()),
                        factory.variable(1, stateP.getY()),
                        factory.variable(2, stateP.getZ()));

        // Velocity of the spacecraft expressed as a derivative structure
        // The components of the velocity are the 3 second derivative parameters
        final Vector3D stateV = statePV.getVelocity();
        final FieldVector3D<DerivativeStructure> vDS = new FieldVector3D<DerivativeStructure>(
                        factory.variable(3, stateV.getX()),
                        factory.variable(4, stateV.getY()),
                        factory.variable(5, stateV.getZ()));

        // Acceleration of the spacecraft
        // The components of the acceleration are not derivative parameters
        final Vector3D stateA = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<DerivativeStructure> aDS = new FieldVector3D<DerivativeStructure>(
                        factory.constant(stateA.getX()),
                        factory.constant(stateA.getY()),
                        factory.constant(stateA.getZ()));

        // Place the derivative structures in a time-stamped PV
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS =
                        new TimeStampedFieldPVCoordinates<DerivativeStructure>(state.getDate(), pDS, vDS, aDS);

        // Master station topocentric frame (east-north-zenith) in master station parent frame expressed as DerivativeStructures
        // The components of master station's position in offset frame are the 3 third derivative parameters
        final OffsetDerivatives masterOd = masterStation.getOffsetDerivatives(factory, 6, 7, 8);

        // Slave station topocentric frame (east-north-zenith) in slave station parent frame expressed as DerivativeStructures
        // The components of slave station's position in offset frame are the 3 last derivative parameters
        final OffsetDerivatives slaveOd = slaveStation.getOffsetDerivatives(factory, 9, 10, 11);

        // Master station body frame
        final Frame masterBodyFrame = masterStation.getOffsetFrame().getParentShape().getBodyFrame();

        // Master station PV in inertial frame at measurement date
        final AbsoluteDate measurementDate = this.getDate();
        final Transform masterBodyToInert = masterBodyFrame.getTransformTo(state.getFrame(), measurementDate);
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QMaster =
                        masterBodyToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(
                                        measurementDate,
                                        masterOd.getOrigin(),
                                        masterOd.getZero(),
                                        masterOd.getZero()));

        // Slave station body frame
        final Frame slaveBodyFrame = slaveStation.getOffsetFrame().getParentShape().getBodyFrame();

        // Slave station PV in inertial frame at measurement date
        final Transform slaveBodyToInert = slaveBodyFrame.getTransformTo(state.getFrame(), measurementDate);
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlave =
                        slaveBodyToInert.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(
                                        measurementDate,
                                        slaveOd.getOrigin(),
                                        slaveOd.getZero(),
                                        slaveOd.getZero()));

        // Compute propagation times
        //
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

        final DerivativeStructure masterTauD = masterStation.
                        signalTimeOfFlight(pvaDS, QMaster.getPosition(), measurementDate);


        // Elapsed time between state date t' and signal arrival to the transit state of the 2nd leg
        final DerivativeStructure dtLeg2 = masterTauD.negate().add(delta);

        // Transit state where the satellite reflected the signal from slave to master station
        final SpacecraftState transitStateLeg2   = state.shiftedBy(dtLeg2.getValue());

        // Transit state pv of leg2 (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateLeg2PV = pvaDS.shiftedBy(dtLeg2);

        // Slave station at transit state date of leg2 (derivatives of masterTauD taken into account)
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlaveAtTransitLeg2 =
                        QSlave.shiftedBy(masterTauD.negate());

        // Uplink time of flight from slave station to transit state of leg2
        final DerivativeStructure slaveTauU = slaveStation.
                        signalTimeOfFlight(QSlaveAtTransitLeg2,
                                           transitStateLeg2PV.getPosition(),
                                           transitStateLeg2.getDate());

        // Total time of flight for leg 2
        final DerivativeStructure tauLeg2 = masterTauD.add(slaveTauU);


        // Compute propagation time for the 1st leg of the signal path
        // --

        // Absolute date of arrival/departure of the signal to slave station
        final AbsoluteDate slaveStationArrivalDate = measurementDate.shiftedBy(-tauLeg2.getValue());

        // Slave station PV in inertial frame at date slaveStationArrivalDate
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QSlaveArrivalDate =
                        QSlave.shiftedBy(tauLeg2.negate());

        // Dowlink time of flight from transitStateLeg1 to slave station at slaveStationArrivalDate
        final DerivativeStructure slaveTauD = slaveStation.
                        signalTimeOfFlight(transitStateLeg2PV, QSlaveArrivalDate.getPosition(), slaveStationArrivalDate);


        // Elapsed time between state date t' and signal arrival to the transit state of the 1st leg
        final DerivativeStructure dtLeg1 = dtLeg2.subtract(slaveTauU).subtract(slaveTauD);

        // Transit state from which the satellite reflected the signal from master to slave station
        final SpacecraftState transitStateLeg1   = state.shiftedBy(dtLeg1.getValue());

        // Transit state pv of leg2 (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateLeg1PV = pvaDS.shiftedBy(dtLeg1);

        // Master station at transit state date of leg1 (derivatives of masterTauD, slaveTauU and slaveTauD taken into account)
        final TimeStampedFieldPVCoordinates<DerivativeStructure> QMasterAtTransitLeg1 =
                        QMaster.shiftedBy(tauLeg2.negate().subtract(slaveTauD));

        // Uplink time of flight from master station to transit state of leg1
        final DerivativeStructure masterTauU = masterStation.
                        signalTimeOfFlight(QMasterAtTransitLeg1,
                                           transitStateLeg1PV.getPosition(),
                                           transitStateLeg1.getDate());

        // Total time of flight for leg 1
        final DerivativeStructure tauLeg1    = slaveTauD.add(masterTauU);


        // --
        // Evaluate the turn-around range value and its derivatives
        // --------------------------------------------------------

        // The state we use to define the estimated measurement is a middle ground between the two transit states
        // This is done to avoid calling "SpacecraftState.shiftedBy" function on long duration
        // Thus we define the state at the date t" = date of arrival of the signal to the slave station
        // Or t" = t -masterTauD -slaveTauU
        // The iterative process in the estimation ensures that, after several iterations, the date stamped in the
        // state S in input of this function will be close to t"
        // Therefore we will shift state S by:
        //  - +slaveTauU to get transitStateLeg2
        //  - -slaveTauD to get transitStateLeg1
        final EstimatedMeasurement<TurnAroundRange> estimated =
                        new EstimatedMeasurement<TurnAroundRange>(this,
                                        iteration, evaluation,
                                        transitStateLeg2.shiftedBy(-slaveTauU.getValue()));

        // Turn-around range value = Total time of flight for the 2 legs divided by 2 and multiplied by c
        final double cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final DerivativeStructure turnAroundRange = (tauLeg2.add(tauLeg1)).multiply(cOver2);
        estimated.setEstimatedValue(turnAroundRange.getValue());


        // Turn-around range partial derivatives with respect to state
        estimated.setStateDerivatives(new double[] {turnAroundRange.getPartialDerivative(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // dROndPx
                                                    turnAroundRange.getPartialDerivative(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // dROndPy
                                                    turnAroundRange.getPartialDerivative(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0), // dROndPz
                                                    turnAroundRange.getPartialDerivative(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0), // dROndVx
                                                    turnAroundRange.getPartialDerivative(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), // dROndVy
                                                    turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0), // dROndVz
        });


        // Set parameter drivers partial derivatives with respect to stations' position in stations'offset topocentric frame
        // Master station
        if (masterStation.getEastOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(masterStation.getEastOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)); // dROndQMTx
        }
        if (masterStation.getNorthOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(masterStation.getNorthOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0)); // dROndQMTy
        }
        if (masterStation.getZenithOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(masterStation.getZenithOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0)); // dROndQMTz
        }

        // Slave station
        if (slaveStation.getEastOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(slaveStation.getEastOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)); // dROndQSTx
        }
        if (slaveStation.getNorthOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(slaveStation.getNorthOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0)); // dROndQSTy
        }
        if (slaveStation.getZenithOffsetDriver().isSelected()) {
            estimated.setParameterDerivatives(slaveStation.getZenithOffsetDriver(),
                                              turnAroundRange.getPartialDerivative(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)); // dROndQSTz
        }

        return estimated;

    }
}
