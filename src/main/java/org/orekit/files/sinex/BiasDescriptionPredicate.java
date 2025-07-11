/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.sinex;

import org.orekit.gnss.PredefinedTimeSystem;

import java.util.function.Predicate;

/** Predicates for bias description blocks.
 * @author Luc Maisonobe
 * @since 13.0
 */
enum BiasDescriptionPredicate implements Predicate<SinexBiasParseInfo> {

    /** Predicate for OBSERVATION_SAMPLING line. */
    OBSERVATION_SAMPLING {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo) {
            parseInfo.getDescription().setObservationSampling(parseInfo.parseInt(41, 12));
        }
    },

    /** Predicate for PARAMETER_SPACING line. */
    PARAMETER_SPACING {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo) {
            parseInfo.getDescription().setParameterSpacing(parseInfo.parseInt(41, 12));
        }
    },

    /** Predicate for DETERMINATION_METHOD line. */
    DETERMINATION_METHOD {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo) {
            parseInfo.getDescription().setDeterminationMethod(parseInfo.parseString(41, 39));
        }
    },

    /** Predicate for BIAS_MODE line. */
    BIAS_MODE {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo) {
            parseInfo.getDescription().setBiasMode(parseInfo.parseString(41, 39));
        }
    },

    /** Predicate for TIME_SYSTEM line. */
    TIME_SYSTEM {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo) {
            final String ts = parseInfo.parseString(41, 3);
            if ("UTC".equals(ts)) {
                parseInfo.setTimeSystem(PredefinedTimeSystem.UTC);
            } else if ("TAI".equals(ts)) {
                parseInfo.setTimeSystem(PredefinedTimeSystem.TAI);
            } else {
                parseInfo.setTimeSystem(PredefinedTimeSystem.parseOneLetterCode(ts));
            }
        }
    };

    /** {@inheritDoc} */
    @Override
    public boolean test(final SinexBiasParseInfo parseInfo) {
        if (name().equals(parseInfo.parseString(1, 39))) {
            // this is the data type we are concerned with
            store(parseInfo);
            return true;
        } else {
            // it is a data type for another predicate
            return false;
        }
    }

    /** Store parsed fields.
     * @param parseInfo container for parse info
     */
    protected abstract void store(SinexBiasParseInfo parseInfo);

}
