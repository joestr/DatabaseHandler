# DatabaseHandler
The DatabaseHandler is one of currently two dependencies for the Minecraft Spigot/Bukkit Plugin [UltimateZones](https://github.com/DerTod2/UltimateZones).
It supports access to different database Systems and and user-friendly managing of tables and entries over Annotations and the TableEntry class.

**Warning**: This Plugin has no own commands or features for the user, its grants access to the own API for other Plugins
### Features
* Support of different database types: mysql, postgresql and sqlite
* Database Pool for mysql and postgresql
* Easy API to use for other Plugins

### Installation
Download the latest release [here](https://github.com/DerTod2/DatabaseHandler/releases/latest) or compile the source with maven

Simply drag the **DatabaseHandler.jar** in the plugins folder. 
After an restart configure the **config.yml** in the DatabaseHandler folder inside the plugins folder.

### Updates
The plugin includes an updater to fetch the newest version automatically. Every Server start and each 12 hours it checks for new versions. In the `config.yml` are two configuration variables to enable/disable auto-checking for updates and automatic downloads of updates. When auto-download is enabled the server owner only needs to restart or reload the server.

### Included Command
The Plugin contains the command `dh` or `databasehandler`. This command allows to reload the configurationa and plugin, restart the connection pool, search for updates, showing statistics and listing all active connections.

The Permissions are:

**/dh reload** -- ``databasehandler.commands.databasehandler.reload``

**/dh restart** -- ``databasehandler.commands.databasehandler.restart``

**/dh update** -- ``databasehandler.commands.databasehandler.update``

**/dh stats** -- ``databasehandler.commands.databasehandler.stats``

**/dh list** -- ``databasehandler.commands.databasehandler.list``


### Use in own Plugins
To use this plugin for own projects simply add the maven repository and dependency

Repository:
```xml
  <repositories>
    <repository>
      <id>dertod2-repo</id>
      <url>http://nexus.dertod2.net/content/repositories/snapshots/</url>
    </repository>
  </repositories>
```

Dependency:
```xml
  <dependencies>
    <dependency>
    	<groupId>net.dertod2</groupId>
    	<artifactId>DatabaseHandler</artifactId>
    	<version>0.2.0-SNAPSHOT</version>
    	<scope>provided</scope>
    </dependency>
  </dependencies>
```

### JavaDocs
They can be found [here](http://javadocs.dertod2.net/DatabaseHandler/)
