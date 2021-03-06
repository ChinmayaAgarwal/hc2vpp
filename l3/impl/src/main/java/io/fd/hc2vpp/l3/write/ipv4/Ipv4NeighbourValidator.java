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

package io.fd.hc2vpp.l3.write.ipv4;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4NeighbourValidator implements Validator<Neighbor> {

    public Ipv4NeighbourValidator(final NamingContext ifcNamingContext) {
        checkNotNull(ifcNamingContext, "Interface context cannot be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Neighbor> id, @Nonnull final Neighbor dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        checkNeighborData(id, dataAfter);

    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Neighbor> id, @Nonnull final Neighbor dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        checkNeighborData(id, dataBefore);
    }

    private void checkNeighborData(final InstanceIdentifier<Neighbor> id, final Neighbor data) {
        checkArgument(id.firstKeyOf(Interface.class) != null, "No parent interface key found");
        checkNotNull(data.getIp(), "Neighbor IP address cannot be null");
        checkNotNull(data.getLinkLayerAddress(), "Neighbor MAC acddress cannot be null");
    }
}
