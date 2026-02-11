# Simple Debugger

![License](https://img.shields.io/badge/license-MIT-green)
![Language](https://img.shields.io/badge/language-Java-orange)
![Platform](https://img.shields.io/badge/platform-Eclipse-blue)
![Version](https://img.shields.io/badge/version-1.0.0.202602112043-blue)

A lightweight Eclipse plugin that simplifies debugging Java applications with enhanced logging and step control.



Target-application:


Plugin:
	  VM-args: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000	
	  
JavaDoc: javadoc -d docs -sourcepath src -subpackages com.gmail.aydinov.sergey.simple_debugger_plugin -classpath "/opt/eclipse-commiters/eclipse/plugins/*" -author -version -windowtitle "Simple Debugger Plugin API" -doctitle "Simple Debugger Plugin Documentation"


Docs: https://sergeiaidinov.github.io/simple_debugger_plugin/docs/

Install: https://sergeiaidinov.github.io/simple_debugger_plugin/simple_debugger_plugin_update_site/

