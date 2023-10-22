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
import org.orekit.estimation.StationDataProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.gnss.antenna.PhaseCenterVariationFunction;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

public class TwoWayRangeMeasurementCreator extends MeasurementCreator {

    private final StationDataProvider          provider;
    private final Vector3D                     stationMeanPosition;
    private final PhaseCenterVariationFunction stationPhaseCenterVariation;
    private final Vector3D                     satelliteMeanPosition;
    private final PhaseCenterVariationFunction satellitePhaseCenterVariation;
    private final ObservableSatellite          satellite;
    private final double                       bias;

    public TwoWayRangeMeasurementCreator(final StationDataProvider context) {
        this(context, Vector3D.ZERO, null, Vector3D.ZERO, null, 0.0);
    }

    public TwoWayRangeMeasurementCreator(final StationDataProvider provider,
                                         final Vector3D stationMeanPosition,   final PhaseCenterVariationFunction stationPhaseCenterVariation,
                                         final Vector3D satelliteMeanPosition, final PhaseCenterVariationFunction satellitePhaseCenterVariation,
                                         final double bias) {
        this.provider                      = provider;
        this.stationMeanPosition           = stationMeanPosition;
        this.stationPhaseCenterVariation   = stationPhaseCenterVariation;
        this.satelliteMeanPosition         = satelliteMeanPosition;
        this.satellitePhaseCenterVariation = satellitePhaseCenterVariation;
        this.satellite                     = new ObservableSatellite(0);
        this.bias                          = bias;
    }

    public StationDataProvider getStationDataProvider() {
        return provider;
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (final GroundStation station : provider.getStations()) {
            for (ParameterDriver driver : Arrays.asList(station.getClockOffsetDriver(),
                                                        station.getClockDriftDriver(),
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

    public void handleStep(final SpacecraftState currentState) {
        for (final GroundStation station : provider.getStations()) {
            final AbsoluteDate     date      = currentState.getDate();
            final Frame            inertial  = currentState.getFrame();
            final Vector3D         position  = currentState.toTransform().toStaticTransform().getInverse().transformPosition(satelliteMeanPosition);

            if (station.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation() > FastMath.toRadians(30.0)) {
                final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

                final double downLinkDelay  = solver.solve(1000, new UnivariateFunction() {
                    public double value(final double x) {
                        final Transform t = station.getOffsetToInertial(inertial, date.shiftedBy(x), true);
                        final double d = Vector3D.distance(position, t.transformPosition(stationMeanPosition));
                        return d - x * Constants.SPEED_OF_LIGHT;
                    }
                }, -1.0, 1.0);
                final AbsoluteDate receptionDate           = currentState.getDate().shiftedBy(downLinkDelay);
                final Transform    stationToInertReception = station.getOffsetToInertial(inertial, receptionDate, true);
                final Vector3D     stationAtReception      = stationToInertReception.transformPosition(stationMeanPosition);
                final double       downLinkDistance        = Vector3D.distance(position, stationAtReception);

                final Vector3D satLosDown = currentState.toTransform().
                                            transformVector(stationAtReception.subtract(position));
                final double   satPCVDown = satellitePhaseCenterVariation == null ?
                                            0.0 :
                                            satellitePhaseCenterVariation.value(0.5 * FastMath.PI - satLosDown.getDelta(),
                                                                                satLosDown.getAlpha());
                final Vector3D staLosDown = stationToInertReception.getInverse().
                                            transformVector(position.subtract(stationAtReception));
                final double   staPCVDown = stationPhaseCenterVariation == null ?
                                            0.0 :
                                            stationPhaseCenterVariation.value(0.5 * FastMath.PI - staLosDown.getDelta(),
                                                                              staLosDown.getAlpha());

                final double correctedDownLinkDistance = downLinkDistance + satPCVDown + staPCVDown;

                final double upLinkDelay = solver.solve(1000, new UnivariateFunction() {
                    public double value(final double x) {
                        final Transform t = station.getOffsetToInertial(inertial, date.shiftedBy(-x), true);
                        final double d = Vector3D.distance(position, t.transformPosition(stationMeanPosition));
                        return d - x * Constants.SPEED_OF_LIGHT;
                    }
                }, -1.0, 1.0);
                final AbsoluteDate emissionDate           = currentState.getDate().shiftedBy(-upLinkDelay);
                final Transform    stationToInertEmission = station.getOffsetToInertial(inertial, emissionDate, true);
                final Vector3D     stationAtEmission      = stationToInertEmission.transformPosition(stationMeanPosition);
                final double       upLinkDistance         = Vector3D.distance(position, stationAtEmission);

                final Vector3D staLosUp   = stationToInertEmission.getInverse().
                                            transformVector(position.subtract(stationAtEmission));
                final double   staPCVUp   = stationPhaseCenterVariation == null ?
                                            0.0 :
                                            stationPhaseCenterVariation.value(0.5 * FastMath.PI - staLosUp.getDelta(),
                                                                              staLosUp.getAlpha());
                final Vector3D satLosUp   = currentState.toTransform().
                                            transformVector(stationAtEmission.subtract(position));
                final double   satPCVUp   = satellitePhaseCenterVariation == null ?
                                            0.0 :
                                            satellitePhaseCenterVariation.value(0.5 * FastMath.PI - satLosUp.getDelta(),
                                                                                satLosUp.getAlpha());

                final double correctedUpLinkDistance = upLinkDistance + satPCVUp + staPCVUp;

                final double clockOffset = station.getClockOffsetDriver().getValue(date);
                addMeasurement(new Range(station, true, receptionDate.shiftedBy(clockOffset),
                                         0.5 * (correctedDownLinkDistance + correctedUpLinkDistance) + bias,
                                         1.0, 10, satellite));

            }

        }
    }

}
