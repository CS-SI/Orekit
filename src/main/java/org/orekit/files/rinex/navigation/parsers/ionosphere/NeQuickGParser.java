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
package org.orekit.files.rinex.navigation.parsers.ionosphere;

import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.navigation.IonosphereAij;
import org.orekit.files.rinex.navigation.IonosphereNequickGMessage;
import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.files.rinex.utils.ParsingUtils;

/** Parser for NeQuick G ionosphere.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NeQuickGParser extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for NeQuick G message. */
    private final IonosphereNequickGMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public NeQuickGParser(final ParseInfo parseInfo, IonosphereNequickGMessage message) {
        super(RecordType.ION);
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        message.setTransmitTime(parseInfo.parseDate(parseInfo.getLine(), message.getSystem()));
        message.getAij().setAi0(parseInfo.parseDouble2(IonosphereAij.SFU));
        message.getAij().setAi1(parseInfo.parseDouble3(IonosphereAij.SFU_PER_DEG));
        message.getAij().setAi2(parseInfo.parseDouble4(IonosphereAij.SFU_PER_DEG2));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setFlags((int) FastMath.rint(ParsingUtils.parseDouble(parseInfo.getLine(), 4, 19)));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addNequickGMessage(message);
    }

}
