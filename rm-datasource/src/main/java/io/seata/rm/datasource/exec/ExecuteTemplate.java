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
package io.seata.rm.datasource.exec;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.mariadb.MariadbInsertOnDuplicateUpdateExecutor;
import io.seata.rm.datasource.exec.mariadb.MariadbUpdateJoinExecutor;
import io.seata.rm.datasource.exec.mysql.MySQLInsertOnDuplicateUpdateExecutor;
import io.seata.rm.datasource.exec.mysql.MySQLUpdateJoinExecutor;
import io.seata.rm.datasource.exec.polardbx.PolarDBXInsertOnDuplicateUpdateExecutor;
import io.seata.rm.datasource.exec.polardbx.PolarDBXUpdateJoinExecutor;
import io.seata.rm.datasource.exec.sqlserver.SqlServerDeleteExecutor;
import io.seata.rm.datasource.exec.sqlserver.SqlServerSelectForUpdateExecutor;
import io.seata.rm.datasource.exec.sqlserver.SqlServerUpdateExecutor;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type Execute template.
 *
 */
public class ExecuteTemplate {

    /**
     * Execute t.
     *
     * @param <T>               the type parameter
     * @param <S>               the type parameter
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param args              the args
     * @return the t
     * @throws SQLException the sql exception
     */
    public static <T, S extends Statement> T execute(StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {
        return execute(null, statementProxy, statementCallback, args);
    }

    /**
     * Execute t.
     *
     * @param <T>               the type parameter
     * @param <S>               the type parameter
     * @param sqlRecognizers    the sql recognizer list
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param args              the args
     * @return the t
     * @throws SQLException the sql exception
     */
    public static <T, S extends Statement> T execute(List<SQLRecognizer> sqlRecognizers,
                                                     StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {
        if (!RootContext.requireGlobalLock() && BranchType.AT != RootContext.getBranchType()) {
            // Just work as original statement
            return statementCallback.execute(statementProxy.getTargetStatement(), args);
        }

        String dbType = statementProxy.getConnectionProxy().getDbType();
        if (CollectionUtils.isEmpty(sqlRecognizers)) {
            sqlRecognizers = SQLVisitorFactory.get(
                    statementProxy.getTargetSQL(),
                    dbType);
        }
        Executor<T> executor;
        if (CollectionUtils.isEmpty(sqlRecognizers)) {
            executor = new PlainExecutor<>(statementProxy, statementCallback);
        } else {
            if (sqlRecognizers.size() == 1) {
                SQLRecognizer sqlRecognizer = sqlRecognizers.get(0);
                switch (sqlRecognizer.getSQLType()) {
                    case INSERT:
                        executor = EnhancedServiceLoader.load(InsertExecutor.class, dbType,
                                    new Class[]{StatementProxy.class, StatementCallback.class, SQLRecognizer.class},
                                    new Object[]{statementProxy, statementCallback, sqlRecognizer});
                        break;
                    case UPDATE:
                        if (JdbcConstants.SQLSERVER.equalsIgnoreCase(dbType)) {
                            executor = new SqlServerUpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        } else {
                            executor = new UpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        }
                        break;
                    case DELETE:
                        if (JdbcConstants.SQLSERVER.equalsIgnoreCase(dbType)) {
                            executor = new SqlServerDeleteExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        } else {
                            executor = new DeleteExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        }
                        break;
                    case SELECT_FOR_UPDATE:
                        if (JdbcConstants.SQLSERVER.equalsIgnoreCase(dbType)) {
                            executor = new SqlServerSelectForUpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        } else {
                            executor = new SelectForUpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        }
                        break;
                    case INSERT_ON_DUPLICATE_UPDATE:
                        switch (dbType) {
                            case JdbcConstants.MYSQL:
                                executor =
                                        new MySQLInsertOnDuplicateUpdateExecutor(statementProxy, statementCallback, sqlRecognizer);
                                break;
                            case JdbcConstants.MARIADB:
                                executor =
                                        new MariadbInsertOnDuplicateUpdateExecutor(statementProxy, statementCallback, sqlRecognizer);
                                break;
                            case JdbcConstants.POLARDBX:
                                executor = new PolarDBXInsertOnDuplicateUpdateExecutor(statementProxy, statementCallback, sqlRecognizer);
                                break;
                            default:
                                throw new NotSupportYetException(dbType + " not support to INSERT_ON_DUPLICATE_UPDATE");
                        }
                        break;
                    case UPDATE_JOIN:
                        switch (dbType) {
                            case JdbcConstants.MYSQL:
                                executor = new MySQLUpdateJoinExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                                break;
                            case JdbcConstants.MARIADB:
                                executor = new MariadbUpdateJoinExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                                break;
                            case JdbcConstants.POLARDBX:
                                executor = new PolarDBXUpdateJoinExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                                break;
                            default:
                                throw new NotSupportYetException(dbType + " not support to " + SQLType.UPDATE_JOIN.name());
                        }
                        break;
                    default:
                        executor = new PlainExecutor<>(statementProxy, statementCallback);
                        break;
                }
            } else {
                executor = new MultiExecutor<>(statementProxy, statementCallback, sqlRecognizers);
            }
        }
        T rs;
        try {
            rs = executor.execute(args);
        } catch (Throwable ex) {
            if (!(ex instanceof SQLException)) {
                // Turn other exception into SQLException
                ex = new SQLException(ex);
            }
            throw (SQLException) ex;
        }
        return rs;
    }

}
