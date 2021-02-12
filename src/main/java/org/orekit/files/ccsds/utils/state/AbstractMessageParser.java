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
package org.orekit.files.ccsds.utils.state;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitInternalError;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.utils.IERSConventions;

/** Parser for CCSDS messages.
 * @param <T> type of the file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractMessageParser<T extends NDMFile<?, ?>, P extends AbstractMessageParser<T, ?>>
    implements MessageParser<T> {

    /** Safety limit for loop over processing states. */
    private static final int MAX_LOOP = 100;

    /** Key for format version. */
    private final String formatVersionKey;

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** Format of the file ready to be parsed. */
    private FileFormat format;

    /** Fallback processing state. */
    private ProcessingState fallback;

    /** Current processing state. */
    private ProcessingState current;

    /** Complete constructor.
     * @param formatVersionKey key for format version
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     */
    protected AbstractMessageParser(final String formatVersionKey,
                                    final IERSConventions conventions,
                                    final boolean simpleEOP,
                                    final DataContext dataContext) {
        this.formatVersionKey = formatVersionKey;
        this.conventions      = conventions;
        this.simpleEOP        = simpleEOP;
        this.dataContext      = dataContext;
        this.current          = null;
    }

    /** {@inheritDoc} */
    @Override
    public String getFormatVersionKey() {
        return formatVersionKey;
    }

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /** Get the data context used for getting frames, time scales, and celestial bodies.
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /** Reset parser to initial state before parsing.
     * @param fileFormat format of the file ready to be parsed
     * @param initialState initial processing state
     */
    protected void reset(final FileFormat fileFormat, final ProcessingState initialState) {
        format  = fileFormat;
        current = initialState;
        setFallback(new ErrorState());
    }

    /** Get the file format.
     * @return file format
     */
    protected FileFormat getFileFormat() {
        return format;
    }

    /** Get file header to fill.
     * @return file header to fill
     */
    public abstract Header getHeader();

    /** Prepare header for parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean prepareHeader();

    /** Acknowledge header parsing has started.
     * @return true if parser was able to perform the action
     */
    public abstract boolean inHeader();

    /** Finalize header after parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean finalizeHeader();

    /** Prepare metadata for parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean prepareMetadata();

    /** Acknowledge metada parsing has started.
     * @return true if parser was able to perform the action
     */
    public abstract boolean inMetadata();

    /** Finalize metadata after parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean finalizeMetadata();

    /** Prepare data for parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean prepareData();

    /** Acknowledge data parsing has started.
     * @return true if parser was able to perform the action
     */
    public abstract boolean inData();

    /** Finalize data after parsing.
     * @return true if parser was able to perform the action
     */
    public abstract boolean finalizeData();

    /** Set fallback processing state.
     * @param fallback processing state
     */
    public void setFallback(final ProcessingState fallback) {
        this.fallback = fallback;
    }

    /** {@inheritDoc} */
    @Override
    public void process(final ParseToken token) {

        // loop over the various states until one really processes the token
        for (int i = 0; i < MAX_LOOP; ++i) {
            if (current.processToken(token)) {
                return;
            }
            current  = fallback;
            fallback = new ErrorState();
        }

        // this should never happen
        throw new OrekitInternalError(null);

    }

}
