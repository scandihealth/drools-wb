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

1. Make sure you run Windows 7 (64bit) 
2. Install Git for windows, IntelliJ IDEA 2016.3, Java JDK 8 64bit, maven (>=3.3.9)
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
12. Download [WildFly 8.1.0.Final](http://download.jboss.org/wildfly/8.1.0.Final/wildfly-8.1.0.Final.zip) and unzip it to your drools-wb parent folder. 
    - Example: If drools-wb is placed here: C:\dev\drools-wb then WildFly should be placed here: C:\dev\wildfly-8.1.0.Final
    - Insert this into _wildfly-8.1.0.Final\standalone\configuration\standalone.xml_ after the `<extensions>` element
    (modify paths to your liking)
    ```
            <system-properties>
               <property name="org.uberfire.nio.git.dir" value="C:\dev\drools-wb-devdb"/>
               <property name="org.uberfire.nio.git.ssh.cert.dir" value="C:\dev\drools-wb-devdb"/>
               <property name="org.uberfire.metadata.index.dir" value="C:\dev\drools-wb-devdb"/>
               <property name="org.guvnor.m2repo.dir" value="C:\dev\drools-wb-devdb"/>
               <property name="org.kie.demo" value="false"/>
           </system-properties>
    ```
13. You are now ready to run Maven commands and develop
    - Run Drools Workbench (root) -> package (takes 10-15 minutes)
14. Set up a new Run configuration
    - First make sure you have enabled JBoss integration
        - [Drools](https://confluence.jetbrains.com/display/IntelliJIDEA/Getting+Started+with+JBoss+Technologies+in+IntelliJ+IDEA#GettingStartedwithJBossTechnologiesinIntelliJIDEA-DroolsExpert)
        - [WildFly](https://confluence.jetbrains.com/display/IntelliJIDEA/Getting+Started+with+JBoss+Technologies+in+IntelliJ+IDEA#GettingStartedwithJBossTechnologiesinIntelliJIDEA-JBossEAPandWildFly)
    - Run -> Edit confiugrations -> Add New Configuration (+ button) -> JBoss -> Local
        - Application server: JBoss 8.1.0.Final (if not present then click _Configure..._ and choose the folder where you downloaded Wildfly 8.1.0.Final)
        - After launch: http://localhost:8080/drools-wb-webapp/drools-wb.html
        - Deployment tab -> Add (+ button) -> pick _drools-wb-webapp:war exploded_
        - Remove all tasks in 'Before Launch'
15. Now you should be able to run this configuration and the Drools Workbench login page should open automatically
    - Remember on _Drools Workbench - WebApp_ to run Maven 'clean' and then 'install' if you have made GWT code changes you want to include
    - Remember on _Drools Workbench - WebApp_ to run Maven 'clean' and then 'install' if switching from debugging GWT to running normally

##How To Setup GWT Super Dev Mode
_Work in progress - we are still figuring out the best way to run Super Dev Mode_
###Running GWT code server through maven (works!) 
Based on: https://tkobayas.wordpress.com/2015/11/20/debugging-kie-workbench/

1. On _Drools Workbench - WebApp_ run Maven 'clean'
2. On _Drools Workbench - WebApp_ run Maven 'csc-gwt:debug' (use the "Execute Maven Goal" button in IDEA - remember to set _Working directory_ to drools-wb-webapp)
3. Attach a remote debugger on port 8000 (Now the GWT Development Mode window should pop up)
4. Click the "Launch Default Browser" button when it is available

###Running GWT code server through IDEA (not working) 
http://blog.athico.com/2014/05/running-drools-wb-with-gwts-superdevmode.html
http://www.uberfireframework.org/docs/tutorial/intellij_idea.html
http://docs.jboss.org/errai/latest/errai/reference/html_single/#_running_and_debugging_in_your_ide_using_gwt_tooling

Use parameters from gwt-maven-plugin <extraJvmArgs> element:
Example:
VM Options: -Xmx2048m -XX:MaxPermSize=256m -Xms1024m -XX:PermSize=128m -Xss1M -XX:CompileThreshold=7000 -Derrai.jboss.home=C:\dev\wildfly-8.1.0.Final -Derrai.marshalling.server.classOutput=src/main/webapp/WEB-INF/classes -Dorg.uberfire.async.executor.safemode=true
Dev Mode parameters: -server org.jboss.errai.cdi.server.gwt.EmbeddedWildFlyLauncher
