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
package org.orekit.files.rinex.navigation.parsers.ephemeris;

import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.propagation.analytical.gnss.data.CivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.CivilianNavigationMessageFactory;
import org.orekit.utils.units.Unit;

/** Parser for QZSS and GPS civilian messages.
 * @param <T> type of the navigation message
 * @param <F> type of the navigation message factory
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class CivilianLevel2NavigationParser<T extends CivilianNavigationMessage<T>,
                                                     F extends CivilianNavigationMessageFactory<T>>
    extends CivilianLevel1NavigationParser<T, F> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param factory factory for navigation message
     */
    protected CivilianLevel2NavigationParser(final ParseInfo parseInfo, final F factory) {
        super(parseInfo, factory);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        final F         factory   = getFactory();
        final ParseInfo parseInfo = getParseInfo();
        factory.getIDotDriver().setValue(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        factory.getDeltaN0DotDriver().setValue(parseInfo.parseDouble2(RinexNavigationParser.RAD_PER_S2));
        factory.setUraiNed0(parseInfo.parseInt3());
        factory.setUraiNed1(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final F         factory   = getFactory();
        final ParseInfo parseInfo = getParseInfo();
        factory.setUraiEd(parseInfo.parseInt1());
        factory.setSvHealth(parseInfo.parseInt2());
        factory.setTGD(parseInfo.parseDouble3(Unit.SECOND));
        factory.setUraiNed2(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final F         factory   = getFactory();
        final ParseInfo parseInfo = getParseInfo();
        factory.setIscL1CA(parseInfo.parseDouble1(Unit.SECOND));
        factory.setIscL2C(parseInfo.parseDouble2(Unit.SECOND));
        factory.setIscL5I5(parseInfo.parseDouble3(Unit.SECOND));
        factory.setIscL5Q5(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine08() {
        final F         factory   = getFactory();
        final ParseInfo parseInfo = getParseInfo();
        if (factory.isCnv2()) {
            // in CNAV2 messages, there is an additional line for L1 CD and L1 CP inter signal delay
            factory.setIscL1CD(parseInfo.parseDouble1(Unit.SECOND));
            factory.setIscL1CP(parseInfo.parseDouble2(Unit.SECOND));
        } else {
            parseTransmissionTimeLine();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine09() {
        parseTransmissionTimeLine();
    }

    /** Parse transmission time line.
     */
    protected void parseTransmissionTimeLine() {
        final F         factory   = getFactory();
        final ParseInfo parseInfo = getParseInfo();
        factory.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        factory.setWeek(parseInfo.parseInt2());
        factory.setFlags(getParseInfo().parseInt3());
        parseInfo.closePendingRecord();
    }

}
