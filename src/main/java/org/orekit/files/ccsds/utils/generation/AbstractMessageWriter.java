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
package org.orekit.files.ccsds.utils.generation;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;

/**
 * Base class for Navigation Data Message (NDM) files.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractMessageWriter {

    /** Default value for {@link HeaderKey#ORIGINATOR}. */
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    public static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** String format used for dates. **/
    private static final String DATE_FORMAT = "%04d-%02d-%02dT%02d:%02d:%012.9f";

    /** File name for error messages. */
    private final String fileName;

    /** Key for format version. */
    private final String formatVersionKey;

    /** Default format version. */
    private final double defaultVersion;

    /** File header. */
    private final Header header;

    /** Current context binding. */
    private ContextBinding context;

    /** Current converter for dates. */
    private TimeConverter converter;

    /**
     * Constructor used to create a new NDM writer configured with the necessary parameters
     * to successfully fill in all required fields that aren't part of a standard object.
     * <p>
     * If creation date and originator are not present in header, built-in defaults will be used
     * </p>
     * @param formatVersionKey key for format version
     * @param defaultVersion default format version
     * @param header file header (may be null)
     * @param context context binding (may be reset for each segment)
     * @param fileName file name for error messages
     */
    public AbstractMessageWriter(final String formatVersionKey, final double defaultVersion,
                                 final Header header, final ContextBinding context,
                                 final String fileName) {

        this.defaultVersion   = defaultVersion;
        this.formatVersionKey = formatVersionKey;
        this.header           = header;
        this.fileName         = fileName;

        setContext(context);

    }

    /** Reset context binding.
     * @param context context binding to use
     */
    public void setContext(final ContextBinding context) {
        this.context   = context;
        this.converter = context.getTimeSystem().getConverter(context);
    }

    /** Get the current context.
     * @return current context
     */
    public ContextBinding getContext() {
        return context;
    }

    /** Get the file name.
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /** Writes the standard AEM header for the file.
     * @param generator generator to use for producing output
     * @throws IOException if the stream cannot write to stream
     */
    public void writeHeader(final Generator generator) throws IOException {

        final double version = (header == null || Double.isNaN(header.getFormatVersion())) ?
                               defaultVersion : header.getFormatVersion();
        generator.startMessage(formatVersionKey, version);

        // comments are optional
        if (header != null) {
            generator.writeComments(header);
        }

        // creation date is informational only, but mandatory and always in UTC
        if (header == null || header.getCreationDate() == null) {
            final ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
            generator.writeEntry(HeaderKey.CREATION_DATE.name(),
                                 String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                                               zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                                               zdt.getHour(), zdt.getMinute(), (double) zdt.getSecond()),
                                 true);
        } else {
            final DateTimeComponents creationDate =
                            header.getCreationDate().getComponents(context.getDataContext().getTimeScales().getUTC());
            final DateComponents dc = creationDate.getDate();
            final TimeComponents tc = creationDate.getTime();
            generator.writeEntry(HeaderKey.CREATION_DATE.name(),
                                 String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                                               dc.getYear(), dc.getMonth(), dc.getDay(),
                                               tc.getHour(), tc.getMinute(), tc.getSecond()),
                                 true);
        }

        // Use built-in default if mandatory originator not present
        generator.writeEntry(HeaderKey.ORIGINATOR.name(),
                             (header == null || header.getOriginator() == null) ? DEFAULT_ORIGINATOR : header.getOriginator(),
                             true);

        if (header != null) {
            generator.writeEntry(HeaderKey.MESSAGE_ID.name(), header.getMessageId(), false);
        }

        // add an empty line for presentation
        generator.writeEmptyLine();

    }


    /** Convert a date to string value with high precision.
     * @param date date to write
     * @return date as a string
     */
    protected String dateToString(final AbsoluteDate date) {
        final DateTimeComponents dt = converter.components(date);
        return String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                             dt.getDate().getYear(),
                             dt.getDate().getMonth(),
                             dt.getDate().getDay(),
                             dt.getTime().getHour(),
                             dt.getTime().getMinute(),
                             dt.getTime().getSecond());
    }

}