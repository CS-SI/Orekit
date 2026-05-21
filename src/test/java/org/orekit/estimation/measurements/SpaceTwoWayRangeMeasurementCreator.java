/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.estimation.StationDataProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.gnss.antenna.PhaseCenterVariationFunction;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class SpaceTwoWayRangeMeasurementCreator extends MeasurementCreator {

    private final StationDataProvider provider;
    private final Vector3D observerMeanPosition;
    private final PhaseCenterVariationFunction observerPhaseCenterVariation;
    private final Vector3D satelliteMeanPosition;
    private final PhaseCenterVariationFunction satellitePhaseCenterVariation;
    private final ObservableSatellite observedSatellite;
    private final double bias;

    public SpaceTwoWayRangeMeasurementCreator(final StationDataProvider context) {
        this(context, Vector3D.ZERO, null, Vector3D.ZERO, null, 0.0);
    }

    public SpaceTwoWayRangeMeasurementCreator(final StationDataProvider provider,
            final Vector3D observerMeanPosition, final PhaseCenterVariationFunction observerPhaseCenterVariation,
            final Vector3D satelliteMeanPosition, final PhaseCenterVariationFunction satellitePhaseCenterVariation,
            final double bias) {
        this.provider = provider;
        this.observerMeanPosition = observerMeanPosition;
        this.observerPhaseCenterVariation = observerPhaseCenterVariation;
        this.satelliteMeanPosition = satelliteMeanPosition;
        this.satellitePhaseCenterVariation = satellitePhaseCenterVariation;
        this.observedSatellite = new ObservableSatellite(0);
        this.bias = bias;
    }

    public StationDataProvider getContextProvider() {
        return provider;
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (final ObserverSatellite satellite : provider.getSatellites()) {
            for (ParameterDriver driver : satellite.getParametersDrivers()) {
                if (driver.getReferenceDate() == null) {
                    driver.setReferenceDate(s0.getDate());
                }
            }
        }
    }

    public void handleStep(final SpacecraftState currentState) {

        for (final ObserverSatellite satellite : provider.getSatellites()) {
            final AbsoluteDate date = currentState.getDate();
            final Frame inertial = currentState.getFrame();
            final Vector3D position = currentState.toStaticTransform().getInverse()
                    .transformPosition(satelliteMeanPosition);

            final Vector3D p1 = satellite.getPVCoordinatesProvider().getPosition(date, provider.getEarth().getBodyFrame());
            final Vector3D p2 = currentState.getPVCoordinates(provider.getEarth().getBodyFrame()).getPosition();
            final double minAltitude = provider.getEarth().lowestAltitudeIntermediate(p1, p2).getAltitude();

            // 100 km min altitude
            if (minAltitude > 1.0e5) {

                // Solve for the downlink delay
                final double downLinkDelay = solveDownlinkDelay(satellite, currentState, satelliteMeanPosition);
                final AbsoluteDate receptionDate = currentState.getDate().shiftedBy(downLinkDelay);
                final Transform stationToInertReception = satellite.getOffsetToInertial(inertial, receptionDate, true);
                final Vector3D stationAtReception = satellite.getPVCoordinatesProvider().getPosition(receptionDate, inertial);
                final double downLinkDistance = Vector3D.distance(position, stationAtReception);

                final Vector3D satLosDown = currentState.toTransform()
                        .transformVector(stationAtReception.subtract(position));
                final double satPCVDown = satellitePhaseCenterVariation == null ? 0.0
                        : satellitePhaseCenterVariation.value(0.5 * FastMath.PI - satLosDown.getDelta(),
                                satLosDown.getAlpha());
                final Vector3D staLosDown = stationToInertReception.getInverse()
                        .transformVector(position.subtract(stationAtReception));
                final double staPCVDown = observerPhaseCenterVariation == null ? 0.0
                        : observerPhaseCenterVariation.value(0.5 * FastMath.PI - staLosDown.getDelta(),
                                staLosDown.getAlpha());

                final double correctedDownLinkDistance = downLinkDistance + satPCVDown + staPCVDown;

                // Solve for the uplink delay
                final double upLinkDelay = solveUplinkDelay(satellite, currentState, satelliteMeanPosition);
                final AbsoluteDate emissionDate = currentState.getDate().shiftedBy(-upLinkDelay);
                final Transform stationToInertEmission = satellite.getOffsetToInertial(inertial, emissionDate, true);
                final Vector3D stationAtEmission = stationToInertEmission.transformPosition(observerMeanPosition);
                final double upLinkDistance = Vector3D.distance(position, stationAtEmission);

                final Vector3D staLosUp = stationToInertEmission.getInverse()
                        .transformVector(position.subtract(stationAtEmission));
                final double staPCVUp = observerPhaseCenterVariation == null ? 0.0
                        : observerPhaseCenterVariation.value(0.5 * FastMath.PI - staLosUp.getDelta(),
                                staLosUp.getAlpha());
                final Vector3D satLosUp = currentState.toTransform()
                        .transformVector(stationAtEmission.subtract(position));
                final double satPCVUp = satellitePhaseCenterVariation == null ? 0.0
                        : satellitePhaseCenterVariation.value(0.5 * FastMath.PI - satLosUp.getDelta(),
                                satLosUp.getAlpha());

                final double correctedUpLinkDistance = upLinkDistance + satPCVUp + staPCVUp;

                final double clockOffset = satellite.getOffsetValue(receptionDate);
                addMeasurement(new Range(satellite, true, receptionDate.shiftedBy(clockOffset),
                        0.5 * (correctedDownLinkDistance + correctedUpLinkDistance) + bias,
                        1.0, 10, observedSatellite));

            }

        }
    }

}
