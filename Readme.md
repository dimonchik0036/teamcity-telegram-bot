# TeamCity Telegram bot

## Build
```bash
./gradlew :fatJar
```

## Run
1. Create config [file](src/main/resources/tcbot.properties)
2. Run bot `java -jar teamcity-telegram-bot-0.1.jar <path/to/config>`
3. Add a bot to the channel or start a private conversation with it.
4. Enter `/login`  
    4.1 You can add filtering. See the next paragraph. 
5. Excellent! Watch the build in Telegram.

## Command list
`[]` - optional, `<>` - required argument.
* `/help [command_name]` - Show a command description.  
* `/commands` - Description commands like BotFather.  
* `/count <name>` - Number of name.  Available: `running_builds`.
* `/login [auth_key]` - Enable notifications.  
* `/logout` -  Disable notifications.  
* `/filter <filter_name> <pattern>` - Add filtering. Available: `build_configuration`, `branch`. 
* `/filter_check <filter_name> <text>` - Check current filter.   
`<pattern>` - is a [regular](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) expression, for example `rr/.*`.

## Config
* `server_url` - TeamCity server, example `https://teamcity.jetbrains.com`.  
  * Required  
* `bot_token` - Telegram bot token.  
  * Required  
* `creator_id` - Telegram user id.  
  * Required  
* `auth_key` - The key required to enable notifications.  
  * Optional
  * Default: without authorization
* `root_project_id` - List of projects separated by a space. Example `Kotlin_dev Kotlin_1320`. 
  * Optional
  * Default: all roots  
* `cascade_mode` - Subprojects processing mode.`RECURSIVELY` checked all subproject, `ONLY_ROOT` checked only root projects.
  * Optional
  * Default: `ONLY_ROOT`
* `projects_delay` - Delay between checks of new build configurations.  
  * Optional
  * Default: 10m  
* `updates_delay` - Delay between checks of new updates.   
  * Optional  
  * Default: 30s
* [Auth](https://confluence.jetbrains.com/display/TCD10/REST+API#RESTAPI-RESTAuthentication) - User or guest.
    * Default: guest
    * `teamcity_username` - TeamCity username.  
      * Optional  
    * `teamcity_password` - TeamCity password.  
      * Optional  