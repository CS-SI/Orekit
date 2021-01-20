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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NDMHeaderParsingState;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.EventType;
import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.files.ccsds.utils.lexical.ParseEvent;
import org.orekit.files.ccsds.utils.lexical.ParsingState;
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
public class TDMParser extends MessageParser<TDMFile, TDMParser> {

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

    /** Simple constructor.
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #withMissionReferenceDate(AbsoluteDate)}.
     * </p>
     * <p>
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    @DefaultDataContext
    public TDMParser() {
        this(DataContext.getDefault());
    }

    /** Constructor with data context.
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #withMissionReferenceDate(AbsoluteDate)}.
     * </p>
     * <p>
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     *
     * @param dataContext used by the parser.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    public TDMParser(final DataContext dataContext) {
        this(null, true, dataContext, AbsoluteDate.FUTURE_INFINITY);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     */
    private TDMParser(final IERSConventions conventions,
                      final boolean simpleEOP,
                      final DataContext dataContext,
                      final AbsoluteDate missionReferenceDate) {
        super(FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext);
        this.missionReferenceDate = missionReferenceDate;
    }

    /** {@inheritDoc} */
    @Override
    protected TDMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext) {
        return create(newConventions, newSimpleEOP, newDataContext, missionReferenceDate);
    }

    /** Build a new instance.
     * @param newConventions IERS conventions to use while parsing
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param newDataContext data context used for frames, time scales, and celestial bodies
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance with changed parameters
     * @since 11.0
     */
    protected TDMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext,
                               final AbsoluteDate newMissionReferenceDate) {
        return new TDMParser(newConventions, newSimpleEOP, newDataContext, newMissionReferenceDate);
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public TDMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new TDMParser(getConventions(), isSimpleEOP(), getDataContext(), newMissionReferenceDate);
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
    public void reset() {
        file              = new TDMFile();
        metadata          = null;
        context           = null;
        observationsBlock = null;
        reset(new NDMHeaderParsingState(getDataContext(), getFormatVersionKey(),
                                        file.getHeader(), this::processStructureEvent));
    }

    /** {@inheritDoc} */
    @Override
    public TDMFile build() {
        file.checkTimeSystems();
        return file;
    }

    /** Process one structure event.
     * @param event event to process
     * @param next queue for pending events waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming events
     */
    private ParsingState processStructureEvent(final ParseEvent event, final Deque<ParseEvent> next) {
        switch (event.getName()) {
            case "tdm":
                // ignored
                return this::processStructureEvent;
            case "header":
                return new NDMHeaderParsingState(getDataContext(), getFormatVersionKey(),
                                                 file.getHeader(), this::processStructureEvent);
            case "body":
                // ignored
                return this::processStructureEvent;
            case "segment":
                // ignored
                return this::processStructureEvent;
            case "META" : case "metadata" :
                if (event.getType() == EventType.START) {
                    // next parse events will be handled as metadata
                    metadata = new TDMMetadata();
                    context  = new ParsingContext(this::getConventions,
                                                  this::isSimpleEOP,
                                                  this::getDataContext,
                                                  this::getMissionReferenceDate,
                                                  metadata::getTimeSystem);
                    return this::processMetadataEvent;
                } else if (event.getType() == EventType.END) {
                    // nothing to do here, we expect a DATA_START next
                    return this::processStructureEvent;
                }
                break;
            case "DATA" : case "data" :
                if (event.getType() == EventType.START) {
                    // next parse events will be handled as data
                    observationsBlock = new ObservationsBlock();
                    return this::processDataEvent;
                } else if (event.getType() == EventType.END) {
                    file.addSegment(new NDMSegment<>(metadata, observationsBlock));
                    metadata          = null;
                    context           = null;
                    observationsBlock = null;
                    // we expect a META_START next
                    return this::processStructureEvent;
                }
                break;
            default :
                // nothing to do here, errors are handled below
        }
        throw event.generateException();
    }

    /** Process one metadata event.
     * @param event event to process
     * @param next queue for pending events waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming events
     */
    private ParsingState processMetadataEvent(final ParseEvent event, final Deque<ParseEvent> next) {
        try {
            final TDMMetadataKey key = TDMMetadataKey.valueOf(event.getName());
            key.parse(event, context, metadata);
            return this::processMetadataEvent;
        } catch (IllegalArgumentException iae) {
            // event has not been recognized, it is most probably the end of the metadata section
            // we push the event back into next queue and let the structure parser handle it
            next.offerLast(event);
            return this::processStructureEvent;
        }
    }

    /** Process one data event.
     * @param event event to process
     * @param next queue for pending events waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming events
     */
    private ParsingState processDataEvent(final ParseEvent event, final Deque<ParseEvent> next) {
        try {
            final TDMDataKey key = TDMDataKey.valueOf(event.getName());
            key.process(event, context, observationsBlock);
            return this::processDataEvent;
        } catch (IllegalArgumentException iae) {
            // event has not been recognized, it is most probably the end of the data section
            // we push the event back into next queue and let the structure parser handle it
            next.offerLast(event);
            return this::processStructureEvent;
        }
    }

}
