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
import java.util.Map;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;
/**
 * Class creating a list of turn-around range measurement
 * @author Maxime Journot
 *
 */
public class TurnAroundRangeMeasurementCreator extends MeasurementCreator {

    private final Context context;
    private final Vector3D antennaPhaseCenter;
    private final ObservableSatellite satellite;

    public TurnAroundRangeMeasurementCreator(final Context context) {
        this(context, Vector3D.ZERO);
    }

    public TurnAroundRangeMeasurementCreator(final Context context, final Vector3D antennaPhaseCenter) {
        this.context            = context;
        this.antennaPhaseCenter = antennaPhaseCenter;
        this.satellite          = new ObservableSatellite(0);
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {
            for (final GroundStation station : Arrays.asList(entry.getKey(), entry.getValue())) {
                for (ParameterDriver driver : Arrays.asList(station.getClockOffsetDriver(),
                                                            station.getEastOffsetDriver(),
                                                            station.getNorthOffsetDriver(),
                                                            station.getZenithOffsetDriver(),
                                                            station.getPrimeMeridianOffsetDriver(),
                                                            station.getPrimeMeridianDriftDriver(),
                                                            station.getPolarOffsetXDriver(),
                                                            station.getPolarDriftXDriver(),
                                                            station.getPolarOffsetYDriver(),
                                                            station.getPolarDriftYDriver())) {
                    if (driver.getReferenceDate() == null) {
                        driver.setReferenceDate(s0.getDate());
                    }
                }

            }
        }
    }

