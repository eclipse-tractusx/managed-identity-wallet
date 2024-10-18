/*
 * *******************************************************************************
 *  Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.managedidentitywallets.revocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.exception.RevocationException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * The type Revocation error decoder.
 */
public class RevocationErrorDecoder implements ErrorDecoder {
    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        Response.Body responseBody = response.body();
        HttpStatus responseStatus = HttpStatus.valueOf(response.status());
        if (responseBody != null && response.body() != null) {
            String data = StreamUtils.copyToString(response.body().asInputStream(), Charset.defaultCharset());
            if (responseStatus.value() == HttpStatus.CONFLICT.value()) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> map = objectMapper.readValue(data, Map.class);
                if (map.containsKey("detail")) {
                    throw new RevocationException(responseStatus.value(), map.get("detail").toString(), map);
                } else {
                    throw new RevocationException(responseStatus.value(), data, map);
                }
            } else {
                throw new RevocationException(responseStatus.value(), data, Map.of());

            }
        } else {
            throw new RevocationException(responseStatus.value(), "Error in revocation service", Map.of());
        }
    }
}
