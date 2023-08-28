/* Copyright 2023 Thales Alenia Space
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
package org.orekit.files.rinex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.files.rinex.section.RinexBaseHeader;
import org.orekit.files.rinex.section.RinexComment;

/** Container for Rinex file.
 * @param <T> Type of the header
 * @author Luc Maisonobe
 * @since 12.0
 */
public class RinexFile<T extends RinexBaseHeader> {

    /** Header. */
    private final T header;

    /** Comments. */
    private final List<RinexComment> comments;

    /** Simple constructor.
     * @param header header
     */
    protected RinexFile(final T header) {
        this.header       = header;
        this.comments     = new ArrayList<>();
    }

    /** Get the header.
     * @return header
     */
    public T getHeader() {
        return header;
    }

    /** Get an unmodifiable view of the comments.
     * @return unmodifiable view of the comments
     */
    public List<RinexComment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Add a comment.
     * @param comment comment to add
     */
    public void addComment(final RinexComment comment) {
        comments.add(comment);
    }

}
