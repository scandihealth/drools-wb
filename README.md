## DXC version of Drools Workbench for LPR
This repository is used by DXC (CSC Scandihealth) to modify Drools Workbench for use with LPR.<br>
This branch contains modifications to [6.5.0.Final](https://github.com/kiegroup/drools-wb/tree/6.5.0.Final) made by DXC (CSC Scandihealth).
  
## New Dependency to <code>csc-gwt-maven-plugin</code>
Running the **gwt-maven-plugin** to compile the gwt code in the <code>drools-wb-webapp</code> project under Windows 7 gives an error (see https://github.com/gwt-maven-plugin/gwt-maven-plugin/issues/113).<br>
To fix this we have backported the 2.8.0 fix to our own fork of <code>gwt-maven-plugin</code> 2.7.0 found here: https://github.com/scandihealth/gwt-maven-plugin/tree/csc-2.7.0 <br>
We have packaged that fork using maven and uploaded the jar to our own internal Artifactory maven repository so that it can be resolved when building this <code>drools-wb</code> fork internally at DXC.

# How To Setup For Development (Windows 7 64bit)
## Setting Up Build Configuration
1. Install Git for windows, IntelliJ IDEA & Java JDK 8 64bit
2. Configure Maven to use our internal Scandihealth Artifactory maven repository
   - Copy \\\sh\shares\scvcomn\LPR3\udviklersetup\settings.xml to your %USERPROFILE%\\.m2 folder (create folder if necessary)
3. Configure Git to use long paths
   - Open a command prompt with administrator rights and run: <code>git config --system core.longpaths true</code>
4. Open IDEA (64 bit) by opening idea64.exe -> Default Settings -> Maven
   - Change Maven -> Runner -> VM Options to <code>-Xms256m -Xmx3G</code>
5. Using IDEA (64 bit): Checkout the repository from Git https://github.com/scandihealth/drools-wb.git
   - Open the root pom.xml as a project
   - Make sure to "Enable Auto Import" in Maven and add GWT facets (IDEA should ask you these questions in popup balloons)
   - Change File -> Project Structure -> Project -> Project SDK to <code>1.8</code> (64bit)
6. Click the "Toggle 'Skip Tests' Mode" button in the Maven Projects sidebar window
7. You are now ready to run Maven commands and develop
    - Run Drools Workbench (root) -> install (takes 10-15 minutes)

## Setting Up Run Configuration
1. Download and unzip [WildFly 10.1.0.Final](http://download.jboss.org/wildfly/10.1.0.Final/wildfly-10.1.0.Final.zip)
    - Insert this into _wildfly-10.1.0.Final\standalone\configuration\standalone.xml_ after the `<extensions>` element
    (modify to your liking)
    ```
        <system-properties>
            <property name="org.uberfire.nio.git.dir" value="C:\dev\drools-wb-devdb"/>
            <property name="org.uberfire.nio.git.ssh.cert.dir" value="C:\dev\drools-wb-devdb"/>
            <property name="org.uberfire.metadata.index.dir" value="C:\dev\drools-wb-devdb"/>
            <property name="org.guvnor.m2repo.dir" value="C:\dev\drools-wb-devdb"/>
        </system-properties>
    ```
2. Setup a new Run configuration in IDEA
    - First make sure you have enabled JBoss integration
        - [Drools](https://confluence.jetbrains.com/display/IntelliJIDEA/Getting+Started+with+JBoss+Technologies+in+IntelliJ+IDEA#GettingStartedwithJBossTechnologiesinIntelliJIDEA-DroolsExpert)
        - [WildFly](https://confluence.jetbrains.com/display/IntelliJIDEA/Getting+Started+with+JBoss+Technologies+in+IntelliJ+IDEA#GettingStartedwithJBossTechnologiesinIntelliJIDEA-JBossEAPandWildFly)
    - Run -> Edit confiugrations -> Add New Configuration (+ button) -> JBoss -> Local
        - Application server: JBoss 10.1.0.Final (if not present then click _Configure..._ and choose the folder where you unzipped Wildfly 10.1.0.Final)
        - Deployment tab -> Add (+ button) -> pick _drools-wb-webapp:war exploded_
3. Now you should be able to run this configuration and the Drools Workbench login page should open automatically
    - Remember on _Drools Workbench - WebApp_ to run Maven 'clean' and then 'install' if you have made changes to GWT code you want to include
    - Remember on _Drools Workbench - WebApp_ to run Maven 'clean' and then 'install' if switching from debugging GWT to running normally
4. Login with admin/admin    

## Setting Up Code Style
1. Download the official droolsjbpm IDEA Code Style and save it in %USERPROFILE%\\.\<YOUR IDEA VERSION>\\config\\codestyles  
   - Get it here: https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/ide-configuration/intellij-configuration/code-style/intellij-code-style_droolsjbpm-java-conventions.xml
2. Change the Code Style used by IDEA for this project
   - Select Settings -> Editor -> Code Style -> Scheme: <code>Drools and jBPM: Java Conventions</code>
3. Change .properties file encoding to UTF-8
   - Settings -> Editor -> File Encodings -> Set Default encoding for properties files to UTF-8
   - Note: normal i18n properties files must be in ISO-8859-1 as specified by the java ResourceBundle contract.
   - Note on note: GWT i18n properties files override that and must be in UTF-8 as specified by the GWT contract.   
4. Follow some of the other (TBD) code style recommendations described here (e.g. XML tab spaces) 
    - https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/README.md#developing-with-intellij

## Debugging client code with GWT Super Dev Mode
1. On _Drools Workbench - WebApp_ run Maven 'clean' then 'csc-gwt:debug' 
    - This can be setup as a 1-click job by using the "Execute Maven Goal" button in IDEA and set _Working directory_ to the drools-wb-webapp folder and enter 'clean csc-gwt:debug -e' in _Command line_
2. Attach a IDEA Remote debugger on port 8000 (Now the GWT Development Mode window should pop up)
3. Click the "Launch Default Browser" button when it is available

# Troubleshooting
- If deployment to WildFly fails with a file system or persistence related error it most likely means that a previous Drools-WB Java process was not terminated and has taken a file lock or JVM address binding
  - Make sure drools-wb-webapp is not already deployed (check standalone.xml \<deployment> tags and check \wildfly-10.1.0.Final\standalone\deployments folder)
  - Close the server and kill all instances of java.exe in your Windows task manager
- If this doesn't fix the rpoblem, then stop the server and delete the .index and .niogit folders in the dir specified by "org.uberfire.nio.git.dir" and "org.uberfire.metadata.index.dir" and try again
  - Note: This will reset all your application data (Data Model, rules, etc)

# Tips & Tricks
- To open <b>standalone mode</b> use the following URL: http://localhost:8080/drools-wb-webapp/drools-wb.html?standalone=true&perspective=AuthoringPerspective
  - <u>perspective</u> can be replaced by <u>path</u> parameter which will open the file (in it's appropriate editor) directly, e.g. 
    - Example: http://localhost:8080/drools-wb-webapp/drools-wb.html?standalone=true&path=default://master@uf-playground/mortgages/src/main/resources/org/mortgages/Dummy%2520rule.drl
  - If using the <u>path</u> parameter, an optional <u>editor</u> parameter can be appended to open any "Place" (e.g. the "Search Asset" screen). A "Place" is a WorkbenchPerspective, a WorkbenchScreen, a WorkbenchPopup, a WorkbenchEditor, or a WorkbenchPart 
    - Example: http://localhost:8080/drools-wb-webapp/drools-wb.html?standalone=true&path=default://master@uf-playground/mortgages/src/main/resources/org/mortgages/Dummy%2520rule.drl&editor=FindForm
