package com.aoindustries.aoserv.examples.mysql;

/*
 * Copyright 2001-2010 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.AOServer;
import com.aoindustries.aoserv.client.Business;
import com.aoindustries.aoserv.client.MySQLDatabase;
import com.aoindustries.aoserv.client.MySQLServer;
import com.aoindustries.aoserv.client.MySQLUser;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.Username;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Adds a <code>MySQLUser</code> to the system.
 *
 * @author  AO Industries, Inc.
 */
final public class AddMySQLUser {

    private AddMySQLUser() {
    }

    /**
     * Adds a <code>MySQLUser</code> to the system.
     *
     * @param  aoClient     the <code>SimpleAOClient</code> to use
     * @param  accounting   the accounting code of the <code>Business</code>
     * @param  username     the new username to allocate
     * @param  mysqlServer  the name of the MySQL instance
     * @param  server       the hostname of the server to add the account to
     * @param  database     the new user will be granted access to this database
     * @param  password     the password for the new account
     */
    public static void addMySQLUser(
        SimpleAOClient aoClient,
        String accounting,
        String username,
        String mysqlServer,
        String server,
        String database,
        String password
    ) throws IOException, SQLException {
        // Reserve the username
        aoClient.addUsername(accounting, username);

        // Grant access to the server
        aoClient.addMySQLUser(username, mysqlServer, server, MySQLUser.ANY_LOCAL_HOST);

        // Grant access to the database
        aoClient.addMySQLDBUser(database, mysqlServer, server, username, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

        // Commit the changes before setting the password
        aoClient.waitForMySQLUserRebuild(server);

        // Set the password
        aoClient.setMySQLUserPassword(username, mysqlServer, server, password);
    }

    /**
     * Adds a <code>MySQLUser</code> to the system.
     *
     * @param  conn         the <code>AOServConnector</code> to use
     * @param  accounting   the accounting code of the <code>Business</code>
     * @param  username     the new username to allocate
     * @param  mysqlServer  the name of the MySQL instance
     * @param  server       the hostname of the server to add the account to
     * @param  database     the new user will be granted access to this database
     * @param  password     the password for the new account
     *
     * @return  the new <code>MySQLUser</code>
     */
    public static MySQLUser addMySQLUser(
        AOServConnector conn,
        String accounting,
        String username,
        String mysqlServer,
        String server,
        String database,
        String password
    ) throws IOException, SQLException {
        // Find the Package
        Business bu=conn.getBusinesses().get(accounting);

        // Resolve the Server
        AOServer ao=conn.getAoServers().get(server);

        // Resolve the MySQLServer
        MySQLServer ms=ao.getMySQLServer(mysqlServer);

        // Reserve the username
        bu.addUsername(username);
        Username un=conn.getUsernames().get(username);

        // Grant access to the server
        int muPKey=un.addMySQLUser(ms, MySQLUser.ANY_LOCAL_HOST);
        MySQLUser mu=conn.getMysqlUsers().get(muPKey);

        // Find the MySQLDatabase
        MySQLDatabase md=ms.getMySQLDatabase(database);

        // Grant access to the database
        md.addMySQLDBUser(mu, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

        // Commit the changes before setting the password
        ao.waitForMySQLUserRebuild();

        // Set the password
        mu.setPassword(password);

        // Return the object
        return mu;
    }
}