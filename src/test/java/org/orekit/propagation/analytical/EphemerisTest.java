package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

public class EphemerisTest {
    private AbsoluteDate initDate;
    private AbsoluteDate finalDate;

    @Test
    public void testAttitudeOverride() throws IllegalArgumentException, OrekitException {
    	final double positionTolerance = 1e-6;
    	final double velocityTolerance = 1e-5;
    	final double attitudeTolerance = 1e-6;
        double mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;
        double mu  = 3.9860047e14;
        Frame inertialFrame = FramesFactory.getEME2000();

        int numberOfInterals = 1440;
        double deltaT = finalDate.durationFrom(initDate)/((double)numberOfInterals);

        Orbit initialState = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                            inertialFrame, initDate, mu);
        Propagator propagator = new KeplerianPropagator(initialState);
        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

        List<SpacecraftState> states = new ArrayList<SpacecraftState>(numberOfInterals + 1);
        for (int j = 0; j<= numberOfInterals; j++) {
            SpacecraftState state = propagator.propagate(initDate.shiftedBy((j * deltaT)));
            states.add(new SpacecraftState(state.getOrbit(), state.getAttitude(), state.getMass()));
        }
        
        

        int numInterpolationPoints = 2;
        Ephemeris ephemPropagator = new Ephemeris(states, numInterpolationPoints);
        
        //First test that we got position, velocity and attitude nailed
        int numberEphemTestIntervals = 2880;
        deltaT = finalDate.durationFrom(initDate)/((double)numberEphemTestIntervals);
        for(int j = 0; j <= numberEphemTestIntervals; j++) {
        	AbsoluteDate currentDate = initDate.shiftedBy(j * deltaT);
        	SpacecraftState ephemState = ephemPropagator.propagate(currentDate);
        	SpacecraftState keplerState = propagator.propagate(currentDate);
        	double positionDelta = calculatePositionDelta(ephemState, keplerState);
        	double velocityDelta = calculateVelocityDelta(ephemState, keplerState);
        	double attitudeDelta = calculateAttitudeDelta(ephemState, keplerState);
        	Assert.assertEquals("VVLH Unmatched Position at: " + currentDate, 0.0, positionDelta, positionTolerance);
        	Assert.assertEquals("VVLH Unmatched Velocity at: " + currentDate, 0.0, velocityDelta, velocityTolerance);
        	Assert.assertEquals("VVLH Unmatched Attitude at: " + currentDate, 0.0, attitudeDelta, attitudeTolerance);
        }
        
        //Now force an override on the attitude and check it against a Keplerian propagator
        //setup identically to the first but with a different attitude
        //If override isn't working this will fail.
        propagator = new KeplerianPropagator(initialState);
        propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.QSW));
        
        ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame,LOFType.QSW));
        for(int j = 0; j <= numberEphemTestIntervals; j++) {
        	AbsoluteDate currentDate = initDate.shiftedBy(j * deltaT);
        	SpacecraftState ephemState = ephemPropagator.propagate(currentDate);
        	SpacecraftState keplerState = propagator.propagate(currentDate);
        	double positionDelta = calculatePositionDelta(ephemState, keplerState);
        	double velocityDelta = calculateVelocityDelta(ephemState, keplerState);
        	double attitudeDelta = calculateAttitudeDelta(ephemState, keplerState);
        	Assert.assertEquals("QSW Unmatched Position at: " + currentDate, 0.0, positionDelta, positionTolerance);
        	Assert.assertEquals("QSW Unmatched Velocity at: " + currentDate, 0.0, velocityDelta, velocityTolerance);
        	Assert.assertEquals("QSW Unmatched Attitude at: " + currentDate, 0.0, attitudeDelta, attitudeTolerance);
        }

    }

    @Before
    public void setUp() throws IllegalArgumentException, OrekitException {
        Utils.setDataRoot("regular-data");

        initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

        finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                 TimeComponents.H00,
                 TimeScalesFactory.getUTC());

    }

	private double calculatePositionDelta(SpacecraftState state1, SpacecraftState state2) {
		return Vector3D.distance(state1.getPVCoordinates().getPosition(), state2.getPVCoordinates().getPosition());
	}
	
	private double calculateVelocityDelta(SpacecraftState state1, SpacecraftState state2) {
		return Vector3D.distance(state1.getPVCoordinates().getVelocity(), state2.getPVCoordinates().getVelocity());
	}
	
	private double calculateAttitudeDelta(SpacecraftState state1, SpacecraftState state2) {
		return Rotation.distance(state1.getAttitude().getRotation(), state2.getAttitude().getRotation());
	}
}
