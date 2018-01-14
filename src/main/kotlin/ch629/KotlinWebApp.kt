package ch629

import ch629.Comments.comment
import ch629.Comments.user
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.content.files
import io.ktor.content.static
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.request.receive
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.sessions.*
import io.ktor.util.ValuesMap
import io.ktor.util.hex
import kotlinx.html.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.main() {
    initDatabase()
    val sessionKey = hex("03e156f6058a13813816065")

    install(DefaultHeaders)
    install(CallLogging)
    install(Sessions) {
        cookie<LoginSession>("SESSION") {
            transform(SessionTransportTransformerMessageAuthentication(sessionKey))
        }
    }

    routing {
        static("static") {
            files("css")
            files("js")
        }

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
                call.sessions.set(LoginSession(principle!!.name))
                call.respondRedirect("/comments") //TODO: Redirect to postComment when implemented
            }
        }

        get("/login") {
            call.respondHtml {
                head { addStyles() }
                body {
                    form(action = "/authenticate", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
                        div("form-group") {
                            p {
                                +"user:"
                                textInput(name = "user", classes = "form-control")
                            }

                            p {
                                +"password:"
                                passwordInput(name = "pass", classes = "form-control")
                            }

                            p {
                                submitInput(classes = "btn btn-primary") { value = "Login" }
                            }
                        }
                    }
                }
            }
        }

        get("/") {
            call.respondHtml {
                head {
                    title { +"Testing Site" }
                    addStyles()
                }
                body {
                    h1 { +"Sample application with HTML builders" }
                    widget {
                        +"Widget Function"
                    }
                }
            }
        }

        get("/comments") {
            if (!call.isLoggedIn()) println("Not Logged In")
            else println("Logged In")

            call.respondHtml {
                head {
                    title { +"Comments" }
                    addStyles()
                }
                body {
                    val comments = findAllComments()
                    if (comments.isEmpty()) +"No Comments Found."
                    else div("comments") { comments.forEach { commentWidget(it) } }
                }
            }
        }

        get("/postComment") {
            if (!call.isLoggedIn()) call.respondRedirect("/login")
            else {
                call.respondHtml {
                    head {
                        title { +"Post Comment" }
                        addStyles()
                    }

                    body {
                        form(action = "/postComment", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
                            div("form-group") {
                                p {
                                    div("row") {
                                        div("col-1") {
                                            +"Comment:"
                                        }
                                        div("col-10") {
                                            textArea(classes = "form-control") {
                                                name = "comment"
                                            }
                                        }
                                    }
                                }

                                p {
                                    submitInput(classes = "btn btn-primary") { value = "Send" }
                                }
                            }
                        }
                    }
                }
            }
        }

        post("/postComment") {
            if (call.isLoggedIn()) {
                val name = call.sessions.get<LoginSession>()!!.name
                val params = call.receive<ValuesMap>()

                val commentText = params["comment"]

                transaction {
                    Comments.insert {
                        it[user] = name
                        it[comment] = commentText!!
                    }
                }

                call.respondRedirect("/comments")
            } else call.respondRedirect("/login")
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

data class Comment(val name: String, val comment: String)

fun findAllComments(): Array<Comment> {
    var comments: List<Comment> = emptyList()

    transaction {
        comments = Comments.selectAll().map { Comment(it[user], it[comment]) }
    }

    return comments.toTypedArray()
}

fun FlowContent.widget(body: FlowContent.() -> Unit) {
    div { body() }
}

fun HEAD.addStyles() {
    styleLink("https://cdnjs.cloudflare.com/ajax/libs/normalize/7.0.0/normalize.min.css")
    styleLink("static/styles.css")
    styleLink("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta.3/css/bootstrap.min.css")
}

fun FlowContent.commentWidget(comment: Comment) {
    div("row comment") {
        div("col-2 comment-name") {
            +comment.name
        }

        div("col-8 comment-text") {
            +comment.comment
        }
    }
}

fun ApplicationCall.isLoggedIn(): Boolean = sessions.get<LoginSession>() != null //This would usually be done with a session id connected to the database

data class LoginSession(val name: String)