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
package org.orekit.files.ccsds.section;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Container for comments in various CCSDS messages.
 * <p>
 * CCSDS files accept comments only at the beginning of sections.
 * Once header/metadata/data content has started, comments in the
 * corresponding section are refused.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class CommentsContainer {

    /** Metadata comments. The list contains a string for each line of comment. */
    private final List<String> comments;

    /** Indicator for accepting comments. */
    private boolean acceptComments;

    /** Create a new meta-data.
     */
    public CommentsContainer() {
        comments       = new ArrayList<>();
        acceptComments = true;
    }

    /** Get the comments.
     * @return comments
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Set flag to refuse further comments.
     */
    protected void refuseFurtherComments() {
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
