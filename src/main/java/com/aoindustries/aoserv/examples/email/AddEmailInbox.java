/*
 * aoserv-examples - Automation examples for the AOServ Platform.
 * Copyright (C) 2001-2013, 2017, 2018, 2019, 2021, 2022  AO Industries, Inc.
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

package com.aoindustries.aoserv.examples.email;

import com.aoapps.net.DomainName;
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.account.Account;
import com.aoindustries.aoserv.client.email.Address;
import com.aoindustries.aoserv.client.email.Domain;
import com.aoindustries.aoserv.client.linux.Group;
import com.aoindustries.aoserv.client.linux.Server;
import com.aoindustries.aoserv.client.linux.Shell;
import com.aoindustries.aoserv.client.linux.User;
import com.aoindustries.aoserv.client.linux.User.Gecos;
import com.aoindustries.aoserv.client.linux.UserServer;
import com.aoindustries.aoserv.client.linux.UserType;
import java.io.IOException;
import java.sql.SQLException;

/**
 * An email inbox is a restricted Linux account. It can be used for sending
 * and receiving email using the <code>POP2</code>, <code>POP3</code>,
 * <code>IMAP</code>, <code>SPOP3</code>, <code>SIMAP</code>, and <code>SMTP</code>
 * protocols. If a shell connection is established, either via SSH or Telnet, the
 * user is prompted to change their password. All other protocols are refused,
 * including <code>FTP</code>.
 *
 * @author  AO Industries, Inc.
 */
public final class AddEmailInbox {

  /** Make no instances. */
  private AddEmailInbox() {
    throw new AssertionError();
  }

  /**
   * Creates a new email inbox.
   *
   * @param  aoClient     the <code>SimpleAOClient</code> to use
   * @param  packageName  the name of the <code>Package</code>
   * @param  username     the new username to allocate
   * @param  fullName     the user's full name
   * @param  server       the hostname of the server to add the user to
   * @param  address      the part of the email address before the <code>@</code>
   * @param  password     the password for the new user
   */
  public static void addEmailInbox(
      SimpleAOClient aoClient,
      Account.Name packageName,
      User.Name username,
      Gecos fullName,
      String server,
      String address,
      DomainName domain,
      String password
  ) throws IOException, SQLException {
    // Reserve the username
    aoClient.addUsername(packageName, username);

    // Indicate the username will be used for Linux accounts
    aoClient.addLinuxAccount(username, Group.MAILONLY, fullName, null, null, null, UserType.EMAIL, Shell.PASSWD);

    // Grant the new Linux account access to the server
    aoClient.addLinuxServerAccount(username, server, null);

    // Attach the email address to the new inbox
    aoClient.addLinuxAccAddress(address, domain, server, username);

    // Wait for rebuild
    aoClient.waitForLinuxAccountRebuild(server);

    // Set the password
    aoClient.setLinuxServerAccountPassword(username, server, password);
  }

  /**
   * Creates a new email inbox.
   *
   * @param  conn         the <code>AOServConnector</code> to use
   * @param  packageName  the name of the <code>Package</code>
   * @param  username     the new username to allocate
   * @param  fullName     the user's full name
   * @param  server       the hostname of the server to add the user to
   * @param  address      the part of the email address before the <code>@</code>
   * @param  password     the password for the new account
   *
   * @return  the new <code>UserServer</code>
   */
  public static UserServer addEmailInbox(
      AOServConnector conn,
      Account.Name packageName,
      User.Name username,
      Gecos fullName,
      String server,
      String address,
      DomainName domain,
      String password
  ) throws IOException, SQLException {
    // Resolve the Package
    com.aoindustries.aoserv.client.billing.Package pk = conn.getBilling().getPackage().get(packageName);

    // Reserve the username
    pk.addUsername(username);
    com.aoindustries.aoserv.client.account.User un = conn.getAccount().getUser().get(username);

    // Indicate the username will be used for Linux accounts
    un.addLinuxAccount(Group.MAILONLY, fullName, null, null, null, UserType.EMAIL, Shell.PASSWD);
    User la = un.getLinuxAccount();

    // Find the Server
    Server linuxServer = conn.getNet().getHost().get(server).getLinuxServer();

    // Grant the new Linux account access to the server
    int lsaPKey = la.addLinuxServerAccount(linuxServer, UserServer.getDefaultHomeDirectory(username));
    UserServer lsa = conn.getLinux().getUserServer().get(lsaPKey);

    // Find the Domain
    Domain sd = linuxServer.getEmailDomain(domain);

    // Create the new email address
    int eaPKey = sd.addEmailAddress(address);
    Address ea = conn.getEmail().getAddress().get(eaPKey);

    // Attach the email address to the new inbox
    lsa.addEmailAddress(ea);

    // Wait for rebuild
    linuxServer.waitForLinuxAccountRebuild();

    // Set the password
    lsa.setPassword(password);

    // Return the new object
    return lsa;
  }
}
