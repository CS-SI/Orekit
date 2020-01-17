/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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


import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

public class ExtendedPVCoordinatesTest {

    @Test
    public void testConversion() {
        final ExtendedPVCoordinatesProvider provider = new ExtendedPVCoordinatesProvider() {
            
            @Override
            public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return null;
            }
            
            @Override
            public <T extends RealFieldElement<T>> TimeStampedFieldPVCoordinates<T>
                getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame)
                    {
                return new TimeStampedFieldPVCoordinates<>(date,
                                                           FieldVector3D.getPlusI(date.getField()),
                                                           FieldVector3D.getPlusJ(date.getField()),
                                                           FieldVector3D.getPlusK(date.getField()));
            }
        };

        Field<Decimal64> field = Decimal64Field.getInstance();
        final FieldPVCoordinatesProvider<Decimal64> converted =
                        provider.toFieldPVCoordinatesProvider(field);
        FieldPVCoordinates<Decimal64> pv = converted.getPVCoordinates(FieldAbsoluteDate.getJ2000Epoch(field),
                                                                      FramesFactory.getGCRF());
        Assert.assertEquals(0.0, FieldVector3D.distance(pv.getPosition(),FieldVector3D.getPlusI(field)).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldVector3D.distance(pv.getVelocity(),FieldVector3D.getPlusJ(field)).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldVector3D.distance(pv.getAcceleration(),FieldVector3D.getPlusK(field)).getReal(), 1.0e-15);

    }

}
