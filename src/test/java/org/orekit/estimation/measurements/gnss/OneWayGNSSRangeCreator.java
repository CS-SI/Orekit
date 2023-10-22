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
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.MeasurementCreator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class OneWayGNSSRangeCreator extends MeasurementCreator {

    private final BoundedPropagator    ephemeris;
    private final double               remoteClk;
    private final Vector3D             antennaPhaseCenter1;
    private final Vector3D             antennaPhaseCenter2;
    private final ObservableSatellite  local;

    public OneWayGNSSRangeCreator(final BoundedPropagator ephemeris,
                                  final double localClockOffset,
                                  final double remoteClockOffset) {
        this(ephemeris, localClockOffset, remoteClockOffset, Vector3D.ZERO, Vector3D.ZERO);
    }

    public OneWayGNSSRangeCreator(final BoundedPropagator ephemeris,
                                  final double localClockOffset,
                                  final double remoteClockOffset,
                                  final Vector3D antennaPhaseCenter1,
                                  final Vector3D antennaPhaseCenter2) {
        this.ephemeris           = ephemeris;
        this.remoteClk           = remoteClockOffset;
        this.antennaPhaseCenter1 = antennaPhaseCenter1;
        this.antennaPhaseCenter2 = antennaPhaseCenter2;
        this.local               = new ObservableSatellite(0);
        this.local.getClockOffsetDriver().setValue(localClockOffset);
    }

    public ObservableSatellite getLocalSatellite() {
        return local;
    }

    public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
        if (local.getClockOffsetDriver().getReferenceDate() == null) {
            local.getClockOffsetDriver().setReferenceDate(s0.getDate());
        }
    }

    public void handleStep(final SpacecraftState currentState) {
        try {
            final double           localClk  = local.getClockOffsetDriver().getValue(currentState.getDate());
            final double           deltaD    = Constants.SPEED_OF_LIGHT * (localClk - remoteClk);
            final AbsoluteDate     date      = currentState.getDate();
            final Vector3D         position  = currentState.toTransform().toStaticTransform().getInverse().transformPosition(antennaPhaseCenter1);

            final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

            final double downLinkDelay  = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) {
                    final Vector3D other = ephemeris.
                                    propagate(date.shiftedBy(-x)).
                                    toTransform().
                                    getInverse().
                                    transformPosition(antennaPhaseCenter2);
                    final double d = Vector3D.distance(position, other);
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);
            final AbsoluteDate transitDate = currentState.getDate().shiftedBy(-downLinkDelay);
            final Vector3D otherAtTransit =
                            ephemeris.propagate(transitDate).
                            toTransform().
                            getInverse().
                            transformPosition(antennaPhaseCenter2);
            final double downLinkDistance = Vector3D.distance(position, otherAtTransit);

            // Generate measurement
            addMeasurement(new OneWayGNSSRange(ephemeris, remoteClk, date.shiftedBy(localClk), downLinkDistance + deltaD, 1.0, 10, local));

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

}
