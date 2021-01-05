/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.orekit.bodies.CelestialBodies;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.CenterName;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for all CCSDS Attitude Data Message parsers.
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
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class ADMParser<T extends NDMFile<?, ?>> {

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Gravitational coefficient. */
    private final  double mu;

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     */
    protected ADMParser(final AbsoluteDate missionReferenceDate, final double mu,
                        final IERSConventions conventions, final boolean simpleEOP,
                        final DataContext dataContext) {
        this.missionReferenceDate = missionReferenceDate;
        this.mu                   = mu;
        this.conventions          = conventions;
        this.simpleEOP            = simpleEOP;
        this.dataContext          = dataContext;
    }

    /**
     * Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public abstract ADMParser<T> withMissionReferenceDate(AbsoluteDate newMissionReferenceDate);

    /**
     * Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /**
     * Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient date replaced
     * @see #getMu()
     */
    public abstract ADMParser<T> withMu(double newMu);

    /**
     * Get gravitational coefficient.
     * @return gravitational coefficient to use while parsing
     * @see #withMu(double)
     */
    public double getMu() {
        return mu;
    }

    /**
     * Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public abstract ADMParser<T> withConventions(IERSConventions newConventions);

    /**
     * Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /**
     * Set EOP interpolation method.
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return a new instance, with EOP interpolation method replaced
     * @see #isSimpleEOP()
     */
    public abstract ADMParser<T> withSimpleEOP(boolean newSimpleEOP);

    /**
     * Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /**
     * Get the data context used for getting frames, time scales, and celestial bodies.
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Set the data context.
     * @param newDataContext used for frames, time scales, and celestial bodies.
     * @return a new instance with the data context replaced.
     */
    public abstract ADMParser<T> withDataContext(DataContext newDataContext);

    /**
     * Parse a CCSDS Attitude Data Message.
     * @param fileName name of the file containing the message
     * @return parsed ADM file
     */
    public T parse(final String fileName) {
        try (InputStream stream = new FileInputStream(fileName)) {
            return parse(stream, fileName);
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
    }

    /**
     * Parse a CCSDS Attitude Data Message.
     * @param stream stream containing message
     * @return parsed ADM file
     */
    public T parse(final InputStream stream) {
        return parse(stream, "<unknown>");
    }

    /**
     * Parse a CCSDS Attitude Data Message.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     * @return parsed ADM file
     */
    public abstract T parse(InputStream stream, String fileName);

    /**
     * Parse an entry from the header.
     * @param keyValue key = value pair
     * @param admFile instance to update with parsed entry
     * @return true if the keyword was a header keyword and has been parsed
     */
    protected boolean parseHeaderEntry(final KeyValue keyValue, final T admFile) {
        switch (keyValue.getKeyword()) {

            case COMMENT:
                admFile.getHeader().addComment(keyValue.getValue());
                return true;

            case CREATION_DATE:
                admFile.getHeader().setCreationDate(new AbsoluteDate(keyValue.getValue(),
                                                                     dataContext.getTimeScales().getUTC()));
                return true;

            case ORIGINATOR:
                admFile.getHeader().setOriginator(keyValue.getValue());
                return true;

            default:
                return false;

        }

    }

    /**
     * Parse a meta-data key = value entry.
     * @param keyValue key = value pair
     * @param metaData instance to update with parsed entry
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue, final ADMMetadata metaData) {
        switch (keyValue.getKeyword()) {
            case COMMENT:
                metaData.addComment(keyValue.getValue());
                return true;

            case OBJECT_NAME:
                metaData.setObjectName(keyValue.getValue());
                return true;

            case OBJECT_ID: {
                metaData.setObjectID(keyValue.getValue());
                return true;
            }

            case CENTER_NAME:
                metaData.setCenterName(keyValue.getValue());
                final String canonicalValue;
                if (keyValue.getValue().equals("SOLAR SYSTEM BARYCENTER") || keyValue.getValue().equals("SSB")) {
                    canonicalValue = "SOLAR_SYSTEM_BARYCENTER";
                } else if (keyValue.getValue().equals("EARTH MOON BARYCENTER") || keyValue.getValue().equals("EARTH-MOON BARYCENTER") ||
                        keyValue.getValue().equals("EARTH BARYCENTER") || keyValue.getValue().equals("EMB")) {
                    canonicalValue = "EARTH_MOON";
                } else {
                    canonicalValue = keyValue.getValue();
                }
                for (final CenterName c : CenterName.values()) {
                    if (c.name().equals(canonicalValue)) {
                        metaData.setHasCreatableBody(true);
                        final CelestialBodies celestialBodies =
                                getDataContext().getCelestialBodies();
                        metaData.setCenterBody(c.getCelestialBody(celestialBodies));
                    }
                }
                return true;

            case TIME_SYSTEM:
                if (!CcsdsTimeScale.contains(keyValue.getValue())) {
                    throw new OrekitException(
                            OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED,
                            keyValue.getValue());
                }
                final CcsdsTimeScale timeSystem =
                        CcsdsTimeScale.valueOf(keyValue.getValue());
                metaData.setTimeSystem(timeSystem);
                return true;

            default:
                return false;
        }
    }

    /**
     * Parse a date.
     * @param date date to parse, as the value of a CCSDS key=value line
     * @param timeSystem time system to use
     * @return parsed date
     */
    protected AbsoluteDate parseDate(final String date, final CcsdsTimeScale timeSystem) {
        return timeSystem.parseDate(date, getConventions(), getMissionReferenceDate(),
                                    getDataContext().getTimeScales());
    }

}
