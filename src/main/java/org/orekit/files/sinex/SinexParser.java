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

import org.orekit.time.TimeScales;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Parser for Solution INdependent EXchange (SINEX) files.
 * <p>
 * The parser can be used to load several data types contained in Sinex files.
 * The current supported data are: station coordinates, site eccentricities, EOP.
 * </p>
 * <p>
 * The parsing of EOP parameters for multiple data sources in different SinexParser objects might pose
 * a problem in case validity dates are overlapping. As Sinex daily solution files provide a single EOP
 * entry, the Sinex parser will add points at the limits of data dates (startDate, endDate) of the Sinex
 * file, which in case of overlap will lead to inconsistencies in the final EOPHistory object. Multiple
 * data sources can be parsed using a single SinexParser to overcome this issue.
 * </p>
 * @author Luc Maisonobe
 * @since 13.0
 */
public class SinexParser extends AbstractSinexParser<Sinex, SinexParseInfo> {

    /** Top level parsers. */
    private final List<LineParser<SinexParseInfo>> topParsers;

    /** Simple constructor.
     * @param timeScales time scales
     */
    public SinexParser(final TimeScales timeScales) {

        super(timeScales);

        // set up parsers for supported blocks
        final List<BlockParser<SinexParseInfo>> blockParsers = new ArrayList<>();
        blockParsers.add(new BlockParser<>("SITE/ID",
                                           Collections.singletonList(SingleLineBlockPredicate.SITE_ID)));
        blockParsers.add(new BlockParser<>("SITE/ANTENNA",
                                           Collections.singletonList(SingleLineBlockPredicate.SITE_ANTENNA)));
        blockParsers.add(new BlockParser<>("SITE/ECCENTRICITY",
                                           Collections.singletonList(SingleLineBlockPredicate.SITE_ECCENTRICITY)));
        blockParsers.add(new BlockParser<>("SOLUTION/EPOCHS",
                                           Collections.singletonList(SingleLineBlockPredicate.SOLUTION_EPOCHS)));
        blockParsers.add(new BlockParser<>("SOLUTION/ESTIMATE",
                                           Arrays.asList(StationPredicate.STAX, StationPredicate.STAY, StationPredicate.STAZ,
                                                         StationPredicate.VELX, StationPredicate.VELY, StationPredicate.VELZ,
                                                         PsdPredicate.AEXP_E, PsdPredicate.TEXP_E,
                                                         PsdPredicate.ALOG_E, PsdPredicate.TLOG_E,
                                                         PsdPredicate.AEXP_N, PsdPredicate.TEXP_N,
                                                         PsdPredicate.ALOG_N, PsdPredicate.TLOG_N,
                                                         PsdPredicate.AEXP_U, PsdPredicate.TEXP_U,
                                                         PsdPredicate.ALOG_U, PsdPredicate.TLOG_U,
                                                         EopPredicate.XPO,    EopPredicate.YPO,
                                                         EopPredicate.LOD,    EopPredicate.UT,
                                                         EopPredicate.NUT_LN, EopPredicate.NUT_OB,
                                                         EopPredicate.NUT_X,  EopPredicate.NUT_Y,
                                                         new IgnoredBlockContentPredicate<>())));

        // append at the end of the list one catch-all parser ignoring all remaining not supported blocks
        blockParsers.add(new IgnoredBlockParser<>());

        // add the parser for the footer
        topParsers = new ArrayList<>(blockParsers);
        topParsers.add(new FooterParser<>("%ENDSNX"));

        // set up siblings
        blockParsers.forEach(parser -> parser.setSiblingParsers(topParsers));

    }

    /** {@inheritDoc} */
    @Override
    protected LineParser<SinexParseInfo> firstLineParser() {
        return new VersionParser<SinexParseInfo>("SNX") {
            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser<SinexParseInfo>> allowedNextParsers(final SinexParseInfo parseInfo) {
                return topParsers;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    protected SinexParseInfo buildParseInfo() {
        final SinexParseInfo parseInfo = new SinexParseInfo(getTimeScales());
        parseInfo.setTimeScale(getTimeScales().getUTC());
        return parseInfo;
    }

}
