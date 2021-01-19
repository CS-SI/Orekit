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
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.lexical.EventType;
import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.files.ccsds.utils.lexical.ParseEvent;
import org.orekit.files.ccsds.utils.lexical.ParsingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;


/**
 * Class for CCSDS Tracking Data Message parsers.
 *
 * <p> This base class is immutable, and hence thread safe. When parts must be
 * changed, such as reference date for Mission Elapsed Time or Mission Relative
 * Time time systems, or the gravitational coefficient or the IERS conventions,
 * the various {@code withXxx} methods must be called, which create a new
 * immutable instance with the new parameters. This is a combination of the <a
 * href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
 * pattern</a> and a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a>.
 *
 * <p> This class allow the handling of both "keyvalue" and "xml" TDM file formats.
 * Format can be inferred if file names ends respectively with ".txt" or ".xml".
 * Otherwise it must be explicitely set using {@link #withFileFormat(TDMFileFormat)}
 *
 * <p>ParseInfo subclass regroups common parsing functions; and specific handlers were added
 * for both file formats.
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
    private TDMMetadata currentMetadata;

    /** Current Observation Block being parsed. */
    private ObservationsBlock currentObservationsBlock;

    /** Current observation epoch. */
    private AbsoluteDate currentObservationEpoch;

    /** TDMFile object being filled. */
    private TDMFile tdmFile;

    /** Simple constructor.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as IERS conventions, EOP style or data context,
     * the various {@code withXxx} methods must be called,
     * which create a new immutable instance with the new parameters. This
     * is a combination of the
     * <a href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
     * pattern</a> and a
     * <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
     * interface</a>.
     * </p>
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
     * <p>
     * The TDM file format to use is not set here. It may be automatically inferred while parsing
     * if the name of the file to parse ends with ".txt" or ".xml".
     * Otherwise it must be initialized before parsing by calling {@link #withFileFormat(TDMFileFormat)}
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    @DefaultDataContext
    public TDMParser() {
        this(null, true, DataContext.getDefault(), AbsoluteDate.FUTURE_INFINITY);
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
        tdmFile                  = new TDMFile();
        currentMetadata          = null;
        currentObservationsBlock = null;
        reset(new NDMHeaderParsingState(getConventions(),  missionReferenceDate, getFormatVersionKey(),
                                        tdmFile.getHeader(), this::parseStructureEvent));
    }

    /** {@inheritDoc} */
    @Override
    public TDMFile build() {
        tdmFile.checkTimeSystems();
        return tdmFile;
    }

    /** Parse one structure event.
     * @param event event to process
     * @param next queue for pending events waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming events
     */
    private ParsingState parseStructureEvent(final ParseEvent event, final Deque<ParseEvent> next) {
        switch (event.getName()) {
            case "tdm":
                // ignored
                return this::parseStructureEvent;
            case "header":
                return new NDMHeaderParsingState(getConventions(),  missionReferenceDate, getFormatVersionKey(),
                                                 tdmFile.getHeader(), this::parseStructureEvent);
            case "body":
                // ignored
                return this::parseStructureEvent;
            case "segment":
                // ignored
                return this::parseStructureEvent;
            case "META" : case "metadata" :
                if (event.getType() == EventType.START) {
                    // next parse events will be handled as metadata
                    currentMetadata = new TDMMetadata(getConventions(), isSimpleEOP(), getDataContext());
                    return this::parseMetadataEvent;
                } else if (event.getType() == EventType.END) {
                    // nothing to do here, we expect a DATA_START next
                    return this::parseStructureEvent;
                }
                break;
            case "DATA" : case "data" :
                if (event.getType() == EventType.START) {
                    // next parse events will be handled as data
                    currentObservationsBlock = new ObservationsBlock();
                    return this::parseDataEvent;
                } else if (event.getType() == EventType.END) {
                    tdmFile.addSegment(new NDMSegment<>(currentMetadata, currentObservationsBlock));
                    // we expect a META_START next
                    return this::parseStructureEvent;
                }
                break;
            default :
                // nothing to do here, errors are handled below
        }
        throw event.generateException();
    }

    /** Parse one metadata event.
     * @param event event to process
     * @param next queue for pending events waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming events
     */
    private ParsingState parseMetadataEvent(final ParseEvent event, final Deque<ParseEvent> next) {
        try {
            final TDMMetadataKey key = TDMMetadataKey.valueOf(event.getName());
            key.parse(event, currentMetadata);
            return this::parseMetadataEvent;
        } catch (IllegalArgumentException iae) {
            // event has not been recognized, it is most probably the end of the metadata section
            // we push the event back into next queue and let the structure parser handle it
            next.offerLast(event);
            return this::parseStructureEvent;
        }
    }

    /** Parse one data event.
     * @param event event to process
     * @param next queue for pending events waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming events
     */
    private ParsingState parseDataEvent(final ParseEvent event, final Deque<ParseEvent> next) {
        try {
            final TDMDataKey key = TDMDataKey.valueOf(event.getName());
            key.parse(event, this);
            return this::parseDataEvent;
        } catch (IllegalArgumentException iae) {
            // event has not been recognized, it is most probably the end of the data section
            // we push the event back into next queue and let the structure parser handle it
            next.offerLast(event);
            return this::parseStructureEvent;
        }
    }

    /** Get the time system for current metadata.
     * @return time system for current metadata
     */
    CcsdsTimeScale getMetadataTimeSystem() {
        return currentMetadata.getTimeSystem();
    }

    /** Add a comment to the observation block.
     * @param comment comment to add
     */
    void addObservationsBlockComment(final String comment) {
        currentObservationsBlock.addComment(comment);
    }

    /** Add the epoch of current observation.
     * @param epoch current observation epoch
     */
    void addObservationEpoch(final AbsoluteDate epoch) {
        currentObservationEpoch = epoch;
    }

    /** Check if observation epoch has been set.
     * @return true if observation epoch has been set
     */
    boolean hasObservationEpoch() {
        return currentObservationEpoch != null;
    }

    /** Add the value of current observation.
     * @param keyword keyword of the observation
     * @param measurement measurement of the observation
     */
    void addObservationValue(final String keyword, final double measurement) {
        currentObservationsBlock.addObservation(keyword, currentObservationEpoch, measurement);
        currentObservationEpoch = null;
    }

}
