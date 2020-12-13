/* Copyright 2002-2020 CS GROUP
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

import org.orekit.files.ccsds.utils.DataType;
import org.orekit.files.ccsds.utils.Keyword;

/** Keywords allowed in ADM/ODM/TDM header.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum NDMHeaderKeyword implements Keyword {

    /** Header comment. */
    COMMENT(false, DataType.FREE_TEXT),

    /** Creation date. */
    CREATION_DATE(true, DataType.CALENDAR_DATE),

    /** Creating agency or operator. */
    ORIGINATOR(true, DataType.FREE_TEXT);

    /** Mandatory flag. */
    private final boolean mandatory;

    /** Data type. */
    private final DataType dataType;

    /** Simple constructor.
     * @param mandatory mandatory flag
     * @param dataType data type
     */
    NDMHeaderKeyword(final boolean mandatory, final DataType dataType) {
        this.mandatory = mandatory;
        this.dataType  = dataType;
    }

    /**  {@inheritDoc} */
    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    /**  {@inheritDoc} */
    @Override
    public DataType getdataType() {
        return dataType;
    }

}
