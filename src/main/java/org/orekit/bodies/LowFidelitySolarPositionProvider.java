/* Copyright 2022-2024 Romain Serra
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
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Class computing low-fidelity positions for the Sun. They should only be used in the decades around the year 2000.
 * <br> Reference: Montenbruck, Oliver, and Gill, Eberhard. Satellite orbits : models, methods, and
 * applications. Berlin New York: Springer, 2000.
 *
 * @author Romain Serra
 * @since 12.2
 */
public class LowFidelitySolarPositionProvider implements ExtendedPositionProvider {

    /** Sine anc cosine of approximate ecliptic angle used when converting from ecliptic to EME2000. */
    private static final SinCos SIN_COS_ECLIPTIC_ANGLE_EME2000 = FastMath.sinCos(FastMath.toRadians(23.43929111));

    /** Precomputed constant angle used in calculations. */
    private static final double INTERMEDIATE_ANGLE = FastMath.toRadians(282.9400);

    /** EME2000 frame. */
    private final Frame eme2000;

    /** Time scale for Julian date. */
    private final TimeScale timeScale;

    /**
     * Constructor.
     * @param dataContext data context
     */
    public LowFidelitySolarPositionProvider(final DataContext dataContext) {
        this.eme2000 = dataContext.getFrames().getEME2000();
        this.timeScale = dataContext.getTimeScales().getUTC();
    }

    /**
     * Constructor with default data context.
     */
    @DefaultDataContext
    public LowFidelitySolarPositionProvider() {
        this(DataContext.getDefault());
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
     * Computes the Sun's position vector in EME2000.
     * @param date date
     * @return solar position
     */
    private Vector3D getEME2000Position(final AbsoluteDate date) {
        final double tt = (date.getJD(timeScale) - 2451545.0) / 36525.0;
        final double M = FastMath.toRadians(357.5256 + 35999.049 * tt);
        final SinCos sinCosM = FastMath.sinCos(M);
        final SinCos sinCos2M = FastMath.sinCos(2 * M);
        final double r = (149.619 - 2.499 * sinCosM.cos() - 0.021 * sinCos2M.cos()) * 1.0e9;
        final double lambda = INTERMEDIATE_ANGLE + M + FastMath.toRadians(6892.0 * sinCosM.sin() + 72.0 * sinCos2M.sin()) / 3600.0;
        final SinCos sinCosLambda = FastMath.sinCos(lambda);
        return new Vector3D(r * sinCosLambda.cos(), r * sinCosLambda.sin() * SIN_COS_ECLIPTIC_ANGLE_EME2000.cos(),
            r * sinCosLambda.sin() * SIN_COS_ECLIPTIC_ANGLE_EME2000.sin());
    }

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
     * Computes the Sun's position vector in EME2000.
     * @param date date
     * @param <T> field type
     * @return solar position
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldEME2000Position(final FieldAbsoluteDate<T> date) {
        final T tt = date.getJD(timeScale).subtract(2451545.0).divide(36525.0);
        final T M = FastMath.toRadians(tt.multiply(35999.049).add(357.5256));
        final FieldSinCos<T> sinCosM = FastMath.sinCos(M);
        final FieldSinCos<T> sinCos2M = FastMath.sinCos(M.multiply(2));
        final T r = (sinCosM.cos().multiply(-2.499).subtract(sinCos2M.cos().multiply(0.021)).add(149.619)).multiply(1.0e9);
        final T lambda = M.add(INTERMEDIATE_ANGLE).add(FastMath.toRadians(
            sinCosM.sin().multiply(6892.0).add(sinCos2M.sin().multiply(72.0)).divide(3600.0)));
        final FieldSinCos<T> sinCosLambda = FastMath.sinCos(lambda);
        return new FieldVector3D<>(r.multiply(sinCosLambda.cos()),
            r.multiply(sinCosLambda.sin()).multiply(SIN_COS_ECLIPTIC_ANGLE_EME2000.cos()),
            r.multiply(sinCosLambda.sin()).multiply(SIN_COS_ECLIPTIC_ANGLE_EME2000.sin()));
    }
}
