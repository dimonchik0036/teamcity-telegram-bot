package io.github.dimonchik0036.tcbot

import com.google.gson.Gson
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.GetUpdates
import com.pengrad.telegrambot.response.BaseResponse
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.jetbrains.teamcity.rest.BuildState
import org.jetbrains.teamcity.rest.BuildStatus
import kotlin.concurrent.thread
import kotlin.test.*

class TeamCityTest {
    private val creatorId: Long = 227605930
    private val creatorUsername = "dimonchik0036"
    private val config = Config(
        mapOf(
            "bot_token" to "<TOKEN>",
            "server_url" to "https://teamcity.jetbrains.com",
            "creator_id" to "$creatorId",
            "auth_key" to "123test"
        )
    )
    private val sender = Channel<String>(10)
    private val receiver = Channel<Request>(10)
    private suspend fun receiveText(): String = receiver.receive().text

    private val bot = TeamCityTelegramBot(
        sender = MockTelegramBot(config.botToken, receiver, sender),
        creatorId = config.creatorId,
        authKey = config.authKey,
        commands = ALL_COMMANDS.associate { it.name to it }
    )

    private val myRandomBuilds: Set<TeamCityBuild> = setOf(
        createBuild(),
        createBuild(branchName = "not-rr-branch"),
        createBuild(branchName = "master", buildConfigurationId = "Kotlin"),
        createBuild(branchName = null),
        createBuild(state = BuildState.DELETED, status = null, containChanges = false)
    )

    private val myRunningBuilds: Set<TeamCityBuild> = myRandomBuilds.filter { it.state == BuildState.RUNNING }.toSet()

    @BeforeTest
    fun start() = runBlocking {
        val service: TeamCityService = mockk {
            every { runningBuilds } returns myRunningBuilds
        }

        bot.start(service)
        doTest {
            assertEquals("Run", receiveText())
        }
    }

    @AfterTest
    fun stop() {
        bot.stop()
    }

    @Test
    fun `test login`() = doTest {
        bot.chatStorage.remove(creatorId)
        bot.userStorage.remove(creatorId.toInt())
        assertNull(bot.chatStorage[creatorId])
        assertNull(bot.userStorage[creatorId.toInt()])
        login()
        logout()
        assertNotNull(bot.chatStorage[creatorId])
        assertNotNull(bot.userStorage[creatorId.toInt()])
    }

    @Test
    fun `test bad auth`() = doTest {
        invokeCommand("login", BotCommand.FAILED_AUTH, "", "12")
        assertFalse(bot.chatStorage.getValue(creatorId).isAuth)
    }

    @Test
    fun `test not permitted`() = doTest {
        invokeCommand("filter", BotCommand.NOT_PERMITTED)
    }

    @Test
    fun `test filter`() {
        testFilter("branch", "rr/.*")
        testFilter("build_configuration", "Kotlin_dev_AggregateBranch")
    }

    @Test
    fun `test invalid filter`() = doTest(withAuth = true) {
        checkInvalidCommandWithHelp("filter", "", "branch", "???")
    }

    @Test
    fun `test invalid filter check`() = doTest(withAuth = true) {
        checkInvalidCommandWithHelp("filter_check", "", "branch", "???")
    }

    @Test
    fun `test filter check`() = Filter.FILTER_NAMES.forEach {
        testFilterCheck(it, "pa..ern", "pattern", true)
        testFilterCheck(it, "master", "rr/dd", false)
    }

    @Test
    fun `test count`() = doTest(withAuth = true) {
        val command = "count"
        checkInvalidCommandWithHelp(command, "", "???")
        invokeCommand(command, myRunningBuilds.size.toString(), "running_builds")
    }

    @Test
    fun `test simple`() = doTest(timeout = 5_000, withAuth = true) {
        val builds = generateBuilds(10)
        checkMessage(
            baseSet = builds,
            sendAction = bot::buildInfoHandler,
            baseTransformer = TeamCityBuild::markdownDescription,
            requestTransformer = Request::text
        )
    }

    @Test
    fun `test running`() = doTest(withAuth = true) {
        addFilters("branch rr/.*")

        var running = getRunningBuilds("all")
        var expected = myRunningBuilds.map(TeamCityBuild::markdownDescription).toSet()
        assertEquals(expected, running)

        val filter = getFilter()
        running = getRunningBuilds("")
        expected = myRunningBuilds.filter(filter::matches).map(TeamCityBuild::markdownDescription).toSet()
        assertEquals(expected, running)
    }

    private suspend fun checkInvalidCommandWithHelp(command: String, vararg args: String) {
        val help = ALL_COMMANDS.first { it.name == command }.help
        invokeCommand(command = command, args = *args, expected = help)
    }

    private fun testFilterCheck(filterName: String, pattern: String, text: String, expected: Boolean) =
        doTest(withAuth = true) {
            assertEquals(expected, Regex(pattern).matches(text))
            addFilters("$filterName $pattern")
            invokeCommand("filter_check", expected.toString(), "$filterName $text")
        }

    private fun testFilter(filterName: String, pattern: String) = doTest(withAuth = true) {
        val expectedFilter = Filter()
        expectedFilter.setFilterByName(filterName, Regex(pattern))
        val expected = myRandomBuilds.filter(expectedFilter::matches).toSet()

        addFilters("$filterName $pattern")
        val actualFilter = getFilter()
        val actual = myRandomBuilds.filter(actualFilter::matches).toSet()

        assertEquals(expected, actual)

        checkMessage(
            baseSet = myRandomBuilds,
            sendAction = bot::buildInfoHandler,
            baseTransformer = TeamCityBuild::markdownDescription,
            requestTransformer = Request::text,
            baseFilter = actualFilter::matches,
            expectedCount = expected.size
        )
    }

