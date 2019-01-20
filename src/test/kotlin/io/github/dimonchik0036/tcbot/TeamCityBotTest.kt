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

    private val bot = TeamCityTelegramBot(
        sender = MockTelegramBot(config.botToken, receiver, sender),
        creatorId = config.creatorId,
        authKey = config.authKey,
        commands = ALL_COMMANDS.associate { it.name to it }
    )

    @BeforeTest
    fun start() = runBlocking {
        val service: TeamCityService = mockk {
            every { runningBuilds } returns emptySet()
        }

        bot.start(service)
        doTest {
            val expected = Request(creatorId, "Run")
            assertEquals(expected, receiver.receive())
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
    }

    @Test
    fun `test bad auth`() = doTest {
        sender.send(createUpdate("", creatorId, creatorId.toInt(), creatorUsername, "login"))
        sender.send(createUpdate("12", creatorId, creatorId.toInt(), creatorUsername, "login"))
        assertEquals(failedAuth, receiver.receive())
        assertEquals(failedAuth, receiver.receive())
        assertFalse(bot.chatStorage.getValue(creatorId).isAuth)
    }

    @Test
    fun `test not permitted`() = doTest {
        logout()
        sender.send(createUpdate("", creatorId, creatorId.toInt(), creatorUsername, "filter"))
        assertEquals(notPermitted, receiver.receive())
    }

    @Test
    fun `test simple`() = doTest(timeout = 10_000) {
        login()
        val builds = generateBuilds(10)
        checkMessage(
            baseSet = builds,
            baseTransformer = TeamCityBuild::markdownDescription,
            requestTransformer = Request::text,
            sendAction = bot::buildInfoHandler
        )
    }

    private suspend fun <T, R> checkMessage(
        baseSet: Set<T>,
        baseTransformer: (T) -> R,
        requestTransformer: (Request) -> R,
        sendAction: (T) -> Unit
    ) {
        GlobalScope.launch {
            baseSet.forEach(sendAction)
        }

        val expected = baseSet.map(baseTransformer).toSet()
        val requests = hashSetOf<R>()
        repeat(expected.size) {
            requests += requestTransformer(receiver.receive())
        }
        assertEquals(expected, requests)
    }

    private fun doTest(timeout: Long = 3_000, block: suspend CoroutineScope.() -> Unit) = runBlocking {
        withTimeout(timeout) {
            block()
        }
    }

    private val successAuth = Request(creatorId, BotCommand.SUCCESS_AUTH)
    private val failedAuth = Request(creatorId, BotCommand.FAILED_AUTH)
    private val notPermitted = Request(creatorId, BotCommand.NOT_PERMITTED)
    private val logoutRequest = Request(creatorId, BotCommand.LOGOUT_MESSAGE)
    private suspend fun CoroutineScope.login() {
        launch {
            sender.send(
                createUpdate(
                    " ${config.authKey}",
                    creatorId,
                    creatorId.toInt(),
                    creatorUsername,
                    "login"
                )
            )
        }
        assertEquals(successAuth, receiver.receive())
        assertTrue(bot.chatStorage.getValue(creatorId).isAuth)
    }

    private suspend fun CoroutineScope.logout() {
        launch { sender.send(createUpdate("", creatorId, creatorId.toInt(), creatorUsername, "logout")) }
        assertEquals(logoutRequest, receiver.receive())
        assertFalse(bot.chatStorage.getValue(creatorId).isAuth)
    }

    private fun generateBuilds(count: Int): Set<TeamCityBuild> {
        val set = hashSetOf<TeamCityBuild>()
        repeat(count) {
            set += createBuild()
        }
        return set
    }

    var buildId = 42
    private fun createBuild(
        state: BuildState = BuildState.RUNNING,
        status: BuildStatus = BuildStatus.SUCCESS,
        branchName: String = "rr/dimonchik0036/test-teamcity-bot",
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
}

data class Request(
    val chatId: Long,
    val text: String
)

fun createUpdate(
    textOrArguments: String,
    chatId: Long,
    userId: Int,
    username: String,
    command: String? = null
): String {
    val resultText = if (command != null) {
        """"/$command $textOrArguments", "entities": [ {
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

class MockTelegramBot(
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
