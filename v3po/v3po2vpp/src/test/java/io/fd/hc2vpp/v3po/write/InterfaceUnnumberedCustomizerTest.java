/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.write;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.InterfaceUnnumberedAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceUnnumberedCustomizerTest extends AbstractUnnumberedCustomizerTest {
    private static final String UNNUMBERED_IFC_NAME = "eth2";
    private static final int UNNUMBERED_IFC_ID = 2;
    private static final InstanceIdentifier<Unnumbered> UNNUMBERED_IFC_IID = InstanceIdentifier.create(Interfaces.class)
        .child(Interface.class, new InterfaceKey(UNNUMBERED_IFC_NAME))
        .augmentation(InterfaceUnnumberedAugmentation.class)
        .child(Unnumbered.class);

    @Override
    protected int getUnnumberedIfcId() {
        return UNNUMBERED_IFC_ID;
    }

    @Override
    protected String getUnnumberedIfcName() {
        return UNNUMBERED_IFC_NAME;
    }

    @Override
    protected InstanceIdentifier<Unnumbered> getUnnumberedIfcIId() {
        return UNNUMBERED_IFC_IID;
    }

    @Override
    protected AbstractUnnumberedCustomizer getCustomizer() {
        return new InterfaceUnnumberedCustomizer(api, new NamingContext("ifc-prefix", IFC_CTX_NAME));
    }

}
