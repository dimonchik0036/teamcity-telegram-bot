package io.github.dimonchik0036.tcbot.core

import io.github.dimonchik0036.tcbot.core.teamcity.TeamCityDatabase
import io.github.dimonchik0036.tcbot.core.teamcity.TeamCityType
import io.github.dimonchik0036.tcbot.core.telegram.TelegramDatabase
import io.github.dimonchik0036.tcbot.core.telegram.TelegramType

interface Database : TeamCityDatabase, TelegramDatabase
interface DataType : TeamCityType, TelegramType