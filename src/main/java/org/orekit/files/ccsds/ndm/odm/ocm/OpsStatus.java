/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm.ocm;

/** Operational status used in CCSDS {@link Ocm Orbit Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OpsStatus {

    /** An operational spacecraft that has the capability to maneuver. */
    OPERATIONAL_MANEUVERABLE("Operational and maneuverable"),

    /** An operational spacecraft that has no capability to maneuver, either due to equipment malfunction or by design. */
    OPERATIONAL_NONMANEUVERABLE("Operational and non-maneuverable"),

    /** Spacecraft that can no longer perform any operational mission role(s) but which may retain the ability to either transmit/receive, maneuver, or reorient. */
    NONOPERATIONAL("Non-operational"),

    /** A spacecraft for which operations are substantively degraded. */
    DEGRADED_OPERATIONS("Degrated operations"),

    /** A spacecraft that is in backup, storage, or standby mode. */
    BACKUP_STORAGE_STANDBY("Backup, storage, or standby"),

    /** An operational spacecraft in a mission phase that has been continued past the planned end-of-mission schedule. */
    EXTENDED_MISSION("Extended mission"),

    /** A space object that is below 150 km or will reenter within several orbital revolutions. */
    REENTRY_MODE("Reentry mode"),

    /** A space object for which the orbit has now decayed. */
    DECAYED("Decayed"),

    /** A space object for which the status is unknown. */
    UNKNOWN("Unknown"),

    /** Fully non-functional and nonmaneuverable space objects, including space debris fragments and passivated or derelict spacecraft. */
    DEAD("Dead"),

    /** DEPRECATED: Operational object. */
    OPERATIONAL("Operational"),

    /** DEPRECATED: partially operational object. */
    PARTIALLY_OPERATIONAL("Partially operational"),

    /** DEPRECATED: Backup object. */
    BACKUP("Backup"),

    /** DEPRECATED: Object in stand-by. */
    STANBY("Stand-by");

    /** Description. */
    private final String description;

    /** Simple constructor.
     * @param description description
     */
    OpsStatus(final String description) {
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return description;
    }

}
