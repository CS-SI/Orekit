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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.files.ccsds.section.Header;

/**
 * Header of a CCSDS Conjunction Data Message.
 * @author Melina Vanel
 * @since 11.2
 */
public class CdmHeader extends Header {

    /** ID that uniquely identifies a message from a given originator. */
    private String messageFor;

    /**
     * Constructor.
     */
    public CdmHeader() {
        super(1.0, 2.0);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
    }

    /**
     * Get the spacecraft name for which the CDM is provided stored in MESSAGE_FOR key.
     * @return messageFor the spacecraft name for which the CDM is provided.
     */
    public String getMessageFor() {
        return messageFor;
    }

    /**
     * Set the spacecraft name for which the CDM is provided stored in MESSAGE_FOR key.
     * @param spacecraftNames the spacecraft name for which the CDM is provided.
     */
    public void setMessageFor(final String spacecraftNames) {
        refuseFurtherComments();
        this.messageFor = spacecraftNames;
    }

}
