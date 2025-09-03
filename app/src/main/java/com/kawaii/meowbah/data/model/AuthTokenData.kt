package com.kawaii.meowbah.data.model

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("grant_type")
    val grantType: String,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("code")
    val code: String,
    // redirect_uri is often not strictly required for installed app flows
    // when using serverAuthCode with Google Sign-In, especially if the
    // client_id is correctly configured as an Android type.
    // We can add it if Google's token endpoint complains.
    // @SerializedName("redirect_uri")
    // val redirectUri: String? = null
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("refresh_token")
    val refreshToken: String?, // Refresh token might not always be returned
    @SerializedName("scope")
    val scope: String,
    @SerializedName("token_type")
    val tokenType: String
)
