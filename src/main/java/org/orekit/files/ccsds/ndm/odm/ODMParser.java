/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for all CCSDS Orbit Data Message parsers.
 *
 * <p> This base class is immutable, and hence thread safe. When parts must be
 * changed, such as reference date for Mission Elapsed Time or Mission Relative
 * Time time systems, or the gravitational coefficient or the IERS conventions,
 * the various {@code withXxx} methods must be called, which create a new
 * immutable instance with the new parameters. This is a combination of the <a
 * href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
 * pattern</a> and a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a>.
 * @param <T> type of the parsed file
 * @param <P> type of the parser
 *
 * @author Luc Maisonobe
 * @since 6.1
 */
public abstract class ODMParser<T extends ODMFile<?>, P extends ODMParser<T, ?>> {

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** Indicators for expected keywords.
     * @since 11.0
     */
    private Set<Keyword> expected;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     * @since 10.1
     */
    protected ODMParser(final IERSConventions conventions, final boolean simpleEOP,
                        final DataContext dataContext) {
        this.conventions = conventions;
        this.simpleEOP   = simpleEOP;
        this.expected    = new HashSet<>();
        this.dataContext = dataContext;
    }

    /** Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public P withConventions(final IERSConventions newConventions) {
        return create(newConventions, simpleEOP, dataContext);
    }

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Set EOP interpolation method.
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return a new instance, with EOP interpolation method replaced
     * @see #isSimpleEOP()
     */
    public P withSimpleEOP(final boolean newSimpleEOP) {
        return create(conventions, newSimpleEOP, dataContext);
    }

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /**
     * Get the data context used for getting frames, time scales, and celestial bodies.
     *
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Set the data context.
     *
     * @param newDataContext used for frames, time scales, and celestial bodies.
     * @return a new instance with the data context replaced.
     */
    public P withDataContext(final DataContext newDataContext) {
        return create(getConventions(), isSimpleEOP(), newDataContext);
    }

    /** Build a new instance.
     * @param newConventions IERS conventions to use while parsing
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param newDataContext data context used for frames, time scales, and celestial bodies
     * @return a new instance with changed parameters
     * @since 11.0
     */
    protected abstract P create(IERSConventions newConventions,
                                boolean newSimpleEOP,
                                DataContext newDataContext);

    /** Parse a CCSDS Orbit Data Message.
     * @param fileName name of the file containing the message
     * @return parsed orbit
     */
    public T parse(final String fileName) {
        try (InputStream stream = new FileInputStream(fileName)) {
            return parse(stream, fileName);
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param stream stream containing message
     * @return parsed orbit
     */
    public T parse(final InputStream stream) {
        return parse(stream, "<unknown>");
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     * @return parsed orbit
     */
    public abstract T parse(InputStream stream, String fileName);

    /** Parse an entry from the header.
     * @param keyValue key = value pair
     * @param odmFile instance to update with parsed entry
     * @return true if the keyword was a header keyword and has been parsed
     */
    protected boolean parseHeaderEntry(final KeyValue keyValue, final T odmFile) {
        switch (keyValue.getKeyword()) {

            case CREATION_DATE:
                odmFile.getHeader().setCreationDate(new AbsoluteDate(keyValue.getValue(),
                                                                     dataContext.getTimeScales().getUTC()));
                return true;

            case ORIGINATOR:
                odmFile.getHeader().setOriginator(keyValue.getValue());
                return true;

            case MESSAGE_ID:
                odmFile.getHeader().setMessageId(keyValue.getValue());
                return true;

            default:
                return false;

        }

    }

    /** Parse a meta-data key = value entry.
     * @param keyValue key = value pair
     * @param metaData instance to update with parsed entry
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue, final ODMMetadata metaData) {
        switch (keyValue.getKeyword()) {

            case COMMENT:
                metaData.addComment(keyValue.getValue());
                return true;

            case OBJECT_NAME:
                metaData.setObjectName(keyValue.getValue());
                return true;

            default:
                return false;
        }
    }

    /** Declare a keyword to be expected later during parsing.
     * @param keyword keyword that is expected
     * @since 11.0
     */
    protected void declareExpected(final Keyword keyword) {
        expected.add(keyword);
    }

    /** Declare a keyword as found during parsing.
     * @param keyword keyword found
     * @since 11.0
     */
    protected void declareFound(final Keyword keyword) {
        expected.remove(keyword);
    }

    /** Check if all expected keywords have been found.
     * @param fileName name of the file
     * @exception OrekitException if some expected keywords are missing
     * @since 11.0
     */
    protected void checkExpected(final String fileName) throws OrekitException {
        if (!expected.isEmpty()) {
            throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                      expected.iterator().next(), fileName);
        }
    }

}
