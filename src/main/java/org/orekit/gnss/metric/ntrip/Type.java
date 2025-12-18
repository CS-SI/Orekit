/* Copyright 2002-2025 CS GROUP
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
package org.orekit.gnss.metric.ntrip;

import java.util.List;

import org.orekit.frames.Frame;
import org.orekit.gnss.metric.parser.IgsSsrMessagesParser;
import org.orekit.gnss.metric.parser.MessagesParser;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;
import org.orekit.time.TimeScales;

/**
 * Enumerate for messages type.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public enum Type {

    /** RTCM. */
    RTCM() {

        /** {@inheritDoc} */
        @Override
        public MessagesParser getParser(final List<Integer> messages, final TimeScales timeScales,
                                        final Frame inertial, final Frame bodyFixed) {
            return new RtcmMessagesParser(messages, timeScales, inertial, bodyFixed);
        }

    },

    /** IGS SSR. */
    IGS_SSR() {

        /** {@inheritDoc} */
        @Override
        public MessagesParser getParser(final List<Integer> messages, final TimeScales timeScales,
                                        final Frame inertial, final Frame bodyFixed) {
            return new IgsSsrMessagesParser(messages, timeScales, inertial, bodyFixed);
        }

    };

    /**
     * Get the message parser associated to the SSR type.
     * @param messages list of needed messages
     * @param timeScales known time scales
     * @param inertial       reference inertial frame
     * @param bodyFixed      body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @return a configured message parser
     * @since 14.0
     */
    public abstract MessagesParser getParser(List<Integer> messages, TimeScales timeScales,
                                             Frame inertial, Frame bodyFixed);

}
