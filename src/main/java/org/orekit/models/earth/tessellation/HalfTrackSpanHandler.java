/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.models.earth.tessellation;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Event handler for half track span.
 * @since 7.1
 * @see AlongTrackAiming
 * @author Luc Maisonobe
 */
class HalfTrackSpanHandler implements EventHandler<HalfTrackSpanDetector> {

    /** Indicator for zone tiling with respect to ascending or descending orbits. */
    private final boolean isAscending;

    /** Halftrack start date. */
    private AbsoluteDate start;

    /** Halftrack end date. */
    private AbsoluteDate end;

    /** Simple constructor.
     * @param isAscending indicator for zone tiling with respect to ascending
     * or descending orbits
     */
    HalfTrackSpanHandler(final boolean isAscending) {
        this.isAscending = isAscending;
        this.start       = null;
        this.end         = null;
    }

    /** Get the start date of the half track.
     * @return start date of the half track
     */
    public AbsoluteDate getStart() {
        return start;
    }

    /** Get the end date of the half track.
     * @return end date of the half track
     */
    public AbsoluteDate getEnd() {
        return end;
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s,
                                final HalfTrackSpanDetector detector,
                                final boolean increasing) {
        if (increasing ^ isAscending) {
            // we have found an end event
            if (start == null) {
                // we don't have the start event yet,
                // so we ignore this and wait for the next orbit
                return Action.CONTINUE;
            } else {
                end = s.getDate();
                return Action.STOP;
            }
        } else {
            // we have found the start of the span
            start = s.getDate();
            return Action.CONTINUE;
        }
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final HalfTrackSpanDetector detector,
                                      final SpacecraftState oldState) {
        return oldState;
    }

}
