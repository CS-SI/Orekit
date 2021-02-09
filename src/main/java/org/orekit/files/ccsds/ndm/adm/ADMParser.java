/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.state.AbstractMessageParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for Attitude Data Message parsers.
 * @param <T> type of the file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class ADMParser<T extends NDMFile<?, ?>, P extends AbstractMessageParser<T, ?>>
    extends AbstractMessageParser<T, P> {

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Complete constructor.
     * @param formatVersionKey key for format version
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     */
    protected ADMParser(final String formatVersionKey,
                        final IERSConventions conventions, final boolean simpleEOP,
                        final DataContext dataContext, final AbsoluteDate missionReferenceDate) {
        super(formatVersionKey, conventions, simpleEOP, dataContext);
        this.missionReferenceDate = missionReferenceDate;
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Process a CCSDS Euler angles sequence as a {@link RotationOrder}.
     * @param sequence Euler angles sequence token
     * @param consumer consumer of the rotation order
     * @return always return {@code true}
     */
    public static boolean processRotationOrder(final ParseToken sequence,
                                               final RotationOrderConsumer consumer) {
        if (sequence.getType() == TokenType.ENTRY) {
            try {
                consumer.accept(RotationOrder.valueOf(sequence.getContentAsNormalizedString().
                                                      replace('1', 'X').
                                                      replace('2', 'Y').
                                                      replace('3', 'Z')));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(OrekitMessages.CCSDS_INVALID_ROTATION_SEQUENCE,
                                          sequence.getContentAsNormalizedString(),
                                          sequence.getLineNumber(), sequence.getFileName());
            }
        }
        return true;
    }

    /** Interface representing instance methods that consume otation order values. */
    public interface RotationOrderConsumer {
        /** Consume a data.
         * @param value value to consume
         */
        void accept(RotationOrder value);
    }

}
