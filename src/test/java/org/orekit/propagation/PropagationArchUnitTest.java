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
package org.orekit.propagation;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PropagationArchUnitTest {

    private static final String ANALYTICAL_NAME = "..analytical..";
    private static final String NUMERICAL_NAME = "..numerical..";
    private static final String COVARIANCE_NAME = "..covariance..";
    private static final String INTEGRATION_NAME = "..integration..";
    private static final String EVENTS_NAME = "..events..";
    private static final String SAMPLING_NAME = "..sampling..";
    private static final String SEMI_ANALYTICAL_NAME = "..semianalytical..";

    // GIVEN
    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.orekit.propagation");

    @Test
    void testNoClassesSamplingPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(SAMPLING_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SEMI_ANALYTICAL_NAME, NUMERICAL_NAME,
                        INTEGRATION_NAME, ANALYTICAL_NAME, COVARIANCE_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesCovariancePackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(COVARIANCE_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(EVENTS_NAME, INTEGRATION_NAME, SEMI_ANALYTICAL_NAME,
                        ANALYTICAL_NAME, NUMERICAL_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesEventsPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(EVENTS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SEMI_ANALYTICAL_NAME, NUMERICAL_NAME,
                        ANALYTICAL_NAME, INTEGRATION_NAME, COVARIANCE_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesAnalyticalPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ANALYTICAL_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SEMI_ANALYTICAL_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesIntegrationPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(INTEGRATION_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SEMI_ANALYTICAL_NAME, NUMERICAL_NAME, COVARIANCE_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesSemiAnalyticalPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(SEMI_ANALYTICAL_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(NUMERICAL_NAME, ANALYTICAL_NAME, COVARIANCE_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesNumericalPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(NUMERICAL_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SEMI_ANALYTICAL_NAME, COVARIANCE_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }
}
