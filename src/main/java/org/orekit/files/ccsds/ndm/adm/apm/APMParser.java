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
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.ADMMetadata;
import org.orekit.files.ccsds.ndm.adm.ADMMetadataKey;
import org.orekit.files.ccsds.ndm.adm.ADMParser;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderProcessingState;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XMLStructureProcessingState;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.state.ErrorState;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CCSDS APM (Attitude Parameter Message).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMParser extends ADMParser<APMFile, APMParser> {

    /** Root element for XML files. */
    private static final String ROOT = "apm";

    /** File header. */
    private Header header;

    /** File segments. */
    private List<Segment<ADMMetadata, APMData>> segments;

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

    /** Processor for global message structure. */
    private ProcessingState structureProcessor;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * (may be null if time system is absolute)
     */
    public APMParser(final IERSConventions conventions, final boolean simpleEOP,
                     final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate) {
        super(APMFile.FORMAT_VERSION_KEY, conventions, simpleEOP, dataContext, missionReferenceDate);
    }

    /** {@inheritDoc} */
    @Override
    public Header getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public void reset(final FileFormat fileFormat) {
        header                    = new Header();
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
            structureProcessor = new XMLStructureProcessingState(ROOT, this);
            reset(fileFormat, structureProcessor);
        } else {
            structureProcessor = new ErrorState(); // should never be called
            reset(fileFormat, new HeaderProcessingState(getDataContext(), this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareHeader() {
        setFallback(new HeaderProcessingState(getDataContext(), this));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inHeader() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeHeader() {
        header.checkMandatoryEntries();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareMetadata() {
        if (metadata != null) {
            return false;
        }
        metadata  = new ADMMetadata();
        context   = new ParsingContext(this::getConventions,
                                       this::isSimpleEOP,
                                       this::getDataContext,
                                       this::getMissionReferenceDate,
                                       metadata::getTimeSystem);
        setFallback(this::processMetadataToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inMetadata() {
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processQuaternionToken);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean finalizeMetadata() {
        metadata.checkMandatoryEntries();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean prepareData() {
        quaternionBlock = new APMQuaternion();
        setFallback(this::processQuaternionToken);
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
            final APMData data = new APMData(quaternionBlock, eulerBlock,
                                             spinStabilizedBlock, spacecraftParametersBlock);
            for (final APMManeuver maneuver : maneuvers) {
                data.addManeuver(maneuver);
            }
            data.checkMandatoryEntries();
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
    public APMFile build() {
        // APM KVN file lack a DATA_STOP keyword, hence we can't call finalizeData()
        // automatically before the end of the file
        finalizeData();
        final APMFile file = new APMFile(header, segments,
                                         getConventions(), isSimpleEOP(), getDataContext());
        return file;
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
                return ADMMetadataKey.valueOf(token.getName()).process(token, context, metadata);
            } catch (IllegalArgumentException iaeD) {
                // token has not been recognized
                return false;
            }
        }
    }

    /** Process one quaternion data token.
     * @param token token to process
     * @return true if token was processed, false otherwise
     */
    private boolean processQuaternionToken(final ParseToken token) {
        if (quaternionBlock == null) {
            // APM KVN file lack a META_STOP keyword, hence we can't call finalizeMetadata()
            // automatically before the first data token arrives
            finalizeMetadata();
            // APM KVN file lack a DATA_START keyword, hence we can't call prepareData()
            // automatically before the first data token arrives
            prepareData();
        }
        inData();
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processEulerToken);
        try {
            return token.getName() != null &&
                   APMQuaternionKey.valueOf(token.getName()).process(token, context, quaternionBlock);
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
            eulerBlock = new APMEuler();
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processSpinStabilizedToken);
        try {
            return token.getName() != null &&
                   APMEulerKey.valueOf(token.getName()).process(token, context, eulerBlock);
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
            spinStabilizedBlock = new APMSpinStabilized();
            if (moveCommentsIfEmpty(eulerBlock, spinStabilizedBlock)) {
                // get rid of the empty logical block
                eulerBlock = null;
            }
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processSpacecraftParametersToken);
        try {
            return token.getName() != null &&
                   APMSpinStabilizedKey.valueOf(token.getName()).process(token, context, spinStabilizedBlock);
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
            spacecraftParametersBlock = new APMSpacecraftParameters();
            if (moveCommentsIfEmpty(spinStabilizedBlock, spacecraftParametersBlock)) {
                // get rid of the empty logical block
                spinStabilizedBlock = null;
            }
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : this::processManeuverToken);
        try {
            return token.getName() != null &&
                   APMSpacecraftParametersKey.valueOf(token.getName()).process(token, context, spacecraftParametersBlock);
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
            currentManeuver = new APMManeuver();
            if (moveCommentsIfEmpty(spacecraftParametersBlock, currentManeuver)) {
                // get rid of the empty logical block
                spacecraftParametersBlock = null;
            }
        }
        setFallback(getFileFormat() == FileFormat.XML ? structureProcessor : new ErrorState());
        try {
            if (token.getName() != null &&
                APMManeuverKey.valueOf(token.getName()).process(token, context, currentManeuver)) {
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
        if (origin.acceptComments()) {
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
