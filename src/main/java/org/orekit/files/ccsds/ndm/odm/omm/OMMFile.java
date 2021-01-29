/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm.omm;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.OStateFile;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.utils.IERSConventions;

/**
 * This class gathers the informations present in the Orbital Mean-Elements Message (OMM),
 * and contains methods to generate a {@link CartesianOrbit}, a {@link KeplerianOrbit},
 * a {@link SpacecraftState} and, eventually, a {@link TLE}.
 * @author sports
 * @since 6.1
 */
public class OMMFile extends OStateFile<OMMMetadata, OMMData> {

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_OMM_VERS";

    /** Simple constructor.
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public OMMFile(final IERSConventions conventions, final DataContext dataContext) {
        super(conventions, dataContext);
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianOrbit generateKeplerianOrbit() {
        final OMMData data = getData();
        final double a;
        if (Double.isNaN(data.getA())) {
            a = FastMath.cbrt(getMu() / (data.getMeanMotion() * data.getMeanMotion()));
        } else {
            a = data.getA();
        }
        return new KeplerianOrbit(a, data.getE(), data.getI(), data.getPa(), data.getRaan(), data.getAnomaly(),
                                  PositionAngle.MEAN, getMetadata().getFrame(), data.getEpoch(), getMu());
    }

    /** {@inheritDoc} */
    @Override
    public CartesianOrbit generateCartesianOrbit() {
        return new CartesianOrbit(generateKeplerianOrbit());
    }

    /** Generate TLE from OMM file. Launch Year, Launch Day and Launch Piece are not present in the
     * OMM file, they have to be set manually by the user with the AdditionalData static class.
     * @return the tle
     */
    public TLE generateTLE() {
        final OCommonMetadata metadata = getMetadata();
        final OMMData data = getData();
        return new TLE(data.getNoradID(), data.getClassificationType(),
                       metadata.getLaunchYear(), metadata.getLaunchNumber(), metadata.getLaunchPiece(),
                       data.getEphemerisType(), Integer.parseInt(data.getElementSetNumber()), data.getEpoch(),
                       data.getMeanMotion(), data.getMeanMotionDot(), data.getMeanMotionDotDot(),
                       data.getE(), data.getI(), data.getPa(), data.getRaan(), data.getAnomaly(), data.getRevAtEpoch(),
                       data.getBStar(), getDataContext().getTimeScales().getUTC());
    }

}
