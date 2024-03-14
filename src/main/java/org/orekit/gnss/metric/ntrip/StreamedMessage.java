/* Copyright 2002-2024 CS GROUP
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
package org.orekit.gnss.metric.ntrip;

/** Container for streamed messages meta-data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class StreamedMessage {

    /** Message id. */
    private final String id;

    /** Message rate (seconds). */
    private final int rate;

    /** Simple constructor.
     * @param id message id
     * @param rate refresh rate in seconds (-1 if unknown)
     */
    StreamedMessage(final String id, final int rate) {
        this.id   = id;
        this.rate = rate;
    }

    /** Get message id.
     * @return message id
     */
    public String getId() {
        return id;
    }

    /** Get refresh rate.
     * @return refresh rate in seconds, -1 if unknown
     */
    public int getRate() {
        return rate;
    }

}
