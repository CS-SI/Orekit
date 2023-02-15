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

package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.section.Metadata;

/** This class gathers the meta-data present in the Orbital Data Message (ODM).
 * @author sports
 * @since 6.1
 */
public class OdmMetadata extends Metadata {

    /** Spacecraft name for which the orbit state is provided. */
    private String objectName;

    /** Simple constructor.
     * @param defaultTimeSystem default time system (may be null)
     */
    protected OdmMetadata(final TimeSystem defaultTimeSystem) {
        super(defaultTimeSystem);
    }

    /** Get the spacecraft name for which the orbit state is provided.
     * @return the spacecraft name
     */
    public String getObjectName() {
        return objectName;
    }

    /** Set the spacecraft name for which the orbit state is provided.
     * @param objectName the spacecraft name to be set
     */
    public void setObjectName(final String objectName) {
        refuseFurtherComments();
        this.objectName = objectName;
    }

}



