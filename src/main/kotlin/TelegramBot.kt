import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {
    val botToken = args[0]
    val myBot = TelegramBotService(botToken)

    while (true) {
        Thread.sleep(2000)
        val updates = myBot.getUpdates()

        val chatIdRegex = "\"id\":(.+?),".toRegex()
        val matchResultChatId: MatchResult? = chatIdRegex.find(updates)
        val groupsId = matchResultChatId?.groups
        val chatId: Int? = groupsId?.get(1)?.value?.toInt()

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
        val matchResult: MatchResult? = messageTextRegex.find(updates)
        val groups = matchResult?.groups
        val text = groups?.get(1)?.value
        myBot.sendMessageToBot(chatId, text)
    }
}


class TelegramBotService(private val botToken: String) {
    private var updateId = 0

    init {
        getUpdates()
    }

    fun getUpdates(): String {
        val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        val updates = response.body()
        val startUpdateId = updates.lastIndexOf("update_id")
        val endUpdateId = updates.lastIndexOf(",\n\"message\"")
        if (startUpdateId == -1 || endUpdateId == -1) return ""
        val updateString = updates.substring(startUpdateId + 11, endUpdateId)
        updateId = updateString.toInt() + 1
        return updates
    }

    fun sendMessageToBot(chatId: Int?, text: String?) {
        val urlSendMessage = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$text"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val client: HttpClient = HttpClient.newBuilder().build()
        client.send(request, HttpResponse.BodyHandlers.discarding())
    }
}
