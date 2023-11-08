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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;


/** International Terrestrial Reference Frame.
 * <p> Handles pole motion effects and depends on {@link TIRFProvider}, its
 * parent frame.</p>
 * @author Luc Maisonobe
 */
class ITRFProvider implements EOPBasedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130922L;

    /** S' rate in radians per julian century.
     * Approximately -47 microarcsecond per julian century (Lambert and Bizouard, 2002)
     */
    private static final double S_PRIME_RATE = -47e-6 * Constants.ARC_SECONDS_TO_RADIANS;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Simple constructor.
     * @param eopHistory EOP history
     */
    ITRFProvider(final EOPHistory eopHistory) {
        this.eopHistory = eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public ITRFProvider getNonInterpolatingProvider() {
        return new ITRFProvider(eopHistory.getEOPHistoryWithoutCachedTidalCorrection());
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {

        // offset from J2000 epoch in Julian centuries
        final double tts = date.durationFrom(eopHistory.getTimeScales().getJ2000Epoch());
        final double ttc =  tts / Constants.JULIAN_CENTURY;

        // pole correction parameters
        final PoleCorrection eop = eopHistory.getPoleCorrection(date);

        // pole motion in terrestrial frame
        final Rotation wRot = new Rotation(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM,
                                           eop.getYp(), eop.getXp(), -S_PRIME_RATE * ttc);

        // combined effects
        final Rotation combined = wRot.revert();

        // set up the transform from parent TIRF
        return new Transform(date, combined, Vector3D.ZERO);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // offset from J2000 epoch in Julian centuries
        final T tts = date.durationFrom(eopHistory.getTimeScales().getJ2000Epoch());
        final T ttc =  tts.divide(Constants.JULIAN_CENTURY);

        // pole correction parameters
        final FieldPoleCorrection<T> eop = eopHistory.getPoleCorrection(date);

        // pole motion in terrestrial frame
        final FieldRotation<T> wRot = new FieldRotation<>(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM,
                                                          eop.getYp(),
                                                          eop.getXp(),
                                                          ttc.multiply(-S_PRIME_RATE));

        // combined effects
        final FieldRotation<T> combined = wRot.revert();

        // set up the transform from parent TIRF
        return new FieldTransform<>(date, combined, FieldVector3D.getZero(date.getField()));

    }

}
