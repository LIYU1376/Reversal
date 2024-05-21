package cn.stars.elixir.account

import com.google.gson.JsonObject
import com.mojang.authlib.Agent
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication
import cn.stars.elixir.compat.Session
import cn.stars.elixir.exception.LoginException
import cn.stars.elixir.utils.set
import cn.stars.elixir.utils.string
import java.net.Proxy

class MojangAccount : MinecraftAccount("Mojang") {
    override var name = ""
    var password = ""
    private var uuid = ""
    private var accessToken = ""

    override val session: Session
        get() {
            if(uuid.isEmpty() || accessToken.isEmpty()) {
                update()
            }

            return Session(name, uuid, accessToken, "mojang")
        }

    override fun update() {
        val userAuthentication = YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT) as YggdrasilUserAuthentication

        userAuthentication.setUsername(name)
        userAuthentication.setPassword(password)

        try {
            userAuthentication.logIn()
            name = userAuthentication.selectedProfile.name
            uuid = userAuthentication.selectedProfile.id.toString()
            accessToken = userAuthentication.authenticatedToken
        } catch (exception: AuthenticationUnavailableException) {
            throw LoginException("Mojang server is unavailable")
        } catch (exception: AuthenticationException) {
            throw LoginException(exception.message ?: "Unknown error")
        }
    }

    override fun toRawJson(json: JsonObject) {
        json["name"] = name
        json["password"] = password
    }

    override fun fromRawJson(json: JsonObject) {
        name = json.string("name")!!
        password = json.string("password")!!
    }
}