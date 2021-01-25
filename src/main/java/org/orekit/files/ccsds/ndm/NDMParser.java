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
package org.orekit.files.ccsds.ndm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for all CCSDS Navigation Data Message parsers.
 *
 * <p> This base class is immutable, and hence thread safe. When parts must be
 * changed, such as IERS conventions, EOP style or data context,
 * the various {@code withXxx} methods must be called, which create a new
 * immutable instance with the new parameters. This is a combination of the <a
 * href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
 * pattern</a> and a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a>.
 * @param <T> type of the file
 * @param <P> type of the parser
 *
 * @author Luc Maisonobe
 * @since 6.1
 */
public abstract class NDMParser<T extends NDMFile<?, ?>, P extends NDMParser<T, ?>> {

    /** Pattern for dash. */
    private static final Pattern DASH = Pattern.compile("-");

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     * @since 10.1
     */
    protected NDMParser(final IERSConventions conventions, final boolean simpleEOP,
                        final DataContext dataContext) {
        this.conventions  = conventions;
        this.simpleEOP    = simpleEOP;
        this.dataContext  = dataContext;
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

    /**
     * Get the data context used for getting frames, time scales, and celestial bodies.
     *
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Set the data context.
     *
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
     * @since 11.0
     */
    protected abstract P create(IERSConventions newConventions,
                                boolean newSimpleEOP,
                                DataContext newDataContext);

    /** Parse a CCSDS frame.
     * @param frameName name of the frame, as the value of a CCSDS key=value line
     * @return CCSDS frame corresponding to the name
     */
    protected CCSDSFrame parseCCSDSFrame(final String frameName) {
        return CCSDSFrame.valueOf(DASH.matcher(frameName).replaceAll(""));
    }

    /** Parse a date.
     * @param date date to parse, as the value of a CCSDS key=value line
     * @param timeSystem time system to use
     * @param lineNumber number of line being parsed
     * @param fileName name of the parsed file
     * @param line full parsed line
     * @return parsed date
     */
    protected AbsoluteDate parseDate(final String date, final CcsdsTimeScale timeSystem,
                                     final int lineNumber, final String fileName, final String line) {
        if (timeSystem == null) {
            throw new OrekitException(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_READ_YET,
                                      lineNumber, fileName, line);
        }
        try {
            return timeSystem.parseDate(date, conventions, null, dataContext.getTimeScales());
        } catch (OrekitIllegalArgumentException oiae) {
            throw new OrekitException(oiae, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, fileName, line);
        }
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param fileName name of the file containing the message
     * @return parsed orbit
     */
    public T parse(final String fileName) {
        try (InputStream stream = new FileInputStream(fileName)) {
            return parse(stream, fileName);
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param stream stream containing message
     * @return parsed orbit
     */
    public T parse(final InputStream stream) {
        return parse(stream, "<unknown>");
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     * @return parsed orbit
     */
    public abstract T parse(InputStream stream, String fileName);

}
