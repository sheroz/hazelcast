/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.partition.impl;

import com.hazelcast.internal.partition.InternalPartition;
import com.hazelcast.internal.partition.PartitionListener;
import com.hazelcast.internal.partition.PartitionReplica;
import com.hazelcast.nio.Address;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;

public class InternalPartitionImpl implements InternalPartition {

    @SuppressFBWarnings(value = "VO_VOLATILE_REFERENCE_TO_ARRAY", justification =
            "The contents of this array will never be updated, so it can be safely read using a volatile read."
                    + " Writing to `replicas` is done under InternalPartitionServiceImpl.lock,"
                    + " so there's no need to guard `replicas` field or to use a CAS.")
    private volatile PartitionReplica[] replicas = new PartitionReplica[MAX_REPLICA_COUNT];
    private final int partitionId;
    private final PartitionListener partitionListener;
    private volatile PartitionReplica localReplica;
    private volatile boolean isMigrating;

    InternalPartitionImpl(int partitionId, PartitionListener partitionListener, PartitionReplica localReplica) {
        assert localReplica != null;
        this.partitionId = partitionId;
        this.partitionListener = partitionListener;
        this.localReplica = localReplica;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public InternalPartitionImpl(int partitionId, PartitionListener listener, PartitionReplica localReplica,
            PartitionReplica[] replicas) {
        this(partitionId, listener, localReplica);
        this.replicas = replicas;
    }

    @Override
    public int getPartitionId() {
        return partitionId;
    }

    @Override
    public boolean isMigrating() {
        return isMigrating;
    }

    public void setMigrating(boolean isMigrating) {
        this.isMigrating = isMigrating;
    }

    @Override
    public boolean isLocal() {
        return localReplica.equals(getOwnerReplicaOrNull());
    }

    @Override
    public Address getOwnerOrNull() {
        return getAddress(replicas[0]);
    }

    @Override
    public PartitionReplica getOwnerReplicaOrNull() {
        return replicas[0];
    }

    @Override
    public Address getReplicaAddress(int replicaIndex) {
        PartitionReplica member = replicas[replicaIndex];
        return getAddress(member);
    }

    @Override
    public PartitionReplica getReplica(int replicaIndex) {
        return replicas[replicaIndex];
    }

    /** Swaps the replicas for {@code index1} and {@code index2} and call the partition listeners */
    void swapReplicas(int index1, int index2) {
        PartitionReplica[] newReplicas = Arrays.copyOf(replicas, MAX_REPLICA_COUNT);

        PartitionReplica a1 = newReplicas[index1];
        PartitionReplica a2 = newReplicas[index2];
        newReplicas[index1] = a2;
        newReplicas[index2] = a1;

        replicas = newReplicas;
        callPartitionListener(index1, a1, a2);
        callPartitionListener(index2, a2, a1);
    }

    // Not doing a defensive copy of given Address[]
    // This method is called under InternalPartitionServiceImpl.lock,
    // so there's no need to guard `addresses` field or to use a CAS.
    void setInitialReplicas(PartitionReplica[] newReplicas) {
        PartitionReplica[] oldReplicas = replicas;
        for (int replicaIndex = 0; replicaIndex < MAX_REPLICA_COUNT; replicaIndex++) {
            if (oldReplicas[replicaIndex] != null) {
                throw new IllegalStateException("Partition is already initialized!");
            }
        }
        replicas = newReplicas;
    }

    // Not doing a defensive copy of given Address[]
    // This method is called under InternalPartitionServiceImpl.lock,
    // so there's no need to guard `addresses` field or to use a CAS.
    void setReplicas(PartitionReplica[] newReplicas) {
        PartitionReplica[] oldReplicas = replicas;
        replicas = newReplicas;
        callPartitionListener(newReplicas, oldReplicas);
    }

    void setReplica(int replicaIndex, PartitionReplica newReplica) {
        PartitionReplica[] newReplicas = Arrays.copyOf(replicas, MAX_REPLICA_COUNT);
        PartitionReplica oldReplica = newReplicas[replicaIndex];
        newReplicas[replicaIndex] = newReplica;
        replicas = newReplicas;
        callPartitionListener(replicaIndex, oldReplica, newReplica);
    }

    /** Calls the partition listener for all changed addresses. */
    private void callPartitionListener(PartitionReplica[] newReplicas, PartitionReplica[] oldReplicas) {
        if (partitionListener != null) {
            for (int replicaIndex = 0; replicaIndex < MAX_REPLICA_COUNT; replicaIndex++) {
                PartitionReplica oldReplicasId = oldReplicas[replicaIndex];
                PartitionReplica newReplicasId = newReplicas[replicaIndex];
                callPartitionListener(replicaIndex, oldReplicasId, newReplicasId);
            }
        }
    }

    /** Sends a {@link PartitionReplicaChangeEvent} if the address has changed. */
    private void callPartitionListener(int replicaIndex, PartitionReplica oldReplica, PartitionReplica newReplica) {
        boolean changed;
        if (oldReplica == null) {
            changed = newReplica != null;
        } else {
            changed = !oldReplica.equals(newReplica);
        }
        if (changed) {
            PartitionReplicaChangeEvent event
                    = new PartitionReplicaChangeEvent(partitionId, replicaIndex, getAddress(oldReplica), getAddress(newReplica));
            partitionListener.replicaChanged(event);
        }
    }

    private static Address getAddress(PartitionReplica replica) {
        return replica != null ? replica.address() : null;
    }

    InternalPartitionImpl copy(PartitionListener listener) {
        return new InternalPartitionImpl(partitionId, listener, localReplica, Arrays.copyOf(replicas, MAX_REPLICA_COUNT));
    }

    PartitionReplica[] getReplicas() {
        return replicas;
    }

    @Override
    public boolean isOwnerOrBackup(Address address) {
        if (address == null) {
            return false;
        }

        for (int i = 0; i < MAX_REPLICA_COUNT; i++) {
            if (address.equals(getAddress(replicas[i]))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getReplicaIndex(PartitionReplica replica) {
        return getReplicaIndex(replicas, replica);
    }

    public boolean isOwnerOrBackup(PartitionReplica replica) {
        return getReplicaIndex(replicas, replica) >= 0;
    }

    /**
     * Returns the index of the {@code replica} in {@code replicas} or -1 if the {@code replica} is {@code null} or
     * not present.
     */
    static int getReplicaIndex(PartitionReplica[] replicas, PartitionReplica replica) {
        if (replica == null) {
            return -1;
        }

        for (int i = 0; i < MAX_REPLICA_COUNT; i++) {
            if (replica.equals(replicas[i])) {
                return i;
            }
        }
        return -1;
    }

    int replaceReplica(PartitionReplica oldReplica, PartitionReplica newReplica) {
        for (int i = 0; i < MAX_REPLICA_COUNT; i++) {
            PartitionReplica currentReplica = replicas[i];
            if (currentReplica == null) {
                break;
            }

            if (currentReplica.equals(oldReplica)) {
                PartitionReplica[] newReplicas = Arrays.copyOf(replicas, MAX_REPLICA_COUNT);
                newReplicas[i] = newReplica;
                replicas = newReplicas;
                callPartitionListener(i, oldReplica, newReplica);
                return i;
            }
        }
        return -1;
    }

    void reset(PartitionReplica localReplica) {
        assert localReplica != null;
        this.replicas = new PartitionReplica[MAX_REPLICA_COUNT];
        this.localReplica = localReplica;
        setMigrating(false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Partition [").append(partitionId).append("]{\n");
        for (int i = 0; i < MAX_REPLICA_COUNT; i++) {
            PartitionReplica replica = replicas[i];
            if (replica != null) {
                sb.append('\t');
                sb.append(i).append(":").append(replica);
                sb.append("\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
