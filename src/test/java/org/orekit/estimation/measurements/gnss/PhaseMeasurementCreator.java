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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.MeasurementCreator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.modifiers.PhaseAmbiguityModifier;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.antenna.PhaseCenterVariationFunction;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

public class PhaseMeasurementCreator extends MeasurementCreator {

    private final Context                      context;
    private final double                       wavelength;
    private final PhaseAmbiguityModifier       ambiguity;
    private final Vector3D                     stationMeanPosition;
    private final PhaseCenterVariationFunction stationPhaseCenterVariation;
    private final Vector3D                     satelliteMeanPosition;
    private final PhaseCenterVariationFunction satellitePhaseCenterVariation;
    private final ObservableSatellite          satellite;

    public PhaseMeasurementCreator(final Context context, final Frequency frequency,
                                   final int ambiguity, final double satClockOffset) {
        this(context, frequency, ambiguity, satClockOffset,
             Vector3D.ZERO, null, Vector3D.ZERO, null);
    }

    public PhaseMeasurementCreator(final Context context, final Frequency frequency,
                                   final int ambiguity, final double satClockOffset,
                                   final Vector3D stationMeanPosition,   final PhaseCenterVariationFunction stationPhaseCenterVariation,
                                   final Vector3D satelliteMeanPosition, final PhaseCenterVariationFunction satellitePhaseCenterVariation) {
        this.context                       = context;
        this.wavelength                    = frequency.getWavelength();
        this.ambiguity                     = new PhaseAmbiguityModifier(0, ambiguity);
        this.stationMeanPosition           = stationMeanPosition;
        this.stationPhaseCenterVariation   = stationPhaseCenterVariation;
        this.satelliteMeanPosition         = satelliteMeanPosition;
        this.satellitePhaseCenterVariation = satellitePhaseCenterVariation;
        this.satellite                     = new ObservableSatellite(0);
        this.satellite.getClockOffsetDriver().setValue(satClockOffset);
    }

    public ObservableSatellite getSatellite() {
        return satellite;
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (final GroundStation station : context.stations) {
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
        if (satellite.getClockOffsetDriver().getReferenceDate() == null) {
            satellite.getClockOffsetDriver().setReferenceDate(s0.getDate());
        }
    }

    public void handleStep(final SpacecraftState currentState) {
        try {
            final double n      = ambiguity.getParametersDrivers().get(0).getValue();
            for (final GroundStation station : context.stations) {
            	final AbsoluteDate     date      = currentState.getDate();
                final Frame            inertial  = currentState.getFrame();
                final Vector3D         position  = currentState.toTransform().toStaticTransform().getInverse().transformPosition(satelliteMeanPosition);

                if (station.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation() > FastMath.toRadians(30.0)) {
                    final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

                    final double downLinkDelay  = solver.solve(1000, new UnivariateFunction() {
                        public double value(final double x) {
                            final Transform t = station.getOffsetToInertial(inertial, date.shiftedBy(x), true);
                            final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
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

                    final double groundClk = station.getClockOffsetDriver().getValue(date);
                    final double satClk    = satellite.getClockOffsetDriver().getValue(date);
                    final double correctedDownLinkDistance = downLinkDistance + satPCVDown + staPCVDown +
                                                             (groundClk - satClk) * Constants.SPEED_OF_LIGHT;
                    final Phase  phase = new Phase(station, receptionDate.shiftedBy(groundClk),
                                                   correctedDownLinkDistance / wavelength + n,
                                                   wavelength, 1.0, 10, satellite);
                    phase.addModifier(ambiguity);
                    addMeasurement(phase);
                }

            }
        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

}
