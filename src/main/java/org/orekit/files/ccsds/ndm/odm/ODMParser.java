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
package org.orekit.files.ccsds.ndm.odm;

import java.util.HashSet;
import java.util.Set;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.ndm.NDMParser;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for all CCSDS Orbit Data Message parsers.
 *
 * @param <T> type of the parsed file
 * @param <P> type of the parser
 *
 * @author Luc Maisonobe
 * @since 6.1
 */
public abstract class ODMParser<T extends ODMFile<?>, P extends ODMParser<T, ?>> extends NDMParser<T, P> {

    /** Indicators for expected keywords.
     * @since 11.0
     */
    private Set<Keyword> expected;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales
     * @since 10.1
     */
    protected ODMParser(final IERSConventions conventions, final boolean simpleEOP,
                        final DataContext dataContext) {
        super(conventions, simpleEOP, dataContext);
        this.expected    = new HashSet<>();
    }


    /** Parse an entry from the header.
     * @param keyValue key = value pair
     * @param odmFile instance to update with parsed entry
     * @return true if the keyword was a header keyword and has been parsed
     */
    protected boolean parseHeaderEntry(final KeyValue keyValue, final T odmFile) {
        switch (keyValue.getKeyword()) {

            case CREATION_DATE:
                odmFile.getHeader().setCreationDate(new AbsoluteDate(keyValue.getValue(),
                                                                     getDataContext().getTimeScales().getUTC()));
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
     * @param metadata instance to update with parsed entry
     * @param lineNumber number of line being parsed
     * @param fileName name of the parsed file
     * @param line full parsed line
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue, final ODMMetadata metadata,
                                         final int lineNumber, final String fileName, final String line) {
        switch (keyValue.getKeyword()) {

            case COMMENT:
                metadata.addComment(keyValue.getValue());
                return true;

            case OBJECT_NAME:
                metadata.setObjectName(keyValue.getValue());
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
