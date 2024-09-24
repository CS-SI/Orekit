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

import org.orekit.time.AbsoluteDate;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** Predicate for EOP entries in SINEX files.
 * @author Luc Maisonobe
 * @since 13.0
 */
enum EopPredicate implements Predicate<SinexParseInfo> {

    /** Parser for XPO line. */
    XPO(SinexEopEntry::setxPo),

    /** Parser for YPO line. */
    YPO(SinexEopEntry::setyPo),

    /** Parser for LOD line. */
    LOD(SinexEopEntry::setLod),

    /** Parser for UT line. */
    UT(SinexEopEntry::setUt1MinusUtc),

    /** Parser for NUT_LN line. */
    NUT_LN(SinexEopEntry::setNutLn),

    /** Parser for NUT_OB line. */
    NUT_OB(SinexEopEntry::setNutOb),

    /** Parser for NUT_X line. */
    NUT_X(SinexEopEntry::setNutX),

    /** Parser for NUT_Y line. */
    NUT_Y(SinexEopEntry::setNutY);

    /** Consumer for value. */
    private final BiConsumer<SinexEopEntry, Double> consumer;

    /** Simple constructor.
     * @param consumer consumer for value
     */
    EopPredicate(final BiConsumer<SinexEopEntry, Double> consumer) {
        this.consumer = consumer;
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(final SinexParseInfo parseInfo) {
        if (name().equals(parseInfo.parseString(7, 6))) {
            // this is the data type we are concerned with
            final AbsoluteDate date = parseInfo.stringEpochToAbsoluteDate(parseInfo.parseString(27, 12), false);
            consumer.accept(parseInfo.createEOPEntry(date), parseInfo.parseDoubleWithUnit(40, 4, 47, 21));
            return true;
        } else {
            // it is a data type for another predicate
            return false;
        }
    }

}
