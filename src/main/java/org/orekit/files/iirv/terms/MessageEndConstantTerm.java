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

import org.orekit.files.iirv.terms.base.ConstantValuedIIRVTerm;

/**
 * 5-character immutable end of the message: "ITERM".
 * <p>
 * Valid values: ITERM
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class MessageEndConstantTerm extends ConstantValuedIIRVTerm {

    /** End of the message is always "ITERM". */
    public static final String MESSAGE_END_TERM_STRING = "ITERM";

    /**
     * Constructor.
     * <p>
     * See {@link ConstantValuedIIRVTerm#ConstantValuedIIRVTerm(String)}
     */
    public MessageEndConstantTerm() {
        super(MESSAGE_END_TERM_STRING);
    }
}
