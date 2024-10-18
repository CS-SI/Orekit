/* Copyright 2002-2024 Luc Maisonobe
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

import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.function.Predicate;

/** Predicates for station coordinates blocks.
 * @author Luc Maisonobe
 * @since 13.0
 */
enum StationPredicate implements Predicate<SinexParseInfo> {

    /** Predicate for STAX line. */
    STAX {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexParseInfo parseInfo, final double coordinate, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setPx(coordinate, station, epoch);
        }
    },

    /** Predicate for STAY line. */
    STAY {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexParseInfo parseInfo, final double coordinate, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setPy(coordinate, station, epoch);
        }
    },

    /** Predicate for STAZ line. */
    STAZ {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexParseInfo parseInfo, final double coordinate, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setPz(coordinate, station, epoch);
        }
    },

    /** Predicate for VELX line. */
    VELX {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexParseInfo parseInfo, final double coordinate, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setVx(coordinate / Constants.JULIAN_YEAR, station);
        }
    },

    /** Predicate for VELY line. */
    VELY {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexParseInfo parseInfo, final double coordinate, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setVy(coordinate / Constants.JULIAN_YEAR, station);
        }
    },

    /** Predicate for VELZ line. */
    VELZ {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexParseInfo parseInfo, final double coordinate, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setVz(coordinate / Constants.JULIAN_YEAR, station);
        }
    };

    /** {@inheritDoc} */
    @Override
    public boolean test(final SinexParseInfo parseInfo) {
        if (name().equals(parseInfo.parseString(7, 6))) {
            // this is the data type we are concerned with
            store(parseInfo, parseInfo.parseDouble(47, 22),
                  parseInfo.getCurrentLineStation(14),
                  parseInfo.stringEpochToAbsoluteDate(parseInfo.parseString(27, 12), false));
            return true;
        } else {
            // it is a data type for another predicate
            return false;
        }
    }

    /** Store parsed fields.
     * @param parseInfo  container for parse info
     * @param coordinate station coordinate
     * @param station    station
     * @param epoch      current epoch
     */
    protected abstract void store(SinexParseInfo parseInfo, double coordinate, Station station, AbsoluteDate epoch);

}
