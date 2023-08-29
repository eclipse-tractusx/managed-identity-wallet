#!/bin/bash
#
# /********************************************************************************
#  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0.
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
# ********************************************************************************/
#

# download the latest version of the Eclipse Dash License tool
if [[ -f "dash.jar" ]]; then
  echo "'dash.jar' already downloaded. Remove manually to get the newest version."
else
  # '-L' follow redirects
  # '--output' name the downloaded file 'dash.jar'
  curl -L --output dash.jar \
  "https://repo.eclipse.org/service/local/artifact/maven/redirect?r=dash-licenses&g=org.eclipse.dash&a=org.eclipse.dash.licenses&v=LATEST"
fi

# use '-q' to get the raw output of the command (contained in build.gradle)
./gradlew -q dashDependencies |
  # run the dash tool and output the summary into the 'DEPENDENCIES' file
  java -jar dash.jar -summary DEPENDENCIES - |
  # output any restricted dependencies
  grep restricted
