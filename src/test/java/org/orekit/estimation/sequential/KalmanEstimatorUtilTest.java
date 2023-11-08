/* Copyright 2002-2023 CS GROUP
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
package org.orekit.estimation.sequential;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase.Status;
import org.orekit.estimation.measurements.Range;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeStampedPVCoordinates;

public class KalmanEstimatorUtilTest {

	@Test
	public void testDimension() {

		// Orbital drivers
		final ParameterDriversList orbital = new ParameterDriversList();
		orbital.add(createDriver("a", false));
		orbital.add(createDriver("e", true));

		// Propagation drivers
		final ParameterDriversList prop = new ParameterDriversList();
		prop.add(createDriver("Cr", false));
		prop.add(createDriver("Cd", true));

		// Measurement drivers
		final ParameterDriversList meas = new ParameterDriversList();
		meas.add(createDriver("Range_bias", false));
		meas.add(createDriver("Clock", true));

		// Works
		KalmanEstimatorUtil.checkDimension(3, orbital, prop, meas);

		// Test exception
        try {
        	KalmanEstimatorUtil.checkDimension(4, orbital, prop, meas);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.DIMENSION_INCONSISTENT_WITH_PARAMETERS, oe.getSpecifier());
        }
		
	}

	@Test
	public void testRejectedMeasurement() {
		final EstimatedMeasurement<Range> estimated = new EstimatedMeasurement<>(null, 1, 1, new SpacecraftState[1], new TimeStampedPVCoordinates[1]);
		estimated.setStatus(Status.REJECTED);
		Assertions.assertNull(KalmanEstimatorUtil.computeInnovationVector(estimated, new double[1]));
	}

	private ParameterDriver createDriver(final String name, final boolean estimated) {
		final ParameterDriver driver = new ParameterDriver(name, 1.0, 1.0, 0.0, 2.0);
		driver.setSelected(estimated);
		return driver;
	}

}
