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

import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

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
                             final String svn, final SatInSystem satId, final String siteCode,
                             final ObservationType obs1, final ObservationType obs2,
                             final AbsoluteDate start, final AbsoluteDate end,
                             final double bias) {
            if (siteCode.isEmpty()) {
                // this is a satellite bias
                final SatelliteDifferentialSignalBias dsb = parseInfo.getSatelliteDsb(satId);
                dsb.getDsb().addBias(obs1, obs2, start, end, bias);
            } else {
                // this is a station bias
                final StationDifferentialSignalBias dsb = parseInfo.getStationDsb(siteCode);
                dsb.getDsb(satId.getSystem()).addBias(obs1, obs2, start, end, bias);
            }
        }
    },

    /** Predicate for OSB line. */
    OSB {
        /** {@inheritDoc} */
        @Override
        protected void store(final SinexBiasParseInfo parseInfo,
                             final String svn, final SatInSystem satId, final String siteCode,
                             final ObservationType obs1, final ObservationType obs2,
                             final AbsoluteDate start, final AbsoluteDate end,
                             final double bias) {
            if (siteCode.isEmpty()) {
                // this is a satellite bias
                final SatelliteObservableSpecificSignalBias osb = parseInfo.getSatelliteOsb(satId);
                osb.getOsb().addBias(obs1, start, end, bias);
            } else {
                // this is a station bias
                final StationObservableSpecificSignalBias osb = parseInfo.getStationOsb(siteCode);
                osb.getOsb(satId.getSystem()).addBias(obs1, start, end, bias);
            }
        }
    };

    /** {@inheritDoc} */
    @Override
    public boolean test(final SinexBiasParseInfo parseInfo) {
        if (name().equals(parseInfo.parseString(1, 3))) {
            // this is the data type we are concerned with
            final String          svn      = parseInfo.parseString(6, 4);
            final SatInSystem     satId    = new SatInSystem(parseInfo.parseString(11, 3));
            final String          siteCode = parseInfo.parseString(15, 9);
            final ObservationType obs1     = parseInfo.parseObservationType(satId.getSystem(), 25, 4);
            final ObservationType obs2     = parseInfo.parseObservationType(satId.getSystem(), 30, 4);
            final AbsoluteDate    start    = parseInfo.stringEpochToAbsoluteDate(parseInfo.parseString(35, 14), true);
            final AbsoluteDate    end      = parseInfo.stringEpochToAbsoluteDate(parseInfo.parseString(50, 14), false);

            // code biases are in time units (ns converted to seconds by parseDoubleWithUnit),
            // they must be converted to meters
            // phase biases are in cycles, no conversion is needed for them
            final double          factor   =
                obs1.getMeasurementType() == MeasurementType.PSEUDO_RANGE ? Constants.SPEED_OF_LIGHT :  1;
            final double          bias     = factor * parseInfo.parseDoubleWithUnit(65, 4, 70, 21);

            store(parseInfo, svn, satId, siteCode, obs1, obs2, start, end, bias);
            return true;
        } else {
            // it is a data type for another predicate
            return false;
        }
    }

    /** Store parsed fields.
     * @param parseInfo container for parse info
     * @param svn satellite SVN
     * @param satId satellite identifier
     * @param siteCode station site code
     * @param obs1 code of first observable
     * @param obs2 code of second observable
     * @param start validity start date
     * @param end validity end date
     * @param bias estimated bias
     */
    protected abstract void store(SinexBiasParseInfo parseInfo,
                                  String svn, SatInSystem satId, String siteCode,
                                  ObservationType obs1, ObservationType obs2,
                                  AbsoluteDate start, AbsoluteDate end,
                                  double bias);

}
