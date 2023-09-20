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
package org.orekit.gnss.attitude;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/**
 * Base class for attitude providers for navigation satellites.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
abstract class AbstractGNSSAttitudeProvider implements GNSSAttitudeProvider {

    /** Start of validity for this provider. */
    private final AbsoluteDate validityStart;

    /** End of validity for this provider. */
    private final AbsoluteDate validityEnd;

    /** Provider for Sun position. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Inertial frame where velocity are computed. */
    private final Frame inertialFrame;

    /** Turns already encountered. */
    private final SortedSet<TimeStamped> turns;

    /** Turns already encountered. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, SortedSet<TimeStamped>> fieldTurns;

    /** Simple constructor.
     * @param validityStart start of validity for this provider
     * @param validityEnd end of validity for this provider
     * @param sun provider for Sun position
     * @param inertialFrame inertial frame where velocity are computed
     */
    protected AbstractGNSSAttitudeProvider(final AbsoluteDate validityStart,
                                           final AbsoluteDate validityEnd,
                                           final ExtendedPVCoordinatesProvider sun,
                                           final Frame inertialFrame) {
        this.validityStart = validityStart;
        this.validityEnd   = validityEnd;
        this.sun           = sun;
        this.inertialFrame = inertialFrame;
        this.turns         = new TreeSet<>(new ChronologicalComparator());
        this.fieldTurns    = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate validityStart() {
        return validityStart;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate validityEnd() {
        return validityEnd;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date,
                                final Frame frame) {

        // compute yaw correction
        final TurnSpan                      turnSpan  = getTurnSpan(date);
        final GNSSAttitudeContext           context   = new GNSSAttitudeContext(date, sun, pvProv, inertialFrame, turnSpan);
        final TimeStampedAngularCoordinates corrected = correctedYaw(context);
        if (turnSpan == null && context.getTurnSpan() != null) {
            // we have encountered a new turn, store it
            turns.add(context.getTurnSpan());
        }

        return new Attitude(inertialFrame, corrected).withReferenceFrame(frame);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                            final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {

        // compute yaw correction
        final FieldTurnSpan<T>                      turnSpan  = getTurnSpan(date);
        final GNSSFieldAttitudeContext<T>           context   = new GNSSFieldAttitudeContext<>(date, sun, pvProv, inertialFrame, turnSpan);
        final TimeStampedFieldAngularCoordinates<T> corrected = correctedYaw(context);
        if (turnSpan == null && context.getTurnSpan() != null) {
            // we have encountered a new turn, store it
            fieldTurns.get(date.getField()).add(context.getTurnSpan());
        }

        return new FieldAttitude<>(inertialFrame, corrected).withReferenceFrame(frame);

    }

    /** Get the turn span covering a date.
     * @param date date to check
     * @return turn span covering the date, or null if no span covers this date
     */
    private TurnSpan getTurnSpan(final AbsoluteDate date) {

        // as the reference date of the turn span is the end + margin date,
        // the span to consider can only be the first span that is after date
        final SortedSet<TimeStamped> after = turns.tailSet(date);
        if (!after.isEmpty()) {
            final TurnSpan ts = (TurnSpan) after.first();
            if (ts.inTurnTimeRange(date)) {
                return ts;
            }
        }

        // no turn covers the date
        return null;

    }

    /** Get the turn span covering a date.
     * @param date date to check
     * @param <T> type of the field elements
     * @return turn span covering the date, or null if no span covers this date
     */
    private <T extends CalculusFieldElement<T>> FieldTurnSpan<T> getTurnSpan(final FieldAbsoluteDate<T> date) {

        SortedSet<TimeStamped> sortedSet = fieldTurns.get(date.getField());
        if (sortedSet == null) {
            // this is the first time we manage such a field, prepare a sorted set for it
            sortedSet = new TreeSet<>(new ChronologicalComparator());
            fieldTurns.put(date.getField(), sortedSet);
        }

        // as the reference date of the turn span is the end + margin date,
        // the span to consider can only be the first span that is after date
        final AbsoluteDate dateDouble = date.toAbsoluteDate();
        final SortedSet<TimeStamped> after = sortedSet.tailSet(dateDouble);
        if (!after.isEmpty()) {
            @SuppressWarnings("unchecked")
            final FieldTurnSpan<T> ts = (FieldTurnSpan<T>) after.first();
            if (ts.inTurnTimeRange(dateDouble)) {
                return ts;
            }
        }

        // no turn covers the date
        return null;

    }

    /** Get provider for Sun position.
     * @return provider for Sun position
     * @since 12.0
     */
    protected ExtendedPVCoordinatesProvider getSun() {
        return sun;
    }

    /** Get inertial frame where velocity are computed.
     * @return inertial frame where velocity are computed
     */
    protected Frame getInertialFrame() {
        return inertialFrame;
    }

    /** Select the
    /** Compute GNSS attitude with midnight/noon yaw turn correction.
     * @param context context data for attitude computation
     * @return corrected yaw, using inertial frame as the reference
     */
    protected abstract TimeStampedAngularCoordinates correctedYaw(GNSSAttitudeContext context);

    /** Compute GNSS attitude with midnight/noon yaw turn correction.
     * @param context context data for attitude computation
     * @param <T> type of the field elements
     * @return corrected yaw, using inertial frame as the reference
     */
    protected abstract <T extends CalculusFieldElement<T>> TimeStampedFieldAngularCoordinates<T>
        correctedYaw(GNSSFieldAttitudeContext<T> context);

}
