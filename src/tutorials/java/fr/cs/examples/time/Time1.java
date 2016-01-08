/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package fr.cs.examples.time;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for dates support.
 * <p>This tutorial shows basic date usage.</p>
 * @author Luc Maisonobe
 */
public class Time1 {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // get the UTC and TAI time scales
            TimeScale utc = TimeScalesFactory.getUTC();
            TimeScale tai = TimeScalesFactory.getTAI();

            // create a start date from its calendar components in UTC time scale
            AbsoluteDate start = new AbsoluteDate(2005, 12, 31, 23, 59, 50, utc);

            // create an end date 20 seconds after the start date
            double duration = 20.0;
            AbsoluteDate end = start.shiftedBy(duration);

            // output header line
            System.out.println("        UTC date                  TAI date");

            // loop from start to end using a one minute step
            // (a leap second was introduced this day, so the display should show
            //  the rare case of an UTC minute with more than 60 seconds)
            double step = 0.5;
            for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(step)) {
                System.out.println(date.toString(utc) + "   " + date.toString(tai));
            }

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }
}
