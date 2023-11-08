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

import java.util.Arrays;
import java.util.Map;
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
     * A turn-around measurement needs 2 stations, a primary and a secondary
     * The measurement is a signal:
     * - Emitted from the primary ground station
     * - Reflected on the spacecraft
     * - Reflected on the secondary ground station
     * - Reflected on the spacecraft again
     * - Received on the primary ground station
     * Its value is the elapsed time between emission and reception
     * divided by 2c were c is the speed of light.
     *
     * The path of the signal is divided into 2 legs:
     *  - The 1st leg goes from emission by the primary station to reception by the secondary station
     *  - The 2nd leg goes from emission by the secondary station to reception by the primary station
     *
     * The spacecraft state date should, after a few iterations of the estimation process, be
     * set to the date of arrival/departure of the signal to/from the secondary station.
     * It is guaranteed by implementation of the estimated measurement.
     * This is done to avoid big shifts in time to compute the transit states.
     * See TurnAroundRange.java for more
     * Thus the spacecraft date is the date when the 1st leg of the path ends and the 2nd leg begins
     */
    public void handleStep(final SpacecraftState currentState) {
        try {
            for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {

                final GroundStation    primaryStation = entry.getKey();
                final GroundStation    secondaryStation  = entry.getValue();
                final AbsoluteDate     date          = currentState.getDate();
                final Frame            inertial      = currentState.getFrame();
                final Vector3D         position      = currentState.toTransform().toStaticTransform().getInverse().transformPosition(antennaPhaseCenter);

                // Create a TAR measurement only if elevation for both stations is higher than elevationMin°
                if ((primaryStation.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation() > FastMath.toRadians(30.0))&&
                    (secondaryStation.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation()  > FastMath.toRadians(30.0))) {

                    // The solver used
                    final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

                    // Spacecraft date t = date of arrival/departure of the signal to/from from the secondary station
                    // secondary station position in inertial frame at t
                    final Vector3D secondaryStationPosition =
                                    secondaryStation.getOffsetToInertial(inertial, date, false).transformPosition(Vector3D.ZERO);

                    // Downlink time of flight to secondary station
                    // The date of arrival/departure of the signal to/from the secondary station is known and
                    // equal to spacecraft date t.
                    // Therefore we can use the function "downlinkTimeOfFlight" from GroundStation class
                    // final double secondaryTauD = secondaryStation.downlinkTimeOfFlight(currentState, date);
                    final double secondaryTauD  = solver.solve(1000, new UnivariateFunction() {
                        public double value(final double x) {
                            final SpacecraftState transitState = currentState.shiftedBy(-x);
                            final double d = Vector3D.distance(transitState.toTransform().toStaticTransform().getInverse().transformPosition(antennaPhaseCenter),
                                                               secondaryStationPosition);
                            return d - x * Constants.SPEED_OF_LIGHT;
                        }
                    }, -1.0, 1.0);

                    // Uplink time of flight from secondary station
                    // A solver is used to know where the satellite is when it receives the signal
                    // back from the secondary station
                    final double secondaryTauU  = solver.solve(1000, new UnivariateFunction() {
                        public double value(final double x) {
                            final SpacecraftState transitState = currentState.shiftedBy(+x);
                            final double d = Vector3D.distance(transitState.toTransform().toStaticTransform().getInverse().transformPosition(antennaPhaseCenter),
                                                               secondaryStationPosition);
                            return d - x * Constants.SPEED_OF_LIGHT;
                        }
                    }, -1.0, 1.0);


                    // Find the position of the primary station at signal departure and arrival
                    // ----

                    // Transit state position & date for the 1st leg of the signal path
                    final SpacecraftState S1 = currentState.shiftedBy(-secondaryTauD);
                    final Vector3D        P1 = S1.toTransform().toStaticTransform().getInverse().transformPosition(antennaPhaseCenter);
                    final AbsoluteDate    T1 = date.shiftedBy(-secondaryTauD);

                    // Transit state position & date for the 2nd leg of the signal path
                    final Vector3D     P2  = currentState.shiftedBy(+secondaryTauU).toTransform().toStaticTransform().getInverse().transformPosition(antennaPhaseCenter);
                    final AbsoluteDate T2  = date.shiftedBy(+secondaryTauU);


                    // Primary station downlink delay - from P2 to primary station
                    // We use a solver to know where the primary station is when it receives
                    // the signal back from the satellite on the 2nd leg of the path
                    final double primaryTauD  = solver.solve(1000, new UnivariateFunction() {
                        public double value(final double x) {
                            final Transform t = primaryStation.getOffsetToInertial(inertial, T2.shiftedBy(+x), false);
                            final double d = Vector3D.distance(P2, t.transformPosition(Vector3D.ZERO));
                            return d - x * Constants.SPEED_OF_LIGHT;
                        }
                    }, -1.0, 1.0);

                    final AbsoluteDate primaryReceptionDate  = T2.shiftedBy(+primaryTauD);
                    final TimeStampedPVCoordinates primaryStationAtReception =
                                    primaryStation.getOffsetToInertial(inertial, primaryReceptionDate, false).
                                    transformPVCoordinates(new TimeStampedPVCoordinates(primaryReceptionDate, PVCoordinates.ZERO));


                    // Primary station uplink delay - from primary station to P1
                    // Here the state date is known. Thus we can use the function "signalTimeOfFlight"
                    // of the AbstractMeasurement class
                    final double primaryTauU = AbstractMeasurement.signalTimeOfFlight(primaryStationAtReception, P1, T1);

                    final AbsoluteDate primaryEmissionDate   = T1.shiftedBy(-primaryTauU);

                    final Vector3D primaryStationAtEmission  =
                                    primaryStation.getOffsetToInertial(inertial, primaryEmissionDate, false).transformPosition(Vector3D.ZERO);


                    // Uplink/downlink distance from/to secondary station
                    final double secondaryDownLinkDistance  = Vector3D.distance(P1, secondaryStationPosition);
                    final double secondaryUpLinkDistance    = Vector3D.distance(P2, secondaryStationPosition);

                    // Uplink/downlink distance from/to primary station
                    final double primaryUpLinkDistance   = Vector3D.distance(P1, primaryStationAtEmission);
                    final double primaryDownLinkDistance = Vector3D.distance(P2, primaryStationAtReception.getPosition());

                    addMeasurement(new TurnAroundRange(primaryStation, secondaryStation, primaryReceptionDate,
                                             0.5 * (primaryUpLinkDistance + secondaryDownLinkDistance +
                                                    secondaryUpLinkDistance  + primaryDownLinkDistance), 1.0, 10, satellite));
                }

            }
        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

}
