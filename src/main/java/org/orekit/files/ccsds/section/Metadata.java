/* Copyright 2002-2023 CS GROUP
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

package org.orekit.files.ccsds.section;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.TimeSystem;

/** This class gathers the meta-data present in the Navigation Data Message (ADM, ODM and TDM).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Metadata extends CommentsContainer {

    /** Pattern for international designator. */
    private static final Pattern INTERNATIONAL_DESIGNATOR = Pattern.compile("(\\p{Digit}{4})-(\\p{Digit}{3})(\\p{Upper}{1,3})");

    /** Time System: used for metadata, orbit state and covariance data. */
    private TimeSystem timeSystem;

    /** Simple constructor.
     * @param defaultTimeSystem default time system (may be null)
     */
    protected Metadata(final TimeSystem defaultTimeSystem) {
        this.timeSystem = defaultTimeSystem;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(timeSystem, MetadataKey.TIME_SYSTEM.name());
    }

    /** Get the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @return the time system
     */
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Set the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @param timeSystem the time system to be set
     */
    public void setTimeSystem(final TimeSystem timeSystem) {
        refuseFurtherComments();
        this.timeSystem = timeSystem;
    }

    /** Get the launch year.
     * @param objectID object identifier
     * @return launch year
     */
    protected int getLaunchYear(final String objectID) {
        final Matcher matcher = INTERNATIONAL_DESIGNATOR.matcher(objectID);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new OrekitException(OrekitMessages.NOT_VALID_INTERNATIONAL_DESIGNATOR, objectID);
    }

    /** Get the launch number.
     * @param objectID object identifier
     * @return launch number
     */
    protected int getLaunchNumber(final String objectID) {
        final Matcher matcher = INTERNATIONAL_DESIGNATOR.matcher(objectID);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        }
        throw new OrekitException(OrekitMessages.NOT_VALID_INTERNATIONAL_DESIGNATOR, objectID);
    }

    /** Get the piece of launch.
     * @param objectID object identifier
     * @return piece of launch
     */
    protected String getLaunchPiece(final String objectID) {
        final Matcher matcher = INTERNATIONAL_DESIGNATOR.matcher(objectID);
        if (matcher.matches()) {
            return matcher.group(3);
        }
        throw new OrekitException(OrekitMessages.NOT_VALID_INTERNATIONAL_DESIGNATOR, objectID);
    }

}