    /**
     * Function handling the steps of the propagator
     * A turn-around measurement needs 2 stations, a master and a slave
     * The measurement is a signal:
     * - Emitted from the master ground station
     * - Reflected on the spacecraft
     * - Reflected on the slave ground station
     * - Reflected on the spacecraft again
     * - Received on the master ground station
     * Its value is the elapsed time between emission and reception
     * divided by 2c were c is the speed of light.
     *
     * The path of the signal is divided into 2 legs:
     *  - The 1st leg goes from emission by the master station to reception by the slave station
     *  - The 2nd leg goes from emission by the slave station to reception by the master station
     *
     * The spacecraft state date should, after a few iterations of the estimation process, be
     * set to the date of arrival/departure of the signal to/from the slave station.
     * It is guaranteed by implementation of the estimated measurement.
     * This is done to avoid big shifts in time to compute the transit states.
     * See TurnAroundRange.java for more
     * Thus the spacecraft date is the date when the 1st leg of the path ends and the 2nd leg begins
     */
    public void handleStep(final SpacecraftState currentState, final boolean isLast) {
        try {
            for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {

                final GroundStation    masterStation = entry.getKey();
                final GroundStation    slaveStation  = entry.getValue();
                final AbsoluteDate     date          = currentState.getDate();
                final Frame            inertial      = currentState.getFrame();
                final Vector3D         position      = currentState.toTransform().getInverse().transformPosition(antennaPhaseCenter);

                // Create a TAR measurement only if elevation for both stations is higher than elevationMin°
                if ((masterStation.getBaseFrame().getElevation(position, inertial, date) > FastMath.toRadians(30.0))&&
                    (slaveStation.getBaseFrame().getElevation(position, inertial, date)  > FastMath.toRadians(30.0))) {

                    // The solver used
                    final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

                    // Spacecraft date t = date of arrival/departure of the signal to/from from the slave station
                    // Slave station position in inertial frame at t
                    final Vector3D slaveStationPosition =
                                    slaveStation.getOffsetToInertial(inertial, date).transformPosition(Vector3D.ZERO);

                    // Downlink time of flight to slave station
                    // The date of arrival/departure of the signal to/from the slave station is known and
                    // equal to spacecraft date t.
                    // Therefore we can use the function "downlinkTimeOfFlight" from GroundStation class
                    // final double slaveTauD = slaveStation.downlinkTimeOfFlight(currentState, date);
                    final double slaveTauD  = solver.solve(1000, new UnivariateFunction() {
                        public double value(final double x) {
                            final SpacecraftState transitState = currentState.shiftedBy(-x);
                            final double d = Vector3D.distance(transitState.toTransform().getInverse().transformPosition(antennaPhaseCenter),
                                                               slaveStationPosition);
                            return d - x * Constants.SPEED_OF_LIGHT;
                        }
                    }, -1.0, 1.0);

                    // Uplink time of flight from slave station
                    // A solver is used to know where the satellite is when it receives the signal
                    // back from the slave station
                    final double slaveTauU  = solver.solve(1000, new UnivariateFunction() {
                        public double value(final double x) {
                            final SpacecraftState transitState = currentState.shiftedBy(+x);
                            final double d = Vector3D.distance(transitState.toTransform().getInverse().transformPosition(antennaPhaseCenter),
                                                               slaveStationPosition);
                            return d - x * Constants.SPEED_OF_LIGHT;
                        }
                    }, -1.0, 1.0);


                    // Find the position of the master station at signal departure and arrival
                    // ----

                    // Transit state position & date for the 1st leg of the signal path
                    final SpacecraftState S1 = currentState.shiftedBy(-slaveTauD);
                    final Vector3D        P1 = S1.toTransform().getInverse().transformPosition(antennaPhaseCenter);
                    final AbsoluteDate    T1 = date.shiftedBy(-slaveTauD);

                    // Transit state position & date for the 2nd leg of the signal path
                    final Vector3D     P2  = currentState.shiftedBy(+slaveTauU).toTransform().getInverse().transformPosition(antennaPhaseCenter);
                    final AbsoluteDate T2  = date.shiftedBy(+slaveTauU);


                    // Master station downlink delay - from P2 to master station
                    // We use a solver to know where the master station is when it receives
                    // the signal back from the satellite on the 2nd leg of the path
                    final double masterTauD  = solver.solve(1000, new UnivariateFunction() {
                        public double value(final double x) {
                            final Transform t = masterStation.getOffsetToInertial(inertial, T2.shiftedBy(+x));
                            final double d = Vector3D.distance(P2, t.transformPosition(Vector3D.ZERO));
                            return d - x * Constants.SPEED_OF_LIGHT;
                        }
                    }, -1.0, 1.0);

                    final AbsoluteDate masterReceptionDate  = T2.shiftedBy(+masterTauD);
                    final TimeStampedPVCoordinates masterStationAtReception =
                                    masterStation.getOffsetToInertial(inertial, masterReceptionDate).
                                    transformPVCoordinates(new TimeStampedPVCoordinates(masterReceptionDate, PVCoordinates.ZERO));


                    // Master station uplink delay - from master station to P1
                    // Here the state date is known. Thus we can use the function "signalTimeOfFlight"
                    // of the AbstractMeasurement class
                    final double masterTauU = AbstractMeasurement.signalTimeOfFlight(masterStationAtReception, P1, T1);

                    final AbsoluteDate masterEmissionDate   = T1.shiftedBy(-masterTauU);

                    final Vector3D masterStationAtEmission  =
                                    masterStation.getOffsetToInertial(inertial, masterEmissionDate).transformPosition(Vector3D.ZERO);


                    // Uplink/downlink distance from/to slave station
                    final double slaveDownLinkDistance  = Vector3D.distance(P1, slaveStationPosition);
                    final double slaveUpLinkDistance    = Vector3D.distance(P2, slaveStationPosition);

                    // Uplink/downlink distance from/to master station
                    final double masterUpLinkDistance   = Vector3D.distance(P1, masterStationAtEmission);
                    final double masterDownLinkDistance = Vector3D.distance(P2, masterStationAtReception.getPosition());

                    addMeasurement(new TurnAroundRange(masterStation, slaveStation, masterReceptionDate,
                                             0.5 * (masterUpLinkDistance + slaveDownLinkDistance +
                                                    slaveUpLinkDistance  + masterDownLinkDistance), 1.0, 10, satellite));
                }

            }
        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

}
