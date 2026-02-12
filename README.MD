# Simple Debugger

![License](https://img.shields.io/badge/license-MIT-green)
![Language](https://img.shields.io/badge/language-Java-orange)
![Platform](https://img.shields.io/badge/platform-Eclipse-blue)
![Version](https://img.shields.io/badge/version-1.0.0.202602112043-blue)

## Project Description

**Simple Debugger** is a lightweight plugin for the Eclipse IDE.

This is the first working version of the plugin and currently provides a limited but functional feature set.

In addition to the plugin itself, the repository includes a demonstration application called `target_debug`, which can be used to showcase the plugin’s functionality.

When launched, the plugin automatically sets a breakpoint at the first line of executable code. It allows the user to:

- define basic startup parameters,
- modify local variable values during debugging,
- change the value of a field within the context of the currently executing method (without altering the actual field value in the class),
- view the current call stack,
- inspect the console output.

Further development of the plugin is planned, and additional functionality will be introduced in future versions.

## Installation

The plugin can be installed directly from the GitHub Pages update site.

1. Open Eclipse and go to **Help → Install New Software…**  
   Click **Add…**, enter a name for the update site (for example, *Simple Debugger*), and specify the following URL:
https://sergeiaidinov.github.io/simple_debugger_plugin/simple_debugger_plugin_update_site/


<!-- Screenshot: update site name and URL -->

![Add update site](screenshots/install_add_site.png)

2. After confirming the update site, select **Simple Debugger** from the list and click **Finish**.

<!-- Screenshot: Finish button -->

![Finish installation](screenshots/install_finish.png)

3. When prompted by Eclipse, confirm the installation by clicking **Trust Selected**.

<!-- Screenshot: Trust Selected dialog -->

![Trust selected](screenshots/install_trust.png)

4. Restart Eclipse if prompted to complete the installation.





Target-application:


Plugin:
	  VM-args: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000	
	  
JavaDoc: javadoc -d docs -sourcepath src -subpackages com.gmail.aydinov.sergey.simple_debugger_plugin -classpath "/opt/eclipse-commiters/eclipse/plugins/*" -author -version -windowtitle "Simple Debugger Plugin API" -doctitle "Simple Debugger Plugin Documentation"


Docs: https://sergeiaidinov.github.io/simple_debugger_plugin/docs/

Install: https://sergeiaidinov.github.io/simple_debugger_plugin/simple_debugger_plugin_update_site/

