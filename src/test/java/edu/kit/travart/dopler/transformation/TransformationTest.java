/*******************************************************************************
 * SPDX-License-Identifier: MPL-2.0
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 * <p>
 * Contributors:
 *    @author Yannick Kraml
 *    @author Kevin Feichtinger
 * <p>
 * Copyright 2024 Karlsruhe Institute of Technology (KIT)
 * KASTEL - Dependability of Software-intensive Systems
 *******************************************************************************/
package edu.kit.travart.dopler.transformation;

import at.jku.cps.travart.core.common.IModelTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TransformationTest<FromModel, ToModel> {

    private static final Pattern LINE_SEPARATOR = Pattern.compile("\\R");

    private static void assertModel(String expectedModel2, String transformedModel2) {
        Assertions.assertEquals(LINE_SEPARATOR.matcher(expectedModel2).replaceAll(System.lineSeparator()),
                LINE_SEPARATOR.matcher(transformedModel2).replaceAll(System.lineSeparator()));
    }

    /**
     * Compares the real model from the file with the transformed model.
     *
     * @param pathOfTheBeTransformedModel Expected model
     * @param pathOfExpectedModel         Real, transformed model
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("oneWayDataSourceMethod")
    @Execution(ExecutionMode.CONCURRENT)
    void testOneWayTransformation(Path pathOfTheBeTransformedModel, Path pathOfExpectedModel) throws Exception {
        FromModel modelToTransform = getFromModelFromPath(pathOfTheBeTransformedModel);
        String transformedModel = convertToModelToString(
                transformFromModelToToModel(modelToTransform, IModelTransformer.STRATEGY.ONE_WAY));

        //Remove comment in order to override the existing test data
        //Files.writeString(pathOfExpectedModel, transformedModel);

        String expectedModel = readToModelAsString(pathOfExpectedModel);
        assertModel(expectedModel, transformedModel);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTripDataSourceMethod")
    @Execution(ExecutionMode.CONCURRENT)
    void testRoundTripTransformation(Path path1, Path path2, Path path3) throws Exception {
        // first transformation
        FromModel modelToTransform = getFromModelFromPath(path1);
        String transformedModel = convertToModelToString(
                transformFromModelToToModel(modelToTransform, IModelTransformer.STRATEGY.ROUNDTRIP));

        //Remove comment in order to override the existing test data
        //Files.writeString(path2, transformedModel);

        String expectedModel = readToModelAsString(path2);
        assertModel(expectedModel, transformedModel);

        // second transformation
        ToModel modelToTransform2 = getToModelFromString(transformedModel);
        String transformedModel2 = convertFromModelToString(transformToModelToFromModel(modelToTransform2));

        //Remove comment in order to override the existing test data
        //Files.writeString(path3, transformedModel2);

        String expectedModel2 = readFromModelAsString(path3);
        assertModel(expectedModel2, transformedModel2);
    }

    /**
     * Generates the data for the test method.
     *
     * @return Set of Arguments. Each argument consists of the expected data from the file and the real transformed
     * model.
     */
    private Stream<Arguments> oneWayDataSourceMethod() throws IOException {

        //Collect files depending on getFromEnding()
        List<Path> filePathsSet;
        try (Stream<Path> filePaths = Files.walk(getOneWayDataPath())
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(getFromEnding()))) {
            filePathsSet = filePaths.toList();
        }

        //Create Arguments
        List<Arguments> arguments = new ArrayList<>();
        for (Path pathToBeTransformed : filePathsSet) {
            String pathToBeTransformedIn = pathToBeTransformed.toString().replace(getFromEnding(), getToEnding());
            arguments.add(Arguments.of(pathToBeTransformed, Path.of(pathToBeTransformedIn)));
        }

        return arguments.stream();
    }

    private Stream<Arguments> roundTripDataSourceMethod() throws IOException {

        //Collect files depending on getFromEnding()
        List<Path> filePathsSet;
        try (Stream<Path> filePaths = Files.walk(getRoundTripDataPath())
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".1" + getFromEnding()))) {
            filePathsSet = filePaths.toList();
        }

        //Create Arguments
        List<Arguments> arguments = new ArrayList<>();
        for (Path path1 : filePathsSet) {
            String path2 = path1.toString().replace(".1" + getFromEnding(), ".2" + getToEnding());
            String path3 = path1.toString().replace(".1" + getFromEnding(), ".3" + getFromEnding());
            arguments.add(Arguments.of(path1, Path.of(path2), Path.of(path3)));
        }

        return arguments.stream();
    }

    protected abstract Path getRoundTripDataPath();

    protected abstract String getToEnding();

    protected abstract String getFromEnding();

    protected abstract Path getOneWayDataPath();

    protected abstract FromModel getFromModelFromPath(Path path) throws Exception;

    protected abstract ToModel getToModelFromString(String model) throws Exception;

    protected abstract ToModel transformFromModelToToModel(FromModel modelToBeTransformed,
                                                           IModelTransformer.STRATEGY strategy) throws Exception;

    protected abstract FromModel transformToModelToFromModel(ToModel modelToBeTransformed) throws Exception;

    protected abstract String readToModelAsString(Path path) throws Exception;

    protected abstract String readFromModelAsString(Path path) throws Exception;

    protected abstract String convertToModelToString(ToModel toModel) throws Exception;

    protected abstract String convertFromModelToString(FromModel toModel) throws Exception;
}