    private suspend fun addFilters(vararg filters: String) = invokeCommand("filter", BotCommand.SUCCESS, *filters)

    private suspend fun getRunningBuilds(filter: String): Set<String> {
        sender.send(createUpdate(command = "running", textOrArguments = filter))
        return receiveText().split("\n\n").toSet()
    }

    private fun getFilter(): Filter = bot.chatStorage.getValue(creatorId).filter

    private fun resetFilter() {
        val chat = bot.chatStorage[creatorId] ?: return
        val filter = chat.filter
        for (name in Filter.FILTER_NAMES) {
            assertTrue(filter.setFilterByName(name, Filter.DEFAULT_FILTER))
        }
    }

    private suspend fun <T, R> checkMessage(
        baseSet: Set<T>,
        sendAction: (T) -> Unit,
        baseTransformer: (T) -> R,
        requestTransformer: (Request) -> R,
        baseFilter: (T) -> Boolean = { true },
        expectedCount: Int? = null
    ) {
        GlobalScope.launch {
            baseSet.forEach(sendAction)
        }

        val expected = baseSet.filter(baseFilter).map(baseTransformer).toSet()
        if (expectedCount != null) assertEquals(expectedCount, expected.size)
        val requests = hashSetOf<R>()
        repeat(expected.size) {
            requests += requestTransformer(receiver.receive())
        }
        assertEquals(expected, requests)
    }

    private suspend fun invokeCommand(command: String, expected: String, vararg args: String) {
        assertTrue(expected.isNotBlank())
        if (args.isEmpty()) {
            sender.send(createUpdate(command = command))
            assertEquals(expected, receiveText())
        } else args.forEach {
            sender.send(createUpdate(command = command, textOrArguments = it))
            assertEquals(expected, receiveText())
        }
    }

    private fun doTest(
        timeout: Long = 3_000,
        withAuth: Boolean = false,
        block: suspend CoroutineScope.() -> Unit
    ) = runBlocking {
        resetFilter()
        withTimeout(timeout) {
            if (withAuth) login()
            block()
            if (withAuth) logout()
        }
    }

    private suspend fun login() {
        invokeCommand("login", BotCommand.SUCCESS_AUTH, "${config.authKey}")
        assertTrue(bot.chatStorage.getValue(creatorId).isAuth)
    }

    private suspend fun logout() {
        invokeCommand("logout", BotCommand.LOGOUT_MESSAGE)
        assertFalse(bot.chatStorage.getValue(creatorId).isAuth)
    }

    private fun generateBuilds(count: Int): Set<TeamCityBuild> {
        val set = hashSetOf<TeamCityBuild>()
        repeat(count) {
            set += createBuild()
        }
        return set
    }

    private var buildId = 42
    private fun createBuild(
        state: BuildState = BuildState.RUNNING,
        status: BuildStatus? = BuildStatus.SUCCESS,
        branchName: String? = "rr/dimonchik0036/test-teamcity-bot",
        buildConfigurationId: String = "Kotlin_dev_AggregateBranch",
        containChanges: Boolean = true
    ): TeamCityBuild {
        buildId++
        return TeamCityBuild(
            id = buildId.toString(),
            name = "Beautiful build name",
            number = "#1.3.30-dev-$buildId",
            branchName = branchName,
            lastAuthor = if (containChanges) "dimonchik0036" else null,
            status = status,
            state = state,
            url = "https://github.com/dimonchik0036/teamcity-telegram-bot/$buildId",
            buildConfigurationId = buildConfigurationId
        )
    }

    private fun createUpdate(
        command: String? = null,
        textOrArguments: String = "",
        chatId: Long = creatorId,
        userId: Int = creatorId.toInt(),
        username: String = creatorUsername
    ): String {
        val commandWithArgs = if (textOrArguments.isEmpty()) command else "$command $textOrArguments"
        val resultText = if (command != null) {
            """"/$commandWithArgs", "entities": [ {
            "type":"bot_command",
            "offset": 0,
            "length":${command.length + 1}
        }]
            """
        } else textOrArguments
        return """
            {
               "message": {
                    "message_id":1,
                    "chat": {
                        "id": $chatId,
                        "username":"$username"
                    },
                    "from":{
                        "id": $userId,
                        "username":"$username",
                        "first_name":"Dmitry"
                    },
                    "text": $resultText
               }
           }
"""
    }
}

private data class Request(
    val chatId: Long,
    val text: String
)

private class MockTelegramBot(
    token: String,
    private val outChannel: Channel<Request>,
    private val inChannel: Channel<String>
) :
    TelegramBot(token) {
    override fun <T : BaseRequest<out BaseRequest<*, *>, *>?, R : BaseResponse?> execute(request: BaseRequest<T, R>): R {
        val message = request.parameters["text"] as? String
        val id = request.parameters["chat_id"] as? Long
        if (message != null && id != null) runBlocking {
            outChannel.send(Request(id, message))
        }
        return GSON.fromJson("{ \"ok\":\"true\" } ", request.responseType)
    }

    override fun removeGetUpdatesListener() = runBlocking { inChannel.send("stop") }

    override fun setUpdatesListener(listener: UpdatesListener, request: GetUpdates) {
        thread {
            runBlocking {
                for (message in inChannel) {
                    if (message == "stop") return@runBlocking
                    else {
                        val update: Update = GSON.fromJson(message, Update::class.java)
                        listener.process(listOf(update))
                    }
                }
            }
        }
    }
}

private val GSON = Gson()
