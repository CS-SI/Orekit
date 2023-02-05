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
package org.orekit.files.ccsds.ndm;

import java.util.Collections;
import java.util.List;

/** CCSDS Navigation Data Message.
 * This class is a container for comments and {@link NdmConstituent constituents}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Ndm {

    /** File comments. */
    private final List<String> comments;

    /** Constituents of the message. */
    private final List<NdmConstituent<?, ?>> constituents;

    /** Simple constructor.
     * @param comments file comments
     * @param constituents constituents of the message
     */
    public Ndm(final List<String> comments, final List<NdmConstituent<?, ?>> constituents) {
        this.comments     = comments;
        this.constituents = constituents;
    }

    /** Get an unmodifiable view of the comments.
     * @return unmodifiable view of the comment
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Get an unmodifiable view of the constituents.
     * @return unmodifiable view of the constituents
     */
    public List<NdmConstituent<?, ?>> getConstituents() {
        return Collections.unmodifiableList(constituents);
    }

}
