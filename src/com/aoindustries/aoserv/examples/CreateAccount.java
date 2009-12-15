package com.aoindustries.aoserv.examples;

/*
 * Copyright 2001-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.aoserv.client.AOServConnector;
import com.aoindustries.aoserv.client.Business;
import com.aoindustries.aoserv.client.HttpdSite;
import com.aoindustries.aoserv.client.LinuxAccountType;
import com.aoindustries.aoserv.client.LinuxGroupType;
import com.aoindustries.aoserv.client.PackageCategory;
import com.aoindustries.aoserv.client.PackageDefinition;
import com.aoindustries.aoserv.client.Shell;
import com.aoindustries.aoserv.client.SimpleAOClient;
import com.aoindustries.io.ChainWriter;
import com.aoindustries.sql.SQLUtility;
import java.io.IOException;
import java.sql.SQLException;

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
     *                             newly created <code>Business</code>
     * @param  serverAdmin         the email address of the business_administrator who is responsible for web site maintenance
     * @param  primaryHttpHostname  the primary hostname for the HTTP server
     * @param  altHttpHostnames    the alternate hostnames for the HTTP server
     * @param  httpsHostname       the hostname for the HTTPS server
     * @param  tomcatVersion       the version of Tomcat to install
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
        String tomcatVersion
    ) throws IOException, SQLException {
    	long startTime=System.currentTimeMillis();
    	SimpleAOClient client=conn.getSimpleAOClient();

        // Resolve the parent business
        Business parent=conn.getBusinesses().get(parentBusiness);
        if(parent==null) throw new SQLException("Unable to find Business: "+parentBusiness);

        // Resolve the PackageDefinition
        PackageCategory pc=conn.getPackageCategories().get(packageDefinitionCategory);
        if(pc==null) throw new SQLException("Unable to find PackageCategory: "+packageDefinitionCategory);
        PackageDefinition packageDefinition=parent.getPackageDefinition(pc, packageDefinitionName, packageDefinitionVersion);
        if(packageDefinition==null) throw new SQLException("Unable to find PackageDefinition: accounting="+parentBusiness+", category="+packageDefinitionCategory+", name="+packageDefinitionName+", version="+packageDefinitionVersion);

        // Create the business
        String accounting=client.generateAccountingCode(accountingTemplate);
        client.addBusiness(accounting, null, server, parentBusiness, false, false, true, true, packageDefinition.getPkey());
        if(out!=null) out.print("Business added, accounting=").println(accounting).flush();

        // Find the site_name that will be used
        String siteName=client.generateSiteName(siteNameTemplate);

        // Add the Linux group that the JVM and FTP account will use
        client.addLinuxGroup(groupName, accounting, LinuxGroupType.USER);
        if(out!=null) out.print("LinuxGroup added, name=").println(groupName).flush();
        int linuxServerGroupPKey=client.addLinuxServerGroup(groupName, server);
        if(out!=null) out.print("LinuxServerGroup added, pkey=").println(linuxServerGroupPKey).flush();

        // Add the Linux account that the JVM will run as
        client.addUsername(accounting, jvmUsername);
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
        client.addUsername(accounting, ftpUsername);
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
        int mysqlDatabasePKey=client.addMySQLDatabase(mysqlDatabaseName, server, accounting);
        if(out!=null) out.print("MySQLDatabase added, pkey=").println(mysqlDatabasePKey).flush();

        // Create the MySQL database application user
        if(client.isUsernameAvailable(mysqlAppUsername)) {
            client.addUsername(accounting, mysqlAppUsername);
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
            client.setIPAddressBusiness(ipAddress, server, netDevice, accounting);
            if(out!=null) out.print("IPAddress business set, accounting="+accounting).flush();
        }

        // Create the site
        int tomcatStdSitePKey=client.addHttpdTomcatStdSite(
            server,
            siteName,
            accounting,
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
