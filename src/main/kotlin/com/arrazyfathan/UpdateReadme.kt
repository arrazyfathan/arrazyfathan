package com.arrazyfathan

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId

class UpdateReadmeCommand : CliktCommand() {

    private val outputFile by option("-o", help = "The README.md file to write")
        .file()
        .required()

    override fun run() {
        val json = createJson()
        createHttpClient(json).use { ktorHttpClient ->
            val githubActivity = fetchGithubActivity(ktorHttpClient)
            // val blogActivity = fetchBlogActivity(ktorHttpClient) TODO try medium blogs

            val existingWakaSection = outputFile.takeIf { it.exists() }
                ?.readText()
                ?.extractSection(WAKA_START_MARKER, WAKA_END_MARKER)
            val newReadMe = createReadMe(githubActivity)
                .preserveSection(WAKA_START_MARKER, WAKA_END_MARKER, existingWakaSection)
            outputFile.writeText(newReadMe)
        }
    }
}

private fun fetchGithubActivity(
    client: HttpClient
): List<ActivityItem> {
    val githubApi = GithubApi(client)
    val activity = runBlocking { githubApi.getUserActivity("arrazyfathan") }
    return activity
        .filter { it.public }
        .mapNotNull { event ->
            when (val payload = event.payload) {
                UnknownPayload -> return@mapNotNull null
                is IssuesEventPayload -> {
                    ActivityItem(
                        "${payload.action} issue [#${payload.issue.number}](${payload.issue.htmlUrl}) on ${event.repo?.markdownUrl()}: \"${payload.issue.title}\"",
                        event.createdAt
                    )
                }

                is IssueCommentEventPayload -> {
                    ActivityItem(
                        "commented on [#${payload.issue.number}](${payload.comment.htmlUrl}) in ${event.repo?.markdownUrl()}",
                        event.createdAt
                    )
                }

                is PullRequestPayload -> {
                    val action =
                        if (payload.pullRequest.merged == true) "merged" else payload.action
                    val repoText = event.repo?.markdownUrl() ?: "unknown repository"
                    val pullRequestLink = payload.pullRequest.htmlUrl?.let { "[#${payload.number}]($it)" }
                        ?: "#${payload.number}"
                    val pullRequestTitle = payload.pullRequest.title?.let { ": \"$it\"" } ?: ""
                    ActivityItem(
                        text = "$action PR $pullRequestLink to $repoText$pullRequestTitle",
                        timestamp = event.createdAt
                    )
                }

                is CreateEvent -> {
                    ActivityItem(
                        text = "created ${payload.refType}${payload.ref?.let { " `$it`" } ?: ""} on ${event.repo?.markdownUrl()}",
                        timestamp = event.createdAt
                    )
                }

                is DeleteEvent -> {
                    ActivityItem(
                        text = "deleted ${payload.refType}${payload.ref?.let { " `$it`" } ?: ""} on ${event.repo?.markdownUrl()}",
                        timestamp = event.createdAt
                    )
                }

                is ForkEventPayload -> {
                    val forkee = payload.forkee
                    ActivityItem(
                        text = "forked repository ${event.repo?.markdownUrl()}${forkee?.htmlUrl?.let { " to [${forkee.fullName ?: forkee.name ?: "fork"}]($it)" } ?: ""}",
                        timestamp = event.createdAt
                    )
                }

                is PushEventPayload -> {
                    val firstCommit = payload.commits.firstOrNull()
                    val pushTarget = payload.ref?.substringAfterLast("/")
                    ActivityItem(
                        text = if (firstCommit != null) {
                            "pushed ${firstCommit.markdownUrl()} to ${event.repo?.markdownUrl()}: \"${firstCommit.message}\""
                        } else {
                            "pushed${pushTarget?.let { " `$it`" } ?: ""} to ${event.repo?.markdownUrl()}${
                                payload.head?.let {
                                    " at `${
                                        it.take(
                                            7
                                        )
                                    }`"
                                } ?: ""
                            }"
                        },
                        timestamp = event.createdAt
                    )
                }

                is WatchEventPayload -> {
                    ActivityItem(
                        text = "watched repository ${event.repo?.markdownUrl()}",
                        timestamp = event.createdAt
                    )
                }
            }
        }
        .take(10)
}

fun createJson() = Json {
    isLenient = true
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

fun createHttpClient(json: Json) = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(json = json)
    }

    install(HttpTimeout) {
        this.requestTimeoutMillis = 60000
        this.connectTimeoutMillis = 60000
        this.socketTimeoutMillis = 60000
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
}

data class ActivityItem(
    val text: String,
    val timestamp: String
) {
    override fun toString(): String {
        val timestamp = Instant.parse(timestamp)
        return "**${timestamp.atZone(ZoneId.of("America/New_York")).toLocalDate()}** — $text"
    }
}

fun main(argv: Array<String>) {
    UpdateReadmeCommand().main(argv)
}

private const val WAKA_START_MARKER = "<!--START_SECTION:waka-->"
private const val WAKA_END_MARKER = "<!--END_SECTION:waka-->"

private fun String.extractSection(
    startMarker: String,
    endMarker: String
): String? {
    val startIndex = indexOf(startMarker)
    if (startIndex == -1) return null

    val endIndex = indexOf(endMarker, startIndex)
    if (endIndex == -1) return null

    return substring(startIndex, endIndex + endMarker.length)
}

private fun String.preserveSection(
    startMarker: String,
    endMarker: String,
    replacementSection: String?
): String {
    if (replacementSection == null) return this

    val existingSection = extractSection(startMarker, endMarker) ?: return this
    return replace(existingSection, replacementSection)
}
