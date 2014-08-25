/*
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.continuuity.loom.macro.eval;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.macro.IncompleteClusterException;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to an ip address on the specified node.
 */
public class IPSelfEvaluator implements Evaluator {
  private final String ipType;

  public IPSelfEvaluator(String ipType) {
    this.ipType = ipType;
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    String ip = node.getProperties().getIPAddress(ipType);
    if (ip == null) {
      throw new IncompleteClusterException("node " + node.getId() + " has no ip for macro expansion.");
    }
    return ImmutableList.of(ip);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IPSelfEvaluator that = (IPSelfEvaluator) o;

    return Objects.equal(ipType, that.ipType);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ipType);
  }
}