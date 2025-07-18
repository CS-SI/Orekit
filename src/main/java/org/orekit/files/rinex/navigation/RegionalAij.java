/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.navigation;

/** Container for data contained in several ionosphere messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class RegionalAij extends IonosphereAij {

    /** IDF. */
    private double idf;

    /** Longitude min. */
    private double lonMin;

    /** Longitude max. */
    private double lonMax;

    /** MODIP min. */
    private double modipMin;

    /** MODIP max. */
    private double modipMax;

    /** Get IDF.
     * @return IDF
     */
    public double getIDF() {
        return idf;
    }

    /** Set IDF.
     * @param newIdf IDF
     */
    public void setIDF(final double newIdf) {
        this.idf = newIdf;
    }

    /** Get longitude min.
     * @return longitude min
     */
    public double getLonMin() {
        return lonMin;
    }

    /** Set longitude min.
     * @param lonMin longitude min
     */
    public void setLonMin(final double lonMin) {
        this.lonMin = lonMin;
    }

    /** Get longitude max.
     * @return longitude max
     */
    public double getLonMax() {
        return lonMax;
    }

    /** Set longitude max.
     * @param lonMax longitude max
     */
    public void setLonMax(final double lonMax) {
        this.lonMax = lonMax;
    }

    /** Get MODIP min.
     * @return MODIP min
     */
    public double getModipMin() {
        return modipMin;
    }

    /** Set MODIP min.
     * @param modipMin MODIP min
     */
    public void setModipMin(final double modipMin) {
        this.modipMin = modipMin;
    }

    /** Get MODIP max.
     * @return MODIP max
     */
    public double getModipMax() {
        return modipMax;
    }

    /** Set MODIP max.
     * @param modipMax MODIP max
     */
    public void setModipMax(final double modipMax) {
        this.modipMax = modipMax;
    }

}
