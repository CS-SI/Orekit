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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Container for comments in various CCSDS messages.
 * <p>
 * CCSDS files accept comments only at the beginning of sections.
 * Once header/metadata/data content has started, comments in the
 * corresponding section are refused.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class CommentsContainer implements Section {

    /** Comments, as one line per string. */
    private final List<String> comments;

    /** Indicator for accepting comments. */
    private boolean acceptComments;

    /** Create a new meta-data.
     */
    public CommentsContainer() {
        comments       = new ArrayList<>();
        acceptComments = true;
    }

    /** Complain if a field is negative.
     * @param field field to check
     * @param key key associated with the field
     */
    public void checkNotNegative(final int field, final String key) {
        if (field < 0) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, key);
        }
    }

    /** Complain if a field is NaN.
     * @param field field to check
     * @param key key associated with the field
     */
    public void checkNotNaN(final double field, final String key) {
        if (Double.isNaN(field)) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, key);
        }
    }

    /** Complain if a field is null.
     * @param field field to check
     * @param key key associated with the field
     */
    public void checkNotNull(final Object field, final String key) {
        if (field == null) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, key);
        }
    }

    /** Complain if a key is not allowed.
     * @param version format version
     * @param field field to check
     * @param key key associated with the field
     * @param minVersion version at which key started to be allowed
     * @param maxVersion version at which key started to be forbidden
     */
    public void checkAllowed(final double version, final Object field, final String key,
                             final double minVersion, final double maxVersion) {
        if (field != null && (version < minVersion || version >= maxVersion)) {
            throw new OrekitException(OrekitMessages.CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION,
                                      key, version);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        // nothing to do here
    }

    /** Get the comments.
     * @return comments
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Check if container is still accepting comments.
     * <p>
     * A container that still accept comments does not contain any other data.
     * </p>
     * @return true if container is still accepting comments
     */
    public boolean acceptComments() {
        return acceptComments;
    }

    /** Set flag to refuse further comments.
     */
    public void refuseFurtherComments() {
        acceptComments = false;
    }

    /**
     * Add comment.
     * <p>
     * Comments are accepted only at start. Once
     * other content is stored in the same section, comments are refused.
     * </p>
     * @param comment comment line
     * @return true if comment was accepted
     */
    public boolean addComment(final String comment) {
        if (acceptComments) {
            comments.add(comment);
            return true;
        } else {
            return false;
        }
    }
}
