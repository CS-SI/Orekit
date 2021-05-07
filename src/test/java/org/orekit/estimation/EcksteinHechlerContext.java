package org.orekit.estimation;

import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.displacement.StationDisplacement;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.EcksteinHechlerPropagatorBuilder;
import org.orekit.time.TimeScale;
import org.orekit.time.UT1Scale;
import org.orekit.utils.IERSConventions;

public class EcksteinHechlerContext {

    public IERSConventions                        conventions;
    public OneAxisEllipsoid                       earth;
    public UnnormalizedSphericalHarmonicsProvider gravity;
    public TimeScale                              utc;
    public UT1Scale                               ut1;
    public Orbit                                  initialOrbit;
    public StationDisplacement[]                  displacements;
    public List<GroundStation>                    stations;

    public EcksteinHechlerPropagatorBuilder createBuilder(final double dP) {

        final EcksteinHechlerPropagatorBuilder propagatorBuilder =
                        new EcksteinHechlerPropagatorBuilder(initialOrbit, gravity, PositionAngle.MEAN, dP);

        return propagatorBuilder;

    }

    public  GroundStation createStation(double latitudeInDegrees, double longitudeInDegrees,
                                        double altitude, String name) {
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(latitudeInDegrees),
                                                   FastMath.toRadians(longitudeInDegrees),
                                                   altitude);
        return new GroundStation(new TopocentricFrame(earth, gp, name),
                                 ut1.getEOPHistory(), displacements);
    }

}
