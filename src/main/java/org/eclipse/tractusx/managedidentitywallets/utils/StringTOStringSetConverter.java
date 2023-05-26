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

package org.eclipse.tractusx.managedidentitywallets.utils;

import jakarta.persistence.AttributeConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class StringTOStringSetConverter implements AttributeConverter<Set<String>, String> {

    private static final String SPLIT_CHAR = ",";

    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        if (CollectionUtils.isEmpty(attribute)) {
            return org.apache.commons.lang3.StringUtils.EMPTY;
        }
        return StringUtils.collectionToDelimitedString(attribute, SPLIT_CHAR);
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasLength(dbData)) {
            return Collections.emptySet();
        }
        return Arrays.stream(dbData.split(SPLIT_CHAR)).map(String::trim).collect(Collectors.toSet());
    }
}
