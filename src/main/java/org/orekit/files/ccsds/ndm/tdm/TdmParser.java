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
package org.orekit.files.ccsds.ndm.tdm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.KvnStructureProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureProcessingState;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;
import org.orekit.utils.IERSConventions;


/**
 * Class for CCSDS Tracking Data Message parsers.
 * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>
 *
 * <p>References:</p>
 * <ul>
 *   <li><a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard</a> ("Tracking Data Message", Blue Book, Issue 1, November 2007)</li>
 *   <li><a href="https://public.ccsds.org/Pubs/505x0b1.pdf">CCSDS 505.0-B-1 recommended standard</a> ("XML Specification for Navigation Data Message", Blue Book, Issue 1, December 2010)</li>
 * </ul>
 *
 * @author Maxime Journot
 * @since 9.0
 */
public class TdmParser extends AbstractConstituentParser<TdmHeader, Tdm, TdmParser> {

    /** Converter for {@link RangeUnits#RU Range Units} (may be null). */
    private final RangeUnitsConverter converter;

    /** Metadata for current observation block. */
    private TdmMetadata metadata;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** Current Observation Block being parsed. */
    private ObservationsBlock observationsBlock;

    /** File header. */
    private TdmHeader header;

    /** File segments. */
    private List<Segment<TdmMetadata, ObservationsBlock>> segments;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildTdmParser()
     * parserBuilder.buildTdmParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     * @param converter converter for {@link RangeUnits#RU Range Units} (may be null if there
     * are no range observations in {@link RangeUnits#RU Range Units})
     * @param filters filters to apply to parse tokens
     * @since 12.0
     */
    public TdmParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                     final ParsedUnitsBehavior parsedUnitsBehavior, final RangeUnitsConverter converter,
                     final Function<ParseToken, List<ParseToken>>[] filters) {
        super(Tdm.ROOT, Tdm.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, parsedUnitsBehavior, filters);
        this.converter = converter;
    }

    /** {@inheritDoc} */
    @Override
    public TdmHeader getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header             = new TdmHeader();
        segments           = new ArrayList<>();
        metadata           = null;
        context            = null;
        observationsBlock  = null;
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(Tdm.ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new KvnStructureProcessingState(this);
            reset(fileFormat, new HeaderProcessingState(this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Tdm build() {
        return new Tdm(header, segments, getConventions(), getDataContext());
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareHeader() {
        anticipateNext(new HeaderProcessingState(this));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inHeader() {
        anticipateNext(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeHeader() {
        header.validate(header.getFormatVersion());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareMetadata() {
        if (metadata != null) {
            return false;
        }
        metadata  = new TdmMetadata();
        context   = new ContextBinding(
            this::getConventions, this::isSimpleEOP,
            this::getDataContext, this::getParsedUnitsBehavior,
            () -> null, metadata::getTimeSystem, () -> 0.0, () -> 1.0);
        anticipateNext(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        anticipateNext(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeMetadata() {
        metadata.validate(header.getFormatVersion());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        observationsBlock = new ObservationsBlock();
        anticipateNext(this::processDataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inData() {
        anticipateNext(structureProcessor);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeData() {
        segments.add(new Segment<>(metadata, observationsBlock));
        metadata          = null;
        context           = null;
        observationsBlock = null;
        return true;
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
                return TdmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
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
            try {
                // global tokens (observation wrapper, comments, epoch in XML)
                return token.getName() != null &&
                       TdmDataKey.valueOf(token.getName()).process(token, context, observationsBlock);
            } catch (IllegalArgumentException iae) {
                // observation
                return ObservationType.valueOf(token.getName()).process(token, context, converter, metadata, observationsBlock);
            }
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

}
