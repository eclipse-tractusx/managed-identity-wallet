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

package org.eclipse.tractusx.managedidentitywallets.config.security;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dto.ValidationResult;
import org.eclipse.tractusx.managedidentitywallets.service.STSTokenValidationService;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COMA_SEPARATOR;

public class PresentationIatpFilter extends GenericFilterBean {

    RequestMatcher customFilterUrl = new AntPathRequestMatcher(RestURI.API_PRESENTATIONS_IATP);

    STSTokenValidationService validationService;

    public PresentationIatpFilter(STSTokenValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (customFilterUrl.matches(httpServletRequest)) {
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (StringUtils.isEmpty(authHeader)) {
                httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                ValidationResult result = validationService.validateToken(authHeader);
                if (!result.isValid()) {
                    List<String> errorValues = new ArrayList<>();
                    result.getErrors().forEach(c -> errorValues.add(c.name()));
                    String content = String.join(COMA_SEPARATOR, errorValues);

                    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpServletResponse.setContentLength(content.length());
                    httpServletResponse.getWriter().write(content);
                } else {
                    chain.doFilter(request, response);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
