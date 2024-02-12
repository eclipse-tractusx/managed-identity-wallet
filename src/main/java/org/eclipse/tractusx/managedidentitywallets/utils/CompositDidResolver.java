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

package org.eclipse.tractusx.managedidentitywallets.utils;

import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolverException;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CompositDidResolver implements DidResolver {
    DidResolver[] didResolvers;

    public CompositDidResolver(DidResolver... didResolvers) {
        this.didResolvers = didResolvers;
    }

    public DidDocument resolve(Did did) throws DidResolverException {
        DidResolver[] var2 = this.didResolvers;
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            DidResolver didResolver = var2[var4];
            if (didResolver.isResolvable(did)) {
                try {
                    DidDocument result = didResolver.resolve(did);
                    if (result != null) {
                        return result;
                    }
                } catch (DidResolverException var7) {
                    throw var7;
                } catch (Throwable var8) {
                    throw new DidResolverException(String.format("Unrecognized exception: %s", var8.getClass().getName()), var8);
                }
            }
        }

        return null;
    }

    public boolean isResolvable(Did did) {
        return Arrays.stream(this.didResolvers).anyMatch((resolver) -> resolver.isResolvable(did));
    }

    public static org.eclipse.tractusx.ssi.lib.did.resolver.CompositeDidResolver append(DidResolver target, DidResolver toBeAppended) {
        return new org.eclipse.tractusx.ssi.lib.did.resolver.CompositeDidResolver(target, toBeAppended);
    }
}

