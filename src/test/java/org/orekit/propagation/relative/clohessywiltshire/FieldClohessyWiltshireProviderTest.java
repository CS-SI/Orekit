/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative.clohessywiltshire;

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;


class FieldClohessyWiltshireProviderTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testFieldClohessyWiltshireProvider(){

        Field<Binary64> field = Binary64Field.getInstance();
        final Frame eme2000 = FramesFactory.getEME2000();

        final double orbitRadius = 35800e3 + Constants.EGM96_EARTH_EQUATORIAL_RADIUS;

        final AbsoluteDate date = new AbsoluteDate(2025, 3, 18, 10, 33, 15,
                                                   TimeScalesFactory.getUTC());
        final FieldAbsoluteDate<Binary64> dateField = new FieldAbsoluteDate<>(field,date);

        final KeplerianOrbit targetOrbit = new KeplerianOrbit(orbitRadius, 0, 0, 0, 0, 0,
                                                              PositionAngleType.TRUE, eme2000, date,
                                                              Constants.EIGEN5C_EARTH_MU);
        final FieldKeplerianOrbit<Binary64> targetOrbitField = new FieldKeplerianOrbit<>(field,targetOrbit);

        // Initialize chaser position 1km away from the target
        final Vector3D initialTargetPosition = targetOrbit.getPosition();
        Vector3D deltaPos = (new Vector3D(0.33,0.33,0.33)).scalarMultiply(1000.);
        PVCoordinates initialChaserPV =
                        new PVCoordinates(initialTargetPosition.add(deltaPos),
                                          targetOrbit.getPVCoordinates().getVelocity());

        final FieldVector3D<Binary64> initialTargetPositionField = targetOrbitField.getPosition();
        FieldVector3D<Binary64> deltaPosField = new FieldVector3D<>(field,deltaPos);
        FieldPVCoordinates<Binary64> initialChaserPVField =
                        new FieldPVCoordinates<>(initialTargetPositionField.add(deltaPosField),
                                                 targetOrbitField.getPVCoordinates().getVelocity());

        final TimeStampedPVCoordinates chaserTimePV = new TimeStampedPVCoordinates(date,initialChaserPV);
        final TimeStampedFieldPVCoordinates<Binary64> chaserTimePVField =
                        new TimeStampedFieldPVCoordinates<>(dateField,initialChaserPVField);

        final ClohessyWiltshireProvider cwProvider =
                        new ClohessyWiltshireProvider(targetOrbit, chaserTimePV, eme2000,
                                                      "Clohessy-Wiltshire Equations");
        final FieldClohessyWiltshireProvider<Binary64> cwProviderField =
                        new FieldClohessyWiltshireProvider<>(targetOrbitField,chaserTimePVField, eme2000,
                                                             "Clohessy-Wiltshire Field");

        final KeplerianPropagator targetPropagator = new KeplerianPropagator(targetOrbit);
        final FieldKeplerianPropagator<Binary64> targetPropagatorField =
                        new FieldKeplerianPropagator<>(targetOrbitField);

        targetPropagator.addAdditionalDataProvider(cwProvider);
        targetPropagatorField.addAdditionalDataProvider(cwProviderField);

        final SpacecraftState finalTargetState = targetPropagator.propagate(date.shiftedBy(1000.));
        final FieldSpacecraftState<Binary64> finalTargetStateField =
                        targetPropagatorField.propagate(dateField.shiftedBy(1000.));

        final double[] chaserFinalCW = cwProvider.getAdditionalData(finalTargetState);
        final Binary64[] chaserFinalCWField = cwProviderField.getAdditionalData(finalTargetStateField);

