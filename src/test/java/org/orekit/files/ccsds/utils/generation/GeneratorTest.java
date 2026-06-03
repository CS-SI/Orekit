/* Copyright 2002-2026 CS GROUP
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
package org.orekit.files.ccsds.utils.generation;

import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Formatter;
import org.orekit.utils.units.Unit;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratorTest {

    /**
     * Test added for coverage of {@link Generator#dateToString(DateTimeComponents)} default interface method.
     */
    @Test
    void testDateToString() {
        // GIVEN
        // Create date
        final String       referenceDateString = "2026-05-21T23:04:01.000";
        final TimeScale    tai                 = TimeScalesFactory.getTAI();
        final AbsoluteDate date                = new AbsoluteDate(referenceDateString, tai);
        DateTimeComponents components          = date.getComponents(tai);

        // Create generator
        final FakeGenerator generator = new FakeGenerator();

        // WHEN
        generator.dateToString(components);

        // THEN
        assertEquals(2026, generator.getCapturedYear());
        assertEquals(5, generator.getCapturedMonth());
        assertEquals(21, generator.getCapturedDay());
        assertEquals(23, generator.getCapturedHour());
        assertEquals(4, generator.getCapturedMinute());
        assertEquals(1, generator.getCapturedSeconds());
    }

    /**
     * A mock implementation of the {@link Generator} interface. This class is used for testing or placeholder purposes.
     * It captures certain input values for later retrieval and provides empty or default implementations for all
     * interface methods.
     */
    private static class FakeGenerator implements Generator {

        private int    capturedYear;
        private int    capturedMonth;
        private int    capturedDay;
        private int    capturedHour;
        private int    capturedMinute;
        private double capturedSeconds;

        @Override
        public String getOutputName() {
            return "";
        }

        @Override
        public FileFormat getFormat() {
            return null;
        }

        @Override
        public Formatter getFormatter() {
            return null;
        }

        @Override
        public void startMessage(String root, String messageTypeKey, double version) throws IOException {

        }

        @Override
        public void endMessage(String root) throws IOException {

        }

        @Override
        public void writeComments(List<String> comments) throws IOException {

        }

        @Override
        public void writeEntry(String key, String value, Unit unit, boolean mandatory) throws IOException {

        }

        @Override
        public void writeEntry(String key, List<String> value, boolean mandatory) throws IOException {

        }

        @Override
        public void writeEntry(String key, Enum<?> value, boolean mandatory) throws IOException {

        }

        @Override
        public void writeEntry(String key, TimeConverter converter, AbsoluteDate date, boolean forceCalendar, boolean mandatory) throws IOException {

        }

        @Override
        public void writeEntry(String key, char value, boolean mandatory) throws IOException {

        }

        @Override
        public void writeEntry(String key, int value, boolean mandatory) throws IOException {

        }

        @Override
        public void writeEntry(String key, double value, Unit unit, boolean mandatory) throws IOException {

        }

        @Override
        public void writeEntry(String key, Double value, Unit unit, boolean mandatory) throws IOException {

        }

        @Override
        public void newLine() throws IOException {

        }

        @Override
        public void writeRawData(char data) throws IOException {

        }

        @Override
        public void writeRawData(CharSequence data) throws IOException {

        }

        @Override
        public void enterSection(String name) throws IOException {

        }

        @Override
        public String exitSection() throws IOException {
            return "";
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public String dateToString(TimeConverter converter, AbsoluteDate date) {
            return "";
        }

        @Override
        public String dateToCalendarString(TimeConverter converter, AbsoluteDate date) {
            return "";
        }

        @Override
        public String dateToString(int year, int month, int day, int hour, int minute, double seconds) {
            capturedYear    = year;
            capturedMonth   = month;
            capturedDay     = day;
            capturedHour    = hour;
            capturedMinute  = minute;
            capturedSeconds = seconds;
            return "";
        }

        @Override
        public String doubleToString(double value) {
            return "";
        }

        @Override
        public String unitsListToString(List<Unit> units) {
            return "";
        }

        @Override
        public String siToCcsdsName(String siName) {
            return "";
        }

        public int getCapturedYear() {
            return capturedYear;
        }

        public int getCapturedMonth() {
            return capturedMonth;
        }

        public int getCapturedDay() {
            return capturedDay;
        }

        public int getCapturedHour() {
            return capturedHour;
        }

        public int getCapturedMinute() {
            return capturedMinute;
        }

        public double getCapturedSeconds() {
            return capturedSeconds;
        }
    }

}