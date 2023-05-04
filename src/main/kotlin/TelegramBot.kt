import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val json = Json { ignoreUnknownKeys = true }

    val botToken = args[0]
    val myBot = TelegramBotService(botToken)

    var lastUpdateId = 0L
    val trainers = HashMap<Long, LearnWordsTrainer>()

    while (true) {
        val responseString = myBot.getUpdates(lastUpdateId)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, trainers, myBot) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun handleUpdate(
    firstUpdate: Update,
    json: Json,
    trainers: HashMap<Long, LearnWordsTrainer>,
    myBot: TelegramBotService
) {
    val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id ?: return
    val text = firstUpdate.message?.text
    val data = firstUpdate.callbackQuery?.data

    val trainer = trainers.getOrPut(chatId) {
        LearnWordsTrainer("$chatId.txt", 4, 3)
    }

    if (text?.lowercase() == "/start" || data == EXIT_MENU) {
        myBot.sendMenu(json, chatId)
    }
    if (data?.lowercase() == STATISTICS_CLICKED) {
        val statistics = trainer.getStatistics()
        myBot.sendMessageToBot(
            json,
            chatId,
            "Выучено ${statistics.learnedWords} из ${statistics.totalWords} | ${statistics.percent}%"
        )
    }

    if (data?.lowercase() == LEARN_WORD_CLICKED) {
        myBot.checkNextQuestionAndSend(json, trainer, chatId)
    }

    if (data?.lowercase() == RESET_CLICKED) {
        trainer.resetProgress()
        myBot.sendMessageToBot(json, chatId, "Прогресс сброшен!")
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val inputAnswer = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        if (trainer.checkAnswer(inputAnswer)) {
            myBot.sendMessageToBot(json, chatId, "Правильно!")
        } else {
            myBot.sendMessageToBot(
                json,
                chatId, "Неправильно! ${trainer.question?.correctAnswer?.original} " +
                        "- это [ ${trainer.question?.correctAnswer?.translated} ]"
            )
        }
        myBot.checkNextQuestionAndSend(json, trainer, chatId)
    }
}


class TelegramBotService(private val botToken: String) {
    companion object {
        const val PREFIX_URL = "https://api.telegram.org/bot"
    }

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$PREFIX_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()

    }

    fun sendMessageToBot(json: Json, chatId: Long, text: String): String {

        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = text,
        )
        val requestBodyString = json.encodeToString(requestBody)

        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMenu(json: Json, chatId: Long): String {
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(text = "Изучать слова", callbackData = LEARN_WORD_CLICKED),
                        InlineKeyboard(text = "Статистика", callbackData = STATISTICS_CLICKED),
                    ),
                    listOf(
                        InlineKeyboard(text = "Сбросить статистику", callbackData = RESET_CLICKED),
                    )
                )
            )
        )

        val requestBodyString = json.encodeToString(requestBody)

        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun sendQuestion(json: Json, chatId: Long, question: Question): String {
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"

        val answerButtons: List<List<InlineKeyboard>> =
            question.variants.mapIndexed { index, word ->
                listOf(InlineKeyboard(text = word.translated, callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"))
            }
        val exitButton = listOf(listOf(InlineKeyboard(text = "Выйти в меню", callbackData = EXIT_MENU)))
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.original,
            replyMarkup = ReplyMarkup(answerButtons + exitButton)
        )
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun checkNextQuestionAndSend(json: Json, trainerBot: LearnWordsTrainer, chatId: Long) {
        val question = trainerBot.getNextQuestion()
        if (question == null) {
            sendMessageToBot(json, chatId, "Вы выучили все слова!")
            return
        } else sendQuestion(json, chatId, question)
    }
}
