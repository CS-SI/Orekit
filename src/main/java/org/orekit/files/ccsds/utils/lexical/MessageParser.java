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
package org.orekit.files.ccsds.utils.lexical;

import java.util.ArrayDeque;
import java.util.Deque;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.utils.IERSConventions;

/** Parser for CCSDS messages.
 * @param <T> type of the file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class MessageParser<T extends NDMFile<?, ?>, P extends MessageParser<T, ?>> {

    /** Key for format version. */
    private final String formatVersionKey;

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** Current parsing state. */
    private ParsingState state;

    /** Complete constructor.
     * @param formatVersionKey key for format version
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     */
    protected MessageParser(final String formatVersionKey,
                            final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext) {
        this.formatVersionKey = formatVersionKey;
        this.conventions      = conventions;
        this.simpleEOP        = simpleEOP;
        this.dataContext      = dataContext;
        this.state            = null;
    }

    /** Get the key for format version.
     * @return format version key
     */
    public String getFormatVersionKey() {
        return formatVersionKey;
    }

    /** Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public P withConventions(final IERSConventions newConventions) {
        return create(newConventions, simpleEOP, dataContext);
    }

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Set EOP interpolation method.
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return a new instance, with EOP interpolation method replaced
     * @see #isSimpleEOP()
     */
    public P withSimpleEOP(final boolean newSimpleEOP) {
        return create(conventions, newSimpleEOP, dataContext);
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

    /** Set the data context.
     * @param newDataContext used for frames, time scales, and celestial bodies.
     * @return a new instance with the data context replaced.
     */
    public P withDataContext(final DataContext newDataContext) {
        return create(getConventions(), isSimpleEOP(), newDataContext);
    }

    /** Build a new instance.
     * @param newConventions IERS conventions to use while parsing
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param newDataContext data context used for frames, time scales, and celestial bodies
     * @return a new instance with changed parameters
     */
    protected abstract P create(IERSConventions newConventions,
                                boolean newSimpleEOP,
                                DataContext newDataContext);

    /** Reset parser to initial state before parsing.
     */
    public abstract void reset();

    /** Reset parser to initial state before parsing.
     * @param initialState initial parsing state
     */
    protected void reset(final ParsingState initialState) {
        state = initialState;
    }

    /** Process a parse event.
     * @param parseEvent event to process
     */
    public void process(final ParseEvent parseEvent) {

        // prepare a FIFO queue so states can use it to delegate parsing to other states
        final Deque<ParseEvent> pending = new ArrayDeque<>();
        pending.offerLast(parseEvent);

        // process the event
        // we use a loop as some parse event may generate additional events,
        // for example for message formats that lack XXX_STOP keywords,O
        // or message formats that use raw data lines that must be split into fields
        // another case when the loop is needed is when one state cannot parse the event
        // and we have to switch to the next state
        while (!pending.isEmpty()) {
            final ParseEvent current = pending.pollLast();
            state = state.processEvent(current, pending);
        }

    }

    /** Build the file from parsed entries.
     * @return parsed file
     */
    public abstract T build();

}
