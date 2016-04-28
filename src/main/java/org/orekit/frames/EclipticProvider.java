/* Contributed in the public domain.
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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.utils.IERSConventions;

/**
 * An inertial frame aligned with the ecliptic.
 * <p>
 * The IAU defines the ecliptic as "the plane perpendicular to the mean heliocentric
 * orbital angular momentum vector of the Earth-Moon barycentre in the BCRS (IAU 2006
 * Resolution B1)." The +z axis is aligned with the angular momentum vector, and the +x
 * axis is aligned with +x axis of {@link FramesFactory#getMOD(IERSConventions) MOD}.
 * </p>
 *
 * <p>
 * This implementation agrees with the JPL 406 ephemerides to within 0.5 arc seconds.
 * </p>
 *
 * @since 7.0
 */
public class EclipticProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140516L;

    /** IERS conventions. */
    private final IERSConventions conventions;

    /** the obliquity of the ecliptic, in radians as a function of time. */
    private final transient TimeFunction<Double> obliquity;

    /**
     * Create a transform provider from MOD to an ecliptically aligned frame.
     * @param conventions IERS conventions
     * @throws OrekitException if the mean obliquity of the ecliptic function can not be
     *                         loaded.
     */
    public EclipticProvider(final IERSConventions conventions) throws OrekitException {
        this.conventions = conventions;
        this.obliquity   = conventions.getMeanObliquityFunction();
    }

    @Override
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {
        //mean obliquity of date
        final double epsA = obliquity.value(date);
        return new Transform(date, new Rotation(Vector3D.MINUS_I, epsA, RotationConvention.VECTOR_OPERATOR));
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(conventions);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140516L;

        /** IERS conventions. */
        private final IERSConventions conventions;

        /** Simple constructor.
         * @param conventions IERS conventions
         */
        DataTransferObject(final IERSConventions conventions) {
            this.conventions = conventions;
        }

        /** Replace the deserialized data transfer object with a {@link EclipticProvider}.
         * @return replacement {@link EclipticProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a transform
                return new EclipticProvider(conventions);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
