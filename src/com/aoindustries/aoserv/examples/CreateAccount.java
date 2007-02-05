package com.aoindustries.aoserv.examples;

/*
 * Copyright 2001-2007 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.aoserv.client.*;
import com.aoindustries.io.*;
import com.aoindustries.sql.*;
import java.io.*;
import java.sql.*;

/**
 * Code to create an basic, but complete account with one web
 * site.  This is only representative of how to create an account.
 * AO Industries is not responsible for maintaining this code.
 *
 * @author  AO Industries, Inc.
 */
final public class CreateAccount {

    /**
     * Creates an account, automatically allocating as many resources as possible.
     * More control of account layout may be obtained by customizing this code.
     *
     * @param  conn                the <code>AOServConnector</code> to communicate with
     * @param  out                 if provided, verbose output is displayed during account creation
     * @param  accountingTemplate  the beginning part of the accounting code
     * @param  server              the hostname of the server to set up the account on
     * @param  parentBusiness      the accounting code of the parent business
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
     * @param  mysqlAdminUsername  the username of the existing MySQLUser that is allowed to admin the new DB
     * @param  mysqlAppUsername    the username that will have limited access to the database
     * @param  mysqlAppPassword    the password associated with the newly created application user account
     * @param  ipAddress           the IP address the site will respond to
     * @param  ownsIPAddress       if <code>true</code>, the IP address ownership will be changed to the
     *                             newly created <code>Package</code>
     * @param  serverAdmin         the email address of the business_administrator who is responsible for web site maintenance
     * @param  primaryHttpHostname  the primary hostname for the HTTP server
     * @param  altHttpHostnames    the alternate hostnames for the HTTP server
     * @param  httpsHostname       the hostname for the HTTPS server
     * @param  tomcatVersion       the version of Tomcat to install
     * @param  contentSrc          the source archive for the site, <code>null</code> will result in a default empty site
     */
    public static void createAccount(
	AOServConnector conn,
	ChainWriter out,
	String accountingTemplate,
	String server,
	String parentBusiness,
        String packageDefinitionCategory,
        String packageDefinitionName,
        String packageDefinitionVersion,
	String jvmUsername,
	String jvmPassword,
	String ftpUsername,
	String ftpPassword,
	String groupName,
	String siteNameTemplate,
	String mysqlAdminUsername,
	String mysqlAppUsername,
	String mysqlAppPassword,
	String ipAddress,
        String netDevice,
	boolean ownsIPAddress,
	String serverAdmin,
	String primaryHttpHostname,
	String[] altHttpHostnames,
	String tomcatVersion,
	String contentSrc
    ) throws IOException, SQLException {
	long startTime=System.currentTimeMillis();

	SimpleAOClient client=new SimpleAOClient(conn);

        // Resolve the parent business
        Business parent=conn.businesses.get(parentBusiness);
        if(parent==null) throw new SQLException("Unable to find Business: "+parentBusiness);

        // Create the business
	String accounting=client.generateAccountingCode(accountingTemplate);
	client.addBusiness(accounting, null, server, parentBusiness, false, false, true, true);
	if(out!=null) out.print("Business added, accounting=").println(accounting).flush();

        // Resolve the PackageDefinition
        PackageCategory pc=conn.packageCategories.get(packageDefinitionCategory);
        if(pc==null) throw new SQLException("Unable to find PackageCategory: "+packageDefinitionCategory);
        PackageDefinition packageDefinition=parent.getPackageDefinition(pc, packageDefinitionName, packageDefinitionVersion);
        if(packageDefinition==null) throw new SQLException("Unable to find PackageDefinition: accounting="+parentBusiness+", category="+packageDefinitionCategory+", name="+packageDefinitionName+", version="+packageDefinitionVersion);

        // Add a Package to the Business
	String packageName=client.generatePackageName(accounting+'_');
	client.addPackage(
            packageName,
            accounting,
            packageDefinition.getPKey()
	);
	if(out!=null) out.print("Package added, name=").println(packageName).flush();

	// Find the site_name that will be used
	String siteName=client.generateSiteName(siteNameTemplate);

	// Add the Linux group that the JVM and FTP account will use
	client.addLinuxGroup(groupName, packageName, LinuxGroupType.USER);
	if(out!=null) out.print("LinuxGroup added, name=").println(groupName).flush();
	int linuxServerGroupPKey=client.addLinuxServerGroup(groupName, server);
	if(out!=null) out.print("LinuxServerGroup added, pkey=").println(linuxServerGroupPKey).flush();

	// Add the Linux account that the JVM will run as
	client.addUsername(packageName, jvmUsername);
	if(out!=null) out.print("Username added, username=").println(jvmUsername).flush();
	client.addLinuxAccount(
            jvmUsername,
            groupName,
            siteName+" Java VM",
            null,
            null,
            null,
            LinuxAccountType.USER,
            Shell.BASH
	);
	if(out!=null) out.print("LinuxAccount added, username=").println(jvmUsername).flush();
	int jvmLinuxServerAccountPKey=client.addLinuxServerAccount(jvmUsername, server, HttpdSite.WWW_DIRECTORY+'/'+siteName);
	if(out!=null) out.print("LinuxServerAccount added, pkey=").println(jvmLinuxServerAccountPKey).flush();

	// Add the Linux account that will have FTP only access
	client.addUsername(packageName, ftpUsername);
	if(out!=null) out.print("Username added, username=").println(ftpUsername).flush();
	client.addLinuxAccount(
            ftpUsername,
            groupName,
            siteName+" FTP",
            null,
            null,
            null,
            LinuxAccountType.FTPONLY,
            Shell.FTPPASSWD
	);
	if(out!=null) out.print("LinuxAccount added, username=").println(ftpUsername).flush();
	client.addFTPGuestUser(ftpUsername);
	if(out!=null) out.print("LinuxAccount flagged as FTPGuestUser, username=").println(ftpUsername).flush();
	int ftpLinuxServerAccountPKey=client.addLinuxServerAccount(ftpUsername, server, HttpdSite.WWW_DIRECTORY+'/'+siteName+"/webapps");
	if(out!=null) out.print("LinuxServerAccount added, pkey=").println(ftpLinuxServerAccountPKey).flush();

	// Make sure the account rebuild is complete before continuing
	if(out!=null) out.print("Waiting for LinuxServerAccount rebuild on ").println(server).flush();
	client.waitForLinuxAccountRebuild(server);

	// Set the passwords for the two new accounts
	client.setLinuxServerAccountPassword(jvmUsername, server, jvmPassword);
	if(out!=null) out.print("Password set for LinuxServerAccount ").println(jvmUsername).flush();
	client.setLinuxServerAccountPassword(ftpUsername, server, ftpPassword);
	if(out!=null) out.print("Password set for LinuxServerAccount ").println(ftpUsername).flush();

	// Add the MySQL database
	/*String mysqlDatabaseName=client.generateMySQLDatabaseName(siteName.replace('-', '_'), "_");
	int mysqlDatabasePKey=client.addMySQLDatabase(mysqlDatabaseName, server, packageName);
	if(out!=null) out.print("MySQLDatabase added, pkey=").println(mysqlDatabasePKey).flush();

	// Create the MySQL database application user
	if(client.isUsernameAvailable(mysqlAppUsername)) {
            client.addUsername(packageName, mysqlAppUsername);
            if(out!=null) out.print("Username added, username=").println(mysqlAppUsername).flush();
	}
	client.addMySQLUser(mysqlAppUsername);
	if(out!=null) out.print("MySQLUser added, username=").println(mysqlAppUsername).flush();
	int mysqlServerUserPKey=client.addMySQLServerUser(mysqlAppUsername, server, MySQLHost.ANY_LOCAL_HOST);
	if(out!=null) out.print("MySQLServerUser added, pkey=").println(mysqlServerUserPKey).flush();

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
	if(out!=null) out.print("Waiting for MySQLServerUser rebuilds on ").println(server).flush();
	client.waitForMySQLUserRebuild(server);

	// Set the password for the application MySQL user
	client.setMySQLServerUserPassword(mysqlAppUsername, server, mysqlAppPassword);
	if(out!=null) out.print("Password set for MySQLServerUser ").println(mysqlAppUsername).flush();
*/
	// Change the IP Address ownership if a private IP is being allotted
	if(ownsIPAddress) {
            client.setIPAddressPackage(ipAddress, server, netDevice, packageName);
            if(out!=null) out.print("IPAddress package set, package="+packageName).flush();
	}

	// Create the site
	int tomcatStdSitePKey=client.addHttpdTomcatStdSite(
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
            tomcatVersion,
            contentSrc
	);
	if(out!=null) out.print("HttpdTomcatStdSite added, pkey=").println(tomcatStdSitePKey).flush();

	// Wait for batched and processing updates to complete
	if(out!=null) out.print("Waiting for HttpdSite rebuilds on ").println(server).flush();
	client.waitForHttpdSiteRebuild(server);

	// Set the access password for the site
	//client.initializeHttpdSitePasswdFile(siteName, server, jvmUsername, jvmPassword);
	//if(out!=null) out.println("Initialized passwd file").flush();

	int timeSpan=(int)(System.currentTimeMillis()-startTime);

	out.print("Done in ").print(SQLUtility.getMilliDecimal(timeSpan)).print(" seconds\n").flush();
    }
}
