package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.PotentialCoefficientsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class DSSTCentralBodyTest {

    @Test
    public void testMeanElementRate() throws OrekitException, IOException, ParseException {

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] Cnm = provider.getC(5, 5, true);
        final double[][] Snm = provider.getS(5, 5, true);
        final DSSTForceModel force = new DSSTCentralBody(provider.getAe(),provider.getMu(),
                                                         Cnm, Snm, null, 1e-4);

        final AbsoluteDate date  = new AbsoluteDate(new DateComponents(2003, 03, 21),
                                                    new TimeComponents(1, 0, 0.),
                                                    TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, provider.getMu());
        final SpacecraftState state = new SpacecraftState(orbit);

        final double[] daidt = force.getMeanElementRate(state);

        for (int i = 0; i < daidt.length; i++) {
            System.out.println(daidt[i]);
        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

}
