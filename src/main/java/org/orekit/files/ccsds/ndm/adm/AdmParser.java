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

import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NdmFile;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.lexical.XmlTokenBuilder;
import org.orekit.files.ccsds.utils.parsing.AbstractMessageParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for Attitude Data Message parsers.
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @param <T> type of the file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AdmParser<T extends NdmFile<?, ?>, P extends AbstractMessageParser<T, ?>>
    extends AbstractMessageParser<T, P> {

    /** Index rotation element name. */
    private static final String ROTATION_1 = "rotation1";

    /** Index rotation element name. */
    private static final String ROTATION_2 = "rotation2";

    /** Index rotation element name. */
    private static final String ROTATION_3 = "rotation3";

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Spin axis in spacecraft body frame. */
    private final Vector3D spinAxis;

    /** Complete constructor.
     * @param root root element for XML files
     * @param formatVersionKey key for format version
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     * @param spinAxis spin axis in spacecraft body frame
     * (may be null if attitude type is neither spin nor spin/nutation)
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     */
    protected AdmParser(final String root, final String formatVersionKey, final IERSConventions conventions,
                        final boolean simpleEOP, final DataContext dataContext,
                        final AbsoluteDate missionReferenceDate, final Vector3D spinAxis,
                        final ParsedUnitsBehavior parsedUnitsBehavior) {
        super(root, formatVersionKey, conventions, simpleEOP, dataContext, parsedUnitsBehavior);
        this.missionReferenceDate = missionReferenceDate;
        this.spinAxis             = spinAxis;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, XmlTokenBuilder> getSpecialXmlElementsBuilders() {

        final Map<String, XmlTokenBuilder> builders = super.getSpecialXmlElementsBuilders();

        // special handling of rotation elements
        builders.put(ROTATION_1, new RotationXmlTokenBuilder());
        builders.put(ROTATION_2, new RotationXmlTokenBuilder());
        builders.put(ROTATION_3, new RotationXmlTokenBuilder());

        return builders;

    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /**
     * Get spin axis in spacecraft body frame.
     * @return spin axis
     */
    public Vector3D getSpinAxis() {
        return spinAxis;
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
                consumer.accept(RotationOrder.valueOf(sequence.getContentAsUppercaseString().
                                                      replace('1', 'X').
                                                      replace('2', 'Y').
                                                      replace('3', 'Z')));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(OrekitMessages.CCSDS_INVALID_ROTATION_SEQUENCE,
                                          sequence.getContentAsUppercaseString(),
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
