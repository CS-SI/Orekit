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
package org.orekit.files.sinex;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.models.earth.displacement.PsdCorrection;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

import java.util.HashMap;
import java.util.Map;

/** Parse information for Solution INdependent EXchange (SINEX) files.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 13.0
 */
public class SinexParseInfo extends ParseInfo<Sinex> {

    /** Station data. */
    private final Map<String, Station> stations;

    /** Earth Orientation Parameters data. */
    private final Map<AbsoluteDate, SinexEopEntry> eop;

    /** Station position X coordinate. */
    private double px;

    /** Station position Y coordinate. */
    private double py;

    /** Station position Z coordinate. */
    private double pz;

    /** Station velocity X coordinate. */
    private double vx;

    /** Station velocity Y coordinate. */
    private double vy;

    /** Station velocity Z coordinate. */
    private double vz;

    /** Correction axis. */
    private PsdCorrection.Axis axis;

    /** Correction time evolution. */
    private PsdCorrection.TimeEvolution evolution;

    /** Correction amplitude. */
    private double amplitude;

    /** Correction relaxation time. */
    private double relaxationTime;

    /** Simple constructor.
     * @param timeScales time scales
     */
    SinexParseInfo(final TimeScales timeScales) {
        super(timeScales);
        this.stations = new HashMap<>();
        this.eop = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    void newSource(final String name) {
        super.newSource(name);
        resetPosition();
        resetVelocity();
        resetPsdCorrection();
    }

    /** Add station.
     * @param station station to add
     */
    void addStation(final Station station) {
        stations.putIfAbsent(station.getSiteCode(), station);
    }

    /** Get station from current line.
     * @param index index of station in current line
     * @return station
     */
    Station getCurrentLineStation(final int index) {
        return stations.get(parseString(index, 4));
    }

    /** Get start date from current line.
     * @return start date
     */
    AbsoluteDate getCurrentLineStartDate() {
        return stringEpochToAbsoluteDate(parseString(16, 12), true);
    }

    /** Get end date from current line.
     * @return end date
     */
    AbsoluteDate getCurrentLineEndDate() {
        return stringEpochToAbsoluteDate(parseString(29, 12), false);
    }

    /** Set station position X coordinate.
     * @param x station position X coordinate
     * @param station station
     * @param epoch   coordinates epoch
     */
    void setPx(final double x, final Station station, final AbsoluteDate epoch) {
        this.px = x;
        finalizePositionIfComplete(station, epoch);
    }

    /** Set station position Y coordinate.
     * @param y station position Y coordinate
     * @param station station
     * @param epoch   coordinates epoch
     */
    void setPy(final double y, final Station station, final AbsoluteDate epoch) {
        this.py = y;
        finalizePositionIfComplete(station, epoch);
    }

    /** Set station position Z coordinate.
     * @param z station position Z coordinate
     * @param station station
     * @param epoch   coordinates epoch
     */
    void setPz(final double z, final Station station, final AbsoluteDate epoch) {
        this.pz = z;
        finalizePositionIfComplete(station, epoch);
    }

    /** Finalize station position if complete.
     * @param station station
     * @param epoch   coordinates epoch
     */
    private void finalizePositionIfComplete(final Station station, final AbsoluteDate epoch) {
        if (!Double.isNaN(px + py + pz)) {
            // all coordinates are available, position is complete
            station.setEpoch(epoch);
            station.setPosition(new Vector3D(px, py, pz));
            resetPosition();
        }
    }

    /** Reset position.
     */
    void resetPosition() {
        px = Double.NaN;
        py = Double.NaN;
        pz = Double.NaN;
    }

    /** Set station velocity X coordinate.
     * @param x station velocity X coordinate
     * @param station station
     */
    void setVx(final double x, final Station station) {
        this.vx = x;
        finalizeVelocityIfComplete(station);
    }

    /** Set station velocity Y coordinate.
     * @param y station velocity Y coordinate
     * @param station station
     */
    void setVy(final double y, final Station station) {
        this.vy = y;
        finalizeVelocityIfComplete(station);
    }

    /** Set station velocity Z coordinate.
     * @param z station velocity Z coordinate
     * @param station station
     */
    void setVz(final double z, final Station station) {
        this.vz = z;
        finalizeVelocityIfComplete(station);
    }

    /** Finalize station velocity if complete.
     * @param station station
     */
    private void finalizeVelocityIfComplete(final Station station) {
        if (!Double.isNaN(vx + vy + vz)) {
            // all coordinates are available, velocity is complete
            station.setVelocity(new Vector3D(vx, vy, vz));
            resetVelocity();
        }
    }

    /** Reset velocity.
     */
    void resetVelocity() {
        vx = Double.NaN;
        vy = Double.NaN;
        vz = Double.NaN;
    }

    /** Set correction axis.
     * @param axis correction axis
     */
    void setAxis(final PsdCorrection.Axis axis) {
        this.axis = axis;
    }

    /** Set correction time evolution.
     * @param evolution correction time evolution
     */
    void setEvolution(final PsdCorrection.TimeEvolution evolution) {
        this.evolution = evolution;
    }

    /** Set correction amplitude.
     * @param correctionAmplitude correction amplitude
     * @param station station
     * @param epoch   coordinates epoch
     */
    void setAmplitude(final double correctionAmplitude, final Station station, final AbsoluteDate epoch) {
        this.amplitude = correctionAmplitude;
        finalizePsdCorrectionIfComplete(station, epoch);
    }

    /** Set correction relaxation time.
     * @param correctionRelaxationTime correction relaxation time
     * @param station station
     * @param epoch   coordinates epoch
     */
    void setRelaxationTime(final double correctionRelaxationTime,
                           final Station station, final AbsoluteDate epoch) {
        this.relaxationTime = correctionRelaxationTime;
        finalizePsdCorrectionIfComplete(station, epoch);
    }

    /** Finalize a Post-Seismic Deformation correction model if complete.
     * @param station station
     * @param epoch   coordinates epoch
     */
    private void finalizePsdCorrectionIfComplete(final Station station, final AbsoluteDate epoch) {
        if (!Double.isNaN(amplitude + relaxationTime)) {
            // both amplitude and relaxation time are available, correction is complete
            final PsdCorrection correction = new PsdCorrection(axis, evolution, epoch, amplitude, relaxationTime);
            station.addPsdCorrectionValidAfter(correction, epoch);
            resetPsdCorrection();
        }
    }

    /** Reset Post-Seismic Deformation correction model.
     */
    private void resetPsdCorrection() {
        axis           = null;
        evolution      = null;
        amplitude      = Double.NaN;
        relaxationTime = Double.NaN;
    }

    /** Create EOP entry.
     * @param date EOP date
     * @return EOP entry at date, creating it if needed
     */
    SinexEopEntry createEOPEntry(final AbsoluteDate date) {
        return eop.computeIfAbsent(date, SinexEopEntry::new);
    }

    /** {@inheritDoc} */
    @Override
    protected Sinex build() {
        return new Sinex(getTimeScales(), getCreationDate(), getStartDate(), getEndDate(), stations, eop);
    }

}
