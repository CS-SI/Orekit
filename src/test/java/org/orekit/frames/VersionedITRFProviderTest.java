/* Copyright 2022-2024 Romain Serra
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


import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

class VersionedITRFProviderTest {

    @Test
    void testGetKinematicTransformNullConverter() {
        // GIVEN
        final ITRFVersion itrfVersion = ITRFVersion.ITRF_2020;
        final ITRFProvider mockedRawProvider = Mockito.mock(ITRFProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final KinematicTransform expectedTransform = Mockito.mock(KinematicTransform.class);
        Mockito.when(mockedRawProvider.getKinematicTransform(date)).thenReturn(expectedTransform);
        final EOPHistory mockedHistory = Mockito.mock(EOPHistory.class);
        Mockito.when(mockedHistory.getITRFVersion(date)).thenReturn(itrfVersion);
        Mockito.when(mockedRawProvider.getEOPHistory()).thenReturn(mockedHistory);
        final VersionedITRFProvider versionedITRFProvider = new VersionedITRFProvider(itrfVersion, mockedRawProvider,
                null);
        // WHEN
        final KinematicTransform actualTransform = versionedITRFProvider.getKinematicTransform(date);
        // THEN
        Assertions.assertEquals(expectedTransform, actualTransform);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFieldGetKinematicTransformNullConverter() {
        // GIVEN
        final ITRFVersion itrfVersion = ITRFVersion.ITRF_2020;
        final ITRFProvider mockedRawProvider = Mockito.mock(ITRFProvider.class);
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final FieldKinematicTransform<Complex> expectedTransform = Mockito.mock(FieldKinematicTransform.class);
        Mockito.when(mockedRawProvider.getKinematicTransform(date)).thenReturn(expectedTransform);
        final EOPHistory mockedHistory = Mockito.mock(EOPHistory.class);
        Mockito.when(mockedHistory.getITRFVersion(date.toAbsoluteDate())).thenReturn(itrfVersion);
        Mockito.when(mockedRawProvider.getEOPHistory()).thenReturn(mockedHistory);
        final VersionedITRFProvider versionedITRFProvider = new VersionedITRFProvider(itrfVersion, mockedRawProvider,
                null);
        // WHEN
        final FieldKinematicTransform<Complex> actualTransform = versionedITRFProvider.getKinematicTransform(date);
        // THEN
        Assertions.assertEquals(expectedTransform, actualTransform);
    }

    @Test
    void testGetStaticTransformNullConverter() {
        // GIVEN
        final ITRFVersion itrfVersion = ITRFVersion.ITRF_2020;
        final ITRFProvider mockedRawProvider = Mockito.mock(ITRFProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final StaticTransform expectedTransform = Mockito.mock(StaticTransform.class);
        Mockito.when(mockedRawProvider.getStaticTransform(date)).thenReturn(expectedTransform);
        final EOPHistory mockedHistory = Mockito.mock(EOPHistory.class);
        Mockito.when(mockedHistory.getITRFVersion(date)).thenReturn(itrfVersion);
        Mockito.when(mockedRawProvider.getEOPHistory()).thenReturn(mockedHistory);
        final VersionedITRFProvider versionedITRFProvider = new VersionedITRFProvider(itrfVersion, mockedRawProvider,
                null);
        // WHEN
        final StaticTransform actualTransform = versionedITRFProvider.getStaticTransform(date);
        // THEN
        Assertions.assertEquals(expectedTransform, actualTransform);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFieldGetStaticTransformNullConverter() {
        // GIVEN
        final ITRFVersion itrfVersion = ITRFVersion.ITRF_2020;
        final ITRFProvider mockedRawProvider = Mockito.mock(ITRFProvider.class);
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final FieldStaticTransform<Complex> expectedTransform = Mockito.mock(FieldStaticTransform.class);
        Mockito.when(mockedRawProvider.getStaticTransform(date)).thenReturn(expectedTransform);
        final EOPHistory mockedHistory = Mockito.mock(EOPHistory.class);
        Mockito.when(mockedHistory.getITRFVersion(date.toAbsoluteDate())).thenReturn(itrfVersion);
        Mockito.when(mockedRawProvider.getEOPHistory()).thenReturn(mockedHistory);
        final VersionedITRFProvider versionedITRFProvider = new VersionedITRFProvider(itrfVersion, mockedRawProvider,
                null);
        // WHEN
        final FieldStaticTransform<Complex> actualTransform = versionedITRFProvider.getStaticTransform(date);
        // THEN
        Assertions.assertEquals(expectedTransform, actualTransform);
    }

}