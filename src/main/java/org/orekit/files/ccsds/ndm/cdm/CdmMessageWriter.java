/* Copyright 2002-2024 CS GROUP
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
package org.orekit.files.ccsds.ndm.cdm;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.MessageWriter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;

/** Cdm message writer.
 * @author Melina Vanel
 * @since 11.2
 */
public abstract class CdmMessageWriter implements MessageWriter<CdmHeader, CdmSegment, Cdm> {

    /** Default value for {@link HeaderKey#ORIGINATOR}. */
    public static final String DEFAULT_ORIGINATOR = "OREKIT";

    /** Root element for XML files. */
    private final String root;

    /** Key for format version. */
    private final String formatVersionKey;

    /** Default format version. */
    private final double defaultVersion;

    /** Current context binding. */
    private ContextBinding context;

    /** Current converter for dates. */
    private TimeConverter timeConverter;

    /** Current format version. */
    private double version;

    /** Boolean to ensure relative metadata will be written only once. */
    private boolean isrelativemetadataWritten;

    /**
     * Constructor used to create a new NDM writer configured with the necessary parameters
     * to successfully fill in all required fields that aren't part of a standard object.
     * <p>
     * If creation date and originator are not present in header, built-in defaults will be used
     * </p>
     * @param root root element for XML files
     * @param formatVersionKey key for format version
     * @param defaultVersion default format version
     * @param context context binding (may be reset for each segment)
     */
    public CdmMessageWriter(final String root, final String formatVersionKey,
                                 final double defaultVersion, final ContextBinding context) {

        this.root                      = root;
        this.defaultVersion            = defaultVersion;
        this.formatVersionKey          = formatVersionKey;
        this.version                   = defaultVersion;
        this.isrelativemetadataWritten = false;

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

    /** Get the default format version.
     * @return default format version
     */
    public double getDefaultVersion() {
        return defaultVersion;
    }

    /** {@inheritDoc} */
    @Override
    public void writeHeader(final Generator generator, final CdmHeader header) throws IOException {

        final ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
        final TimeScale     utc = context.getDataContext().getTimeScales().getUTC();
        final AbsoluteDate date = new AbsoluteDate(zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                                                   zdt.getHour(), zdt.getMinute(), zdt.getSecond(),
                                                   utc);

        // validate before writing
        if (header != null) {

            if (!Double.isNaN(header.getFormatVersion())) {
                // save format version for validating segments
                version = header.getFormatVersion();
            }

            if (header.getCreationDate() == null) {
                header.setCreationDate(date);
            }

            if (header.getOriginator() == null) {
                header.setOriginator(DEFAULT_ORIGINATOR);
            }

            header.validate(version);

        }

        generator.startMessage(root, formatVersionKey, version);

        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.header.name());
        }

        // comments are optional
        if (header != null) {
            generator.writeComments(header.getComments());
        }

        // creation date is informational only, but mandatory and always in UTC
        final DateTimeComponents creationDate = ((header == null) ? date : header.getCreationDate()).getComponents(utc);
        final DateComponents     dc           = creationDate.getDate();
        final TimeComponents     tc           = creationDate.getTime();
        generator.writeEntry(HeaderKey.CREATION_DATE.name(),
                             generator.dateToString(dc.getYear(), dc.getMonth(), dc.getDay(),
                                                    tc.getHour(), tc.getMinute(), tc.getSecond()),
                             null, true);

        // Use built-in default if mandatory originator not present
        generator.writeEntry(HeaderKey.ORIGINATOR.name(),
                             header == null ? DEFAULT_ORIGINATOR : header.getOriginator(),
                             null, true);

        if (header != null) {
            generator.writeEntry(CdmHeaderKey.MESSAGE_FOR.name(), header.getMessageFor(), null, false);
        }

        if (header != null) {
            generator.writeEntry(HeaderKey.MESSAGE_ID.name(), header.getMessageId(), null, false);
        }

        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

        // add an empty line for presentation
        generator.newLine();

        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.body.name());
        }

    }

    /** {@inheritDoc} */
    @Override
    public void writeSegment(final Generator generator, final CdmSegment segment) throws IOException {

        // validate before writing
        segment.getMetadata().validate(version);
        segment.getData().validate(version);

        // relative metadata should only be written once after the header at the beginning of the body
        if (!isrelativemetadataWritten) {
            writeRelativeMetadataContent(generator, version, segment.getMetadata().getRelativeMetadata());
            isrelativemetadataWritten = true;
        }

        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.segment.name());
        }
        writeSegmentContent(generator, version, segment);
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

    /** Write RelativeMetadata part only once after header.
     * @param generator generator to use for producing output
     * @param formatVersion format version to use
     * @param relativeMetadata relative metadata to write
     * @throws IOException if any buffer writing operations fails
     */
    public abstract void writeRelativeMetadataContent(Generator generator, double formatVersion,
                                                      CdmRelativeMetadata relativeMetadata) throws IOException;

    /** Write one segment content (without XML wrapping).
     * @param generator generator to use for producing output
     * @param formatVersion format version to use
     * @param segment segment to write
     * @throws IOException if any buffer writing operations fails
     */
    public abstract void writeSegmentContent(Generator generator, double formatVersion,
                                             Segment<CdmMetadata, CdmData> segment) throws IOException;

    /** {@inheritDoc} */
    @Override
    public void writeFooter(final Generator generator) throws IOException {
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }
        generator.endMessage(root);
    }

    /** {@inheritDoc} */
    @Override
    public String getRoot() {
        return root;
    }

    /** {@inheritDoc} */
    @Override
    public String getFormatVersionKey() {
        return formatVersionKey;
    }

    /** {@inheritDoc} */
    @Override
    public double getVersion() {
        return version;
    }

}
