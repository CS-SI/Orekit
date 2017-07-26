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
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FieldTransform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a range measurement from a ground station.
 * <p>
 * The measurement is considered to be a signal emitted from
 * a ground station, reflected on spacecraft, and received
 * on the same ground station. Its value is the elapsed time
 * between emission and reception divided by 2c were c is the
 * speed of light. The motion of both the station and the
 * spacecraft during the signal flight time are taken into
 * account. The date of the measurement corresponds to the
 * reception on ground of the reflected signal.
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 8.0
 */
public class Range extends AbstractMeasurement<Range> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * <p>
     * This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.
     * </p>
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     */
    public Range(final GroundStation station, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight)
        throws OrekitException {
        this(station, date, range, sigma, baseWeight, 0);
    }

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     * @since 9.0
     */
    public Range(final GroundStation station, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight,
                 final int propagatorIndex)
        throws OrekitException {
        super(date, range, sigma, baseWeight, Arrays.asList(propagatorIndex),
              station.getEastOffsetDriver(),
              station.getNorthOffsetDriver(),
              station.getZenithOffsetDriver(),
              station.getPrimeMeridianOffsetDriver(),
              station.getPrimeMeridianDriftDriver(),
              station.getPolarOffsetXDriver(),
              station.getPolarDriftXDriver(),
              station.getPolarOffsetYDriver(),
              station.getPolarDriftYDriver());
        this.station = station;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Range> theoreticalEvaluation(final int iteration,
                                                                final int evaluation,
                                                                final SpacecraftState[] states)
        throws OrekitException {

        final SpacecraftState state = states[getPropagatorsIndices().get(0)];

        // Range derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (station offsets, pole, prime meridian...)
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                indices.put(driver.getName(), nbParams++);
            }
        }
        final DSFactory                          factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure>         field   = factory.getDerivativeField();
        final FieldVector3D<DerivativeStructure> zero    = FieldVector3D.getZero(field);

        // Coordinates of the spacecraft expressed as a derivative structure
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pvaDS = getCoordinates(state, 0, factory);

        // transform between station and inertial frame, expressed as a derivative structure
        // The components of station's position in offset frame are the 3 last derivative parameters
        final AbsoluteDate downlinkDate = getDate();
        final FieldAbsoluteDate<DerivativeStructure> downlinkDateDS =
                        new FieldAbsoluteDate<>(field, downlinkDate);
        final FieldTransform<DerivativeStructure> offsetToInertialDownlink =
                        station.getOffsetToInertial(state.getFrame(), downlinkDateDS, factory, indices);

        // Station position in inertial frame at end of the downlink leg
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationDownlink =
                        offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDateDS,
                                                                                                            zero, zero, zero));

        // Compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // Downlink delay
        final DerivativeStructure tauD = signalTimeOfFlight(pvaDS, stationDownlink.getPosition(), downlinkDateDS);

        // Transit state
        final double                delta        = downlinkDate.durationFrom(state.getDate());
        final DerivativeStructure   deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState       transitState = state.shiftedBy(deltaMTauD.getValue());

        // Transit state (re)computed with derivative structures
        final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateDS = pvaDS.shiftedBy(deltaMTauD);

        // Station at transit state date (derivatives of tauD taken into account)
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationAtTransitDate =
                        stationDownlink.shiftedBy(tauD.negate());

        // Uplink delay
        final DerivativeStructure tauU =
                        signalTimeOfFlight(stationAtTransitDate, transitStateDS.getPosition(), transitStateDS.getDate());
        final TimeStampedFieldPVCoordinates<DerivativeStructure> stationUplink =
                        stationDownlink.shiftedBy(-tauD.getValue() - tauU.getValue());

        // Prepare the evaluation
        final EstimatedMeasurement<Range> estimated =
                        new EstimatedMeasurement<Range>(this, iteration, evaluation,
                                                        new SpacecraftState[] {
                                                            transitState
                                                        }, new TimeStampedPVCoordinates[] {
                                                            stationUplink.toTimeStampedPVCoordinates(),
                                                            transitStateDS.toTimeStampedPVCoordinates(),
                                                            stationDownlink.toTimeStampedPVCoordinates()
                                                        });

        // Range value
        final double              cOver2 = 0.5 * Constants.SPEED_OF_LIGHT;
        final DerivativeStructure tau    = tauD.add(tauU);
        final DerivativeStructure range  = tau.multiply(cOver2);
        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to state
        final double[] derivatives = range.getAllDerivatives();
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
