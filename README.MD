Target-application:


Plugin:
	  VM-args: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000	
	  
JavaDoc: javadoc -d doc -sourcepath src -subpackages com.gmail.aydinov.sergey.simple_debugger_plugin -classpath "/opt/eclipse-commiters/eclipse/plugins/*" -author -version -windowtitle "Simple Debugger Plugin API" -doctitle "Simple Debugger Plugin Documentation"