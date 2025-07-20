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
package org.orekit.files.rinex.navigation.parsers;

import org.orekit.files.rinex.navigation.MessageType;
import org.orekit.files.rinex.navigation.RinexNavigation;

/** Parser for Glonass CDMA.
 * <p>
 * This parser is not implemented yet!
 * It just ignores all lines
 * </p>
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GlonassCdmaParser extends MessageLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     */
    GlonassCdmaParser(final ParseInfo parseInfo) {
        super(MessageType.ORBIT);
        this.parseInfo = parseInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine04() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine08() {
        parseInfo.closePendingMessage();
    }

    /** {@inheritDoc} */
    @Override
    public void closeMessage(final RinexNavigation file) {
    }

}
