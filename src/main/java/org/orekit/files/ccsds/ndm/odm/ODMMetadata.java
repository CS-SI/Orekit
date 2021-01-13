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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NDMMetadata;
import org.orekit.utils.IERSConventions;

/** This class gathers the meta-data present in the Orbital Data Message (ODM).
 * @author sports
 * @since 6.1
 */
public class ODMMetadata extends NDMMetadata {

    /** Spacecraft name for which the orbit state is provided. */
    private String objectName;

    /** Metadata comments. The list contains a string for each line of comment. */
    private List<String> comments;

    /** Create a new meta-data.
     * @param conventions IERS conventions to use
     * @param dataContext data context to use
     */
    public ODMMetadata(final IERSConventions conventions, final DataContext dataContext) {
        super(conventions, dataContext);
        comments = new ArrayList<>();
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
        this.objectName = objectName;
    }

    /** Get the meta-data comments.
     * @return meta-data comments
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Add a meta-data comment.
     * @param comment comment to add
     */
    public void addComment(final String comment) {
        comments.add(comment);
    }

}



