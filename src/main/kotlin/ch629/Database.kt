package ch629

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table() {
    val id = varchar("id", 10).primaryKey()
    val password = varchar("password", 64)
}

object Comments : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val user = (varchar("user_id", 10) references Users.id)
    val comment = varchar("comment", 255)
}

fun initDatabase() {
    Database.connect("jdbc:h2:accounts", driver = "org.h2.Driver")

    transaction {
        create(Users, Comments)
        val name = "root"
        val pass = "root" //Would be hashed in a real example
        val text = "Lipsum..."

        Users.insert {
            it[id] = name
            it[password] = pass
        }

        val commentId = Comments.insert {
            it[user] = name
            it[comment] = text
        } get Comments.id


        println("Comment Id $commentId")

        println(Comments.select { Comments.id eq commentId }.firstOrNull()!![Comments.comment])
    }
}