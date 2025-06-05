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
package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.FieldAbsoluteDate;

public class ExtendedPVCoordinatesTest {

    @Test
    @Deprecated
    public void testConversion() {
        final ExtendedPositionProvider provider = new ExtendedPositionProvider() {

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(FieldAbsoluteDate<T> date, Frame frame) {
                return FieldVector3D.getPlusI(date.getField());
            }
        }.toExtendedPVCoordinatesProvider();

        Field<Binary64> field = Binary64Field.getInstance();
        final FieldPVCoordinatesProvider<Binary64> converted = provider.toFieldPVCoordinatesProvider(field);
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getJ2000Epoch(field);
        final Frame frame = FramesFactory.getGCRF();
        FieldVector3D<Binary64> p = converted.getPosition(date, frame);
        Assertions.assertEquals(0.0, FieldVector3D.distance(p, FieldVector3D.getPlusI(field)).getReal(), 1.0e-15);

        FieldPVCoordinates<Binary64> pv = converted.getPVCoordinates(date, frame);
        Assertions.assertEquals(0.0, FieldVector3D.distance(pv.getPosition(),     FieldVector3D.getPlusI(field)).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(pv.getVelocity(),     provider.getPVCoordinates(date, frame).getVelocity()).getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, FieldVector3D.distance(pv.getAcceleration(), provider.getPVCoordinates(date, frame).getAcceleration()).getReal(), 1.0e-15);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
