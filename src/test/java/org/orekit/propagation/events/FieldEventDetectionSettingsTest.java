package org.orekit.propagation.events;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;

class FieldEventDetectionSettingsTest {

    @Test
    void testConstructor() {
        // GIVEN
        final double maxCheck = 100.;
        // WHEN
        final FieldEventDetectionSettings<Complex> fieldEventDetectionSettings = new FieldEventDetectionSettings<>(maxCheck,
                new Complex(20.), 10);
        // THEN
        Assertions.assertEquals(maxCheck, fieldEventDetectionSettings.getMaxCheckInterval().currentInterval(null, true));
    }

    @Test
    void testConstructorFromNonField() {
        // GIVEN
        final EventDetectionSettings settings = new EventDetectionSettings(AdaptableInterval.of(10), 100., 1000);
        // WHEN
        final FieldEventDetectionSettings<Complex> fieldEventDetectionSettings = new FieldEventDetectionSettings<>(ComplexField.getInstance(),
                settings);
        // THEN
        Assertions.assertEquals(settings.getMaxIterationCount(), fieldEventDetectionSettings.getMaxIterationCount());
        Assertions.assertEquals(settings.getThreshold(), fieldEventDetectionSettings.getThreshold().getReal());
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates()));
        Assertions.assertEquals(fieldEventDetectionSettings.getMaxCheckInterval().currentInterval(new FieldSpacecraftState<>(ComplexField.getInstance(), state), true),
                settings.getMaxCheckInterval().currentInterval(state, true));
    }

    @Test
    void testToEventDetectionSettings() {
        // GIVEN
        final FieldAdaptableInterval<Complex> interval = FieldAdaptableInterval.of(1.);
        final FieldEventDetectionSettings<Complex> fieldEventDetectionSettings = new FieldEventDetectionSettings<>(interval,
                new Complex(20.), 10);
        // WHEN
        final EventDetectionSettings eventDetectionSettings = fieldEventDetectionSettings.toEventDetectionSettings();
        // THEN
        Assertions.assertEquals(fieldEventDetectionSettings.getMaxIterationCount(), eventDetectionSettings.getMaxIterationCount());
        Assertions.assertEquals(fieldEventDetectionSettings.getThreshold().getReal(), eventDetectionSettings.getThreshold());
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates()));
        Assertions.assertEquals(fieldEventDetectionSettings.getMaxCheckInterval().currentInterval(new FieldSpacecraftState<>(ComplexField.getInstance(), state), true),
                eventDetectionSettings.getMaxCheckInterval().currentInterval(state, true));
    }

    @Test
    void testGetDefaultEventDetectionSettings() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        // WHEN
        final FieldEventDetectionSettings<Binary64> fieldEventDetectionSettings = FieldEventDetectionSettings
                .getDefaultEventDetectionSettings(field);
        // THEN
        final FieldEventDetectionSettings<Binary64> expectedDetectionSettings = new FieldEventDetectionSettings<>(field,
                EventDetectionSettings.getDefaultEventDetectionSettings());
        Assertions.assertEquals(fieldEventDetectionSettings.getMaxIterationCount(), expectedDetectionSettings.getMaxIterationCount());
        Assertions.assertEquals(fieldEventDetectionSettings.getThreshold(), expectedDetectionSettings.getThreshold());
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates()));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        Assertions.assertEquals(fieldEventDetectionSettings.getMaxCheckInterval().currentInterval(fieldState, true),
                expectedDetectionSettings.getMaxCheckInterval().currentInterval(fieldState, true));
    }

    @Test
    void testWithThreshold() {
        // GIVEN
        final FieldEventDetectionSettings<Binary64> defaultSettings = FieldEventDetectionSettings
                .getDefaultEventDetectionSettings(Binary64Field.getInstance());
        final Binary64 expectedThreshold = new Binary64(123);
        // WHEN
        final FieldEventDetectionSettings<Binary64> detectionSettings = defaultSettings.withThreshold(expectedThreshold);
        // THEN
        Assertions.assertEquals(expectedThreshold, detectionSettings.getThreshold());
    }

    @Test
    void testWithMaxIterationCount() {
        // GIVEN
        final FieldEventDetectionSettings<Binary64> defaultSettings = FieldEventDetectionSettings
                .getDefaultEventDetectionSettings(Binary64Field.getInstance());
        final int expectedCount = 123;
        // WHEN
        final FieldEventDetectionSettings<Binary64> detectionSettings = defaultSettings.withMaxIterationCount(expectedCount);
        // THEN
        Assertions.assertEquals(expectedCount, detectionSettings.getMaxIterationCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWithMaxCheckInterval() {
        // GIVEN
        final FieldEventDetectionSettings<Binary64> defaultSettings = FieldEventDetectionSettings
                .getDefaultEventDetectionSettings(Binary64Field.getInstance());
        final FieldAdaptableInterval<Binary64> expectedInterval = Mockito.mock();
        // WHEN
        final FieldEventDetectionSettings<Binary64> detectionSettings = defaultSettings.withMaxCheckInterval(expectedInterval);
        // THEN
        Assertions.assertEquals(expectedInterval, detectionSettings.getMaxCheckInterval());
    }
}
