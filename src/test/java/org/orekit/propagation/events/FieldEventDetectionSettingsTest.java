package org.orekit.propagation.events;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
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
        Assertions.assertEquals(maxCheck, fieldEventDetectionSettings.getMaxCheckInterval().currentInterval(null));
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
        Assertions.assertEquals(fieldEventDetectionSettings.getMaxCheckInterval().currentInterval(new FieldSpacecraftState<>(ComplexField.getInstance(), state)),
                settings.getMaxCheckInterval().currentInterval(state));
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
        Assertions.assertEquals(fieldEventDetectionSettings.getMaxCheckInterval().currentInterval(new FieldSpacecraftState<>(ComplexField.getInstance(), state)),
                eventDetectionSettings.getMaxCheckInterval().currentInterval(state));
    }
}
