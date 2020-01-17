/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

package org.orekit.estimation.iod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.Utils;

/**
 * @author Shiva Iyer
 * @since 10.1
 */
public class IodLaplaceTest {
    private Frame gcrf;

    private Frame itrf;

    private GroundStation observer;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");

	this.gcrf = FramesFactory.getGCRF();
	this.itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, false);

	// The ground station is set to Austin, Texas, U.S.A
	final OneAxisEllipsoid body = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
							   Constants.WGS84_EARTH_FLATTENING, itrf);
	this.observer = new GroundStation(
	    new TopocentricFrame(body, new GeodeticPoint(0.528253, -1.705768, 0.0), "Austin"));
	this.observer.getPrimeMeridianOffsetDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);
	this.observer.getPolarOffsetXDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);
	this.observer.getPolarOffsetYDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);
    }

    // Estimate the orbit of ISS (ZARYA) based on Keplerian motion
    @Test
    public void testLaplaceKeplerian1() {
	final AbsoluteDate date = new AbsoluteDate(2019, 9, 29, 22, 0, 2.0, TimeScalesFactory.getUTC());
        final KeplerianOrbit kep = new KeplerianOrbit(6798938.970424857, 0.0021115522920270016, 0.9008866630545347,
						      1.8278985811406743, -2.7656136723308524,
						      0.8823034512437679, PositionAngle.MEAN, gcrf,
						      date, Constants.EGM96_EARTH_MU);
	final KeplerianPropagator prop = new KeplerianPropagator(kep);

	// With only 3 measurements, we can expect ~400 meters error in position and ~1 m/s in velocity
	final double[] error = estimateOrbit(prop, date, 30.0, 60.0).getErrorNorm();
	Assert.assertEquals(0.0, error[0], 275.0);
	Assert.assertEquals(0.0, error[1], 0.8);
    }

    // Estimate the orbit of Galaxy 15 based on Keplerian motion
    @Test
    public void testLaplaceKeplerian2() {
	final AbsoluteDate date = new AbsoluteDate(2019, 9, 29, 22, 0, 2.0, TimeScalesFactory.getUTC());
        final KeplerianOrbit kep = new KeplerianOrbit(42165414.60406032, 0.00021743441091199163, 0.0019139259842569903,
						      1.8142608912728584, 1.648821262690012,
						      0.11710513241172144, PositionAngle.MEAN, gcrf,
						      date, Constants.EGM96_EARTH_MU);
	final KeplerianPropagator prop = new KeplerianPropagator(kep);

	final double[] error = estimateOrbit(prop, date, 60.0, 120.0).getErrorNorm();
	Assert.assertEquals(0.0, error[0], 395.0);
	Assert.assertEquals(0.0, error[1], 0.03);
    }

    // Estimate the orbit of ISS (ZARYA) based on TLE propagation
    @Test
    public void testLaplaceTLE1() {
	final String tle1 = "1 25544U 98067A   19271.53261574  .00000256  00000-0  12497-4 0  9993";
	final String tle2 = "2 25544  51.6447 208.7465 0007429  92.6235 253.7389 15.50110361191281";

	final TLE tleParser = new TLE(tle1, tle2);
	final TLEPropagator tleProp = TLEPropagator.selectExtrapolator(tleParser);
	final AbsoluteDate obsDate1 = tleParser.getDate();

	final double[] error = estimateOrbit(tleProp, obsDate1, 30.0, 60.0).getErrorNorm();

	// With only 3 measurements, an error of 5km in position and 10 m/s in velocity is acceptable
	// because the Laplace method uses only two-body dynamics
	Assert.assertEquals(0.0, error[0], 5000.0);
	Assert.assertEquals(0.0, error[1], 10.0);
    }

    // Estimate the orbit of COSMOS 382 based on TLE propagation
    @Test
    public void testLaplaceTLE2() {
	final String tle1 = "1  4786U 70103A   19270.85687399 -.00000025 +00000-0 +00000-0 0  9998";
	final String tle2 = "2  4786 055.8645 163.2517 1329144 016.0116 045.4806 08.42042146501862";

	final TLE tleParser = new TLE(tle1, tle2);
	final TLEPropagator tleProp = TLEPropagator.selectExtrapolator(tleParser);
	final AbsoluteDate obsDate1 = tleParser.getDate();

	final double[] error = estimateOrbit(tleProp, obsDate1, 30.0, 60.0).getErrorNorm();
	Assert.assertEquals(0.0, error[0], 5000.0);
	Assert.assertEquals(0.0, error[1], 10.0);
    }

    // Estimate the orbit of GALAXY 15 based on TLE propagation
    @Test
    public void testLaplaceTLE3() {
	final String tle1 = "1 28884U 05041A   19270.71989132  .00000058  00000-0  00000+0 0  9991";
	final String tle2 = "2 28884   0.0023 190.3430 0001786 354.8402 307.2011  1.00272290 51019";

	final TLE tleParser = new TLE(tle1, tle2);
	final TLEPropagator tleProp = TLEPropagator.selectExtrapolator(tleParser);
	final AbsoluteDate obsDate1 = tleParser.getDate();

	final double[] error = estimateOrbit(tleProp, obsDate1, 300.0, 600.0).getErrorNorm();
	Assert.assertEquals(0.0, error[0], 5000.0);
	Assert.assertEquals(0.0, error[1], 10.0);
    }

    // Helper function to generate measurements and estimate orbit for the given propagator
    private Result estimateOrbit(final Propagator prop, final AbsoluteDate obsDate1,
				 final double t2, final double t3)
    {
	// Generate 3 Line Of Sight angles measurements
	final Vector3D los1 = getLOSAngles(prop, obsDate1, observer);

	final AbsoluteDate obsDate2 = obsDate1.shiftedBy(t2);
	final Vector3D los2 = getLOSAngles(prop, obsDate2, observer);

	final AbsoluteDate obsDate3 = obsDate1.shiftedBy(t3);
	final Vector3D los3 = getLOSAngles(prop, obsDate3, observer);

	final TimeStampedPVCoordinates obsPva = observer
	    .getBaseFrame().getPVCoordinates(obsDate2, gcrf);

	// Estimate the orbit using the classical Laplace method
	final TimeStampedPVCoordinates truth = prop.getPVCoordinates(obsDate2, gcrf);
	final CartesianOrbit estOrbit = new IodLaplace(Constants.EGM96_EARTH_MU)
	    .estimate(gcrf, obsPva, obsDate1, los1, obsDate2, los2, obsDate3, los3);
	return(new Result(truth, estOrbit));
    }

    // Helper function to generate a Line of Sight angles measurement for the given
    // observer and date using the TLE propagator.
    private Vector3D getLOSAngles(final Propagator prop, final AbsoluteDate date,
				  final GroundStation observer) {
	final TimeStampedPVCoordinates satPvc = prop.getPVCoordinates(date, gcrf);
	final AngularRaDec raDec = new AngularRaDec(observer, gcrf, date, new double[] {0.0, 0.0},
						   new double[] {1.0, 1.0},
						   new double[] {1.0, 1.0}, new ObservableSatellite(0));

	final double[] angular = raDec.estimate(0, 0, new SpacecraftState[]{new SpacecraftState(
		    new AbsolutePVCoordinates(gcrf, satPvc))}).getEstimatedValue();
	final double ra = angular[0];
	final double dec = angular[1];

	return(new Vector3D(FastMath.cos(dec)*FastMath.cos(ra),
			    FastMath.cos(dec)*FastMath.sin(ra), FastMath.sin(dec)));
    }

    // Private class to calculate the errors between truth and estimated orbits at
    // the central observation time.
    private class Result
    {
	private double[] errorNorm;

	public Result(final TimeStampedPVCoordinates truth, final CartesianOrbit estOrbit)
	{
	    this.errorNorm = new double[2];
	    this.errorNorm[0] = Vector3D.distance(truth.getPosition(),
						  estOrbit.getPVCoordinates().getPosition());
	    this.errorNorm[1] = Vector3D.distance(truth.getVelocity(),
						  estOrbit.getPVCoordinates().getVelocity());
	}

	public double[] getErrorNorm()
	{
	    return(this.errorNorm);
	}
    }
}
