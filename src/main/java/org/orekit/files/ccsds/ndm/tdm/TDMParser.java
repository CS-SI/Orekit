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
package org.orekit.files.ccsds.ndm.tdm;

import java.util.Deque;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KVNStructureProcessingState;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XMLStructureProcessingState;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.state.AbstractMessageParser;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;


/**
 * Class for CCSDS Tracking Data Message parsers.
 *
 * <p> This class allow the handling of both "keyvalue" and "xml" TDM file formats.
 * Format can be inferred if file names ends respectively with ".txt" or ".xml".
 * Otherwise it must be explicitely set using {@link #withFileFormat(TDMFileFormat)}
 *
 * <p>References:<p>
 *  - <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard</a> ("Tracking Data Message", Blue Book, Issue 1, November 2007).<p>
 *  - <a href="https://public.ccsds.org/Pubs/505x0b1.pdf">CCSDS 505.0-B-1 recommended standard</a> ("XML Specification for Navigation Data Message", Blue Book, Issue 1, December 2010).<p>
 *
 * @author Maxime Journot
 * @since 9.0
 */
public class TDMParser extends AbstractMessageParser<TDMFile, TDMParser> {

    /** Root element for XML files. */
    private static final String ROOT = "tdm";

    /** Key for format version. */
    private static final String FORMAT_VERSION_KEY = "CCSDS_TDM_VERS";

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Metadata for current observation block. */
    private TDMMetadata metadata;

    /** Parsing context valid for current metadata. */
    private ParsingContext context;

    /** Current Observation Block being parsed. */
    private ObservationsBlock observationsBlock;

    /** TDMFile object being filled. */
    private TDMFile file;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     */
    public TDMParser(final IERSConventions conventions,
                     final boolean simpleEOP,
                     final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate) {
        super(FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext);
        this.missionReferenceDate = missionReferenceDate;
    }

    /** Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return file.getHeader();
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        file              = new TDMFile(getConventions(), getDataContext());
        metadata          = null;
        context           = null;
        observationsBlock = null;
        reset(fileFormat,
              fileFormat == FileFormat.XML ?
                            new XMLStructureProcessingState(ROOT, this) :
                            new HeaderProcessingState(getDataContext(), getFormatVersionKey(),
                                                      file.getHeader(), new KVNStructureProcessingState(this)));
    }

    /** {@inheritDoc} */
    @Override
    public TDMFile build() {
        file.checkTimeSystems();
        return file;
    }

    /** {@inheritDoc} */
    @Override
    public ProcessingState startMetadata() {
        metadata = new TDMMetadata();
        context  = new ParsingContext(this::getConventions,
                                      this::isSimpleEOP,
                                      this::getDataContext,
                                      this::getMissionReferenceDate,
                                      metadata::getTimeSystem);
        return this::processMetadataToken;
    }

    /** {@inheritDoc} */
    @Override
    public void stopMetadata() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public ProcessingState startData() {
        observationsBlock = new ObservationsBlock();
        return this::processDataToken;
    }

    /** {@inheritDoc} */
    @Override
    public void stopData() {
        file.addSegment(new Segment<>(metadata, observationsBlock));
        metadata          = null;
        context           = null;
        observationsBlock = null;
    }

    /** Process one metadata token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processMetadataToken(final ParseToken token, final Deque<ParseToken> next) {
        try {
            final TDMMetadataKey key = TDMMetadataKey.valueOf(token.getName());
            key.parse(token, context, metadata);
            return this::processMetadataToken;
        } catch (IllegalArgumentException iae) {
            // token has not been recognized, it is most probably the end of the metadata section
            // we push the token back into next queue and let the structure parser handle it
            next.offerLast(token);
            return getFileFormat() == FileFormat.XML ?
                                      new XMLStructureProcessingState(ROOT, this) :
                                      new KVNStructureProcessingState(this);
        }
    }

    /** Process one data token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processDataToken(final ParseToken token, final Deque<ParseToken> next) {
        try {
            final TDMDataKey key = TDMDataKey.valueOf(token.getName());
            key.process(token, context, observationsBlock);
            return this::processDataToken;
        } catch (IllegalArgumentException iae) {
            // token has not been recognized, it is most probably the end of the data section
            // we push the token back into next queue and let the structure parser handle it
            next.offerLast(token);
            return getFileFormat() == FileFormat.XML ?
                                      new XMLStructureProcessingState(ROOT, this) :
                                      new KVNStructureProcessingState(this);
        }
    }

}
