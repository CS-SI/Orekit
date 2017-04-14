/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.Serializable;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
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

    /** Serializable UID. */
    private static final long serialVersionUID = 20130806L;

    /** Function computing CIP/CIO components. */
    private final transient TimeVectorFunction xysPxy2Function;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Simple constructor.
     * @param eopHistory EOP history
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     * @see Frame
     */
    CIRFProvider(final EOPHistory eopHistory)
        throws OrekitException {

        // load the nutation model
        xysPxy2Function = eopHistory.getConventions().getXYSpXY2Function();

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
    public CIRFProvider getNonInterpolatingProvider()
        throws OrekitException {
        return new CIRFProvider(eopHistory.getNonInterpolatingEOPHistory());
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

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
        final double cos    = FastMath.cos(sPe2);
        final double sin    = FastMath.sin(sPe2);
        final double xPr    = xCurrent + r;
        final double xPrCos = xPr * cos;
        final double xPrSin = xPr * sin;
        final double yCos   = yCurrent * cos;
        final double ySin   = yCurrent * sin;
        final Rotation bpn  = new Rotation(zP1 * (xPrCos + ySin), -r * (yCos + xPrSin),
                                           r * (xPrCos - ySin), zP1 * (yCos - xPrSin),
                                           true);

        return new Transform(date, bpn, Vector3D.ZERO);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date)
        throws OrekitException {

        final T[] xys  = xysPxy2Function.value(date);
        final T[] dxdy = eopHistory.getNonRotatinOriginNutationCorrection(date);

        // position of the Celestial Intermediate Pole (CIP)
        final T xCurrent = xys[0].add(dxdy[0]);
        final T yCurrent = xys[1].add(dxdy[1]);

        // position of the Celestial Intermediate Origin (CIO)
        final T sCurrent = xys[2].subtract(xCurrent.multiply(yCurrent).multiply(0.5));

        // set up the bias, precession and nutation rotation
        final T x2Py2  = xCurrent.multiply(xCurrent).add(yCurrent.multiply(yCurrent));
        final T zP1    = x2Py2.subtract(1).negate().sqrt().add(1);
        final T r      = x2Py2.sqrt();
        final T sPe2   = sCurrent.add(yCurrent.atan2(xCurrent)).multiply(0.5);
        final T cos    = sPe2.cos();
        final T sin    = sPe2.sin();
        final T xPr    = xCurrent.add(r);
        final T xPrCos = xPr.multiply(cos);
        final T xPrSin = xPr.multiply(sin);
        final T yCos   = yCurrent.multiply(cos);
        final T ySin   = yCurrent.multiply(sin);
        final FieldRotation<T> bpn  = new FieldRotation<>(zP1.multiply(xPrCos.add(ySin)),
                                                          r.multiply(yCos.add(xPrSin)).negate(),
                                                          r.multiply(xPrCos.subtract(ySin)),
                                                          zP1.multiply(yCos.subtract(xPrSin)),
                                                          true);

        return new FieldTransform<>(date, bpn, FieldVector3D.getZero(date.getField()));

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(eopHistory);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131209L;

        /** EOP history. */
        private final EOPHistory eopHistory;

        /** Simple constructor.
         * @param eopHistory EOP history
         */
        DataTransferObject(final EOPHistory eopHistory) {
            this.eopHistory = eopHistory;
        }

        /** Replace the deserialized data transfer object with a {@link CIRFProvider}.
         * @return replacement {@link CIRFProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new CIRFProvider(eopHistory);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
