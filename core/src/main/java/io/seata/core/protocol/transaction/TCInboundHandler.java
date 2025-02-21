/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.core.protocol.transaction;

import io.seata.core.rpc.RpcContext;

/**
 * The interface Tc inbound handler.
 *
 */
public interface TCInboundHandler {

    /**
     * Handle global begin response.
     *
     * @param globalBegin the global begin
     * @param rpcContext  the rpc context
     * @return the global begin response
     */
    GlobalBeginResponse handle(GlobalBeginRequest globalBegin, RpcContext rpcContext);

    /**
     * Handle global commit response.
     *
     * @param globalCommit the global commit
     * @param rpcContext   the rpc context
     * @return the global commit response
     */
    GlobalCommitResponse handle(GlobalCommitRequest globalCommit, RpcContext rpcContext);

    /**
     * Handle global rollback response.
     *
     * @param globalRollback the global rollback
     * @param rpcContext     the rpc context
     * @return the global rollback response
     */
    GlobalRollbackResponse handle(GlobalRollbackRequest globalRollback, RpcContext rpcContext);

    /**
     * Handle branch register response.
     *
     * @param branchRegister the branch register
     * @param rpcContext     the rpc context
     * @return the branch register response
     */
    BranchRegisterResponse handle(BranchRegisterRequest branchRegister, RpcContext rpcContext);

    /**
     * Handle branch report response.
     *
     * @param branchReport the branch report
     * @param rpcContext   the rpc context
     * @return the branch report response
     */
    BranchReportResponse handle(BranchReportRequest branchReport, RpcContext rpcContext);

    /**
     * Handle global lock query response.
     *
     * @param checkLock  the check lock
     * @param rpcContext the rpc context
     * @return the global lock query response
     */
    GlobalLockQueryResponse handle(GlobalLockQueryRequest checkLock, RpcContext rpcContext);

    /**
     * Handle global status response.
     *
     * @param globalStatus the global status
     * @param rpcContext   the rpc context
     * @return the global status response
     */
    GlobalStatusResponse handle(GlobalStatusRequest globalStatus, RpcContext rpcContext);

    /**
     * Handle global report request.
     *
     * @param globalReport the global report request
     * @param rpcContext   the rpc context
     * @return the global report response
     */
    GlobalReportResponse handle(GlobalReportRequest globalReport, RpcContext rpcContext);

}
