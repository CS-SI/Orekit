/* Copyright 2002-2015 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.utils.IERSConventions;

/** Mean Equator, Mean Equinox Frame.
 * <p>This frame handles precession effects according to to selected IERS conventions.</p>
 * <p>Its parent frame is the GCRF frame.<p>
 * <p>It is sometimes called Mean of Date (MoD) frame.<p>
 * @author Pascal Parraud
 */
class MODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130920L;

    /** Conventions. */
    private final IERSConventions conventions;

    /** Function computing the precession angles. */
    private final transient TimeFunction<double[]> precessionFunction;

    /** Constant rotation betwee ecliptic and equatoror poles at J2000.0. */
    private final Rotation r4;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @exception OrekitException if IERS conventions tables cannot be read
     */
    MODProvider(final IERSConventions conventions) throws OrekitException {
        this.conventions        = conventions;
        this.precessionFunction = conventions.getPrecessionFunction();
        final TimeFunction<Double> epsilonAFunction = conventions.getMeanObliquityFunction();
        final AbsoluteDate date0 = conventions.getNutationReferenceEpoch();
        final double epsilon0 = epsilonAFunction.value(date0);
        r4 = new Rotation(Vector3D.PLUS_I, -epsilon0);
    }

    /** Get the transfrom from parent frame.
     * <p>The update considers the precession effects.</p>
     * @param date new value of the date
     * @return transform at the specified date
     */
    public Transform getTransform(final AbsoluteDate date) {

        // compute the precession angles phiA, omegaA, chiA
        final double[] angles = precessionFunction.value(date);

        // elementary rotations for precession
        final Rotation r1 = new Rotation(Vector3D.PLUS_K, -angles[2]);
        final Rotation r2 = new Rotation(Vector3D.PLUS_I,  angles[1]);
        final Rotation r3 = new Rotation(Vector3D.PLUS_K,  angles[0]);

        // complete precession
        final Rotation precession = r1.applyTo(r2.applyTo(r3.applyTo(r4)));

        // set up the transform from parent GCRF
        return new Transform(date, precession);

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
        private static final long serialVersionUID = 20131209L;

        /** Conventions. */
        private final IERSConventions conventions;

        /** Simple constructor.
         * @param conventions IERSConventions conventions
         */
        DataTransferObject(final IERSConventions conventions) {
            this.conventions = conventions;
        }

        /** Replace the deserialized data transfer object with a {@link MODProvider}.
         * @return replacement {@link MODProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new MODProvider(conventions);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
