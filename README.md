Developing Drools and jBPM
==========================

**If you want to build or contribute to a droolsjbpm project, [read this document](https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/README.md).**

**It will save you and us a lot of time by setting up your development environment correctly.**
It solves all known pitfalls that can disrupt your development.
It also describes all guidelines, tips and tricks.
If you want your pull requests (or patches) to be merged into master, please respect those guidelines.

#CSC Version

This branch contains modifications to 6.5.0.Final made by CSC Scandihealth.<br>
The modifications customizes Drools Workbench for embedded use within the LPR project (under construction).
  
##New Dependency to <code>csc-gwt-maven-plugin</code>

Running the **gwt-maven-plugin** to compile the gwt code in the <code>drools-wb-webapp</code> project under Windows 7 gives an error (see https://github.com/gwt-maven-plugin/gwt-maven-plugin/issues/113).<br>
To fix this we have backported the 2.8.0 fix to our own fork of <code>gwt-maven-plugin</code> 2.7.0 found here: https://github.com/scandihealth/gwt-maven-plugin/tree/csc-2.7.0 <br>
We have packaged that fork using maven and uploaded the jar to our own internal Artifactory maven repository so that it can be resolved when building this <code>drools-wb</code> fork internally at CSC.

#How To Setup For Internal CSC Development (Windows 7 64bit)
_Work in progress - will be updated as we find out if all these steps are necessary or not._

##Setting Up Build Configuration
1. Make sure you run Windows 7 (64bit) 
2. Install Git for windows, IntelliJ IDEA 2016.3, Java JDK 8 64bit, maven (>=3.3.9)
    - If you have any other JDKs you must follow this guide to make sure IDEA runs under 1.8: https://intellij-support.jetbrains.com/hc/en-us/articles/206544879-Selecting-the-JDK-version-the-IDE-will-run-under 
3. Configure Maven to use our internal Artifactory maven repository
   - Copy \\\sh\shares\scvcomn\LPR3\udviklersetup\settings.xml to your %USERPROFILE%\\.m2 folder (create folder if necessary)
4. Configure Git to use long paths
   - Open a command prompt with administrator access and run: <code>git config --system core.longpaths true</code>
5. Download the official droolsjbpm IDEA Code Style and save it in %USERPROFILE%\\.IntelliJIdea2016.3\config\codestyles  
   - Get it here: https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/6.5.0.Final/ide-configuration/intellij-configuration/code-style/intellij-code-style_droolsjbpm-java-conventions.xml   
6. Open IDEA (64 bit) -> Default Settings -> Maven
   - Change Maven home directory to your maven 3.3.9 installation
   - Change Maven -> Runner -> VM Options to <code>-Xms256m -Xmx3056m</code>
   - Change Maven -> Runner -> JRE to <code>1.8</code> (64bit)
7. Open IDEA (64 bit) -> Help -> Edit Custom VM Options 
   - Change the existing -Xmx setting to <code>-Xmx3056m</code>    
8. Using IDEA (64 bit): Checkout the repository from Git https://github.com/scandihealth/drools-wb.git
   - Open the pom.xml as a project
   - Make sure to "Enable Auto Import" in Maven and add GWT facets (IDEA should ask you these questions in popup balloons)
   - Change File -> Project Structure -> Project -> Project SDK to <code>1.8</code> (64bit)
   - Change git branch to origin/csc-6.5.0
9. Change the Code Style used by IDEA for this project
   - Select Settings -> Editor -> Code Style -> Scheme: <code>Drools and jBPM: Java Conventions</code>
10. Follow some of the other (TBD) code style recommendations described here (e.g. XML tab spaces) 
    - https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/README.md#developing-with-intellij
11. Click "Toggle 'Skip Tests' Mode" button in the Maven Projects window
13. You are now ready to run Maven commands and develop
    - Run Drools Workbench (root) -> package (takes 10-15 minutes)
    
##Setting Up Run Configuration
1. Download [WildFly 10.1.0.Final](http://download.jboss.org/wildfly/10.1.0.Final/wildfly-10.1.0.Final.zip) and unzip it to your drools-wb parent folder. 
    - Example: If drools-wb is placed here: C:\dev\drools-wb then WildFly should be placed here: C:\dev\wildfly-10.1.0.Final
    - Insert this into _wildfly-10.1.0.Final\standalone\configuration\standalone-full.xml_ after the `<extensions>` element
    (modify to your liking)
    ```
        <system-properties>
            <property name="org.uberfire.nio.git.dir" value="C:\dev\drools-wb-devdb"/>
            <property name="org.uberfire.nio.git.ssh.cert.dir" value="C:\dev\drools-wb-devdb"/>
            <property name="org.uberfire.metadata.index.dir" value="C:\dev\drools-wb-devdb"/>
            <property name="org.guvnor.m2repo.dir" value="C:\dev\drools-wb-devdb"/>
            <property name="org.kie.server.id" value="dev-kie-server"/>
            <property name="org.kie.server.location" value="http://localhost:8080/kie-server/services/rest/server"/>
            <property name="org.kie.server.controller" value="http://localhost:8080/drools-wb-webapp/rest/controller"/>
            <property name="org.kie.server.controller.user" value="admin"/>
            <property name="org.kie.server.controller.pwd" value="admin"/>
            <property name="org.kie.server.bypass.auth.user" value="true"/>
        </system-properties>
    ```
    - Run the following command: _wildfly-10.1.0.Final\bin\add-user.bat -a -u kieserver -p kieserver1! -ro kie-server_ (make sure JAVA_HOME environment variable is pointing to JRE 1.8)
2. Download [KIE Server 6.5.0.Final ee7 war file](http://repo1.maven.org/maven2/org/kie/server/kie-server/6.5.0.Final/kie-server-6.5.0.Final-ee7.war)
    - Rename it kie-server.war and save it to wildfly-10.1.0.Final\standalone\deployments (or deploy manually via web interface on http://127.0.0.1:9990)
3. Setup a new Run configuration in IDEA
    - First make sure you have enabled JBoss integration
        - [Drools](https://confluence.jetbrains.com/display/IntelliJIDEA/Getting+Started+with+JBoss+Technologies+in+IntelliJ+IDEA#GettingStartedwithJBossTechnologiesinIntelliJIDEA-DroolsExpert)
        - [WildFly](https://confluence.jetbrains.com/display/IntelliJIDEA/Getting+Started+with+JBoss+Technologies+in+IntelliJ+IDEA#GettingStartedwithJBossTechnologiesinIntelliJIDEA-JBossEAPandWildFly)
    - Run -> Edit confiugrations -> Add New Configuration (+ button) -> JBoss -> Local
        - Application server: JBoss 10.1.0.Final (if not present then click _Configure..._ and choose the folder where you downloaded Wildfly 10.1.0.Final)
        - Deployment tab -> Add (+ button) -> pick _drools-wb-webapp:war exploded_
        - Remove all tasks in 'Before Launch'
        - Startup/Connection -> Run + Debug -> Startup script -> Untick 'use default' and add --server-config=standalone-full.xml so it looks something like this: _wildfly-10.1.0.Final\bin\standalone.bat --server-config=standalone-full.xml_
4. Now you should be able to run this configuration and the Drools Workbench login page should open automatically
    - Remember on _Drools Workbench - WebApp_ to run Maven 'clean' and then 'install' if you have made GWT code changes you want to include
    - Remember on _Drools Workbench - WebApp_ to run Maven 'clean' and then 'install' if switching from debugging GWT to running normally

##Setting Up GWT Super Dev Mode
_Work in progress - we are still figuring out the best way to run Super Dev Mode_
###Running GWT code server through maven (works partially) 
1. On _Drools Workbench - WebApp_ run Maven 'clean'
2. On _Drools Workbench - WebApp_ run Maven 'csc-gwt:debug' (use the "Execute Maven Goal" button in IDEA - remember to set _Working directory_ to drools-wb-webapp)
3. Attach a IDEA Remote debugger on port 8000 (Now the GWT Development Mode window should pop up)
4. Click the "Launch Default Browser" button when it is available

**There is still some problems in GWT debug mode:**
Currently it is not possible to connect a KIE Execution Server while debugging drools-wb. 
If you try to connect a KIE server to drools-wb, it will produce errors.

###Running GWT code server through IDEA (not working) 
http://blog.athico.com/2014/05/running-drools-wb-with-gwts-superdevmode.html
http://www.uberfireframework.org/docs/tutorial/intellij_idea.html
http://docs.jboss.org/errai/latest/errai/reference/html_single/#_running_and_debugging_in_your_ide_using_gwt_tooling

Use parameters from gwt-maven-plugin <extraJvmArgs> element:
Example:
VM Options: -Xmx2048m -XX:MaxPermSize=256m -Xms1024m -XX:PermSize=128m -Xss1M -XX:CompileThreshold=7000 -Derrai.jboss.home=C:\dev\wildfly-8.1.0.Final -Derrai.marshalling.server.classOutput=src/main/webapp/WEB-INF/classes -Dorg.uberfire.async.executor.safemode=true
Dev Mode parameters: -server org.jboss.errai.cdi.server.gwt.EmbeddedWildFlyLauncher

#Troubleshooting
- If deployment to WildFly fails with a "JGitFileSystemProvider"-related error it most likely means that a Drools-WB instance is already running and connected to your Git repository and has taken a file lock
  - Just stop all instances, delete drools-wb-webapp <deployment> element in the WildFly standalone.xml and try again
- If deployment to WildFly fails with a "SocialUserProfile"-related error, or a "Lucene"-related error, then stop the server and delete the .index and .niogit folders and try again
  - Note: This will reset all your application data (Data Model, rules, etc)

#Tips & Tricks
- To open <b>standalone mode</b> use the following URL: http://localhost:8080/drools-wb-webapp/drools-wb.html?standalone=true&perspective=AuthoringPerspective
  - <u>perspective</u> can be replaced by <u>path</u> parameter which will open the file (in it's appropriate editor) directly, e.g. 
    - Example: http://localhost:8080/drools-wb-webapp/drools-wb.html?standalone=true&path=default://master@uf-playground/mortgages/src/main/resources/org/mortgages/Dummy%2520rule.drl
  - If using the <u>path</u> parameter, an optional <u>editor</u> parameter can be appended to open any "Place" (e.g. the "Search Asset" screen). A "Place" is a WorkbenchPerspective, a WorkbenchScreen, a WorkbenchPopup, a WorkbenchEditor, or a WorkbenchPart 
    - Example: http://localhost:8080/drools-wb-webapp/drools-wb.html?standalone=true&path=default://master@uf-playground/mortgages/src/main/resources/org/mortgages/Dummy%2520rule.drl&editor=FindForm
