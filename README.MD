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

## Table of Contents

- [Project Description](#project-description)
- [Installation](#installation)
- [Usage](#usage)
- [Documentation](https://sergeiaidinov.github.io/simple_debugger_plugin/docs/)
- [License](#license)

## Installation

The plugin can be installed directly from the GitHub Pages update site.

1. Open Eclipse and go to **Help → Install New Software…**  
   Click **Add…**, enter a name for the update site (for example, *Simple Debugger*), and specify the following URL:
https://sergeiaidinov.github.io/simple_debugger_plugin/simple_debugger_plugin_update_site/


<!-- Screenshot: update site name and URL -->

![Add update site](screenshots/install_add_site.png)

2. After confirming the update site, select **Simple Debugger** from the list and click **Finish**:

<!-- Screenshot: Finish button -->

![Finish installation](screenshots/install_finish.png)

3. When prompted by Eclipse, confirm the installation by clicking **Trust Selected**:

<!-- Screenshot: Trust Selected dialog -->

![Trust selected](screenshots/install_trust.png)

4. Restart Eclipse if prompted to complete the installation.

## Usage

After installation, the plugin can be launched from:

**Run → Simple Debugger**

The plugin provides basic debugging functionality. Follow these steps:

1. **Select the debug configuration**  
   Choose which target application to debug.

   ![Select debug configuration](screenshots/usage_select_configuration.png)

2. **Edit configuration parameters**  
   Modify startup parameters, if necessary.

   ![Edit configuration parameters](screenshots/usage_edit_parameters.png)

3. **Breakpoint at the first line**  
   When launched, the plugin automatically sets a breakpoint at the first line of executable code.

   ![First line breakpoint](screenshots/usage_first_line_breakpoint.png)

4. **Breakpoint at a previously set point**  
   The plugin stops at user-defined breakpoints as well.

   ![Previously set breakpoint](screenshots/usage_existing_breakpoint.png)

5. **Confirm closing the debugger window**  
   When stopping the debugger, confirm any prompts to close the window.

   ![Confirm closing window](screenshots/usage_confirm_close.png)

6. **Debugger disabled notification**  
   After stopping, the plugin notifies that the debugger is disabled and the Java virtual machine continues running.

   ![Debugger disabled notification](screenshots/usage_debugger_disabled.png)

## License

This project is licensed under the [MIT License](LICENSE).

