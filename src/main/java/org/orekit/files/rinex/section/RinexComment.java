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
package org.orekit.files.rinex.section;

/** Container for comment in RINEX file.
 * @author Luc Maisonobe
 * @since 12.0
 *
 */
public class RinexComment {

    /** Line number. */
    private final int lineNumber;

    /** Text. */
    private final String text;

    /** Simple constructor.
     * @param lineNumber line number
     * @param text text
     */
    public RinexComment(final int lineNumber, final String text) {
        this.lineNumber = lineNumber;
        this.text       = text;
    }

    /** Get the line number.
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /** Get the text.
     * @return text
     */
    public String getText() {
        return text;
    }

}