        Assertions.assertEquals(chaserFinalCW[0],chaserFinalCWField[0].getReal(),0);
        Assertions.assertEquals(chaserFinalCW[1],chaserFinalCWField[1].getReal(),0);
        Assertions.assertEquals(chaserFinalCW[2],chaserFinalCWField[2].getReal(),0);
        Assertions.assertEquals(chaserFinalCW[3],chaserFinalCWField[3].getReal(),0);
        Assertions.assertEquals(chaserFinalCW[4],chaserFinalCWField[4].getReal(),0);
        Assertions.assertEquals(chaserFinalCW[5],chaserFinalCWField[5].getReal(),0);
    }

    @Test
    void testGetInitialChaserPVTLof() {
        Binary64Field field = Binary64Field.getInstance();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(10000,0,0,0,0,0,PositionAngleType.TRUE,frame,date,Constants.EIGEN5C_EARTH_MU);
        final FieldKeplerianOrbit<Binary64> targetOrbitField = new FieldKeplerianOrbit<>(field,targetOrbit);
        final TimeStampedPVCoordinates pvt1 = new TimeStampedPVCoordinates(date,Vector3D.ZERO,Vector3D.ZERO);
        final FieldClohessyWiltshireProvider<Binary64> provider = new FieldClohessyWiltshireProvider<>(targetOrbitField);
        TestUtils.validateFieldVector3D(pvt1.getPosition(),provider.getInitialChaserPVTLof().getPosition(),0);
        TestUtils.validateFieldVector3D(pvt1.getVelocity(),provider.getInitialChaserPVTLof().getVelocity(),0);
    }

    @Test
    void testSetInitialChaserPVTLof(){
        Binary64Field field = Binary64Field.getInstance();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame eme2000 = FramesFactory.getEME2000();
        final String string ="test";
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(10000,0,0,0,0,0,
                                                              PositionAngleType.TRUE,eme2000,date,Constants.EIGEN5C_EARTH_MU);
        final FieldKeplerianOrbit<Binary64> targetOrbitField = new FieldKeplerianOrbit<>(field,targetOrbit);
        final TimeStampedPVCoordinates pvt1 = new TimeStampedPVCoordinates(date,Vector3D.ZERO,Vector3D.ZERO);
        final TimeStampedFieldPVCoordinates<Binary64> pvt1Field = new TimeStampedFieldPVCoordinates<>(field,pvt1);
        final FieldClohessyWiltshireProvider<Binary64> provider =
                        new FieldClohessyWiltshireProvider<>(targetOrbitField,pvt1Field,string);

        final TimeStampedPVCoordinates pvt2 = new TimeStampedPVCoordinates(date,
                                                                           new Vector3D(1,1,1),
                                                                           new Vector3D(1,1,1));
        final TimeStampedFieldPVCoordinates<Binary64> pvt2Field = new TimeStampedFieldPVCoordinates<>(field,pvt2);
        provider.setInitialChaserPVTLof(pvt2Field);

        TestUtils.validateFieldVector3D(pvt2.getPosition(),provider.getInitialChaserPVTLof().getPosition(),0);
        TestUtils.validateFieldVector3D(pvt2.getVelocity(),provider.getInitialChaserPVTLof().getVelocity(),0);
    }

    @Test
    void extractChaserPVTTest(){
        Field<Binary64> field = Binary64Field.getInstance();
        final Frame eme2000 = FramesFactory.getEME2000();

        final double orbitRadius = 35800e3 + Constants.EGM96_EARTH_EQUATORIAL_RADIUS;

        final AbsoluteDate date = new AbsoluteDate(2025, 3, 18, 10, 33, 15,
                                                   TimeScalesFactory.getUTC());
        final FieldAbsoluteDate<Binary64> dateField = new FieldAbsoluteDate<>(field,date);

        final KeplerianOrbit targetOrbit = new KeplerianOrbit(orbitRadius, 0, 0, 0, 0, 0,
                                                              PositionAngleType.TRUE, eme2000, date,
                                                              Constants.EIGEN5C_EARTH_MU);
        final FieldKeplerianOrbit<Binary64> targetOrbitField = new FieldKeplerianOrbit<>(field,targetOrbit);

        // Initialize chaser position 1km away from the target
        Vector3D deltaPos = (new Vector3D(0.33,0.33,0.33)).scalarMultiply(1000.);

        final FieldVector3D<Binary64> initialTargetPositionField = targetOrbitField.getPosition();
        FieldVector3D<Binary64> deltaPosField = new FieldVector3D<>(field,deltaPos);
        FieldPVCoordinates<Binary64> initialChaserPVField =
                        new FieldPVCoordinates<>(initialTargetPositionField.add(deltaPosField),
                                                 targetOrbitField.getPVCoordinates().getVelocity());

        final TimeStampedFieldPVCoordinates<Binary64> chaserTimePVField =
                        new TimeStampedFieldPVCoordinates<>(dateField,initialChaserPVField);

        final FieldClohessyWiltshireProvider<Binary64> cwProviderField =
                        new FieldClohessyWiltshireProvider<>(targetOrbitField,chaserTimePVField, eme2000,
                                                             "Clohessy-Wiltshire Field");

        final FieldKeplerianPropagator<Binary64> targetPropagatorField = new FieldKeplerianPropagator<>(targetOrbitField);

        targetPropagatorField.addAdditionalDataProvider(cwProviderField);

        final FieldSpacecraftState<Binary64> finalTargetStateField = targetPropagatorField.propagate(dateField.shiftedBy(1000.));

        final Binary64[] chaserFinalCWField = cwProviderField.getAdditionalData(finalTargetStateField);

        final TimeStampedFieldPVCoordinates<Binary64> chaserPVTLof =
                        new TimeStampedFieldPVCoordinates<>(finalTargetStateField.getDate(), new FieldPVCoordinates<>(
                new FieldVector3D<>(chaserFinalCWField[0], chaserFinalCWField[1], chaserFinalCWField[2]),
                new FieldVector3D<>(chaserFinalCWField[3], chaserFinalCWField[4], chaserFinalCWField[5])));

        final LocalOrbitalFrame targetLofCW = new LocalOrbitalFrame(targetOrbitField.getFrame(), LOFType.QSW,
                                                                    targetOrbit, "QSW LOF target");
        final FieldPVCoordinates<Binary64> inertialPVCW =
                        targetLofCW.getTransformTo(eme2000,
                                                   finalTargetStateField.getDate()).transformPVCoordinates(chaserPVTLof);

        final TimeStampedFieldPVCoordinates<Binary64> chaserInertialWithExtract =
                        cwProviderField.extractChaserPVT(finalTargetStateField,eme2000);

        TestUtils.validateFieldVector3D(inertialPVCW.getPosition().toVector3D(),
                                        chaserInertialWithExtract.getPosition(),0);
        TestUtils.validateFieldVector3D(inertialPVCW.getVelocity().toVector3D(),
                                        chaserInertialWithExtract.getVelocity(),0);
    }

    @Test
    void getTargetOrbitTest() {
        Field<Binary64> field = Binary64Field.getInstance();
        final Frame eme2000 = FramesFactory.getEME2000();

        final double orbitRadius = 35800e3 + Constants.EGM96_EARTH_EQUATORIAL_RADIUS;

        final AbsoluteDate date = new AbsoluteDate(2025, 3, 18, 10, 33, 15, TimeScalesFactory.getUTC());
        final FieldAbsoluteDate<Binary64> dateField = new FieldAbsoluteDate<>(field,date);

        final KeplerianOrbit targetOrbit = new KeplerianOrbit(orbitRadius, 0, 0, 0, 0, 0, PositionAngleType.TRUE, eme2000, date, Constants.EIGEN5C_EARTH_MU);
        final FieldKeplerianOrbit<Binary64> targetOrbitField = new FieldKeplerianOrbit<>(field,targetOrbit);

        // Initialize chaser position 1km away from the target
        Vector3D deltaPos = (new Vector3D(0.33,0.33,0.33)).scalarMultiply(1000.);

        final FieldVector3D<Binary64> initialTargetPositionField = targetOrbitField.getPosition();
        FieldVector3D<Binary64> deltaPosField = new FieldVector3D<>(field,deltaPos);
        FieldPVCoordinates<Binary64> initialChaserPVField = new FieldPVCoordinates<>(initialTargetPositionField.add(deltaPosField),targetOrbitField.getPVCoordinates().getVelocity());

        final TimeStampedFieldPVCoordinates<Binary64> chaserTimePVField = new TimeStampedFieldPVCoordinates<>(dateField,initialChaserPVField);

        final FieldClohessyWiltshireProvider<Binary64> cwProviderField = new FieldClohessyWiltshireProvider<>(targetOrbitField,chaserTimePVField, eme2000, "Clohessy-Wiltshire Field");

        final FieldOrbit<Binary64> orbit = cwProviderField.getTargetOrbit();

        Assertions.assertEquals(targetOrbit.getA(),orbit.getA().getReal(),0);
        Assertions.assertEquals(targetOrbit.getE(),orbit.getE().getReal(),0);
        Assertions.assertEquals(targetOrbit.getI(),orbit.getI().getReal(),0);

    }
}
