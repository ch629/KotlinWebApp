package ch629

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.html.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.main() {
    initDatabase()

    install(DefaultHeaders)
    install(CallLogging)

    routing {
        route("/authenticate") {
            authentication {
                formAuthentication("user", "pass", challenge = FormAuthChallenge.Redirect({ _, _ -> "/login" })) { credential: UserPasswordCredential ->
                    when {
                        validLogin(credential.name, credential.password) -> UserIdPrincipal(credential.name)
                        else -> null
                    }
                }
            }

            handle {
                val principle = call.authentication.principal<UserIdPrincipal>()
                call.respondText("Hello, ${principle?.name}", ContentType.Text.Html)
            }
        }

        get("/login") {
            call.respondHtml {
                body {
                    form(action = "/authenticate", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
                        p {
                            +"user:"
                            textInput(name = "user")
                        }

                        p {
                            +"password:"
                            passwordInput(name = "pass")
                        }

                        p {
                            submitInput { value = "Login" }
                        }
                    }
                }
            }
        }

        get("/") {
            call.respondHtml {
                head {
                    title { +"Testing Site" }
                }
                body {
                    h1 { +"Sample application with HTML builders" }
                    widget {
                        +"Widget Function"
                    }
                }
            }
        }
    }
}

fun validLogin(name: String, pass: String): Boolean {
    var found = false

    transaction {
        found = Users.select { (Users.id eq name) and (Users.password eq pass) }.any()
    }

    return found
}

fun FlowContent.widget(body: FlowContent.() -> Unit) {
    div { body() }
}