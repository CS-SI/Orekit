/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv.terms;

import org.orekit.files.iirv.terms.base.StringValuedIIRVTerm;

/**
 * 1-character source of the message (Default = "0").
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class MessageSourceTerm extends StringValuedIIRVTerm {

    /** Default value for the message source is "0". */
    public static final MessageSourceTerm DEFAULT = new MessageSourceTerm("0");

    /** The length of the message source term within the IIRV vector. */
    public static final int MESSAGE_SOURCE_TERM_LENGTH = 1;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String MESSAGE_SOURCE_TERM_PATTERN = "[0-9A-Za-z ]";

    /**
     * Constructor.
     * <p>
     * See {@link StringValuedIIRVTerm#StringValuedIIRVTerm(String, String, int)}
     *
     * @param value value of message source term
     */
    public MessageSourceTerm(final String value) {
        super(MESSAGE_SOURCE_TERM_PATTERN, value, MESSAGE_SOURCE_TERM_LENGTH);
    }
}
