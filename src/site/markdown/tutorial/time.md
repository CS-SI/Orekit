<!--- Copyright 2002-2016 CS Systèmes d'Information
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Time

This tutorial shows how to handle time in Orekit.

## Basic use

The first example will be devoted to basic use: creating dates,
comparing dates and printing dates. We will build a small program
that loops between a start and an end date with a small step and
print it in two different time scales: UTC and TAI. The loop will
be around a leap second introduction to see the offset change
in the two time scales.

The complete code for this example can be found in the source
tree of the library, in file `src/tutorials/fr/cs/examples/time/Time1.java`.

As explained in the [ time section](../architecture/time.html) of the
library architecture documentation, dates are defined with respect to
time scales. We first get the two time scales we need. These scales are
singletons, so we do not build them but retrieve the singletons instances
as follows:

    TimeScale utc = TimeScalesFactory.getUTC();
    TimeScale tai = TimeScalesFactory.getTAI();

Once we have the UTC time scale available, we can create the start date from
its components with respect to the UTC time scale:

    AbsoluteDate start = new AbsoluteDate(2005, 12, 31, 23, 59, 50, utc);

For the end date of our loop, we will use a relative definition instead of
an absolute definition. We want the end date to be 20 seconds after the
start date.
  
    double duration = 20.0;
    AbsoluteDate end = new AbsoluteDate(start, duration);

We then print a header for our output.
  
    System.out.println("        UTC date                  TAI date");

Now comes the real processing: the loop. Since AbsoluteDate instances
are immutable, we use a new instance at each iteration. For the first
iteration, we simply reuse the start date, and for other iterations
we create an instance relative to the previous one.
  
    double step = 0.5;
    for (AbsoluteDate date = start;
         date.compareTo(end) < 0;
         date = new AbsoluteDate(date, step)) {
       // loop body
    }

Inside the loop body (i.e. replacing the comment in the code snippet above),
we print the date in the two different time scales. One important thing to
notice is that we use only _one_ instance of AbsoluteDate, but can locate
it with respect to several different time scales.
  
    System.out.println(date.toString(utc) + "   " + date.toString(tai));

Since the loop brackets a leap second introduction, we see two different things
in the output of this program. First, the middle iterations show the last minute
of the day in UTC time scale has more than 60 seconds: we see dates 2005-12-31T23:59:60.000
and 2005-12-31T23:59:60.500. Second, we see the offset between the two time scales
grow from 32 seconds at the start of the loop to 33 seconds at the end of the loop:
the leap second has been taken into account.
  
            UTC date                  TAI date
    2005-12-31T23:59:50.000   2006-01-01T00:00:22.000
    2005-12-31T23:59:50.500   2006-01-01T00:00:22.500
              ... 16 lines deleted here ...
    2005-12-31T23:59:59.000   2006-01-01T00:00:31.000
    2005-12-31T23:59:59.500   2006-01-01T00:00:31.500
    2005-12-31T23:59:60.000   2006-01-01T00:00:32.000
    2005-12-31T23:59:60.500   2006-01-01T00:00:32.500
    2006-01-01T00:00:00.000   2006-01-01T00:00:33.000
    2006-01-01T00:00:00.500   2006-01-01T00:00:33.500
              ... 14 lines deleted here ...
    2006-01-01T00:00:08.000   2006-01-01T00:00:41.000
    2006-01-01T00:00:08.500   2006-01-01T00:00:41.500


  