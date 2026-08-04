/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.sinex.orbex;

import org.orekit.files.sinex.AbstractSinexParser;
import org.orekit.files.sinex.BlockParser;
import org.orekit.files.sinex.FooterParser;
import org.orekit.files.sinex.IgnoredBlockContentPredicate;
import org.orekit.files.sinex.IgnoredBlockParser;
import org.orekit.files.sinex.LineParser;
import org.orekit.frames.Frame;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.TimeScales;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Parser for Orbit Exchange Format (ORBEX) files.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class OrbexParser extends AbstractSinexParser<Orbex, OrbexParseInfo> {

    /** Mapping from frame identifier in the file to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** Mapper from string to time system. */
    private final Function<? super String, ? extends TimeSystem> timeSystemBuilder;

    /** Top level parsers. */
    private final List<LineParser<OrbexParseInfo>> topParsers;

    /** Simple constructor.
     * @param frameBuilder      is a function that can construct a frame from an orbex file
     *                          coordinate system string. The coordinate system can be
     *                          any 5 characters string e.g., ITR92, IGb08.
     * @param timeSystemBuilder mapper from string to time system (useful for user-defined time systems)
     * @param timeScales        the set of time scales used for parsing dates
     */
    public OrbexParser(final Function<? super String, ? extends Frame> frameBuilder,
                       final Function<? super String, ? extends TimeSystem> timeSystemBuilder,
                       final TimeScales timeScales) {

        super(timeScales);

        this.frameBuilder      = frameBuilder;
        this.timeSystemBuilder = timeSystemBuilder;

        // set up parsers for supported blocks
        final List<BlockParser<OrbexParseInfo>> blockParsers = new ArrayList<>();
        blockParsers.add(new BlockParser<>("FILE/DESCRIPTION",
                                           Arrays.asList(FileDescriptionPredicate.DESCRIPTION,
                                                         FileDescriptionPredicate.CREATED_BY,
                                                         FileDescriptionPredicate.CREATION_DATE,
                                                         FileDescriptionPredicate.INPUT_DATA,
                                                         FileDescriptionPredicate.CONTACT,
                                                         FileDescriptionPredicate.TIME_SYSTEM,
                                                         FileDescriptionPredicate.START_TIME,
                                                         FileDescriptionPredicate.END_TIME,
                                                         FileDescriptionPredicate.EPOCH_INTERVAL,
                                                         FileDescriptionPredicate.COORD_SYSTEM,
                                                         FileDescriptionPredicate.FRAME_TYPE,
                                                         FileDescriptionPredicate.ORBIT_TYPE,
                                                         FileDescriptionPredicate.LIST_OF_REC_TYPES,
                                                         FileDescriptionPredicate.ORBIT_XYZ_UNITS,
                                                         FileDescriptionPredicate.ORBIT_XYZ_REFERENCE,
                                                         FileDescriptionPredicate.ORBIT_VEL_UNITS,
                                                         FileDescriptionPredicate.SVCLK_UNITS,
                                                         FileDescriptionPredicate.SVCLK_RATE_UNITS)));
        blockParsers.add(new BlockParser<>("SATELLITE/ID_AND_DESCRIPTION",
                                           Collections.singletonList(new SatIdAndDescriptionPredicate())));
        blockParsers.add(new BlockParser<>("EPHEMERIS/DATA",
                                           Arrays.asList(EphemerisDataPredicate.TIME_TAG,
                                                         EphemerisDataPredicate.PCS,
                                                         EphemerisDataPredicate.VCS,
                                                         EphemerisDataPredicate.POS,
                                                         EphemerisDataPredicate.VEL,
                                                         EphemerisDataPredicate.CLK,
                                                         EphemerisDataPredicate.CRT,
                                                         EphemerisDataPredicate.ATT,
                                                         // we currently ignore CPC and CVC records
                                                         new IgnoredBlockContentPredicate<>())));

        // append at the end of the list one catch-all parser ignoring all remaining not supported blocks
        blockParsers.add(new IgnoredBlockParser<>());

        // add the parser for the footer
        topParsers = new ArrayList<>(blockParsers);
        topParsers.add(new FooterParser<>("%=END_ORBEX"));

        // set up siblings
        blockParsers.forEach(parser -> parser.setSiblingParsers(topParsers));

    }

    /** {@inheritDoc} */
    @Override
    protected LineParser<OrbexParseInfo> firstLineParser() {
        return new OrbexVersionParser() {
            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser<OrbexParseInfo>> allowedNextParsers(final OrbexParseInfo parseInfo) {
                return topParsers;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    protected OrbexParseInfo buildParseInfo() {
        return new OrbexParseInfo(frameBuilder, timeSystemBuilder, getTimeScales());
    }

}
