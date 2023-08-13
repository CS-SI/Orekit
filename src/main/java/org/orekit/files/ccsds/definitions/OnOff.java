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
package org.orekit.files.ccsds.definitions;

/** On/Off status for various elements.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OnOff {

    /** On status. */
    ON(true),

    /** Off status. */
    OFF(false);

    /** Status. */
    private final boolean status;

    /** Simple constructor.
     * @param status if true, status is on
     */
    OnOff(final boolean status) {
        this.status = status;
    }

    /** Check if status is "on".
     * @return true if status is "on"
     */
    public boolean isOn() {
        return status;
    }

}
