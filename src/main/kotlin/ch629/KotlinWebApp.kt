package ch629

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.*

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    routing {
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

fun FlowContent.widget(body: FlowContent.() -> Unit) {
    div { body() }
}