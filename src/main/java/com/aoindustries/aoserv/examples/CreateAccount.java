/*
 * aoserv-examples - Automation examples for the AOServ Platform.
 * Copyright (C) 2001-2009, 2015, 2017, 2018, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

package com.aoindustries.aoserv.examples;

import com.aoapps.lang.validation.ValidationException;
import com.aoapps.net.DomainName;
import com.aoapps.net.Email;
import com.aoapps.net.InetAddress;
import com.aoapps.sql.SQLUtility;
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.aoserv.client.account.Account;
import com.aoindustries.aoserv.client.billing.PackageCategory;
import com.aoindustries.aoserv.client.billing.PackageDefinition;
import com.aoindustries.aoserv.client.linux.Group;
import com.aoindustries.aoserv.client.linux.GroupType;
import com.aoindustries.aoserv.client.linux.PosixPath;
import com.aoindustries.aoserv.client.linux.Shell;
import com.aoindustries.aoserv.client.linux.User;
import com.aoindustries.aoserv.client.linux.User.Gecos;
import com.aoindustries.aoserv.client.linux.UserType;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Code to create an basic, but complete account with one web
 * site.  This is only representative of how to create an account.
 * AO Industries is not responsible for maintaining this code.
 *
 * @author  AO Industries, Inc.
 */
public final class CreateAccount {

	/** Make no instances. */
	private CreateAccount() {throw new AssertionError();}

