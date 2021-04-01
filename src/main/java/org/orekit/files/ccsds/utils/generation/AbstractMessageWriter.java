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

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;

/**
 * Base class for Navigation Data Message (NDM) files.
 * @param <H> type of the header
 * @param <S> type of the segments
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractMessageWriter<H extends Header, S extends Segment<?, ?>> implements MessageWriter<H, S> {

    /** Default value for {@link HeaderKey#ORIGINATOR}. */
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /** Key for format version. */
    private final String formatVersionKey;

    /** Default format version. */
    private final double defaultVersion;

    /** Current context binding. */
    private ContextBinding context;

    /** Current converter for dates. */
    private TimeConverter timeConverter;

    /**
     * Constructor used to create a new NDM writer configured with the necessary parameters
     * to successfully fill in all required fields that aren't part of a standard object.
     * <p>
     * If creation date and originator are not present in header, built-in defaults will be used
     * </p>
     * @param formatVersionKey key for format version
     * @param defaultVersion default format version
     * @param context context binding (may be reset for each segment)
     */
    public AbstractMessageWriter(final String formatVersionKey, final double defaultVersion,
                                 final ContextBinding context) {

        this.defaultVersion   = defaultVersion;
        this.formatVersionKey = formatVersionKey;

        setContext(context);

    }

    /** Reset context binding.
     * @param context context binding to use
     */
    public void setContext(final ContextBinding context) {
        this.context       = context;
        this.timeConverter = context.getTimeSystem().getConverter(context);
    }

    /** Get the current context.
     * @return current context
     */
    public ContextBinding getContext() {
        return context;
    }

    /** Get the current time converter.
     * @return current time converter
     */
    public TimeConverter getTimeConverter() {
        return timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    public void writeHeader(final Generator generator, final H header) throws IOException {

        final double version = (header == null || Double.isNaN(header.getFormatVersion())) ?
                               defaultVersion : header.getFormatVersion();
        generator.startMessage(formatVersionKey, version);

        // comments are optional
        if (header != null) {
            generator.writeComments(header.getComments());
        }

        // creation date is informational only, but mandatory and always in UTC
        if (header == null || header.getCreationDate() == null) {
            final ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
            generator.writeEntry(HeaderKey.CREATION_DATE.name(),
                                 generator.dateToString(zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                                                        zdt.getHour(), zdt.getMinute(), (double) zdt.getSecond()),
                                 true);
        } else {
            final DateTimeComponents creationDate =
                            header.getCreationDate().getComponents(context.getDataContext().getTimeScales().getUTC());
            final DateComponents dc = creationDate.getDate();
            final TimeComponents tc = creationDate.getTime();
            generator.writeEntry(HeaderKey.CREATION_DATE.name(),
                                 generator.dateToString(dc.getYear(), dc.getMonth(), dc.getDay(),
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
        generator.newLine();

    }

}
