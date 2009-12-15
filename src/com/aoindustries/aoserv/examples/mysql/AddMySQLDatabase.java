package com.aoindustries.aoserv.examples.mysql;

/*
 * Copyright 2001-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.AOServer;
import com.aoindustries.aoserv.client.Business;
import com.aoindustries.aoserv.client.MySQLDatabase;
import com.aoindustries.aoserv.client.MySQLServer;
import com.aoindustries.aoserv.client.SimpleAOClient;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Adds a new <code>MySQLDatabase</code> to the system.
 *
 * @see  AddMySQLUser
 *
 * @author  AO Industries, Inc.
 */
final public class AddMySQLDatabase {

    /**
     * Adds a <code>MySQLDatabase</code> to a <code>Server</code>
     *
     * @param  aoClient     the <code>SimpleAOClient</code> to use
     * @param  name         the name of the database to add
     * @param  mysqlServer  the name of the MySQL instance
     * @param  server       the hostname of the server to add the database to
     * @param  accounting   the accounting code of the <code>Business</code> that owns the new database
     */
    public static void addMySQLDatabase(
        SimpleAOClient aoClient,
        String name,
        String mysqlServer,
        String server,
        String accounting
    ) throws IOException, SQLException {
        aoClient.addMySQLDatabase(Locale.getDefault(), name, mysqlServer, server, accounting);
    }

    /**
     * Adds a <code>MySQLDatabase</code> to a <code>Server</code>
     *
     * @param  conn         the <code>AOServConnector</code> to use
     * @param  name         the name of the database to add
     * @param  mysqlServer  the name of the MySQL instance
     * @param  server       the hostname of the server to add the database to
     * @param  accounting   the accounting code of the <code>Business</code> that owns the new database
     *
     * @return  the new <code>MySQLDatabase</code>
     */
    public static MySQLDatabase addMySQLDatabase(
        AOServConnector conn,
        String name,
        String mysqlServer,
        String server,
        String accounting
    ) throws IOException, SQLException {

        // Resolve the AOServer
        AOServer ao=conn.getAoServers().get(server);

        // Resolve the MySQLServer
        MySQLServer ms=ao.getMySQLServer(mysqlServer);

        // Resolve the Package
        Business bu=conn.getBusinesses().get(accounting);

        // Add the MySQLDatabase
        int mdPKey=ms.addMySQLDatabase(name, bu);
        MySQLDatabase md=conn.getMysqlDatabases().get(mdPKey);

        // Return the object
        return md;
    }
}
