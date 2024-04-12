# /********************************************************************************
# * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
# *
# * See the NOTICE file(s) distributed with this work for additional
# * information regarding copyright ownership.
# *
# * This program and the accompanying materials are made available under the
# * terms of the Apache License, Version 2.0 which is available at
# * https://www.apache.org/licenses/LICENSE-2.0.
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# * License for the specific language governing permissions and limitations
# * under the License.
# *
# * SPDX-License-Identifier: Apache-2.0
# ********************************************************************************/

FROM eclipse-temurin:17-jre-alpine

# run as non-root user
RUN addgroup -g 11111 -S miw && adduser -u 11111 -S -s /bin/false -G miw miw

# add curl for healthcheck
RUN apk add curl

USER miw

COPY LICENSE NOTICE.md DEPENDENCIES SECURITY.md /miw-service/build/libs/miw-latest.jar /app/

WORKDIR /app

HEALTHCHECK --start-period=30s CMD curl --fail http://localhost:8090/actuator/health/liveness || exit 1

CMD ["java", "-jar", "miw-latest.jar"]
