/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class EstimationArchUnitTest {

    private static final String MEASUREMENTS_NAME = "..measurements..";
    private static final String SEQUENTIAL_NAME = "..sequential..";
    private static final String IOD_NAME = "..iod..";
    private static final String LEAST_SQUARES_NAME = "..leastsquares..";

    // GIVEN
    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.orekit.estimation");

    @Test
    void testNoClassesMeasurementsPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(MEASUREMENTS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SEQUENTIAL_NAME, IOD_NAME, LEAST_SQUARES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesIodPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(IOD_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(LEAST_SQUARES_NAME, SEQUENTIAL_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesLeastSquaresPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(LEAST_SQUARES_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SEQUENTIAL_NAME, IOD_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesSequentialPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(SEQUENTIAL_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(LEAST_SQUARES_NAME, IOD_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }
}
