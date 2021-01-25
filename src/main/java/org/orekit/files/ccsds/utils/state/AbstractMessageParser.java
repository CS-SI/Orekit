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

import java.util.ArrayDeque;
import java.util.Deque;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.lexical.FileFormat;
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

    /** Current processing state. */
    private ProcessingState state;

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
        this.state            = null;
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
        format = fileFormat;
        state  = initialState;
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

    /** Start metadata parsing.
     * @return processing state for metadata
     */
    public abstract ProcessingState startMetadata();

    /** Stop metadata parsing.
     */
    public abstract void stopMetadata();

    /** Start date parsing.
     * @return processing state for data
     */
    public abstract ProcessingState startData();

    /** Stop data parsing.
     */
    public abstract void stopData();

    /** {@inheritDoc} */
    @Override
    public void process(final ParseToken parseToken) {

        // prepare a LIFO queue so states can use it to delegate parsing to other states
        final Deque<ParseToken> pending = new ArrayDeque<>();
        pending.offerLast(parseToken);

        // process the pending tokens
        // we use a loop as some parse tokens may not be processed by the state
        // that is active at loop start and will be pushed back in the pending queue,
        // or some states may generate additional tokens, for example for message
        // formats that use raw data lines that must be split into fields
        while (!pending.isEmpty()) {
            final ParseToken current = pending.pollLast();
            state = state.processToken(current, pending);
        }

    }

}
