/*
 * *******************************************************************************
 *  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ******************************************************************************
 */

package org.eclipse.tractusx.managedidentitywallets.test.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.collections.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The type Report aggregator.
 * This class combine multiple cucumber report json file into one json file
 */
public class ReportAggregator {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException the io exception
     */
    public static void main(String[] args) throws IOException {
        //Note: sout put here intentionally to print information while running gradle task
        System.out.println("ReportAggregator with argument -> " + Arrays.toString(args));
        ObjectMapper objectMapper = new ObjectMapper();
        File reportFolder = new File(args[0]);
        System.out.println("Report folder  -> " + reportFolder.isDirectory() + " " + reportFolder.exists());
        List<Path> reportFiles = Files.walk(reportFolder.toPath()).filter(file -> file.toFile().getName().endsWith(".json")).toList();
        List<Map<String, Object>> finalReportList = new ArrayList<>();
        if (CollectionUtils.hasElements(reportFiles)) {
            System.out.println("Total report file ->" + reportFiles.size());
            reportFiles.forEach(path -> {
                try {

                    String jsonString = Files.readString(path, Charset.defaultCharset());
                    TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<>() {
                    };
                    List<Map<String, Object>> hashMaps = objectMapper.readValue(jsonString, typeRef);
                    finalReportList.addAll(hashMaps);
                } catch (IOException e) {
                    System.err.println("Can not read file -> " + path.toFile().getAbsolutePath());
                }
            });

            if (CollectionUtils.hasElements(finalReportList)) {
                String finalReportFileName = reportFolder.getAbsolutePath() + "/final.json";
                File finalReportFile = new File(finalReportFileName);
                if (finalReportFile.exists()) {
                    boolean delete = finalReportFile.delete();
                    System.out.println("File is deleted -> " + delete);
                }

                Files.writeString(finalReportFile.toPath(), objectMapper.writeValueAsString(finalReportList), Charset.defaultCharset());

                System.out.println("Report file created");
            } else {
                System.err.println("Empty report is created");
                throw new ReportException("Empty report created");
            }
        } else {
            throw new ReportException("No report file found under folder ->" + reportFolder.getAbsolutePath());
        }
    }
}
