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

import java.io.InputStream;

import org.orekit.bodies.CelestialBodies;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMParser;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.CenterName;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for all CCSDS Attitude Data Message parsers.
 *
 * <p> This base class is immutable, and hence thread safe. When parts must be
 * changed, such as the IERS conventions or data context, the various {@code withXxx}
 * methods must be called, which create a new immutable instance with the new parameters.
 * This is a combination of the <a href="https://en.wikipedia.org/wiki/Builder_pattern">builder
 * design pattern</a> and a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a>.
 * @param <T> type of the parsed file
 * @param <P> type of the parser
 *
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class ADMParser<T extends ADMFile<?>, P extends ADMParser<T, ?>> extends NDMParser<T, P> {

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     */
    protected ADMParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                        final AbsoluteDate missionReferenceDate) {
        super(conventions, simpleEOP, dataContext);
        this.missionReferenceDate = missionReferenceDate;
    }

    /** {@inheritDoc} */
    @Override
    protected P create(final IERSConventions newConventions,
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
    protected abstract P create(IERSConventions newConventions,
                                boolean newSimpleEOP,
                                DataContext newDataContext,
                                AbsoluteDate newMissionReferenceDate);

    /**
     * Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public P withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return create(getConventions(), isSimpleEOP(), getDataContext(), newMissionReferenceDate);
    }

    /**
     * Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
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
                                                                     getDataContext().getTimeScales().getUTC()));
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

}
