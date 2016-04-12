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

package io.fd.honeycomb.v3po.impl.data;

import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.CANCELED;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.COMMITED;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.FAILED;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.NEW;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.SUBMITED;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.fd.honeycomb.v3po.impl.trans.VppException;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class VppWriteTransaction implements DOMDataWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(VppWriteTransaction.class);
    private final VppDataTree configDataTree;
    private DataTreeModification modification;
    private TransactionStatus status;

    VppWriteTransaction(@Nonnull final VppDataTree configDataTree,
                        @Nonnull final VppDataTreeSnapshot configSnapshot) {
        this.configDataTree = Preconditions.checkNotNull(configDataTree, "configDataTree should not be null");
        Preconditions.checkNotNull(configSnapshot, "configSnapshot should not be null");
        // initialize transaction state:
        modification = configSnapshot.newModification();
        status = NEW;
    }

    VppWriteTransaction(@Nonnull final VppDataTree configDataTree) {
        this(configDataTree, configDataTree.takeSnapshot());
    }

    private static void checkConfigurationWrite(final LogicalDatastoreType store) {
        Preconditions.checkArgument(LogicalDatastoreType.CONFIGURATION == store, "Write is not supported for operational data store");
    }

    private void checkIsNew() {
        Preconditions.checkState(status == NEW, "Transaction was submitted or canceled");
        Preconditions.checkState(modification != null, "VPPDataTree modification should not be null");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        LOG.debug("VppWriteTransaction.put() store={}, path={}, data={}", store, path, data);
        checkIsNew();
        checkConfigurationWrite(store);
        modification.write(path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        LOG.debug("VppWriteTransaction.merge() store={}, path={}, data={}", store, path, data);
        checkIsNew();
        checkConfigurationWrite(store);
        modification.merge(path, data);
    }

    @Override
    public boolean cancel() {
        if (status != NEW) {
            // only NEW transactions can be cancelled
            return false;
        } else {
            status = CANCELED;
            modification = null;
            return true;
        }
    }

    @Override
    public void delete(LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("VppWriteTransaction.delete() store={}, path={}", store, path);
        checkIsNew();
        checkConfigurationWrite(store);
        modification.delete(path);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        LOG.debug("VppWriteTransaction.submit()");
        checkIsNew();

        // seal transaction:
        modification.ready();
        status = SUBMITED;

        try {
            configDataTree.commit(modification);
            status = COMMITED;
        } catch (DataValidationFailedException | VppException e) {
            status = FAILED;
            LOG.error("Failed to commit VPP state modification", e);
            return Futures.immediateFailedCheckedFuture(
                    new TransactionCommitFailedException("Failed to validate DataTreeModification", e));
        } finally {
            modification = null;
        }
        return Futures.immediateCheckedFuture(null);
    }

    @Override
    @Deprecated
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        throw new UnsupportedOperationException("deprecated");
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
