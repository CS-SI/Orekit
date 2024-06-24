/* Copyright 2002-2024 Thales Alenia Space
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

import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.MeasurementCreator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ClockOffset;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

public class OneWayGNSSRangeRateCreator
    extends MeasurementCreator {

    private final BoundedPropagator    ephemeris;
    private final QuadraticClockModel  remoteClk;
    private final Vector3D             antennaPhaseCenter1;
    private final Vector3D             antennaPhaseCenter2;
    private final ObservableSatellite  local;

    public OneWayGNSSRangeRateCreator(final BoundedPropagator ephemeris,
                                      final double localClockOffset,
                                      final double localClockRate,
                                      final double localClockAcceleration,
                                      final double remoteClockOffset,
                                      final double remoteClockRate,
                                      final double remoteClockAcceleration) {
        this(ephemeris,
             localClockOffset, localClockRate, localClockAcceleration,
             remoteClockOffset, remoteClockRate, remoteClockAcceleration,
             Vector3D.ZERO, Vector3D.ZERO);
    }

    public OneWayGNSSRangeRateCreator(final BoundedPropagator ephemeris,
                                      final double localClockOffset,
                                      final double localClockRate,
                                      final double localClockAcceleration,
                                      final double remoteClockOffset,
                                      final double remoteClockRate,
                                      final double remoteClockAcceleration,
                                      final Vector3D antennaPhaseCenter1,
                                      final Vector3D antennaPhaseCenter2) {
        this.ephemeris           = ephemeris;
        this.antennaPhaseCenter1 = antennaPhaseCenter1;
        this.antennaPhaseCenter2 = antennaPhaseCenter2;
        this.local               = new ObservableSatellite(0);
        this.local.getClockOffsetDriver().setValue(localClockOffset);
        this.local.getClockDriftDriver().setValue(localClockRate);
        this.local.getClockAccelerationDriver().setValue(localClockAcceleration);
        this.remoteClk          = new QuadraticClockModel(ephemeris.getMinDate(),
                                                          remoteClockOffset,
                                                          remoteClockRate,
                                                          remoteClockAcceleration);
    }

    public ObservableSatellite getLocalSatellite() {
        return local;
    }

    public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
        for (final ParameterDriver driver : Arrays.asList(local.getClockOffsetDriver(),
                                                          local.getClockDriftDriver(),
                                                          local.getClockAccelerationDriver())) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(s0.getDate());
            }
        }
    }

    public void handleStep(final SpacecraftState currentState) {
        try {
            final AbsoluteDate     date      = currentState.getDate();
            final PVCoordinates    pv        = currentState.toTransform().getInverse().
                                               transformPVCoordinates(new PVCoordinates(antennaPhaseCenter1));
            final ClockOffset      localClk  = local.getQuadraticClockModel().getOffset(date);

            final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

            final double downLinkDelay  = solver.solve(1000, x -> {
                final Vector3D other = ephemeris.
                                propagate(date.shiftedBy(-x)).
                                toTransform().
                                getInverse().
                                transformPosition(antennaPhaseCenter2);
                final double d = Vector3D.distance(pv.getPosition(), other);
                return d - x * Constants.SPEED_OF_LIGHT;
            }, -1.0, 1.0);
            final AbsoluteDate transitDate = currentState.getDate().shiftedBy(-downLinkDelay);
            final PVCoordinates otherAtTransit = ephemeris.propagate(transitDate).
                                                 toTransform().
                                                 getInverse().
                                                 transformPVCoordinates(new PVCoordinates(antennaPhaseCenter2));
            final PVCoordinates delta = new PVCoordinates(otherAtTransit, pv);
            final double rangeRate = Vector3D.dotProduct(delta.getPosition().normalize(), delta.getVelocity()) +
                Constants.SPEED_OF_LIGHT * (local.getQuadraticClockModel().getOffset(date).getRate() -
                                            remoteClk.getOffset(transitDate).getRate());

            // Generate measurement
            addMeasurement(new OneWayGNSSRangeRate(ephemeris, remoteClk, date.shiftedBy(localClk.getOffset()), rangeRate, 1.0, 10, local));

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

}
