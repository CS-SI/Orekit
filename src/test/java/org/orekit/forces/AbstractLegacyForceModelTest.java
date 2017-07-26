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
package org.orekit.forces;


import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;


public abstract class AbstractLegacyForceModelTest extends AbstractForceModelTest {

    protected abstract FieldVector3D<DerivativeStructure> accelerationDerivatives(ForceModel forceModel,
                                                                                  final AbsoluteDate date, final  Frame frame,
                                                                                  final FieldVector3D<DerivativeStructure> position,
                                                                                  final FieldVector3D<DerivativeStructure> velocity,
                                                                                  final FieldRotation<DerivativeStructure> rotation,
                                                                                  final DerivativeStructure mass)
        throws OrekitException;

    protected void checkStateJacobianVs80Implementation(final SpacecraftState state, final ForceModel forceModel,
                                                        final AttitudeProvider attitudeProvider,
                                                        final double checkTolerance, final boolean print)
        throws OrekitException {
        FieldSpacecraftState<DerivativeStructure> fState = toDS(state, attitudeProvider);
        FieldVector3D<DerivativeStructure> dsNew = forceModel.acceleration(fState,
                                                                           forceModel.getParameters(fState.getDate().getField()));
        FieldVector3D<DerivativeStructure> dsOld = accelerationDerivatives(forceModel, fState.getDate().toAbsoluteDate(),
                                                                           fState.getFrame(),
                                                                           fState.getPVCoordinates().getPosition(),
                                                                           fState.getPVCoordinates().getVelocity(),
                                                                           fState.getAttitude().getRotation(),
                                                                           fState.getMass());
        Vector3D dFdPXRef = new Vector3D(dsOld.getX().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsOld.getY().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsOld.getZ().getPartialDerivative(1, 0, 0, 0, 0, 0));
        Vector3D dFdPXRes = new Vector3D(dsNew.getX().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsNew.getY().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsNew.getZ().getPartialDerivative(1, 0, 0, 0, 0, 0));
        Vector3D dFdPYRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsOld.getY().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsOld.getZ().getPartialDerivative(0, 1, 0, 0, 0, 0));
        Vector3D dFdPYRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsNew.getY().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsNew.getZ().getPartialDerivative(0, 1, 0, 0, 0, 0));
        Vector3D dFdPZRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsOld.getY().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsOld.getZ().getPartialDerivative(0, 0, 1, 0, 0, 0));
        Vector3D dFdPZRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsNew.getY().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsNew.getZ().getPartialDerivative(0, 0, 1, 0, 0, 0));
        Vector3D dFdVXRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsOld.getY().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsOld.getZ().getPartialDerivative(0, 0, 0, 1, 0, 0));
        Vector3D dFdVXRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsNew.getY().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsNew.getZ().getPartialDerivative(0, 0, 0, 1, 0, 0));
        Vector3D dFdVYRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsOld.getY().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsOld.getZ().getPartialDerivative(0, 0, 0, 0, 1, 0));
        Vector3D dFdVYRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsNew.getY().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsNew.getZ().getPartialDerivative(0, 0, 0, 0, 1, 0));
        Vector3D dFdVZRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsOld.getY().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsOld.getZ().getPartialDerivative(0, 0, 0, 0, 0, 1));
        Vector3D dFdVZRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsNew.getY().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsNew.getZ().getPartialDerivative(0, 0, 0, 0, 0, 1));
        if (print) {
            System.out.println("dF/dPX ref norm: " + dFdPXRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPXRef, dFdPXRes) + ", rel error: " + (Vector3D.distance(dFdPXRef, dFdPXRes) / dFdPXRef.getNorm()));
            System.out.println("dF/dPY ref norm: " + dFdPYRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPYRef, dFdPYRes) + ", rel error: " + (Vector3D.distance(dFdPYRef, dFdPYRes) / dFdPYRef.getNorm()));
            System.out.println("dF/dPZ ref norm: " + dFdPZRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPZRef, dFdPZRes) + ", rel error: " + (Vector3D.distance(dFdPZRef, dFdPZRes) / dFdPZRef.getNorm()));
            System.out.println("dF/dVX ref norm: " + dFdVXRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVXRef, dFdVXRes) + ", rel error: " + (Vector3D.distance(dFdVXRef, dFdVXRes) / dFdVXRef.getNorm()));
            System.out.println("dF/dVY ref norm: " + dFdVYRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVYRef, dFdVYRes) + ", rel error: " + (Vector3D.distance(dFdVYRef, dFdVYRes) / dFdVYRef.getNorm()));
            System.out.println("dF/dVZ ref norm: " + dFdVZRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVZRef, dFdVZRes) + ", rel error: " + (Vector3D.distance(dFdVZRef, dFdVZRes) / dFdVZRef.getNorm()));
        }
        checkdFdP(dFdPXRef, dFdPXRes, checkTolerance);
        checkdFdP(dFdPYRef, dFdPYRes, checkTolerance);
        checkdFdP(dFdPZRef, dFdPZRes, checkTolerance);
        checkdFdP(dFdVXRef, dFdVXRes, checkTolerance);
        checkdFdP(dFdVYRef, dFdVYRes, checkTolerance);
        checkdFdP(dFdVZRef, dFdVZRes, checkTolerance);

    }

    private void checkdFdP(final Vector3D reference, final Vector3D result, final double checkTolerance) {
        if (reference.getNorm() == 0) {
            // if dF/dP is exactly zero (i.e. no dependency between F and P),
            // then the result should also be exactly zero
            Assert.assertEquals(0, result.getNorm(), Precision.SAFE_MIN);
        } else {
            Assert.assertEquals(0, Vector3D.distance(reference, result), checkTolerance * reference.getNorm());
        }
    }

}


