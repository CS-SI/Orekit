/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm;

import java.util.Deque;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.utils.lexical.EventType;
import org.orekit.files.ccsds.utils.lexical.ParseEvent;
import org.orekit.files.ccsds.utils.lexical.ParsingState;
import org.orekit.time.AbsoluteDate;

/** {@link ParsingState} for {@link NDMHeader NDM header}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NDMHeaderParsingState implements ParsingState {

    /** Data context used for getting frames, time scales, and celestial bodies. */
    private final DataContext dataContext;

    /** Key for format version. */
    private final String formatVersionKey;

    /** Header. */
    private final NDMHeader header;

    /** Next state to use. */
    private final ParsingState nextState;

    /** Simple constructor.
     * @param dataContext data context used for getting frames, time scales, and celestial bodies
     * @param formatVersionKey key for format version
     * @param header header to fill
     * @param nextState state to use when this state cannot parse an event by itself
     */
    public NDMHeaderParsingState(final DataContext dataContext,
                                 final String formatVersionKey, final NDMHeader header,
                                 final ParsingState nextState) {
        this.dataContext      = dataContext;
        this.nextState        = nextState;
        this.formatVersionKey = formatVersionKey;
        this.header           = header;
    }

    /** {@inheritDoc} */
    @Override
    public ParsingState parseEvent(final ParseEvent event, final Deque<ParseEvent> next) {

        if (formatVersionKey.equals(event.getName())) {
            event.processAsDouble(header::setFormatVersion);
            return this;
        }

        switch (event.getName()) {

            case "COMMENT" :
                event.processAsFreeTextString(header::addComment);
                return this;

            case "CREATION_DATE" :
                if (event.getType() == EventType.ENTRY) {
                    header.setCreationDate(new AbsoluteDate(event.getContent(),
                                                            dataContext.getTimeScales().getUTC()));
                }
                return this;

            case "ORIGINATOR" :
                event.processAsNormalizedString(header::setOriginator);
                return this;

            default :
                // we were not able to parse the event, we push it back in the queue
                next.offerLast(event);
                return nextState;

        }

    }

}
