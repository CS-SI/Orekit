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
package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.section.Header;

/**
 * Header for Orbit Data Message files.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OdmHeader extends Header {

    /** Key for message Id. */
    public static final String MESSAGE_ID = "MESSAGE_ID";

    /** ID that uniquely identifies a message from a given originator. */
    private String messageId;

    /**
     * Get the ID that uniquely identifies a message from a given originator.
     * @return ID that uniquely identifies a message from a given originator
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Set the ID that uniquely identifies a message from a given originator.
     * @param messageId ID that uniquely identifies a message from a given originator
     */
    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

}
