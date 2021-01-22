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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NDMHeaderProcessingState;
import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.ndm.adm.ADMMetadata;
import org.orekit.files.ccsds.ndm.adm.ADMMetadataKey;
import org.orekit.files.ccsds.ndm.adm.ADMSegment;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.lexical.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.state.AbstractMessageParser;
import org.orekit.files.ccsds.utils.state.EndOfMessageState;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CCSDS APM (Attitude Parameter Message).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMParser extends AbstractMessageParser<APMFile, APMParser> {

    /** Key for format version. */
    private static final String FORMAT_VERSION_KEY = "CCSDS_APM_VERS";

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** APM file being read. */
    private APMFile file;

    /** APM metadata being read. */
    private ADMMetadata metadata;

    /** Parsing context valid for current metadata. */
    private ParsingContext context;

    /** APM quaternion logical block being read. */
    private APMQuaternion quaternionBlock;

    /** APM Euler angles logical block being read. */
    private APMEuler eulerBlock;

    /** APM spin-stabilized logical block being read. */
    private APMSpinStabilized spinStabilizedBlock;

    /** APM spacecraft parameters logical block being read. */
    private APMSpacecraftParameters spacecraftParametersBlock;

    /** Current maneuver. */
    private APMManeuver currentManeuver;

    /** All maneuvers. */
    private List<APMManeuver> maneuvers;

    /** Simple constructor.
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #withMissionReferenceDate(AbsoluteDate)}.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    @DefaultDataContext
    public APMParser() {
        this(DataContext.getDefault());
    }

    /** Constructor with data context.
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #withMissionReferenceDate(AbsoluteDate)}.
     * </p>
     *
     * @param dataContext used by the parser.
     *
     * @see #APMParser()
     * @see #withDataContext(DataContext)
     */
    public APMParser(final DataContext dataContext) {
        this(null, true, dataContext, AbsoluteDate.FUTURE_INFINITY);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     */
    private APMParser(final IERSConventions conventions, final boolean simpleEOP,
                      final DataContext dataContext,
                      final AbsoluteDate missionReferenceDate) {
        super(FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext);
        this.missionReferenceDate = missionReferenceDate;
    }

    /** {@inheritDoc} */
    @Override
    protected APMParser create(final IERSConventions newConventions,
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
    protected APMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext,
                               final AbsoluteDate newMissionReferenceDate) {
        return new APMParser(newConventions, newSimpleEOP, newDataContext, newMissionReferenceDate);
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public APMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return create(getConventions(), isSimpleEOP(), getDataContext(),  newMissionReferenceDate);
    }

    /**
     * Get reference date for Mission Elapsed Time and Mission Relative Time time systems.
     * @return the reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        file                      = new APMFile();
        metadata                  = null;
        context                   = null;
        quaternionBlock           = null;
        eulerBlock                = null;
        spinStabilizedBlock       = null;
        spacecraftParametersBlock = null;
        currentManeuver           = null;
        maneuvers                 = new ArrayList<>();
        final ProcessingState initialState =
                        getFileFormat() == FileFormat.XML ?
                                           this::processXMLStructureToken :
                                           new NDMHeaderProcessingState(getDataContext(),
                                                                        getFormatVersionKey(),
                                                                        file.getHeader(),
                                                                        this::processMetadataToken);
        reset(fileFormat, initialState);
    }

    /** {@inheritDoc} */
    @Override
    public APMFile build() {
        finalizeSegment();
        return file;
    }

    /** Finalize a metadata/data segment.
     */
    private void finalizeSegment() {
        if (metadata != null) {
            final APMData data = new APMData(quaternionBlock, eulerBlock,
                                             spinStabilizedBlock, spacecraftParametersBlock);
            for (final APMManeuver maneuver : maneuvers) {
                data.addManeuver(maneuver);
            }
            file.addSegment(new ADMSegment<>(metadata, data));
        }
        metadata                  = null;
        context                   = null;
        quaternionBlock           = null;
        eulerBlock                = null;
        spinStabilizedBlock       = null;
        spacecraftParametersBlock = null;
        currentManeuver           = null;
        maneuvers                 = new ArrayList<>();
    }

    /** Process one structure token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processXMLStructureToken(final ParseToken token, final Deque<ParseToken> next) {
        switch (token.getName()) {
            case "apm":
            case "body":
            case "segment":
                // ignored
                return this::processXMLStructureToken;
            case "header":
                if (token.getType() == TokenType.START) {
                    return new NDMHeaderProcessingState(getDataContext(), getFormatVersionKey(),
                                                        file.getHeader(), this::processXMLStructureToken);
                }
                break;
            case "metadata" :
                if (token.getType() == TokenType.START) {
                    // next parse tokens will be handled as metadata
                    return this::processMetadataToken;
                } else if (token.getType() == TokenType.END) {
                    // nothing to do here, we expect a data next
                    return this::processXMLStructureToken;
                }
                break;
            case "data" :
                if (token.getType() == TokenType.START) {
                    // next parse tokens will be handled as quaternion logical block
                    quaternionBlock = new APMQuaternion();
                    return this::processQuaternionToken;
                } else if (token.getType() == TokenType.END) {
                    finalizeSegment();
                    // there is only one segment in APM file
                    // any further data should be considered as an error
                    return new EndOfMessageState();
                }
                break;
            default :
                // nothing to do here, errors are handled below
        }
        throw token.generateException();
    }

    /** Process one metadata token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processMetadataToken(final ParseToken token, final Deque<ParseToken> next) {
        if (metadata == null) {
            metadata = new ADMMetadata();
            context  = new ParsingContext(this::getConventions,
                                          this::isSimpleEOP,
                                          this::getDataContext,
                                          this::getMissionReferenceDate,
                                          metadata::getTimeSystem);
        }
        try {
            final ADMMetadataKey key = ADMMetadataKey.valueOf(token.getName());
            key.process(token, context, metadata);
            return this::processMetadataToken;
        } catch (IllegalArgumentException iae) {
            // token has not been recognized, it is most probably the end of the metadata section
            // we push the token back into next queue and let the structure parser handle it
            next.offerLast(token);
            return getFileFormat() == FileFormat.XML ?
                                      this::processXMLStructureToken :
                                      this::processQuaternionToken;
        }
    }

    /** Process one quaternion data token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processQuaternionToken(final ParseToken token, final Deque<ParseToken> next) {
        try {
            final APMQuaternionKey key = APMQuaternionKey.valueOf(token.getName());
            if (key.process(token, context, quaternionBlock)) {
                // the token was processed properly
                return this::processQuaternionToken;
            }
        } catch (IllegalArgumentException iae) {
            // ignored, delegate to next state below
        }
        // the token was not processed, we need to pass it to next block processor
        next.offerLast(token);
        return getFileFormat() == FileFormat.XML ?
                                  this::processXMLStructureToken :
                                  this::processEulerToken;
    }

    /** Process one Euler angles data token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processEulerToken(final ParseToken token, final Deque<ParseToken> next) {
        try {
            final APMEulerKey key = APMEulerKey.valueOf(token.getName());
            if (key.process(token, context, eulerBlock)) {
                // the token was processed properly
                return this::processEulerToken;
            }
        } catch (IllegalArgumentException iae) {
            // ignored, delegate to next state below
        }
        // the token was not processed, we need to pass it to next block processor
        next.offerLast(token);
        return getFileFormat() == FileFormat.XML ?
                                  this::processXMLStructureToken :
                                  this::processSpinStabilizedToken;
    }

    /** Process one spin-stabilized data token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processSpinStabilizedToken(final ParseToken token, final Deque<ParseToken> next) {
        try {
            final APMSpinStabilizedKey key = APMSpinStabilizedKey.valueOf(token.getName());
            if (key.process(token, context, spinStabilizedBlock)) {
                // the token was processed properly
                return this::processSpinStabilizedToken;
            }
        } catch (IllegalArgumentException iae) {
            // ignored, delegate to next state below
        }
        // the token was not processed, we need to pass it to next block processor
        next.offerLast(token);
        return getFileFormat() == FileFormat.XML ?
                                  this::processXMLStructureToken :
                                  this::processSpacecraftParametersToken;
    }

    /** Process one spacecraft parameters data token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processSpacecraftParametersToken(final ParseToken token, final Deque<ParseToken> next) {
        try {
            final APMSpacecraftParametersKey key = APMSpacecraftParametersKey.valueOf(token.getName());
            if (key.process(token, context, spacecraftParametersBlock)) {
                // the token was processed properly
                return this::processSpacecraftParametersToken;
            }
        } catch (IllegalArgumentException iae) {
            // ignored, delegate to next state below
        }
        // the token was not processed, we need to pass it to next block processor
        next.offerLast(token);
        return getFileFormat() == FileFormat.XML ?
                                  this::processXMLStructureToken :
                                  this::processManeuverToken;
    }

    /** Process one maneuver data token.
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for parsing upcoming tokens
     */
    private ProcessingState processManeuverToken(final ParseToken token, final Deque<ParseToken> next) {
        try {
            final APMManeuverKey key = APMManeuverKey.valueOf(token.getName());
            if (key.process(token, context, currentManeuver)) {
                // the token was processed properly
                return this::processManeuverToken;
            }
        } catch (IllegalArgumentException iae) {
            // ignored, delegate to next state below
        }
        // the token was not processed, we need to pass it to next block processor
        next.offerLast(token);
        return getFileFormat() == FileFormat.XML ?
                                  this::processXMLStructureToken :
                                  new EndOfMessageState();
    }

}
