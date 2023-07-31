/* Copyright 2002-2023 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;

/** Unit tests for {@link AggregatedPVCoordinatesProvider}. */
public class AggregatedPVCoordinatesProviderTest {
    
    private PVCoordinatesProvider pv1 = Mockito.mock(PVCoordinatesProvider.class);
    
    private PVCoordinatesProvider pv2 = Mockito.mock(PVCoordinatesProvider.class);
    
    private PVCoordinatesProvider pv3 = Mockito.mock(PVCoordinatesProvider.class);
    
    private TimeStampedPVCoordinates pv = Mockito.mock(TimeStampedPVCoordinates.class);
    private AbsoluteDate date1;
    private AbsoluteDate date2;
    private AbsoluteDate date3;
    private AbsoluteDate date4;

    /** Set up test data. */
    @BeforeEach
    public void setup() {
        Utils.setDataRoot("regular-data");

        date1 = AbsoluteDate.J2000_EPOCH;
        date2 = date1.shiftedBy(86400);
        date3 = date2.shiftedBy(86400);
        date4 = date3.shiftedBy(86400);
    }

    @Test
    public void invalidPVProvider() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            final PVCoordinatesProvider pvProv = new AggregatedPVCoordinatesProvider.InvalidPVProvider();
            pvProv.getPVCoordinates(date1, FramesFactory.getGCRF());
        });
    }

    @Test
    public void nominalCase() {
        final AggregatedPVCoordinatesProvider pvProv = new AggregatedPVCoordinatesProvider.Builder()
            .addPVProviderAfter(date1, pv1, false)
            .addPVProviderAfter(date2, pv2, false)
            .addPVProviderAfter(date3, pv3, false)
            .invalidAfter(date4)
            .build();

        Mockito.when(pv1.getPVCoordinates(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(pv);

        Assertions.assertEquals(date1, pvProv.getMinDate());
        Assertions.assertEquals(date4, pvProv.getMaxDate());

        // check start date
        Assertions.assertSame(pv, pvProv.getPVCoordinates(date1, FramesFactory.getGCRF()));
        // check middle date
        Assertions.assertSame(pv, pvProv.getPVCoordinates(date1.shiftedBy(43200), FramesFactory.getGCRF()));

        Mockito.verify(pv1, Mockito.times(2)).getPVCoordinates(ArgumentMatchers.any(),
                ArgumentMatchers.eq(FramesFactory.getGCRF()));
        Mockito.verifyNoMoreInteractions(pv1, pv2, pv3);
    }

    @Test
    public void alwaysValid() {
        final AggregatedPVCoordinatesProvider pvProv = new AggregatedPVCoordinatesProvider.Builder(pv1).build();

        pvProv.getPVCoordinates(date1.shiftedBy(-1.), FramesFactory.getGCRF());
        pvProv.getPVCoordinates(date1, FramesFactory.getGCRF());
        pvProv.getPVCoordinates(date4, FramesFactory.getGCRF());

        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY, pvProv.getMinDate());
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY, pvProv.getMaxDate());

        Mockito.verify(pv1, Mockito.times(3)).getPVCoordinates(ArgumentMatchers.any(),
                ArgumentMatchers.eq(FramesFactory.getGCRF()));
    }

    @Test
    public void alwaysValidAfter() {
        final AggregatedPVCoordinatesProvider pvProv = new AggregatedPVCoordinatesProvider.Builder(pv1)
                .invalidBefore(date1)
                .addPVProviderBefore(date2, pv2, false)
                .build();

        pvProv.getPVCoordinates(date1, FramesFactory.getGCRF());
        pvProv.getPVCoordinates(date3, FramesFactory.getGCRF());

        try {
            pvProv.getPVCoordinates(date1.shiftedBy(-1.), FramesFactory.getGCRF());
            Assertions.fail("expected exception not thrown");
        }
        catch (final OrekitIllegalArgumentException ex) {
            // this exception is expected
        }
        catch (final Exception ex) {
            throw ex;
        }

        Assertions.assertEquals(date1, pvProv.getMinDate());
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY, pvProv.getMaxDate());

        Mockito.verify(pv1).getPVCoordinates(ArgumentMatchers.eq(date3),
                ArgumentMatchers.eq(FramesFactory.getGCRF()));
        Mockito.verify(pv2).getPVCoordinates(ArgumentMatchers.eq(date1),
                ArgumentMatchers.eq(FramesFactory.getGCRF()));
    }

    @Test
    public void invalidBefore() {
        Assertions.assertThrows(OrekitIllegalArgumentException.class, () -> {
            final PVCoordinatesProvider pvProv = new AggregatedPVCoordinatesProvider.Builder()
                    .addPVProviderAfter(date1, pv1, false)
                    .addPVProviderAfter(date2, pv2, false)
                    .addPVProviderAfter(date3, pv3, false)
                    .invalidAfter(date4)
                    .build();
            pvProv.getPVCoordinates(date1.shiftedBy(-1.), FramesFactory.getGCRF());
        });
    }

    @Test
    public void invalidAfter() {
        Assertions.assertThrows(OrekitIllegalArgumentException.class, () -> {
            final PVCoordinatesProvider pvProv = new AggregatedPVCoordinatesProvider.Builder()
                    .addPVProviderAfter(date1, pv1, false)
                    .addPVProviderAfter(date2, pv2, false)
                    .addPVProviderAfter(date3, pv3, false)
                    .invalidAfter(date4)
                    .build();
            pvProv.getPVCoordinates(date4.shiftedBy(1.), FramesFactory.getGCRF());
        });
    }
}
