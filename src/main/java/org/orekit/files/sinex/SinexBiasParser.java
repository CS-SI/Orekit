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

import org.orekit.time.TimeScales;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Parser for Solution INdependent EXchange (SINEX) bias files.
 * @author Luc Maisonobe
 * @since 13.0
 */
public class SinexBiasParser extends AbstractSinexParser<SinexBias, SinexBiasParseInfo> {

    /** Top level parsers. */
    private final List<LineParser<SinexBiasParseInfo>> topParsers;

    /** Simple constructor.
     * @param timeScales time scales
     */
    public SinexBiasParser(final TimeScales timeScales) {

        super(timeScales);

        // set up parsers for supported blocks
        final List<BlockParser<SinexBiasParseInfo>> blockParsers = new ArrayList<>();
        blockParsers.add(new BlockParser<>("BIAS/DESCRIPTION",
                                           Arrays.asList(BiasDescriptionPredicate.OBSERVATION_SAMPLING,
                                                         BiasDescriptionPredicate.PARAMETER_SPACING,
                                                         BiasDescriptionPredicate.DETERMINATION_METHOD,
                                                         BiasDescriptionPredicate.BIAS_MODE,
                                                         BiasDescriptionPredicate.TIME_SYSTEM,
                                                         new IgnoredBlockContentPredicate<>())));
        blockParsers.add(new BlockParser<>("BIAS/SOLUTION",
                                           Arrays.asList(BiasSolutionPredicate.DSB,
                                                         BiasSolutionPredicate.OSB,
                                                         // we currently ignore ISB lines
                                                         new IgnoredBlockContentPredicate<>())));

        // append at the end of the list one catch-all parser ignoring all remaining not supported blocks
        blockParsers.add(new IgnoredBlockParser<>());

        // add the parser for the footer
        topParsers = new ArrayList<>(blockParsers);
        topParsers.add(new FooterParser<>("%=ENDBIA"));

        // set up siblings
        blockParsers.forEach(parser -> parser.setSiblingParsers(topParsers));

    }

    /** {@inheritDoc} */
    @Override
    protected LineParser<SinexBiasParseInfo> firstLineParser() {
        return new VersionParser<SinexBiasParseInfo>("BIA") {
            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser<SinexBiasParseInfo>> allowedNextParsers(final SinexBiasParseInfo parseInfo) {
                return topParsers;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    protected SinexBiasParseInfo buildParseInfo(final TimeScales timeScales) {
        SinexBiasParseInfo parseInfo = new SinexBiasParseInfo(timeScales);
        parseInfo.setTimeScale(getTimeScales().getUTC());
        return parseInfo;
    }

}
