package com.notes.server

import com.notes.server.models.AuthResponse
import com.notes.server.models.ErrorResponse
import com.notes.server.models.LoginRequest
import com.notes.server.models.RefreshResponse
import com.notes.server.models.RefreshTokenRequest
import com.notes.server.models.RegisterRequest
import com.notes.server.models.UserProfile
import com.notes.server.repository.defaultUserRepository
import com.notes.server.security.JwtTokenManager
import com.notes.server.security.PasswordHasher
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthGatewayTest {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        defaultUserRepository.clear()
    }

    @Test
    fun testPasswordHasherBcrypt() {
        val password = "SuperSecretPassword123!"
        val hash = PasswordHasher.hashPassword(password)
        
        assertFalse(hash == password, "Hash must not equal plaintext password")
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"), "Must be a BCrypt hash")
        assertTrue(PasswordHasher.verifyPassword(password, hash), "Password verification must succeed")
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", hash), "Wrong password must fail")
    }

    @Test
    fun testJwtTokenGenerationAndVerification() {
        val token = JwtTokenManager.generateAccessToken("usr_123", "alice@notes.org", "Alice")
        val decoded = JwtTokenManager.verifyToken(token)

        assertNotNull(decoded)
        assertEquals("usr_123", decoded.subject)
        assertEquals("alice@notes.org", decoded.getClaim("email").asString())
        assertEquals("Alice", decoded.getClaim("name").asString())
        assertEquals(JwtTokenManager.ISSUER, decoded.issuer)
    }

    @Test
    fun testUserRegistrationSuccess() = testApplication {
        application { module() }

        val request = RegisterRequest(
            email = "test@example.com",
            password = "SecurePassword2026!",
            displayName = "Test User"
        )

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val auth = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals("test@example.com", auth.email)
        assertEquals("Test User", auth.displayName)
        assertTrue(auth.userId.startsWith("usr_"))
        assertTrue(auth.accessToken.isNotBlank())
        assertTrue(auth.refreshToken.startsWith("ref_"))
        assertEquals(3600L, auth.expiresIn)

        // Verify account stored with BCrypt hash, not plaintext
        val stored = defaultUserRepository.findByEmail("test@example.com")
        assertNotNull(stored)
        assertTrue(PasswordHasher.verifyPassword("SecurePassword2026!", stored.passwordHash))
        assertFalse(stored.passwordHash.contains("SecurePassword2026!"))
    }

    @Test
    fun testDuplicateEmailRegistrationConflict() = testApplication {
        application { module() }

        val request = RegisterRequest(
            email = "duplicate@example.com",
            password = "Password123!",
            displayName = "User One"
        )

        val firstResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        // Attempt second registration with same email
        val secondResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request.copy(displayName = "User Two")))
        }
        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
        val error = json.decodeFromString<ErrorResponse>(secondResponse.bodyAsText())
        assertTrue(error.error.contains("already exists"))
    }

    @Test
    fun testInvalidEmailOrShortPassword() = testApplication {
        application { module() }

        // Invalid email format
        val badEmailResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("notanemail", "ValidPass123!")))
        }
        assertEquals(HttpStatusCode.BadRequest, badEmailResp.status)

        // Password too short (< 8 chars)
        val shortPassResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("good@example.com", "short")))
        }
        assertEquals(HttpStatusCode.BadRequest, shortPassResp.status)
    }

    @Test
    fun testUserLoginSuccess() = testApplication {
        application { module() }

        // Register user first
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("login@example.com", "MySecret123!", "Login User")))
        }

        // Log in
        val loginResp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest("login@example.com", "MySecret123!")))
        }
        assertEquals(HttpStatusCode.OK, loginResp.status)
        val auth = json.decodeFromString<AuthResponse>(loginResp.bodyAsText())
        assertEquals("login@example.com", auth.email)
        assertTrue(auth.accessToken.isNotBlank())
        assertTrue(auth.refreshToken.startsWith("ref_"))
    }

    @Test
    fun testUserLoginInvalidCredentials() = testApplication {
        application { module() }

        // Non-existent user
        val noUserResp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest("unknown@example.com", "SomePassword123!")))
        }
        assertEquals(HttpStatusCode.Unauthorized, noUserResp.status)

        // Existing user with wrong password
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("user@example.com", "CorrectPassword123!")))
        }

        val wrongPassResp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest("user@example.com", "WrongPassword!")))
        }
        assertEquals(HttpStatusCode.Unauthorized, wrongPassResp.status)
    }

    @Test
    fun testRefreshTokenRotation() = testApplication {
        application { module() }

        // Register
        val regResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("rotate@example.com", "RotatePass123!")))
        }
        val auth = json.decodeFromString<AuthResponse>(regResp.bodyAsText())

        // Refresh token request
        val refreshResp = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RefreshTokenRequest(auth.refreshToken)))
        }
        assertEquals(HttpStatusCode.OK, refreshResp.status)
        val refreshed = json.decodeFromString<RefreshResponse>(refreshResp.bodyAsText())
        assertTrue(refreshed.accessToken.isNotBlank())

        // Invalid refresh token
        val invalidResp = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RefreshTokenRequest("invalid_token_xyz")))
        }
        assertEquals(HttpStatusCode.Unauthorized, invalidResp.status)
    }

    @Test
    fun testProtectedEndpointGetMe() = testApplication {
        application { module() }

        // Attempt without token -> 401 Unauthorized
        val unauthResp = client.get("/api/v1/auth/me")
        assertEquals(HttpStatusCode.Unauthorized, unauthResp.status)

        // Register and get token
        val regResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest("me@example.com", "PasswordMe123!", "Profile User")))
        }
        val auth = json.decodeFromString<AuthResponse>(regResp.bodyAsText())

        // Access /me with valid Bearer token
        val authResp = client.get("/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, authResp.status)
        val profile = json.decodeFromString<UserProfile>(authResp.bodyAsText())
        assertEquals("me@example.com", profile.email)
        assertEquals("Profile User", profile.displayName)
        assertEquals(auth.userId, profile.userId)

        // Access /me with invalid Bearer token
        val badTokenResp = client.get("/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer invalid.jwt.payload")
        }
        assertEquals(HttpStatusCode.Unauthorized, badTokenResp.status)
    }
}
