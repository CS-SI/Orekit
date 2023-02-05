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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;

/** Veis 1950 Frame.
 * <p>Its parent frame is the {@link GTODProvider} without EOP correction application.
 * <p>This frame is mainly provided for consistency with legacy softwares.</p>
 * @author Pascal Parraud
 */
class VEISProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130530L;

    /** 1st coef for Veis sidereal time computation in radians (100.075542 deg). */
    private static final double VST0 = 1.746647708617871;

    /** 2nd coef for Veis sidereal time computation in rad/s (0.985612288 deg/s). */
    private static final double VST1 = 0.17202179573714597e-1;

    /** Veis sidereal time derivative in rad/s. */
    private static final double VSTD = 7.292115146705209e-5;

    /** Set of time scales to use. */
    private final transient TimeScales timeScales;

    /** Reference date. */
    private final AbsoluteDate vstReference;

    /**
     * Constructor for the singleton.
     *
     * @param timeScales to use when computing the transform.
     */
    VEISProvider(final TimeScales timeScales) {
        this.timeScales = timeScales;
        this.vstReference =
                new AbsoluteDate(DateComponents.FIFTIES_EPOCH, timeScales.getTAI());
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {

        // offset from FIFTIES epoch (UT1 scale)
        final double dtai = date.durationFrom(vstReference);
        final double dutc = timeScales.getUTC().offsetFromTAI(date);
        final double dut1 = 0.0; // fixed at 0 since Veis parent is GTOD frame WITHOUT EOP corrections

        final double tut1 = dtai + dutc + dut1;
        final double ttd  = tut1 / Constants.JULIAN_DAY;
        final double rdtt = ttd - (int) ttd;

        // compute Veis sidereal time, in radians
        final double vst = (VST0 + VST1 * ttd + MathUtils.TWO_PI * rdtt) % MathUtils.TWO_PI;

        // compute angular rotation of Earth, in rad/s
        final Vector3D rotationRate = new Vector3D(-VSTD, Vector3D.PLUS_K);

        // set up the transform from parent GTOD
        return new Transform(date,
                             new Rotation(Vector3D.PLUS_K, vst, RotationConvention.VECTOR_OPERATOR),
                             rotationRate);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // offset from FIFTIES epoch (UT1 scale)
        final T dtai = date.durationFrom(vstReference);
        final double dutc = timeScales.getUTC().offsetFromTAI(date.toAbsoluteDate());
        final double dut1 = 0.0; // fixed at 0 since Veis parent is GTOD frame WITHOUT EOP corrections

        final T tut1 = dtai.add(dutc + dut1);
        final T ttd  = tut1.divide(Constants.JULIAN_DAY);
        final T rdtt = ttd.subtract((int) ttd.getReal());

        // compute Veis sidereal time, in radians
        final T vst = ttd.multiply(VST1).add(rdtt.multiply(MathUtils.TWO_PI)).add(VST0).remainder(MathUtils.TWO_PI);

        // compute angular rotation of Earth, in rad/s
        final FieldVector3D<T> rotationRate = new FieldVector3D<>(date.getField().getZero().add(-VSTD),
                                                                  Vector3D.PLUS_K);

        // set up the transform from parent GTOD
        return new FieldTransform<>(date,
                                    new FieldRotation<>(FieldVector3D.getPlusK(date.getField()), vst,
                                                        RotationConvention.VECTOR_OPERATOR),
                                    rotationRate);

    }

}
