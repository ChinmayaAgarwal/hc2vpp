/*
 * Copyright (c) 2019 PANTHEON.tech.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.hc2vpp.l3.write.ipv6.nd;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev190527.interfaces._interface.ipv6.nd.proxies.NdProxy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NdProxyValidator implements Validator<NdProxy> {

    public NdProxyValidator(final NamingContext ifcNamingContext) {
        checkNotNull(ifcNamingContext, "Interface context cannot be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<NdProxy> id, @Nonnull final NdProxy dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        // there is nothing to validate yet
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<NdProxy> id, @Nonnull final NdProxy dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        // there is nothing to validate yet
    }
}
