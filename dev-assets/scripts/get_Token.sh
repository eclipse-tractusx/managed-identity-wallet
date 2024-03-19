#!/usr/bin/env bash
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

curl --location "http://localhost:8080/realms/miw_test/protocol/openid-connect/token" \
     --header "Content-Type: application/x-www-form-urlencoded" \
     --data-urlencode "client_id=miw_private_client" \
     --data-urlencode "client_secret=miw_private_client" \
     --data-urlencode "grant_type=password" \
     --data-urlencode "username=catena-x" \
     --data-urlencode "password=password" \
     | jq -r ".access_token"
