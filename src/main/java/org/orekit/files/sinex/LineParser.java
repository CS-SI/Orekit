/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.files.sinex;

/** Parser class for one line.
 * @param <T> type of the SINEX files parse info
 * @author Luc Maisonobe
 * @since 13.0
 */
public interface LineParser<T extends ParseInfo<?>> {

    /** Parse a line if recognized.
     * @param parseInfo holder for transient data
     * @return true if line was recognized
     */
    boolean parseIfRecognized(T parseInfo);

    /** Get allowed next line parsers.
     * @param parseInfo holder for transient data
     * @return allowed parszers for next lines
     */
    Iterable<LineParser<T>> allowedNextParsers(T parseInfo);

}
