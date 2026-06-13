/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.navigation.writers.ephemeris;

import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;

import java.io.IOException;

/** Base writer for NavIC L1NV, QZSS, and GPS civilian messages.
 * @param <T> type of the navigation messages this writer handles
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class CivilianLevel1NavigationMessageWriter<T extends AbstractNavigationMessage<T>>
    extends AbstractNavigationMessageWriter<T> {

    /** {@inheritDoc} */
    @Override
    protected void writeField1Line1(final T message, final RinexNavigationWriter writer)
        throws IOException {
        writer.writeDouble(message.getADot(), RinexNavigationParser.M_PER_S);
    }

}
