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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Class computing low-fidelity positions for the Moon. They should only be used in the decades around the year 2000.
 * <br> Reference: Montenbruck, Oliver, and Gill, Eberhard. Satellite orbits : models, methods, and
 * applications. Berlin New York: Springer, 2000.
 *
 * @author Romain Serra
 * @since 14.0
 */
public class AnalyticalLunarPositionProvider extends AbstractAnalyticalPositionProvider {

    /**
     * Constructor.
     * @param dataContext data context
     */
    public AnalyticalLunarPositionProvider(final DataContext dataContext) {
        super(dataContext);
    }

    /**
     * Constructor with default data context.
     */
    @DefaultDataContext
    public AnalyticalLunarPositionProvider() {
        this(DataContext.getDefault());
    }

    /**
     * Computes the Moon's position vector in EME2000.
     * @param date date
     * @return lunar position
     */
    protected Vector3D getEME2000Position(final AbsoluteDate date) {
        // in ecliptic plane
        final double tt = (date.getJD(getTimeScale()) - 2451545.0) / 36525.0;
        final double L0 = FastMath.toRadians(218.31617 + 481267.88088 * tt - 1.3972 * tt);
        final double l = FastMath.toRadians(134.96292 + 477198.86753 * tt);
        final double lp = FastMath.toRadians(357.52543 + 35999.04944 * tt);
        final double F = FastMath.toRadians(93.27283 + 483202.01873 * tt);
        final double D = FastMath.toRadians(297.85207 + 445267.11135 * tt);
        final double rMoon = 385000.0 - 20905.0 * FastMath.cos(l) - 3699.0 * FastMath.cos(2.0 * D - l) - 2956.0 * FastMath.cos(2.0 * D) - 570.0 * FastMath.cos(2.0 * l) + 246.0 * FastMath.cos(2.0 * l - 2.0 * D) - 205.0 * FastMath.cos(lp - 2.0 * D) - 171.0 * FastMath.cos(l + 2.0 * D) - 152.0 * FastMath.cos(l + lp - 2.0 * D);
        final double lambdaMoon = L0 + (22640.0 * FastMath.sin(l) + 769.0 * FastMath.sin(2.0 * l) - 4586.0 * FastMath.sin(l - 2.0 * D) + 2370.0 * FastMath.sin(2.0 * D) - 668.0 * FastMath.sin(lp) - 412.0 * FastMath.sin(2.0 * F) - 212.0 * FastMath.sin(2.0 * l - 2.0 * D) - 206.0 * FastMath.sin(l + lp - 2.0 * D) + 192.0 * FastMath.sin(l + 2.0 * D) - 165.0 * FastMath.sin(lp - 2.0 * D) + 148.0 * FastMath.sin(l - lp) - 125.0 * FastMath.sin(D) - 110.0 * FastMath.sin(l + lp) - 55.0 * FastMath.sin(2.0 * F - 2.0 * D)) * FastMath.PI / (180.0 * 3600.0);
        final double beta = (18520.0 * FastMath.sin(F + lambdaMoon - L0 + 412.0 * FastMath.sin(2.0 * F) * FastMath.PI / (180.0 * 3600.0) + 541.0 * FastMath.sin(lp) * FastMath.PI / (180.0 * 3600.0)) - 526.0 * FastMath.sin(F - 2.0 * D) + 44.0 * FastMath.sin(l + F - 2.0 * D) - 31.0 * FastMath.sin(-l + F - 2.0 * D) - 25.0 * FastMath.sin(-2.0 * l + F) - 23.0 * FastMath.sin(lp + F - 2.0 * D) + 21.0 * FastMath.sin(-l + F) + 11.0 * FastMath.sin(-lp + F - 2.0 * D)) * FastMath.PI / (180.0 * 3600.0);
        // frame transform
        final Vector3D eclipticPosition = new Vector3D(lambdaMoon, beta).scalarMultiply(rMoon);
        final double xKm = eclipticPosition.getX();
        final double yKm = SIN_COS_ECLIPTIC_ANGLE_EME2000.cos() * eclipticPosition.getY() - SIN_COS_ECLIPTIC_ANGLE_EME2000.sin() * eclipticPosition.getZ();
        final double zKm = SIN_COS_ECLIPTIC_ANGLE_EME2000.sin() * eclipticPosition.getY() + SIN_COS_ECLIPTIC_ANGLE_EME2000.cos() * eclipticPosition.getZ();
        return new Vector3D(xKm, yKm, zKm).scalarMultiply(1e3);
    }

