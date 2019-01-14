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
    4.1 You can add filtering by branch name or build configuration id. See the next paragraph. 
5. Excellent! Watch the build in Telegram.

## Command list
`[]` - optional, `<>` - required argument.
* `/help [command_name]` - Show a command description.  
* `/login [auth_key]` - Enable notifications.  
* `/logout` -  Disable notifications.  
* `/branch_filter <pattern>` - Add filtering by branch.  
* `/branch_filter_check <branch_name>` - Check current branch filter.  
* `/build_filter <pattern>` - Add filtering by build configuration id.  
* `/build_filter_check <build_config_id>` - Check current build filter.  
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
* `root_project_id` - List of projects separated by a space. Default behavior checks all roots. Example `Kotlin_dev Kotlin_1320`. 
  * Also all subprojects will be checked
  * Optional  
* `projects_delay` - Delay between checks of new build configurations. Default is 10m.  
  * Optional  
* `updates_delay` - Delay between checks of new updates. Default is 30s.  
  * Optional  
* [Auth](https://confluence.jetbrains.com/display/TCD10/REST+API#RESTAPI-RESTAuthentication) - Default - guest.
    * `teamcity_username` - TeamCity username.  
      * Optional  
    * `teamcity_password` - TeamCity password.  
      * Optional  