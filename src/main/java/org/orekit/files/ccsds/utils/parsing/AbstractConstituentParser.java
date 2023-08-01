/* Copyright 2002-2023 CS GROUP
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
package org.orekit.files.ccsds.utils.parsing;

import java.util.List;
import java.util.function.Function;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.utils.IERSConventions;

/** Parser for CCSDS messages.
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 * @param <H> type of the header
 * @param <T> type of the file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractConstituentParser<H extends Header, T extends NdmConstituent<H, ?>, P extends AbstractConstituentParser<H, T, ?>>
    extends AbstractMessageParser<T> {

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** Behavior adopted for units that have been parsed from a CCSDS message. */
    private final ParsedUnitsBehavior parsedUnitsBehavior;

    /** Complete constructor.
     * @param root root element for XML files
     * @param formatVersionKey key for format version
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    protected AbstractConstituentParser(final String root,
                                        final String formatVersionKey,
                                        final IERSConventions conventions,
                                        final boolean simpleEOP,
                                        final DataContext dataContext,
                                        final ParsedUnitsBehavior parsedUnitsBehavior,
                                        final Function<ParseToken, List<ParseToken>>[] filters) {
        super(root, formatVersionKey, filters);
        this.conventions         = conventions;
        this.simpleEOP           = simpleEOP;
        this.dataContext         = dataContext;
        this.parsedUnitsBehavior = parsedUnitsBehavior;
    }

    /** Get the behavior to adopt for handling parsed units.
     * @return behavior to adopt for handling parsed units
     */
    public ParsedUnitsBehavior getParsedUnitsBehavior() {
        return parsedUnitsBehavior;
    }

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /** Get the data context used for getting frames, time scales, and celestial bodies.
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /** Get file header to fill.
     * @return file header to fill
     */
    public abstract H getHeader();

    /** Prepare header for parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean prepareHeader();

    /** Acknowledge header parsing has started.
     * @return true if parser was able to perform the action
     */
    public abstract boolean inHeader();

    /** Finalize header after parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean finalizeHeader();

    /** Prepare metadata for parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean prepareMetadata();

    /** Acknowledge metada parsing has started.
     * @return true if parser was able to perform the action
     */
    public abstract boolean inMetadata();

    /** Finalize metadata after parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean finalizeMetadata();

    /** Prepare data for parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean prepareData();

    /** Acknowledge data parsing has started.
     * @return true if parser was able to perform the action
     */
    public abstract boolean inData();

    /** Finalize data after parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean finalizeData();

}
