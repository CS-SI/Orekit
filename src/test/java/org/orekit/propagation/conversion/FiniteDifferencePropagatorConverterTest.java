package org.orekit.propagation.conversion;

import org.hipparchus.linear.ArrayRealVector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/**
 * Unit tests for {@link FiniteDifferencePropagatorConverter}.
 *
 * @author Evan Ward
 */
public class FiniteDifferencePropagatorConverterTest {

    /** Set Orekit data . */
    @BeforeClass
    public static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Test case for bug #362. Check that scaling is only applied once.
     *
     * @throws OrekitException on error.
     */
    @Test
    public void testGetObjectiveFunctionParametersOnlyScaledOnce() throws OrekitException {
        // setup
        // create some arbitrary sample data to run with
        Frame eci = FramesFactory.getGCRF();
        double gm = Constants.EIGEN5C_EARTH_MU;
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Propagator source = new KeplerianPropagator(new KeplerianOrbit(
                6878137, 0, 0, 0, 0, 0, PositionAngle.TRUE, eci, date, gm));
        // Create a mock builder that allows us to check the values passed to it
        PropagatorBuilder builder = Mockito.mock(PropagatorBuilder.class);
        ParameterDriversList list = new ParameterDriversList();
        list.add(new ParameterDriver("p1", 0, 1e-3, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY));
        Mockito.when(builder.getOrbitalParametersDrivers()).thenReturn(list);
        Mockito.when(builder.getPropagationParametersDrivers())
                .thenReturn(new ParameterDriversList());
        Mockito.when(builder.getFrame()).thenReturn(eci);
        Mockito.when(builder.getSelectedNormalizedParameters()).thenReturn(new double[1]);
        Mockito.when(builder.buildPropagator(Mockito.any(double[].class)))
                .thenReturn(source);
        // subject under test
        FiniteDifferencePropagatorConverter converter =
                new FiniteDifferencePropagatorConverter(builder, 1, 100);
        // set some internal variables in FDPC that are not settable using another
        // interface
        converter.convert(source, 1, 2);
        // Forget all the side effect of the call to convert()
        Mockito.clearInvocations(builder);

        // action
        converter.getModel().value(new ArrayRealVector(1));

        // verify
        Mockito.verify(builder).buildPropagator(new double[]{0});
        Mockito.verify(builder).buildPropagator(new double[]{1});
    }

}
