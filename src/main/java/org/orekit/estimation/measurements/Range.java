package org.orekit.estimation.measurements;

import java.util.Map;
import java.util.SortedSet;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class Range extends AbstractMeasurement {

    private final TopocentricFrame station;

    public Range(final TopocentricFrame station, final AbsoluteDate date, final double range, final double sigma) {
        super(date,
              new double[] {
                            range
        }, 
              new double[] {
                            sigma
        });
        this.station = station;
    }

    @Override
    public double[][] getPartialDerivatives(SpacecraftState state,
                                            Map<String, double[]> parameters) throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected double[] getTheoreticalValue(SpacecraftState state,
                                           SortedSet<Parameter> parameters) throws OrekitException {
        PVCoordinates scInStationFrame = state.getPVCoordinates(station);
        return new double[] {
                             scInStationFrame.getPosition().getNorm()
        };
    }

}
