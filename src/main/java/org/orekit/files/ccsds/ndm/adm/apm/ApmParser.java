/* Copyright 2002-2022 CS GROUP
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
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AdmParser;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureProcessingState;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ErrorState;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CCSDS APM (Attitude Parameter Message).
 * @author Bryan Cazabonne * <p>
 * Note than starting with Orekit 11.0, CCSDS message parsers are
 * mutable objects that gather the data being parsed, until the
 * message is complete and the {@link #parseMessage(org.orekit.data.DataSource)
 * parseMessage} method has returned. This implies that parsers
 * should <em>not</em> be used in a multi-thread context. The recommended
 * way to use parsers is to either dedicate one parser for each message
 * and drop it afterwards, or to use a single-thread loop.
 * </p>

 * @since 10.2
 */
public class ApmParser extends AdmParser<Apm, ApmParser> {

    /** File header. */
    private Header header;

    /** File segments. */
    private List<Segment<AdmMetadata, ApmData>> segments;

    /** APM metadata being read. */
    private AdmMetadata metadata;

    /** Context binding valid for current metadata. */
    private ContextBinding context;

    /** APM general comments block being read. */
    private CommentsContainer commentsBlock;

    /** APM quaternion logical block being read. */
    private ApmQuaternion quaternionBlock;

    /** APM Euler angles logical block being read. */
    private Euler eulerBlock;

    /** APM spin-stabilized logical block being read. */
    private SpinStabilized spinStabilizedBlock;

    /** APM spacecraft parameters logical block being read. */
    private SpacecraftParameters spacecraftParametersBlock;

    /** Current maneuver. */
    private Maneuver currentManeuver;

