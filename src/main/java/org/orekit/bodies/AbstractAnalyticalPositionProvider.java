/* Copyright 2022-2026 Romain Serra
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

package org.orekit.bodies;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.data.DataContext;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Abstract lass computing low-fidelity positions for bodies in EME2000.
 *
 * @author Romain Serra
 * @since 14.0
 */
abstract class AbstractAnalyticalPositionProvider implements ExtendedPositionProvider {

    /** Sine anc cosine of approximate ecliptic angle used when converting from ecliptic to EME2000. */
    protected static final SinCos SIN_COS_ECLIPTIC_ANGLE_EME2000 = FastMath.sinCos(FastMath.toRadians(23.43929111));

    /** EME2000 frame. */
    private final Frame eme2000;

    /** Time scale for Julian date. */
    private final TimeScale timeScale;

    /**
     * Constructor.
     * @param dataContext data context
     */
    protected AbstractAnalyticalPositionProvider(final DataContext dataContext) {
        this.eme2000 = dataContext.getFrames().getEME2000();
        this.timeScale = dataContext.getTimeScales().getUTC();
    }

    /**
     * Getter for time scale.
     * @return time scale
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        final Vector3D eme2000Position = getEME2000Position(date);
        if (frame.equals(eme2000)) {
            return eme2000Position;
        } else {
            final StaticTransform transform = eme2000.getStaticTransformTo(frame, date);
            return transform.transformPosition(eme2000Position);
        }
    }

    /**
     * Computes the body's position vector in EME2000.
     * @param date date
     * @return position
     */
    protected abstract Vector3D getEME2000Position(AbsoluteDate date);

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        final FieldVector3D<T> eme2000Position = getFieldEME2000Position(date);
        if (frame.equals(eme2000)) {
            return eme2000Position;
        } else {
            final FieldStaticTransform<T> transform = eme2000.getStaticTransformTo(frame, date);
            return transform.transformPosition(eme2000Position);
        }
    }

    /**
     * Computes the body's position vector in EME2000.
     * @param date date
     * @param <T> field type
     * @return position
     */
    protected abstract  <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldEME2000Position(FieldAbsoluteDate<T> date);
}
