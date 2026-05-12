@file:OptIn(ExperimentalSerializationApi::class)

package com.arrazyfathan

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

class GithubApi(private val httpClient: HttpClient) {

    companion object {
        const val BASE_URL = "https://api.github.com"
    }

    suspend fun getUserActivity(login: String): List<GithubActivityEvent> {
        return httpClient.get("$BASE_URL/users/$login/events").body()
    }
}

@Serializable(with = GithubActivityEventSerializer::class)
data class GithubActivityEvent(
    val id: String,
    val createdAt: String,
    val payload: GitHubActivityEventPayload,
    val public: Boolean,
    val type: String,
    val repo: Repo?,
    val actor: Actor? = null,
    val org: Org? = null
)

@Serializable
sealed class GitHubActivityEventPayload

@Serializable
object UnknownPayload : GitHubActivityEventPayload()

@Serializable
data class IssuesEventPayload(
    val action: String,
    val issue: Issue
) : GitHubActivityEventPayload()

@Serializable
data class IssueCommentEventPayload(
    val action: String,
    val comment: Comment,
    val issue: Issue
) : GitHubActivityEventPayload()

@Serializable
data class PullRequestPayload(
    val action: String,
    val number: Int,
    @SerialName("pull_request")
    val pullRequest: PullRequest
) : GitHubActivityEventPayload()

@Serializable
data class CreateEvent(
    val ref: String?,
    @SerialName("ref_type")
    val refType: String
) : GitHubActivityEventPayload()

@Serializable
data class DeleteEvent(
    val ref: String?,
    @SerialName("ref_type")
    val refType: String
) : GitHubActivityEventPayload()

@Serializable
data class PushEventPayload(
    val ref: String?,
    @SerialName("repository_id")
    val repositoryId: Long? = null,
    @SerialName("push_id")
    val pushId: Long? = null,
    val commits: List<Commits> = listOf(),
    val head: String? = null,
    val before: String? = null
) : GitHubActivityEventPayload()

@Serializable
data class ForkEventPayload(
    @SerialName("forkee")
    val forkee: Forkee? = null
) : GitHubActivityEventPayload()


@Serializable
data class WatchEventPayload(
    val action: String
) : GitHubActivityEventPayload()

@Serializable
data class Commits(
    val url: String,
    val message: String,
    val author: Author,
    val distinct: Boolean,
    val sha: String
) {
    fun adjustedUrl(): String {
        return url.replaceFirst("api.", "")
            .replaceFirst("repos/", "")
    }

    fun markdownUrl(): String = "[${sha.take(7)}](${adjustedUrl()})"
}

@Serializable
data class Author(
    val email: String,
    val name: String
)

@Serializable
data class Issue(
    val title: String,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    val number: Int
)

@Serializable
data class Comment(
    @SerialName("html_url")
    val htmlUrl: String,
    val body: String
)

@Serializable
data class PullRequest(
    @SerialName("html_url")
    val htmlUrl: String? = null,
    val title: String? = null,
    val body: String? = null,
    val url: String? = null,
    val merged: Boolean? = false
)

@Serializable
data class Repo(
    val id: Long? = null,
    val name: String,
    val url: String
) {
    fun adjustedUrl(): String {
        return url.replaceFirst("api.", "")
            .replaceFirst("repos/", "")
    }

    fun markdownUrl(): String = "[$name](${adjustedUrl()})"
}

@Serializable
data class Actor(
    val id: Long,
    val login: String,
    @SerialName("display_login")
    val displayLogin: String? = null,
    @SerialName("gravatar_id")
    val gravatarId: String? = null,
    val url: String,
    @SerialName("avatar_url")
    val avatarUrl: String
)

@Serializable
data class Org(
    val id: Long,
    val login: String,
    @SerialName("gravatar_id")
    val gravatarId: String? = null,
    val url: String,
    @SerialName("avatar_url")
    val avatarUrl: String
)

@Serializable
data class Forkee(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null
)

object GithubActivityEventSerializer : KSerializer<GithubActivityEvent> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("GithubActivityEvent")

    override fun deserialize(decoder: Decoder): GithubActivityEvent {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("GithubActivityEventSerializer only supports JSON")
        val element = jsonDecoder.decodeJsonElement().jsonObject
        val type = element.getValue("type").jsonObjectOrPrimitiveContent()
        val payloadElement = element["payload"] ?: JsonObject(emptyMap())

        return GithubActivityEvent(
            id = element.getValue("id").jsonObjectOrPrimitiveContent(),
            createdAt = element.getValue("created_at").jsonObjectOrPrimitiveContent(),
            payload = decodePayload(jsonDecoder, type, payloadElement),
            public = element["public"]?.jsonObjectOrPrimitiveContent()?.toBooleanStrictOrNull() ?: false,
            type = type,
            repo = element["repo"]?.let { jsonDecoder.json.decodeFromJsonElement(Repo.serializer(), it) },
            actor = element["actor"]?.let { jsonDecoder.json.decodeFromJsonElement(Actor.serializer(), it) },
            org = element["org"]?.let { jsonDecoder.json.decodeFromJsonElement(Org.serializer(), it) }
        )
    }

    override fun serialize(encoder: Encoder, value: GithubActivityEvent) {
        throw SerializationException("GithubActivityEventSerializer only supports deserialization")
    }

    private fun decodePayload(
        jsonDecoder: JsonDecoder,
        eventType: String,
        payload: JsonElement
    ): GitHubActivityEventPayload = when (eventType) {
        "IssuesEvent" -> jsonDecoder.json.decodeFromJsonElement(IssuesEventPayload.serializer(), payload)
        "IssueCommentEvent" -> jsonDecoder.json.decodeFromJsonElement(IssueCommentEventPayload.serializer(), payload)
        "PullRequestEvent" -> jsonDecoder.json.decodeFromJsonElement(PullRequestPayload.serializer(), payload)
        "CreateEvent" -> jsonDecoder.json.decodeFromJsonElement(CreateEvent.serializer(), payload)
        "DeleteEvent" -> jsonDecoder.json.decodeFromJsonElement(DeleteEvent.serializer(), payload)
        "PushEvent" -> jsonDecoder.json.decodeFromJsonElement(PushEventPayload.serializer(), payload)
        "ForkEvent" -> jsonDecoder.json.decodeFromJsonElement(ForkEventPayload.serializer(), payload)
        "WatchEvent" -> jsonDecoder.json.decodeFromJsonElement(WatchEventPayload.serializer(), payload)
        else -> UnknownPayload
    }
}

private fun JsonElement.jsonObjectOrPrimitiveContent(): String =
    this.jsonPrimitive.content
