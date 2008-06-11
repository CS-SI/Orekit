/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.time;

import java.io.Serializable;

/** Holder for date chunks.
 * <p>This class is a simple holder with no processing methods.</p>
 * <p>Instance of this class are guaranteed to be immutable.</p>
 * @see AbsoluteDate
 * @see ChunkedDate
 * @see ChunkedTime
 * @author Luc Maisonobe
 * @version  $Revision$ $Date$
 */
public class ChunksPair implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 7865789190372064119L;

    /** Date chunk. */
    private final ChunkedDate date;

    /** Time chunk. */
    private final ChunkedTime time;

    /** Build a new instance from its components.
     * @param date date chunk
     * @param time time chunk
     */
    public ChunksPair(final ChunkedDate date, final ChunkedTime time) {
        this.date = date;
        this.time = time;
    }

    /** Get the date chunk.
     * @return date chunk
     */
    public ChunkedDate getDate() {
        return date;
    }

    /** Get the time chunk.
     * @return time chunk
     */
    public ChunkedTime getTime() {
        return time;
    }

    /** Return a string representation of this pair.
     * <p>The format used is ISO8601.</p>
     * @return string representation of this pair
     */
    public String toString() {
        return date.toString() + 'T' + time.toString();
    }

}

