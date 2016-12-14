Developing Drools and jBPM
==========================

**If you want to build or contribute to a droolsjbpm project, [read this document](https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/README.md).**

**It will save you and us a lot of time by setting up your development environment correctly.**
It solves all known pitfalls that can disrupt your development.
It also describes all guidelines, tips and tricks.
If you want your pull requests (or patches) to be merged into master, please respect those guidelines.

CSC Version
===========
This branch contains modifications to 6.5.0.Final made by CSC Scandihealth.<br>
The modifications customizes Drools Workbench for embedded use within the LPR project (under construction).  

How To Setup For Internal CSC Development (Windows 7)
==========================
_Work in progress - will be updated as we find out if all these steps are necessary or not._

1. Make sure you run Windows 7 (64bit) and have at least 8BG physical memory 
2. Install Git for windows, IntelliJ IDEA 2016.3, Java JDK 8 64bit, maven (>=3.3.9)
3. Configure Maven to use our internal Artifactory maven repository
   - Copy \\sh\shares\scvcomn\LPR3\udviklersetup\settings.xml to your %USERPROFILE%\\.m2 folder (create folder if necessary)
4. Configure Git to use long paths
   - Open a command prompt with administrator access and run: <code>git config --system core.longpaths true</code>
5. Open IDEA (64 bit) -> Default Settings -> Maven
   - Change Maven home directory to your maven 3.3.9 installation
   - Change Maven -> Runner -> VM Options to <code>-Xms256m -Xmx3056m</code>
   - Change Maven -> Runner -> JRE to <code>1.8</code>
6. Open IDEA (64 bit) -> Help -> Edit Custom VM Options 
   - Change the existing -Xmx setting to <code>-Xmx3056m</code>    
7. Using IDEA (64 bit): Checkout the repository from Git https://github.com/scandihealth/drools-wb.git
   - Open the pom.xml as a project
   - Change git branch to origin/csc-6.5.0
8. You are now ready to run Maven commands
