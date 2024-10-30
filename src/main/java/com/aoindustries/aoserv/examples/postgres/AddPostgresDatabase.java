/*
 * aoserv-examples - Automation examples for the AOServ Platform.
 * Copyright (C) 2001-2013, 2017, 2018, 2020, 2021, 2022, 2024  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aoserv-examples.
 *
 * aoserv-examples is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aoserv-examples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aoserv-examples.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoindustries.aoserv.examples.postgres;

import com.aoapps.net.DomainName;
import com.aoindustries.aoserv.client.AoservConnector;
import com.aoindustries.aoserv.client.SimpleAoservClient;
import com.aoindustries.aoserv.client.postgresql.Database;
import com.aoindustries.aoserv.client.postgresql.Encoding;
import com.aoindustries.aoserv.client.postgresql.Server;
import com.aoindustries.aoserv.client.postgresql.User;
import com.aoindustries.aoserv.client.postgresql.UserServer;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Before creating a new PostgreSQL database, please make sure that a <code>User</code>
 * has been added for use as the Database Administrator (DBA).
 *
 * <p>The possible values for <code>encoding</code> may be found in the <code>postgres_encodings</code> table.</p>
 *
 * @see  AddPostgresUser
 * @see  com.aoindustries.aoserv.client.postgresql.Encoding
 *
 * @author  AO Industries, Inc.
 */
public final class AddPostgresDatabase {

  /** Make no instances. */
  private AddPostgresDatabase() {
    throw new AssertionError();
  }

  /**
   * Adds a <code>Database</code> to a <code>Host</code>.
   *
   * @param  aoClient        the <code>SimpleAoservClient</code> to use
   * @param  name            the name of the database to add
   * @param  postgresServer  the name of the PostgreSQL server
   * @param  server          the hostname of the server to add the database to
   * @param  datdba          the username of the database administrator <code>User</code>
   * @param  encoding        the encoding to use
   * @param  enablePostgis   enables PostGIS on the database
   */
  public static void addPostgresDatabase(
      SimpleAoservClient aoClient,
      Database.Name name,
      Server.Name postgresServer,
      String server,
      User.Name datdba,
      String encoding,
      boolean enablePostgis
  ) throws IOException, SQLException {
    aoClient.addPostgresDatabase(name, postgresServer, server, datdba, encoding, enablePostgis);
  }

  /**
   * Adds a <code>Database</code> to a <code>Host</code>.
   *
   * @param  conn            the <code>AoservConnector</code> to use
   * @param  name            the name of the database to add
   * @param  postgresServer  the name of the PostgreSQL server
   * @param  server          the hostname of the server to add the database to
   * @param  datdba          the username of the database administrator <code>User</code>
   * @param  encoding        the encoding to use
   * @param  enablePostgis   enables PostGIS on the database
   *
   * @return  the new <code>Database</code>
   */
  public static Database addPostgresDatabase(
      AoservConnector conn,
      Database.Name name,
      Server.Name postgresServer,
      DomainName server,
      User.Name datdba,
      String encoding,
      boolean enablePostgis
  ) throws IOException, SQLException {

    // Resolve the Host
    com.aoindustries.aoserv.client.linux.Server ao = conn.getLinux().getServer().get(server);

    // Resolve the Server
    Server ps = ao.getPostgresServer(postgresServer);

    // Resolve the datdba UserServer
    UserServer psu = ps.getPostgresServerUser(datdba);

    // Resolve the Encoding
    Encoding pe = ps.getVersion().getPostgresEncoding(conn, encoding);

    // Add the Database
    int pdId = ps.addPostgresDatabase(name, psu, pe, enablePostgis);

    // Return the object
    return conn.getPostgresql().getDatabase().get(pdId);
  }
}