    /** All maneuvers. */
    private List<Maneuver> maneuvers;

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.ParserBuilder#buildApmParser()
     * parserBuilder.buildApmParser()}.
     * </p>
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     * @param parsedUnitsBehavior behavior to adopt for handling parsed units
     */
    public ApmParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate, final ParsedUnitsBehavior parsedUnitsBehavior) {
        super(Apm.ROOT, Apm.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext,
              missionReferenceDate, parsedUnitsBehavior);
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header                    = new Header(2.0);
        segments                  = new ArrayList<>();
        metadata                  = null;
        context                   = null;
        quaternionBlock           = null;
        eulerBlock                = null;
        spinStabilizedBlock       = null;
        spacecraftParametersBlock = null;
        currentManeuver           = null;
        maneuvers                 = new ArrayList<>();
        if (fileFormat == FileFormat.XML) {
            structureProcessor = new XmlStructureProcessingState(Apm.ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new ErrorState(); // should never be called
            reset(fileFormat, new HeaderProcessingState(this));
        }
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
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
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
        metadata  = new AdmMetadata();
        context   = new ContextBinding(this::getConventions, this::isSimpleEOP,
                                       this::getDataContext, this::getParsedUnitsBehavior,
                                       this::getMissionReferenceDate,
                                       metadata::getTimeSystem, () -> 0.0, () -> 1.0);
        anticipateNext(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        anticipateNext(getFileFormat() == FileFormat.XML ? structureProcessor : this::processGeneralCommentToken);
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
        commentsBlock = new CommentsContainer();
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processGeneralCommentToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inData() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeData() {
        if (metadata != null) {
            final ApmData data = new ApmData(commentsBlock, quaternionBlock, eulerBlock,
                                             spinStabilizedBlock, spacecraftParametersBlock);
            for (final Maneuver maneuver : maneuvers) {
                data.addManeuver(maneuver);
            }
            data.validate(header.getFormatVersion());
            segments.add(new Segment<>(metadata, data));
        }
        metadata                  = null;
        context                   = null;
        quaternionBlock           = null;
        eulerBlock                = null;
        spinStabilizedBlock       = null;
        spacecraftParametersBlock = null;
        currentManeuver           = null;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Apm build() {
        // APM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        final Apm file = new Apm(header, segments, getConventions(), getDataContext());
        return file;
    }

    /** Add a general comment.
     * @param comment comment to add
     * @return always return true
     */
    boolean addGeneralComment(final String comment) {
        return commentsBlock.addComment(comment);
    }

    /** Manage quaternion section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageQuaternionSection(final boolean starting) {
        anticipateNext(starting ? this::processQuaternionToken : structureProcessor);
        return true;
    }

    /** Manage Euler elements / three axis stabilized section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageEulerElementsThreeSection(final boolean starting) {
        anticipateNext(starting ? this::processEulerToken : structureProcessor);
        return true;
    }

    /** Manage Euler elements /spin stabilized section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageEulerElementsSpinSection(final boolean starting) {
        anticipateNext(starting ? this::processSpinStabilizedToken : structureProcessor);
        return true;
    }

    /** Manage spacecraft parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageSpacecraftParametersSection(final boolean starting) {
        anticipateNext(starting ? this::processSpacecraftParametersToken : structureProcessor);
        return true;
    }

    /** Manage maneuver parameters section.
     * @param starting if true, parser is entering the section
     * otherwise it is leaving the section
     * @return always return true
     */
    boolean manageManeuverParametersSection(final boolean starting) {
        anticipateNext(starting ? this::processManeuverToken : structureProcessor);
        return true;
    }

    /** Process one metadata token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processMetadataToken(final ParseToken token) {
        if (metadata == null) {
            // APM KVN file lack a META_START keyword, hence we can't call prepareMetadata()
            // automatically before the first metadata token arrives
            prepareMetadata();
        }
        inMetadata();
        try {
            return token.getName() != null &&
                   MetadataKey.valueOf(token.getName()).process(token, context, metadata);
        } catch (IllegalArgumentException iaeM) {
            try {
                return AdmMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                // token has not been recognized
                return false;
            }
        }
    }

    /** Process one XML data substructure token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processXmlSubStructureToken(final ParseToken token) {
        try {
            return token.getName() != null &&
                   XmlSubStructureKey.valueOf(token.getName()).process(token, this);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one comment token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processGeneralCommentToken(final ParseToken token) {
        if (commentsBlock == null) {
            // APM KVN file lack a META_STOP keyword, hence we can't call finalizeMetadata()
            // automatically before the first data token arrives
            finalizeMetadata();
            // APM KVN file lack a DATA_START keyword, hence we can't call prepareData()
            // automatically before the first data token arrives
            prepareData();
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processQuaternionToken);
        if ("COMMENT".equals(token.getName())) {
            if (token.getType() == TokenType.ENTRY) {
                commentsBlock.addComment(token.getContentAsNormalizedString());
            }
            return true;
        } else {
            return false;
        }
    }

    /** Process one quaternion data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processQuaternionToken(final ParseToken token) {
        commentsBlock.refuseFurtherComments();
        if (quaternionBlock == null) {
            quaternionBlock = new ApmQuaternion();
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processEulerToken);
        try {
            return token.getName() != null &&
                   ApmQuaternionKey.valueOf(token.getName()).process(token, context, quaternionBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one Euler angles data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processEulerToken(final ParseToken token) {
        if (eulerBlock == null) {
            eulerBlock = new Euler();
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processSpinStabilizedToken);
        try {
            return token.getName() != null &&
                   EulerKey.valueOf(token.getName()).process(token, context, eulerBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one spin-stabilized data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processSpinStabilizedToken(final ParseToken token) {
        if (spinStabilizedBlock == null) {
            spinStabilizedBlock = new SpinStabilized();
            if (moveCommentsIfEmpty(eulerBlock, spinStabilizedBlock)) {
                // get rid of the empty logical block
                eulerBlock = null;
            }
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processSpacecraftParametersToken);
        try {
            return token.getName() != null &&
                   SpinStabilizedKey.valueOf(token.getName()).process(token, context, spinStabilizedBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one spacecraft parameters data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processSpacecraftParametersToken(final ParseToken token) {
        if (spacecraftParametersBlock == null) {
            spacecraftParametersBlock = new SpacecraftParameters();
            if (moveCommentsIfEmpty(spinStabilizedBlock, spacecraftParametersBlock)) {
                // get rid of the empty logical block
                spinStabilizedBlock = null;
            }
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : this::processManeuverToken);
        try {
            return token.getName() != null &&
                   SpacecraftParametersKey.valueOf(token.getName()).process(token, context, spacecraftParametersBlock);
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }
    }

    /** Process one maneuver data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processManeuverToken(final ParseToken token) {
        if (currentManeuver == null) {
            currentManeuver = new Maneuver();
            if (moveCommentsIfEmpty(spacecraftParametersBlock, currentManeuver)) {
                // get rid of the empty logical block
                spacecraftParametersBlock = null;
            }
        }
        anticipateNext(getFileFormat() == FileFormat.XML ? this::processXmlSubStructureToken : new ErrorState());
        try {
            if (token.getName() != null &&
                ManeuverKey.valueOf(token.getName()).process(token, context, currentManeuver)) {
                // the token was processed properly
                if (currentManeuver.completed()) {
                    // current maneuver is completed
                    maneuvers.add(currentManeuver);
                    currentManeuver = null;
                }
                return true;
            }
        } catch (IllegalArgumentException iae) {
            // ignored, delegate to next state below
        }
        // the token was not processed
        return false;
    }

    /** Move comments from one empty logical block to another logical block.
     * @param origin origin block
     * @param destination destination block
     * @return true if origin block was empty
     */
    private boolean moveCommentsIfEmpty(final CommentsContainer origin, final CommentsContainer destination) {
        if (origin != null && origin.acceptComments()) {
            // origin block is empty, move the existing comments
            for (final String comment : origin.getComments()) {
                destination.addComment(comment);
            }
            return true;
        } else {
            return false;
        }
    }

}