	/**
	 * Creates an account, automatically allocating as many resources as possible.
	 * More control of account layout may be obtained by customizing this code.
	 *
	 * @param  conn                the <code>AOServConnector</code> to communicate with
	 * @param  out                 if provided, verbose output is displayed during account creation
	 * @param  accountingTemplate  the beginning part of the accounting code
	 * @param  server              the hostname of the server to set up the account on
	 * @param  parentAccount      the accounting code of the parent business
	 * @param  packageDefinitionCategory  the category for the <code>PackageDefinition</code>
	 * @param  packageDefinitionName  the name of the <code>PackageDefinition</code>
	 * @param  packageDefinitionVersion  the version of the <code>PackageDefinition</code>.  Please note
	 *                                   that the combination of parentBusiness, packageDefinitionCategory,
	 *                                   packageDefinitionName, and packageDefinitionVersion uniquely
	 *                                   identifies one <code>PackageDefinition</code>
	 * @param  jvmUsername         the username the JVM will run as
	 * @param  jvmPassword         the password for the JVM
	 * @param  ftpUsername         the username that will be allowed to FTP only to the site
	 * @param  ftpPassword         the password for the FTP access
	 * @param  groupName           the name of the Linux group that the JVM and FTP accounts share
	 * @param  siteNameTemplate    the template used for site name creation
	 * @param  mysqlAdminUsername  the username of the existing User that is allowed to admin the new DB
	 * @param  mysqlAppUsername    the username that will have limited access to the database
	 * @param  mysqlAppPassword    the password associated with the newly created application user account
	 * @param  ipAddress           the IP address the site will respond to
	 * @param  ownsIPAddress       if <code>true</code>, the IP address ownership will be changed to the
	 *                             newly created <code>Package</code>
	 * @param  serverAdmin         the email address of the business_administrator who is responsible for web site maintenance
	 * @param  primaryHttpHostname  the primary hostname for the HTTP server
	 * @param  altHttpHostnames    the alternate hostnames for the HTTP server
	 * @param  tomcatVersion       the version of Tomcat to install
	 */
	public static void createAccount(
		AOServConnector conn,
		PrintWriter out,
		Account.Name accountingTemplate,
		String server,
		Account.Name parentAccount,
		String packageDefinitionCategory,
		String packageDefinitionName,
		String packageDefinitionVersion,
		User.Name jvmUsername,
		String jvmPassword,
		User.Name ftpUsername,
		String ftpPassword,
		Group.Name groupName,
		String siteNameTemplate,
		com.aoindustries.aoserv.client.mysql.User.Name mysqlAdminUsername,
		com.aoindustries.aoserv.client.mysql.User.Name mysqlAppUsername,
		String mysqlAppPassword,
		InetAddress ipAddress,
		String netDevice,
		boolean ownsIPAddress,
		Email serverAdmin,
		DomainName primaryHttpHostname,
		DomainName[] altHttpHostnames,
		String tomcatVersion
	) throws IOException, SQLException, ValidationException {
		long startTime=System.currentTimeMillis();
		SimpleAOClient client=conn.getSimpleAOClient();

		// Resolve the parent account
		Account parent = conn.getAccount().getAccount().get(parentAccount);
		if(parent == null) throw new SQLException("Unable to find Account: " + parentAccount);

		// Create the account
		Account.Name accounting = client.generateAccountingCode(accountingTemplate);
		client.addAccount(accounting, null, server, parentAccount, false, false, true, true);
		if(out!=null) {
			out.print("Account added, accounting=");
			out.println(accounting);
			out.flush();
		}

		// Resolve the PackageDefinition
		PackageCategory pc = conn.getBilling().getPackageCategory().get(packageDefinitionCategory);
		if(pc == null) throw new SQLException("Unable to find PackageCategory: " + packageDefinitionCategory);
		PackageDefinition packageDefinition=parent.getPackageDefinition(pc, packageDefinitionName, packageDefinitionVersion);
		if(packageDefinition==null) throw new SQLException("Unable to find PackageDefinition: accounting="+parentAccount+", category="+packageDefinitionCategory+", name="+packageDefinitionName+", version="+packageDefinitionVersion);

		// Add a Package to the Account
		Account.Name packageName=client.generatePackageName(Account.Name.valueOf(accounting.toString()+'_'));
		client.addPackage(
			packageName,
			accounting,
			packageDefinition.getPkey()
		);
		if(out!=null) {
			out.print("Package added, name=");
			out.println(packageName);
			out.flush();
		}

		// Find the site_name that will be used
		String siteName=client.generateSiteName(siteNameTemplate);

		// Add the Linux group that the JVM and FTP account will use
		client.addLinuxGroup(groupName, packageName, GroupType.USER);
		if(out!=null) {
			out.print("Group added, name=");
			out.println(groupName);
			out.flush();
		}
		int linuxServerGroupPKey=client.addLinuxServerGroup(groupName, server);
		if(out!=null) {
			out.print("LinuxServerGroup added, pkey=");
			out.println(linuxServerGroupPKey);
			out.flush();
		}

		// Add the Linux account that the JVM will run as
		client.addUsername(packageName, jvmUsername);
		if(out!=null) {
			out.print("Username added, username=");
			out.println(jvmUsername);
			out.flush();
		}
		client.addLinuxAccount(
			jvmUsername,
			groupName,
			Gecos.valueOf(siteName+" Java VM"),
			null, // officeLocation
			null, // officePhone
			null, // homePhone
			UserType.USER,
			Shell.BASH
		);
		if(out!=null) {
			out.print("User added, username=");
			out.println(jvmUsername);
			out.flush();
		}
		// Find the directory containing the websites
		PosixPath wwwDir = conn.getLinux().getServer().get(
			DomainName.valueOf(server)
		).getHost().getOperatingSystemVersion().getHttpdSitesDirectory();
		int jvmLinuxServerAccountPKey=client.addLinuxServerAccount(
			jvmUsername,
			server,
			PosixPath.valueOf(wwwDir.toString()+'/'+siteName)
		);
		if(out!=null) {
			out.print("UserServer added, pkey=");
			out.println(jvmLinuxServerAccountPKey);
			out.flush();
		}

		// Add the Linux account that will have FTP only access
		client.addUsername(packageName, ftpUsername);
		if(out!=null) {
			out.print("Username added, username=");
			out.println(ftpUsername);
			out.flush();
		}
		client.addLinuxAccount(
			ftpUsername,
			groupName,
			Gecos.valueOf(siteName+" FTP"),
			null,
			null,
			null,
			UserType.FTPONLY,
			Shell.FTPPASSWD
		);
		if(out!=null) {
			out.print("User added, username=");
			out.println(ftpUsername);
			out.flush();
		}
		client.addFTPGuestUser(ftpUsername);
		if(out!=null) {
			out.print("User flagged as FTPGuestUser, username=");
			out.println(ftpUsername);
			out.flush();
		}
		int ftpLinuxServerAccountPKey=client.addLinuxServerAccount(
			ftpUsername,
			server,
			PosixPath.valueOf(wwwDir.toString()+'/'+siteName+"/webapps")
		);
		if(out!=null) {
			out.print("UserServer added, pkey=");
			out.println(ftpLinuxServerAccountPKey);
			out.flush();
		}

		// Make sure the account rebuild is complete before continuing
		if(out!=null) {
			out.print("Waiting for UserServer rebuild on ");
			out.println(server);
			out.flush();
		}
		client.waitForLinuxAccountRebuild(server);

		// Set the passwords for the two new accounts
		client.setLinuxServerAccountPassword(jvmUsername, server, jvmPassword);
		if(out!=null) {
			out.print("Password set for UserServer ");
			out.println(jvmUsername);
			out.flush();
		}
		client.setLinuxServerAccountPassword(ftpUsername, server, ftpPassword);
		if(out!=null) {
			out.print("Password set for UserServer ");
			out.println(ftpUsername);
			out.flush();
		}

		// Add the MySQL database
		/*String mysqlDatabaseName=client.generateMySQLDatabaseName(siteName.replace('-', '_'), "_");
		int mysqlDatabasePKey=client.addMySQLDatabase(mysqlDatabaseName, server, packageName);
		if(out!=null) out.print("Database added, pkey=").println(mysqlDatabasePKey).flush();

		// Create the MySQL database application user
		if(client.isUsernameAvailable(mysqlAppUsername)) {
			client.addUsername(packageName, mysqlAppUsername);
			if(out!=null) out.print("Username added, username=").println(mysqlAppUsername).flush();
		}
		client.addMySQLUser(mysqlAppUsername);
		if(out!=null) out.print("User added, username=").println(mysqlAppUsername).flush();
		int mysqlServerUserPKey=client.addMySQLServerUser(mysqlAppUsername, server, MySQLHost.ANY_LOCAL_HOST);
		if(out!=null) out.print("UserServer added, pkey=").println(mysqlServerUserPKey).flush();

		// Grant permissions to the administrative MySQL user
		client.addMySQLDBUser(
			mysqlDatabaseName,
			server,
			mysqlAdminUsername,
			true,
			true,
			true,
			true,
			true,
			true,
			true,
			true,
			true,
			true
		);
		if(out!=null) out.print("Granted full privileges to ").println(mysqlAdminUsername).flush();

		// Grant permissions to the application MySQL user
		client.addMySQLDBUser(
			mysqlDatabaseName,
			server,
			mysqlAppUsername,
			true,
			true,
			true,
			true,
			true,
			false,
			false,
			true,
			true,
			true
		);
		if(out!=null) out.print("Granted insert, update, select, delete, create, alter privileges to ").println(mysqlAppUsername).flush();

		// Make sure the MySQL system updates are complete before continuing
		if(out!=null) out.print("Waiting for UserServer rebuilds on ").println(server).flush();
		client.waitForMySQLUserRebuild(server);

		// Set the password for the application MySQL user
		client.setMySQLServerUserPassword(mysqlAppUsername, server, mysqlAppPassword);
		if(out!=null) out.print("Password set for UserServer ").println(mysqlAppUsername).flush();
		*/
		// Change the IP Address ownership if a private IP is being allotted
		if(ownsIPAddress) {
			client.setIPAddressPackage(ipAddress, server, netDevice, packageName);
			if(out!=null) {
				out.print("IPAddress package set, package=");
				out.println(packageName);
				out.flush();
			}
		}

		// Create the site
		int tomcatStdSitePKey = client.addHttpdTomcatStdSite(
			server,
			siteName,
			packageName,
			jvmUsername,
			groupName,
			serverAdmin,
			false,
			ipAddress,
			netDevice,
			primaryHttpHostname,
			altHttpHostnames,
			tomcatVersion
		);
		if(out!=null) {
			out.print("HttpdTomcatStdSite added, pkey=");
			out.println(tomcatStdSitePKey);
			out.flush();
		}

		// Wait for batched and processing updates to complete
		if(out!=null) {
			out.print("Waiting for HttpdSite rebuilds on ");
			out.println(server);
			out.flush();
		}
		client.waitForHttpdSiteRebuild(server);

		// Set the access password for the site
		//client.initializeHttpdSitePasswdFile(siteName, server, jvmUsername, jvmPassword);
		//if(out!=null) out.println("Initialized passwd file").flush();

		long timeSpan = System.currentTimeMillis() - startTime;

		if(out != null) {
			out.print("Done in ");
			out.print(SQLUtility.formatDecimal3(timeSpan));
			out.println(" seconds");
			out.flush();
		}
	}
}
