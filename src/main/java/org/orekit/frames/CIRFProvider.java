/* Copyright 2002-2025 CS GROUP
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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeVectorFunction;

/** Celestial Intermediate Reference Frame.
 * <p>This provider includes precession effects according to either the IAU 2006 precession
 * (also known as Nicole Capitaines's P03 precession theory) and IAU 2000A_R06 nutation
 * for IERS 2010 conventions or the IAU 2000A precession-nutation model for IERS 2003
 * conventions. These models replaced the older IAU-76 precession (Lieske) and IAU-80
 * theory of nutation (Wahr) which were used in the classical equinox-based paradigm.
 * It <strong>must</strong> be used with the Earth Rotation Angle (REA) defined by
 * Capitaine's model and <strong>not</strong> IAU-82 sidereal
 * time which is consistent with the older models only.</p>
 * <p>Its parent frame is the GCRF frame.
 */
class CIRFProvider implements EOPBasedTransformProvider {

    /** Function computing CIP/CIO components. */
    private final transient TimeVectorFunction xysPxy2Function;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Simple constructor.
     * @param eopHistory EOP history
     * @see Frame
     */
    CIRFProvider(final EOPHistory eopHistory) {

        // load the nutation model
        xysPxy2Function = eopHistory.getConventions()
                .getXYSpXY2Function(eopHistory.getTimeScales());

        // store correction to the model
        this.eopHistory = eopHistory;

    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public CIRFProvider getNonInterpolatingProvider() {
        return new CIRFProvider(eopHistory.getEOPHistoryWithoutCachedTidalCorrection());
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {

        final double[] xys  = xysPxy2Function.value(date);
        final double[] dxdy = eopHistory.getNonRotatinOriginNutationCorrection(date);

        // position of the Celestial Intermediate Pole (CIP)
        final double xCurrent = xys[0] + dxdy[0];
        final double yCurrent = xys[1] + dxdy[1];

        // position of the Celestial Intermediate Origin (CIO)
        final double sCurrent = xys[2] - xCurrent * yCurrent / 2;

        // set up the bias, precession and nutation rotation
        final double x2Py2  = xCurrent * xCurrent + yCurrent * yCurrent;
        final double zP1    = 1 + FastMath.sqrt(1 - x2Py2);
        final double r      = FastMath.sqrt(x2Py2);
        final double sPe2   = 0.5 * (sCurrent + FastMath.atan2(yCurrent, xCurrent));
        final SinCos sc     = FastMath.sinCos(sPe2);
        final double xPr    = xCurrent + r;
        final double xPrCos = xPr * sc.cos();
        final double xPrSin = xPr * sc.sin();
        final double yCos   = yCurrent * sc.cos();
        final double ySin   = yCurrent * sc.sin();
        final Rotation bpn  = new Rotation(zP1 * (xPrCos + ySin), -r * (yCos + xPrSin),
                                           r * (xPrCos - ySin), zP1 * (yCos - xPrSin),
                                           true);

        return new Transform(date, bpn, Vector3D.ZERO);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        final T[] xys  = xysPxy2Function.value(date);
        final T[] dxdy = eopHistory.getNonRotatinOriginNutationCorrection(date);

        // position of the Celestial Intermediate Pole (CIP)
        final T xCurrent = xys[0].add(dxdy[0]);
        final T yCurrent = xys[1].add(dxdy[1]);

        // position of the Celestial Intermediate Origin (CIO)
        final T sCurrent = xys[2].subtract(xCurrent.multiply(yCurrent).multiply(0.5));

        // set up the bias, precession and nutation rotation
        final T x2Py2           = xCurrent.multiply(xCurrent).add(yCurrent.multiply(yCurrent));
        final T zP1             = x2Py2.subtract(1).negate().sqrt().add(1);
        final T r               = x2Py2.sqrt();
        final T sPe2            = sCurrent.add(yCurrent.atan2(xCurrent)).multiply(0.5);
        final FieldSinCos<T> sc = FastMath.sinCos(sPe2);
        final T xPr             = xCurrent.add(r);
        final T xPrCos          = xPr.multiply(sc.cos());
        final T xPrSin          = xPr.multiply(sc.sin());
        final T yCos            = yCurrent.multiply(sc.cos());
        final T ySin            = yCurrent.multiply(sc.sin());
        final FieldRotation<T> bpn  = new FieldRotation<>(zP1.multiply(xPrCos.add(ySin)),
                                                          r.multiply(yCos.add(xPrSin)).negate(),
                                                          r.multiply(xPrCos.subtract(ySin)),
                                                          zP1.multiply(yCos.subtract(xPrSin)),
                                                          true);

        return new FieldTransform<>(date, bpn, FieldVector3D.getZero(date.getField()));

    }

}
