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

import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;

import java.util.function.Predicate;

/** Predicates for bias solution blocks.
 * @author Luc Maisonobe
 * @since 13.0
 */
enum BiasSolutionPredicate implements Predicate<SinexBiasParseInfo> {

    /** Predicate for DSB line. */
    DSB {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo,
                             final String svn, final String prn, final String siteCode,
                             final String obs1, final String obs2,
                             final AbsoluteDate beginDate, final AbsoluteDate finalDate,
                             final double bias) {
            if (siteCode.isEmpty()) {
                // this is a satellite bias
                final DsbSatellite dcb = parseInfo.getSatelliteDcb(prn);
                dcb.getDcbData().addDsbLine(obs1, obs2, beginDate, finalDate, bias);
            } else {
                // this is a station bias
                final DsbStation dcb       = parseInfo.getStationDcb(siteCode);
                final SatelliteSystem satSystem = SatelliteSystem.parseSatelliteSystem(prn);
                dcb.getDcbData(satSystem).addDsbLine(obs1, obs2, beginDate, finalDate, bias);
            }
        }
    },

    /** Predicate for OSB line. */
    OSB {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo,
                             final String svn, final String prn, final String siteCode,
                             final String obs1, final String obs2,
                             final AbsoluteDate beginDate, final AbsoluteDate finalDate,
                             final double bias) {
            // TODO
        }
    };

    /** {@inheritDoc} */
    @Override
    public boolean test(final SinexBiasParseInfo parseInfo) {
        if (name().equals(parseInfo.parseString(1, 3))) {
            // this is the data type we are concerned with
            store(parseInfo,
                  parseInfo.parseString(6, 4), parseInfo.parseString(11, 3), parseInfo.parseString(15, 9),
                  parseInfo.parseString(25, 4), parseInfo.parseString(30, 4),
                  parseInfo.stringEpochToAbsoluteDate(parseInfo.parseString(35, 14), true),
                  parseInfo.stringEpochToAbsoluteDate(parseInfo.parseString(50, 14), false),
                  parseInfo.parseDoubleWithUnit(65, 4, 70, 21));
            return true;
        } else {
            // it is a data type for another predicate
            return false;
        }
    }

    /** Store parsed fields.
     * @param parseInfo container for parse info
     * @param svn satellite SVN
     * @param prn satellite PRN
     * @param siteCode station site code
     * @param obs1 code of first observable
     * @param obs2 code of second observable
     * @param beginDate validity begin date
     * @param finalDate validity end date
     * @param bias estimated bias
     */
    protected abstract void store(SinexBiasParseInfo parseInfo,
                                  String svn, String prn, String siteCode,
                                  String obs1, String obs2,
                                  AbsoluteDate beginDate, AbsoluteDate finalDate,
                                  double bias);

}
