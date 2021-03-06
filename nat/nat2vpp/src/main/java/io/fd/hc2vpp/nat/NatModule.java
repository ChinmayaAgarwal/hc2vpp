/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.nat;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.nat.jvpp.JVppNatProvider;
import io.fd.hc2vpp.nat.read.NatReaderFactory;
import io.fd.hc2vpp.nat.read.ifc.IfcNatReaderFactory;
import io.fd.hc2vpp.nat.read.ifc.SubIfcNatReaderFactory;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.hc2vpp.nat.write.NatWriterFactory;
import io.fd.hc2vpp.nat.write.ifc.IfcNatWriterFactory;
import io.fd.hc2vpp.nat.write.ifc.SubIfcNatWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module class instantiating nat plugin components.
 */
public class NatModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(NatModule.class);
    private final Class<? extends Provider<FutureJVppNatFacade>> jvppNatProviderClass;

    public NatModule() {
        this(JVppNatProvider.class);
    }

    @VisibleForTesting
    protected NatModule(Class<? extends Provider<FutureJVppNatFacade>> jvppNatProvider) {
        this.jvppNatProviderClass = jvppNatProvider;
    }

    @Override
    protected void configure() {
        // Mapping entry context util
        bind(MappingEntryContext.class).toInstance(new MappingEntryContext());

        LOG.debug("Installing NAT module");

        // Bind to Plugin's JVPP
        bind(FutureJVppNatFacade.class).toProvider(jvppNatProviderClass).in(Singleton.class);

        final Multibinder<ReaderFactory> readBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readBinder.addBinding().to(IfcNatReaderFactory.class).in(Singleton.class);
        readBinder.addBinding().to(SubIfcNatReaderFactory.class).in(Singleton.class);
        readBinder.addBinding().to(NatReaderFactory.class).in(Singleton.class);

        final Multibinder<WriterFactory> writeBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writeBinder.addBinding().to(IfcNatWriterFactory.class).in(Singleton.class);
        writeBinder.addBinding().to(SubIfcNatWriterFactory.class).in(Singleton.class);
        writeBinder.addBinding().to(NatWriterFactory.class).in(Singleton.class);
        LOG.info("Module NAT successfully configured");
    }
}
