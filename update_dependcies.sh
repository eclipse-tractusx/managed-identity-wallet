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

#!/bin/bash

# download the latest version of the Eclipse Dash License tool
curl --output org.eclipse.dash.licenses.jar \
  https://repo.eclipse.org/service/local/repositories/dash-licenses-snapshots/content/org/eclipse/dash/org.eclipse.dash.licenses/1.0.3-SNAPSHOT/org.eclipse.dash.licenses-1.0.3-20230725.055026-63.jar

# update DEPENDENCIES file
./gradlew dependencies | grep -Poh "(?<=\s)[\w\.-]+:[\w\.-]+:[^:\s]+" | grep -v "^org\.eclipse" | sort | uniq |
  java -jar org.eclipse.dash.licenses.jar -summary DEPENDENCIES - |
  grep restricted
