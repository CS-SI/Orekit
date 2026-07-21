/* Copyright 2025-2026 Hawkeye 360 (HE360)
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

import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class that accepts a PVCoordinatesProvider for a space-
 * based measurement receiver.
 *
 * @author Brianna Aubin
 * @since 14.0
 */
public class ObserverSatellite extends AbstractParticipant implements Observer {

    /** Provides satellite trajectory. */
    private final PVCoordinatesProvider pvCoordsProvider;

    /** Simple constructor.
     * @param name name of receiver
     * @param pvCoordsProvider satellite propagator
     */
    public ObserverSatellite(final String name, final PVCoordinatesProvider pvCoordsProvider) {
        this(name, pvCoordsProvider, createEmptyQuadraticClock(name));
    }

    /** Simple constructor.
     * @param name name of receiver
     * @param pvCoordsProvider position/velocity coordinates provider for receiver
     * @param quadraticClock clock model for receiver
     */
    public ObserverSatellite(final String name, final PVCoordinatesProvider pvCoordsProvider,
                             final QuadraticClockModel quadraticClock) {
        super(name, quadraticClock);
        this.pvCoordsProvider = pvCoordsProvider;
    }

    /** {@inheritDoc} */
    @Override
    public final PVCoordinatesProvider getPVCoordinatesProvider() {
        return pvCoordsProvider;
    }

    /** {@inheritDoc} */
    @Override
    public FieldPVCoordinatesProvider<Gradient> getFieldPVCoordinatesProvider(final int freeParameters,
                                                                              final Map<String, Integer> parameterIndices) {

        // If a FieldPVCoordinatesProvider<Gradient> already exists, use it
        if (pvCoordsProvider instanceof ExtendedPositionProvider provider) {
            final Field<Gradient> check = GradientField.getField(freeParameters);
            return provider.toFieldPVCoordinatesProvider(check);
        }

        // Otherwise, convert the PVCoordinatesProvider to a FieldPVCoordinatesProvider<Gradient>
        else {
            return (date, frame) -> {

                // apply the raw (no derivatives) remote provider
                final AbsoluteDate             dateBase = date.toAbsoluteDate();
                final TimeStampedPVCoordinates pvBase   = pvCoordsProvider.getPVCoordinates(dateBase, frame);
                final TimeStampedFieldPVCoordinates<Gradient> pvWithoutDerivatives =
                    new TimeStampedFieldPVCoordinates<>(date.getField(), pvBase);

                // add derivatives, using a trick: we shift the date by 0, with derivatives
                final Gradient zeroWithDerivatives = date.durationFrom(dateBase);
                return pvWithoutDerivatives.shiftedBy(zeroWithDerivatives);

            };

        }
    }

    /** {@inheritDoc} */
    @Override
    public Transform getOffsetToInertial(final Frame inertial,
                                         final AbsoluteDate date,
                                         final boolean clockOffsetAlreadyApplied) {

        // take clock offset into account
        final AbsoluteDate offsetCompensatedDate = clockOffsetAlreadyApplied ?
                                                   date :
                                                   new AbsoluteDate(date, -getOffsetValue(date));

        // Return transform that will give PV coords of emitter when pos = 0, vel = 0 is entered
        final PVCoordinates coords = getPVCoordinatesProvider().getPVCoordinates(offsetCompensatedDate, inertial);
        return new Transform(offsetCompensatedDate, coords.getPosition(), coords.getVelocity(), coords.getAcceleration());
    }

    /** {@inheritDoc} */
    @Override
    public FieldTransform<Gradient> getOffsetToInertial(final Frame inertial,
                                                        final FieldAbsoluteDate<Gradient> offsetCompensatedDate,
                                                        final int freeParameters,
                                                        final Map<String, Integer> indices) {

        final FieldPVCoordinates<Gradient> coords = getFieldPVCoordinatesProvider(freeParameters, indices).getPVCoordinates(offsetCompensatedDate, inertial);
        return new FieldTransform<>(offsetCompensatedDate, coords.getPosition(), coords.getVelocity(), coords.getAcceleration());

    }

}
