/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.troposphere;

/** Container for tropospheric delay.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class TroposphericDelay {

    /** Hydrostatic zenith delay (m). */
    private final double zh;

    /** Wet zenith delay (m). */
    private final double zw;

    /** Hydrostatic slanted delay (m). */
    private final double sh;

    /** Wet slanted delay (m). */
    private final double sw;

    /** Simple constructor.
     * @param zh hydrostatic zenith delay (m)
     * @param zw wet zenith delay (m)
     * @param sh hydrostatic slanted delay (m)
     * @param sw wet slanted delay (m)
     */
    public TroposphericDelay(final double zh, final double zw, final double sh, final double sw) {
        this.zh = zh;
        this.zw = zw;
        this.sh = sh;
        this.sw = sw;
    }

    /** Get hydrostatic zenith delay (m).
     * @return hydrostatic zenith delay (m)
     */
    public double getZh() {
        return zh;
    }

    /** Get wet zenith delay (m).
     * @return wet zenith delay (m)
     */
    public double getZw() {
        return zw;
    }

    /** Get slanted delay (m).
     * @return slanted delay (m)
     */
    public double getSh() {
        return sh;
    }

    /** Get wet slanted delay (m).
     * @return wet slanted delay (m)
     */
    public double getSw() {
        return sw;
    }

    /** Get the total slanted delay (m).
     * @return total slanted delay (m)
     */
    public double getDelay() {
        return sh + sw;
    }

}
