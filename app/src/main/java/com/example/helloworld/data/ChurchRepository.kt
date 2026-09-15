package com.example.helloworld.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val ok: Boolean = false, val user: UserInfo? = null, val error: String? = null)

@Serializable
data class UserInfo(val id: Long, val username: String, val role: String)

class ChurchRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpCookies)
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            url("https://kingdomfellowshipchristianchurch.onrender.com/")
            header("Origin", "https://kingdomfellowshipchristianchurch.onrender.com")
        }
    }

    suspend fun getSiteContent(): ChurchInfo {
        return try {
            client.get("api/site/content").body()
        } catch (e: Exception) {
            ChurchContent.default
        }
    }

    suspend fun getMedia(): List<MediaItem> {
        return try {
            client.get("api/media").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun login(username: String, password: String): LoginResponse {
        return try {
            val response = client.post("api/admin/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }
            if (response.status == HttpStatusCode.OK) {
                response.body()
            } else {
                val errorMsg = try {
                    response.body<Map<String, String>>()["error"] ?: "Login failed"
                } catch (_: Exception) {
                    "Login failed with status ${response.status}"
                }
                LoginResponse(ok = false, error = errorMsg)
            }
        } catch (e: Exception) {
            LoginResponse(ok = false, error = e.message)
        }
    }

    suspend fun logout() {
        try {
            client.post("api/admin/logout")
        } catch (_: Exception) {}
    }

    suspend fun updateSiteContent(content: ChurchInfo): Boolean {
        return try {
            val response = client.put("api/site/content") {
                contentType(ContentType.Application.Json)
                setBody(content)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadMedia(
        title: String,
        description: String,
        category: String,
        type: String,
        fileBytes: ByteArray,
        fileName: String
    ): Boolean {
        return try {
            val response = client.submitFormWithBinaryData(
                url = "api/media",
                formData = formData {
                    append("title", title)
                    append("description", description)
                    append("category", category)
                    append("type", type)
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            )
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }
}