    /**
     * Computes the Moon's position vector in EME2000.
     * @param date date
     * @param <T> field type
     * @return lunar position
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldEME2000Position(final FieldAbsoluteDate<T> date) {
        // ecliptic plane
        final T tt = date.getJD(getTimeScale()).subtract(2451545.0).divide(36525.0);
        final T L0 = FastMath.toRadians(tt.multiply(481267.88088).subtract(tt.multiply(1.3972)).add(218.31617));
        final T l = FastMath.toRadians(tt.multiply(477198.86753).add(134.96292));
        final T lp = FastMath.toRadians(tt.multiply(35999.04944).add(357.52543));
        final T F = FastMath.toRadians(tt.multiply(483202.01873).add(93.27283));
        final T D = FastMath.toRadians(tt.multiply(445267.11135).add(297.85207));
        final double factor = FastMath.PI / (180.0 * 3600.0);
        final T rMoon = tt.newInstance(385000.0).subtract(FastMath.cos(l).multiply(20905.0)).subtract(FastMath.cos(D.multiply(2).subtract(l)).multiply(3699.0)).subtract(FastMath.cos(D.multiply(2)).multiply(2956.0)).subtract(FastMath.cos(l.multiply(2)).multiply(570.0)).add(FastMath.cos(l.multiply(2).subtract(D.multiply(2))).multiply(246.0)).subtract(FastMath.cos(lp.subtract(D.multiply(2))).multiply(205.0)).subtract(FastMath.cos(l.add(D.multiply(2))).multiply(171.0)).subtract(FastMath.cos(l.add(lp).subtract(D.multiply(2))).multiply(152.0));
        final T lambdaMoon = L0.add((tt.newInstance(22640.0).multiply(FastMath.sin(l)).add(FastMath.sin(l.multiply(2)).multiply(769.0)).subtract(FastMath.sin(l.subtract(D.multiply(2))).multiply(4586.0)).add(FastMath.sin(D.multiply(2)).multiply(2370.0)).subtract(FastMath.sin(lp).multiply(668.0)).subtract(FastMath.sin(F.multiply(2)).multiply(412.0)).subtract(FastMath.sin(l.multiply(2).subtract(D.multiply(2))).multiply(212.0)).subtract(FastMath.sin(l.add(lp).subtract(D.multiply(2))).multiply(206.0)).add(FastMath.sin(l.add(D.multiply(2))).multiply(192.0)).subtract(FastMath.sin(lp.subtract(D.multiply(2))).multiply(165.0)).add(FastMath.sin(l.subtract(lp)).multiply(148.0)).subtract(FastMath.sin(D).multiply(125.0)).subtract(FastMath.sin(l.add(lp)).multiply(110.0)).subtract(FastMath.sin(F.multiply(2).subtract(D.multiply(2))).multiply(55.0))).multiply(factor));
        final T beta = (tt.newInstance(18520.0).multiply(FastMath.sin(F.add(lambdaMoon).subtract(L0).add(FastMath.sin(F.multiply(2)).multiply(412.0 * factor)).add(FastMath.sin(lp).multiply(541.0 * factor)))).subtract(FastMath.sin(F.subtract(D.multiply(2))).multiply(526.0))).add(FastMath.sin(l.add(F).subtract(D.multiply(2))).multiply(44.0)).subtract(FastMath.sin(l.negate().add(F).subtract(D.multiply(2))).multiply(31.0)).subtract(FastMath.sin(l.multiply(-2).add(F)).multiply(25.0)).subtract(FastMath.sin(lp.add(F).subtract(D.multiply(2))).multiply(23.0)).add(FastMath.sin(F.subtract(l)).multiply(21.0)).add(FastMath.sin(lp.negate().add(F).subtract(D.multiply(2))).multiply(11.0)).multiply(factor);
        // frame transform
        final FieldVector3D<T> eclipticPosition = new FieldVector3D<>(lambdaMoon, beta).scalarMultiply(rMoon);
        final T xKm = eclipticPosition.getX();
        final T yKm = eclipticPosition.getY().multiply(SIN_COS_ECLIPTIC_ANGLE_EME2000.cos()).subtract(eclipticPosition.getZ().multiply(SIN_COS_ECLIPTIC_ANGLE_EME2000.sin()));
        final T zKm = eclipticPosition.getY().multiply(SIN_COS_ECLIPTIC_ANGLE_EME2000.sin()).add(eclipticPosition.getZ().multiply(SIN_COS_ECLIPTIC_ANGLE_EME2000.cos()));
        return new FieldVector3D<>(xKm, yKm, zKm).scalarMultiply(1e3);
    }
}
