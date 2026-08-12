package com.denham.skyline.data.repo

import com.denham.skyline.core.PlayerApiResponse
import com.denham.skyline.core.UserInfo
import com.denham.skyline.core.XtreamAccount
import com.denham.skyline.core.XtreamError
import com.denham.skyline.data.api.XtreamApi
import retrofit2.HttpException
import java.io.IOException

sealed interface AuthResult {
    data class Success(val response: PlayerApiResponse) : AuthResult
    data class Failure(val message: String) : AuthResult
}

class AuthRepository(private val apiProvider: (XtreamAccount) -> XtreamApi) {

    /**
     * A call to player_api.php with no action authenticates and returns
     * user_info + server_info. `auth` and `status` both gate access.
     */
    suspend fun authenticate(account: XtreamAccount): AuthResult = try {
        val response = apiProvider(account).authenticate()
        val user = response.userInfo
        when {
            user == null || !user.auth ->
                AuthResult.Failure("Sign-in failed. Check the server address, username and password.")
            !user.status.equals("Active", ignoreCase = true) ->
                AuthResult.Failure(statusMessage(user))
            else -> AuthResult.Success(response)
        }
    } catch (e: HttpException) {
        AuthResult.Failure(XtreamError.messageFor(e.code()))
    } catch (e: IOException) {
        AuthResult.Failure("Couldn't reach the server. Check the address, port and your connection.")
    } catch (e: Exception) {
        AuthResult.Failure("The server sent an unexpected response. Double-check this is an Xtream portal URL.")
    }

    private fun statusMessage(user: UserInfo): String = when (user.status.lowercase()) {
        "expired" -> "This subscription has expired."
        "banned" -> "This account has been banned by the provider."
        "disabled" -> "This account is disabled."
        else -> "Account status: ${user.status.ifBlank { "unknown" }}."
    }
}
