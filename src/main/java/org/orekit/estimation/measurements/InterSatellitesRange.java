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
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way or two-way range measurements between two satellites.
 * <p>
 * Satellite 1 is always considered to be the satellite that receives the
 * signal and computes the measurement.
 * </p>
 * <p>
 * For one-way measurements, a signal is emitted by satellite 2 and received
 * by satellite 1. The measurement value is the elapsed time between emission
 * and reception divided by c were c is the speed of light.
 * </p>
 * <p>
 * For two-way measurements, a signal is emitted by satellite 1, reflected on
 * satellite 2, and received back by satellite 1 again. The measurement value
 * is the elapsed time between emission and reception divided by 2c were c is
 * the speed of light.
 * </p>
 * <p>
 * The motion of both satellites during the signal flight time is
 * taken into account. The date of the measurement corresponds to
 * the reception of the signal by satellite 1.
 * </p>
 * @author Luc Maisonobe
 * @since 9.0
 */
public class InterSatellitesRange extends AbstractMeasurement<InterSatellitesRange> {

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param satellite1Index index of satellite 1 propagator
     * (i.e. the satellite which receives the signal and performs
     * the measurement)
     * @param satellite2Index index of satellite 2 propagator
     * (i.e. the satellite which simply emits the signal in the one-way
     * case, or reflects the signal in the two-way case)
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.utils.ParameterDriver}
     * name conflict occurs
     */
    public InterSatellitesRange(final int satellite1Index, final int satellite2Index,
                                final boolean twoWay,
                                final AbsoluteDate date, final double range,
                                final double sigma, final double baseWeight)
        throws OrekitException {
        super(date, range, sigma, baseWeight, Arrays.asList(satellite1Index, satellite2Index));
        this.twoway = twoWay;
    }

    /** Check if the instance represents a two-way measurement.
     * @return true if the instance represents a two-way measurement
     */
    public boolean isTwoWay() {
        return twoway;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<InterSatellitesRange> theoreticalEvaluation(final int iteration,
                                                                               final int evaluation,
                                                                               final SpacecraftState[] states)
        throws OrekitException {

        // Range derivatives are computed with respect to spacecrafts states in inertial frame
        // ----------------------
        //
        // Parameters:
        //  - 0..2  - Position of the satellite 1 in inertial frame
        //  - 3..5  - Velocity of the satellite 1 in inertial frame
        //  - 6..8  - Position of the satellite 2 in inertial frame
        //  - 9..11 - Velocity of the satellite 2 in inertial frame
        final int nbParams = 12;
        final DSFactory                          factory = new DSFactory(nbParams, 1);
        final Field<DerivativeStructure>         field   = factory.getDerivativeField();

        // coordinates of both satellites
        final SpacecraftState state1 = states[getPropagatorsIndices().get(0)];
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pva1 = getCoordinates(state1, 0, factory);
        final SpacecraftState state2 = states[getPropagatorsIndices().get(1)];
        final TimeStampedFieldPVCoordinates<DerivativeStructure> pva2 = getCoordinates(state2, 6, factory);

        // compute propagation times
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have delta == tauD and transitState will be the same as state)

        // downlink delay
        final FieldAbsoluteDate<DerivativeStructure> arrivalDate = new FieldAbsoluteDate<>(field, getDate());
        final TimeStampedFieldPVCoordinates<DerivativeStructure> s1Downlink =
                        pva1.shiftedBy(arrivalDate.durationFrom(pva1.getDate()));
        final DerivativeStructure tauD = signalTimeOfFlight(pva2, s1Downlink.getPosition(), arrivalDate);

        // Transit state
        final double              delta      = getDate().durationFrom(state2.getDate());
        final DerivativeStructure deltaMTauD = tauD.negate().add(delta);

        // prepare the evaluation
        final EstimatedMeasurement<InterSatellitesRange> estimated;

        final DerivativeStructure range;
        if (twoway) {
            // Transit state (re)computed with derivative structures
            final TimeStampedFieldPVCoordinates<DerivativeStructure> transitStateDS = pva2.shiftedBy(deltaMTauD);

            // uplink delay
            final DerivativeStructure tauU = signalTimeOfFlight(pva1,
                                                                transitStateDS.getPosition(),
                                                                transitStateDS.getDate());
            estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       state1.shiftedBy(deltaMTauD.getValue()),
                                                       state2.shiftedBy(deltaMTauD.getValue())
                                                   }, new TimeStampedPVCoordinates[] {
                                                       state1.shiftedBy(delta - tauD.getValue() - tauU.getValue()).getPVCoordinates(),
                                                       state2.shiftedBy(delta - tauD.getValue()).getPVCoordinates(),
                                                       state1.shiftedBy(delta).getPVCoordinates()
                                                   });

            // Range value
            range  = tauD.add(tauU).multiply(0.5 * Constants.SPEED_OF_LIGHT);

        } else {

            estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       state1.shiftedBy(deltaMTauD.getValue()),
                                                       state2.shiftedBy(deltaMTauD.getValue())
                                                   }, new TimeStampedPVCoordinates[] {
                                                       state2.shiftedBy(delta - tauD.getValue()).getPVCoordinates(),
                                                       state1.shiftedBy(delta).getPVCoordinates()
                                                   });

            // Range value
            range  = tauD.multiply(Constants.SPEED_OF_LIGHT);

        }
        estimated.setEstimatedValue(range.getValue());

        // Range partial derivatives with respect to states
        final double[] derivatives = range.getAllDerivatives();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 1,  7));
        estimated.setStateDerivatives(1, Arrays.copyOfRange(derivatives, 7, 13));

        return estimated;

    }

}
