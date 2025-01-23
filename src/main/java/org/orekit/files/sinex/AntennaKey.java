/* Copyright 2002-2025 Thales Alenia Space
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

import java.util.Arrays;
import java.util.List;

/** Key for antenna.
 * @author Luc Maisonobe
 * @since 13.0
 */
public class AntennaKey {

    /** Constant matching other radome codes. */
    public static final String OTHER_RADOME_CODE = "NONE";

    /** Constant matching any serial numbers. */
    public static final String ANY_SERIAL_NUMBER = "-----";

    /** Antenna name. */
    private final String name;

    /** Radome code. */
    private final String radomeCode;

    /** Serial number. */
    private final String serialNumber;

    /** Simple constructor.
     * <p>
     * The Sinex file specification uses a single 20 characters field named "Antenna type"
     * and described as "Antenna name and model" (Antex specification is similar). In
     * practice this field contains a variable length name and last four characters are a
     * radome code, which may be set to {@link #OTHER_RADOME_CODE "NONE"} for a catch-all
     * entry. Here, we separate this field into its two components, so we can provide
     * {@link #matchingCandidates() fuzzy matching} by tweaking the radome code if needed.
     * </p>
     * @param name antenna name
     * @param radomeCode radome code
     * @param serialNumber serial number
     */
    public AntennaKey(final String name, final String radomeCode, final String serialNumber) {
        this.name         = name;
        this.radomeCode   = radomeCode;
        this.serialNumber = serialNumber;
    }

    /** Get candidates for fuzzy matching of this antenna key.
     * <p>
     * Some Sinex files use specific keys in the SITE/ANTENNA block and catch-all
     * keys in the SITE/GPS_PHASE_CENTER, SITE/GAL_PHASE_CENTER blocks. As
     * an example, file JAX0MGXFIN_20202440000_01D_000_SOL.SNX contains the
     * following entries related to antenna type ASH700936D_M:
     * </p>
     * <pre>
     * SITE/ANTENNA
     * AMU2  A ---- P 00:000:00000 00:000:00000 ASH700936D_M    SCIS 13569
     * ARTU  A ---- P 00:000:00000 00:000:00000 ASH700936D_M    DOME CR130
     * DRAG  A ---- P 00:000:00000 00:000:00000 ASH700936D_M    SNOW CR143
     * PALM  A ---- P 00:000:00000 00:000:00000 ASH700936D_M    SCIS CR141
     * SITE/GPS_PHASE_CENTER
     * ASH700936D_M    NONE -----  .0910  .0004 -.0003  .1204 -.0001 -.0001 igs14_%Y%m
     * ASH700936D_M    SCIS -----  .0879  .0005 -.0001  .1192  .0001 -.0001 igs14_%Y%m
     * ASH700936D_M    SNOW -----  .0909  .0003 -.0002  .1192  .0001  .0001 igs14_%Y%m
     * </pre>
     * <p>
     * Apart from the obvious formatting error of the last field in SITE/GPS_PHASE_CENTER,
     * it appears there are no phase center data for the antenna used at ARTU site, because
     * no radome code match "DOME". We consider here that a "close enough" entry would be
     * to use {@link #OTHER_RADOME_CODE "NONE"} as the radome code, and {@link #ANY_SERIAL_NUMBER "-----"}
     * as the serial number.
     * </p>
     * <p>
     * Another example is file ESA0OPSFIN_20241850000_01D_01D_SOL.SNX which contains the
     * following entries related to antenna type :
     * </p>
     * <pre>
     * SITE/ANTENNA
     * FAIR  A    1 P 24:184:86382 24:185:86382 ASH701945G_M    JPLA CR520    0
     * KOKB  A    1 P 24:184:86382 24:185:86382 ASH701945G_M    NONE CR620    0
     * SUTH  A    1 P 24:184:86382 24:185:86382 ASH701945G_M    NONE CR620    0
     * SITE/GPS_PHASE_CENTER
     * ASH701945G_M    NONE CR520 0.0895 0.0001 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR620 0.0895 0.0001 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR620 0.0895 0.0001 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * SITE/GAL_PHASE_CENTER
     * ASH701945G_M    NONE CR520 0.0895 0.0001 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR520 0.1162 -.0007 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR520 0.1162 -.0007 -.0001                      IGS20_2317
     * ASH701945G_M    NONE CR620 0.0895 0.0001 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR620 0.1162 -.0007 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR620 0.1162 -.0007 -.0001                      IGS20_2317
     * ASH701945G_M    NONE CR620 0.0895 0.0001 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR620 0.1162 -.0007 -.0001 0.1162 -.0007 -.0001 IGS20_2317
     * ASH701945G_M    NONE CR620 0.1162 -.0007 -.0001                      IGS20_2317
     * </pre>
     * <p>
     * Here, the phase centers for serial number CR620 appear twice (fortunately with
     * the same values). There are no phase center data for the antenna used at FAIR site,
     * because no radome code match "JPLA". We consider here that a "close enough" entry
     * would be to use {@link #OTHER_RADOME_CODE "NONE"}, and keep the provided serial number.
     * </p>
     * <p>
     * The logic we adopted is to use the following candidates:
     * </p>
     * <table border="1" style="background-color:#f5f5dc;">
     * <caption>Antenna key matching candidates</caption>
     * <tr style="background-color:#c9d5c9;"><th>order</th><th>name</th><th>radome code</th><th>serial number</th></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">first candidate</td>
     *     <td>{@link #getName()}</td><td>{@link #getRadomeCode()} </td><td>{@link #getSerialNumber()}</td></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">second candidate</td>
     *     <td>{@link #getName()}</td><td>{@link #getRadomeCode()} </td><td>{@link #ANY_SERIAL_NUMBER "-----"}</td></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">third candidate</td>
     *     <td>{@link #getName()}</td><td>{@link #OTHER_RADOME_CODE "NONE"} </td><td>{@link #getSerialNumber()}</td></tr>
     * <tr><td style="background-color:#c9d5c9; padding:5px">fourth candidate</td>
     *     <td>{@link #getName()}</td><td>{@link #OTHER_RADOME_CODE "NONE"} </td><td>{@link #ANY_SERIAL_NUMBER "-----"}</td></tr>
     * </table>
     */
    public List<AntennaKey> matchingCandidates() {
        return Arrays.asList(this,
                             new AntennaKey(getName(), radomeCode,        ANY_SERIAL_NUMBER),
                             new AntennaKey(getName(), OTHER_RADOME_CODE, serialNumber),
                             new AntennaKey(getName(), OTHER_RADOME_CODE, ANY_SERIAL_NUMBER));
    }

    /** Get the antenna name.
     * @return antenna name
     */
    public String getName() {
        return name;
    }

    /** Get the radome code.
     * @return radome code
     */
    public String getRadomeCode() {
        return radomeCode;
    }

    /** Get the serial number.
     * @return serial number
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof AntennaKey) {
            final AntennaKey other = (AntennaKey) object;
            return this.getName().equals(other.getName()) &&
                   this.getRadomeCode().equals(other.getRadomeCode()) &&
                   this.getSerialNumber().equals(other.getSerialNumber());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode() ^ getRadomeCode().hashCode() ^ getSerialNumber().hashCode();
    }

}
