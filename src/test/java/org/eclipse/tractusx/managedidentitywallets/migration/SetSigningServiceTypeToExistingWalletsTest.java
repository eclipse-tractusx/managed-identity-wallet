/*
 * *******************************************************************************
 *  Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ******************************************************************************
 */

package org.eclipse.tractusx.managedidentitywallets.migration;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import org.h2.command.query.Select;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SetSigningServiceTypeToExistingWalletsTest {
    public static final int EXPECTED_WALLET_COUNT = 5;
    private static Database database;

    @BeforeAll
    @SneakyThrows
    public static void beforeAll() {
        Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:framework_test;INIT=CREATE SCHEMA IF NOT EXISTS migration",
                "admin",
                "password");

        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
    }

    @SneakyThrows
    @Test
    void migrateWallets() {
        var connection = (JdbcConnection) database.getConnection();

        // 1. apply changelog without signing service-type
        Liquibase liquibase = new Liquibase("db/signing-service-migration-test/without-changes.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update((String) null);

        // assert that column for signing_service_type does not exist

        List<String> columns = getColumnList(connection);
        assertFalse(columns.contains("signing_service_type".toUpperCase()));

        // insert Wallets
        insertWallets(connection);

        // assert that there are 5 Wallets created
        String sqlCount = "SELECT count(*) as count FROM public.wallet";
        Statement st = connection.createStatement();
        ResultSet count = st.executeQuery(sqlCount);
        assertTrue(count.next());
        assertEquals(EXPECTED_WALLET_COUNT, count.getInt("count"));

        // run migration
        Liquibase migrate = new Liquibase("db/signing-service-migration-test/signing-service-changelog.xml", new ClassLoaderResourceAccessor(), database);
        migrate.update((String) null);

        // assert that column was created
        List<String> newColumns = getColumnList(connection);
        assertTrue(newColumns.contains("signing_service_type".toUpperCase()));

        // assert that every wallet now has signing_service_type = LOCAL by default
        String walletSql = "SELECT * FROM public.wallet";
        Statement walletStatement = connection.createStatement();
        ResultSet walletResult = walletStatement.executeQuery(walletSql);
        while(walletResult.next()){
            assertEquals("LOCAL", walletResult.getString("signing_service_type".toUpperCase()));
        }
    }

    @SneakyThrows
    private List<String> getColumnList(JdbcConnection connection){
        String selectColumns = "SHOW COLUMNS FROM public.wallet";
        // COLUMN_NAME | TYPE | IS_NULLABLE | KEY | DEFAULT
        ResultSet resultSet = connection.createStatement().executeQuery(selectColumns);
        List<String> columns = new ArrayList<>();
        while (resultSet.next()) {
            columns.add(resultSet.getString("COLUMN_NAME"));
        }
        return columns;
    }

    @SneakyThrows
    private void insertWallets(JdbcConnection connection) {
        String insert = "INSERT INTO wallet(id,name,did,bpn,algorithm,did_document,created_at) VALUES(?,?,?,?,?,?,?)";
        PreparedStatement pst = connection.prepareStatement(insert);

        for (int i = 1; i <= EXPECTED_WALLET_COUNT; i++) {
            pst.setInt(1, i);
            pst.setString(2, "name"+i);
            pst.setString(3, "did"+i);
            pst.setString(4, "bpn"+i);
            pst.setString(5, "algorithm"+i);
            pst.setString(6, "document"+i);
            pst.setTimestamp(7, Timestamp.from(Instant.now()));
            pst.execute();
        }
    }

}
