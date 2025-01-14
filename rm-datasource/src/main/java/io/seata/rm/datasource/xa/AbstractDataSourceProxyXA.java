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
package io.seata.rm.datasource.xa;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.PooledConnection;
import io.seata.rm.BaseDataSourceResource;

/**
 * Abstract DataSource proxy for XA mode.
 *
 */
public abstract class AbstractDataSourceProxyXA extends BaseDataSourceResource<ConnectionProxyXA> {

    protected static final String DEFAULT_RESOURCE_GROUP_ID = "DEFAULT_XA";

    /**
     * Get a ConnectionProxyXA instance for finishing XA branch(XA commit/XA rollback)
     * @return ConnectionProxyXA instance
     * @throws SQLException exception
     */
    public ConnectionProxyXA getConnectionForXAFinish(XAXid xaXid) throws SQLException {
        String xaBranchXid = xaXid.toString();
        ConnectionProxyXA connectionProxyXA = lookup(xaBranchXid);
        if (connectionProxyXA != null) {
            if (connectionProxyXA.getWrappedConnection().isClosed()) {
                release(xaBranchXid, connectionProxyXA);
            } else {
                return connectionProxyXA;
            }
        }
        return (ConnectionProxyXA)getConnectionProxyXA();
    }

    protected abstract Connection getConnectionProxyXA() throws SQLException;

    /**
     * Force close the physical connection kept for XA branch of given XAXid.
     * @param xaXid the given XAXid
     * @throws SQLException exception
     */
    public void forceClosePhysicalConnection(XAXid xaXid) throws SQLException {
        ConnectionProxyXA connectionProxyXA = lookup(xaXid.toString());
        if (connectionProxyXA != null) {
            connectionProxyXA.close();
            Connection physicalConn = connectionProxyXA.getWrappedConnection();
            if (physicalConn instanceof PooledConnection) {
                physicalConn = ((PooledConnection)physicalConn).getConnection();
            }
            // Force close the physical connection
            physicalConn.close();
        }


    }
}
