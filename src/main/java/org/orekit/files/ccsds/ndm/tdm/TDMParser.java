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

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KVNStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
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

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

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
        super(TDMFile.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext);
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
        file               = new TDMFile(getConventions(), getDataContext());
        metadata           = null;
        context            = null;
        observationsBlock  = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XMLStructureProcessingState(ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KVNStructureProcessingState(this);
            reset(fileFormat, new HeaderProcessingState(getDataContext(), this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public TDMFile build() {
        file.checkTimeSystems();
        return file;
    }

    /** {@inheritDoc} */
    @Override
    public void prepareHeader() {
        setFallback(new HeaderProcessingState(getDataContext(), this));
    }

    /** {@inheritDoc} */
    @Override
    public void inHeader() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeHeader() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void prepareMetadata() {
        metadata  = new TDMMetadata();
        context   = new ParsingContext(this::getConventions,
                                       this::isSimpleEOP,
                                       this::getDataContext,
                                       this::getMissionReferenceDate,
                                       metadata::getTimeSystem);
        setFallback(this::processMetadataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inMetadata() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeMetadata() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void prepareData() {
        observationsBlock = new ObservationsBlock();
        setFallback(this::processDataToken);
    }

    /** {@inheritDoc} */
    @Override
    public void inData() {
        setFallback(structureProcessor);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeData() {
        file.addSegment(new Segment<>(metadata, observationsBlock));
        metadata          = null;
        context           = null;
        observationsBlock = null;
    }

    /** Process one metadata token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processMetadataToken(final ParseToken token) {
        inMetadata();
        try {
            return token.getName() != null &&
                   MetadataKey.valueOf(token.getName()).process(token, context, metadata);
        } catch (IllegalArgumentException iaeM) {
            try {
                return TDMMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeT) {
                // token has not been recognized
                return false;
            }
        }
    }

    /** Process one data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processDataToken(final ParseToken token) {
        try {
            inData();
            return token.getName() != null &&
                   TDMDataKey.valueOf(token.getName()).process(token, context, observationsBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

}
