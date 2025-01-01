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

import org.orekit.models.earth.displacement.PsdCorrection;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.function.Predicate;

/** Predicates for Post-Seismic Deformation lines.
 * @author Luc Maisonobe
 * @since 13.0
 */
enum PsdPredicate implements Predicate<SinexParseInfo> {

    /** Predicate for AEXP_E line. */
    AEXP_E {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.EXP);
            parseInfo.setAxis(PsdCorrection.Axis.EAST);
            parseInfo.setAmplitude(value, station, epoch);
        }
    },

    /** Predicate for TEXP_E line. */
    TEXP_E {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.EXP);
            parseInfo.setAxis(PsdCorrection.Axis.EAST);
            parseInfo.setRelaxationTime(value * Constants.JULIAN_YEAR, station, epoch);
        }
    },

    /** Predicate for ALOG_E line. */
    ALOG_E {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.LOG);
            parseInfo.setAxis(PsdCorrection.Axis.EAST);
            parseInfo.setAmplitude(value, station, epoch);
        }
    },

    /** Predicate for TLOG_E line. */
    TLOG_E {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.LOG);
            parseInfo.setAxis(PsdCorrection.Axis.EAST);
            parseInfo.setRelaxationTime(value * Constants.JULIAN_YEAR, station, epoch);
        }
    },

    /** Predicate for AEXP_N line. */
    AEXP_N {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.EXP);
            parseInfo.setAxis(PsdCorrection.Axis.NORTH);
            parseInfo.setAmplitude(value, station, epoch);
        }
    },

    /** Predicate for TEXP_N line. */
    TEXP_N {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.EXP);
            parseInfo.setAxis(PsdCorrection.Axis.NORTH);
            parseInfo.setRelaxationTime(value * Constants.JULIAN_YEAR, station, epoch);
        }
    },

    /** Predicate for ALOG_N line. */
    ALOG_N {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.LOG);
            parseInfo.setAxis(PsdCorrection.Axis.NORTH);
            parseInfo.setAmplitude(value, station, epoch);
        }
    },

    /** Predicate for TLOG_N line. */
    TLOG_N {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.LOG);
            parseInfo.setAxis(PsdCorrection.Axis.NORTH);
            parseInfo.setRelaxationTime(value * Constants.JULIAN_YEAR, station, epoch);
        }
    },

    /** Predicate for AEXP_U line. */
    AEXP_U {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.EXP);
            parseInfo.setAxis(PsdCorrection.Axis.UP);
            parseInfo.setAmplitude(value, station, epoch);
        }
    },

    /** Predicate for TEXP_U line. */
    TEXP_U {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.EXP);
            parseInfo.setAxis(PsdCorrection.Axis.UP);
            parseInfo.setRelaxationTime(value * Constants.JULIAN_YEAR, station, epoch);
        }
    },

    /** Predicate for ALOG_U line. */
    ALOG_U {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.LOG);
            parseInfo.setAxis(PsdCorrection.Axis.UP);
            parseInfo.setAmplitude(value, station, epoch);
        }
    },

    /** Predicate for TLOG_U line. */
    TLOG_U {
        protected void store(final SinexParseInfo parseInfo, final double value, final Station station,
                             final AbsoluteDate epoch) {
            parseInfo.setEvolution(PsdCorrection.TimeEvolution.LOG);
            parseInfo.setAxis(PsdCorrection.Axis.UP);
            parseInfo.setRelaxationTime(value * Constants.JULIAN_YEAR, station, epoch);
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

    /**
     * Store parsed fields.
     *
     * @param parseInfo container for parse info
     * @param value     parsed value
     * @param station   station
     * @param epoch     current epoch
     */
    protected abstract void store(SinexParseInfo parseInfo, double value, Station station, AbsoluteDate epoch);

}
