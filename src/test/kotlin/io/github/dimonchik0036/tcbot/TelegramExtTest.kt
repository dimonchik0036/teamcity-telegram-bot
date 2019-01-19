import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.MessageEntity
import io.github.dimonchik0036.tcbot.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelegramExtTest {
    @Test
    fun `test create command`() {
        assertEquals("/login test", createCommand("login", "test").text())
        assertEquals("/login", createCommand("login").text())
    }

    @Test
    fun `test command`() {
        val message = createCommand("login", "test")
        assertTrue(message.isCommand)
    }

    @Test
    fun `test not command`() {
        val message = createMessage("login test")
        assertFalse(message.isCommand)
    }

    @Test
    fun `test not command with other entity`() {
        val message = createMessage("login test", arrayOf(createMessageEntity(MessageEntity.Type.text_link, 6, 0)))
        assertFalse(message.isCommand)
    }

    @Test
    fun `test command name`() {
        val command = "login"
        assertEquals(command, createCommand(command, "test").command)
        assertEquals(command, createCommand("$command@bot_name", "test").command)
    }

    @Test
    fun `test command arguments`() {
        val arguments = "first second"
        val command = createCommand("login", arguments)
        assertEquals(arguments, command.commandArguments)
        assertEquals(listOf("first", "second"), command.commandArgumentsList)
    }

    @Test
    fun `test command name with at`() {
        val command = "login"
        assertEquals(command, createCommand(command, "test").commandWithAt)
        val commandWithAt = "login@botname"
        assertEquals(commandWithAt, createCommand(commandWithAt, "test").commandWithAt)
    }
}

private fun createCommand(command: String, args: String? = null) = createMessage(
    text = args?.let { "/$command $it" } ?: "/$command",
    entities = arrayOf(createMessageEntity(MessageEntity.Type.bot_command, command.length + 1, 0))
)

fun createMessage(text: String, entities: Array<MessageEntity>? = null): Message = object : Message() {
    override fun text(): String = text
    override fun entities(): Array<MessageEntity>? = entities
}

fun createMessageEntity(type: MessageEntity.Type, length: Int, offset: Int): MessageEntity = object : MessageEntity() {
    override fun offset(): Int = offset
    override fun type(): Type = type
    override fun length(): Int = length
}
