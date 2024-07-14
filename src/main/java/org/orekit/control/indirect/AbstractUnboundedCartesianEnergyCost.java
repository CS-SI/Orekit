package org.orekit.control.indirect;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

public abstract class AbstractUnboundedCartesianEnergyCost implements CartesianCost {

    @Override
    public Vector3D getThrustVector(double[] adjointVariables, double mass) {
        return null;
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(T[] adjointVariables, T mass) {
        return null;
    }

    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.empty();
    }

    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
        return Stream.empty();
    }
}
