package ch629

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table() {
    val id = varchar("id", 10).primaryKey()
    val password = varchar("password", 64)
}

object Comments : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val user = (varchar("user_id", 10) references Users.id)
    val comment = varchar("comment", 1024)
}

fun initDatabase() {
    Database.connect("jdbc:h2:accounts", driver = "org.h2.Driver")

    transaction {
        drop(Users, Comments)
        create(Users, Comments)
        val name = "root"
        val pass = "root" //Would be hashed in a real example
        val text = """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed gravida augue est, ornare commodo
                lectus ornare eu. Donec convallis venenatis efficitur. Duis varius eget lacus quis gravida. Proin
                convallis ac lorem vitae varius. Donec est velit, porttitor at pharetra vel, sollicitudin in
                neque. Aliquam maximus vitae turpis vitae porttitor. Aenean enim nulla, posuere nec mollis non,
                hendrerit a lectus. Integer ut dignissim felis, ac scelerisque purus. Nullam facilisis ligula et
                arcu commodo aliquet. Vestibulum porttitor aliquet venenatis. Vivamus fringilla ante et nisl
                consequat, a porta magna convallis. Nam vitae cursus tortor. Maecenas enim leo, venenatis at
                sodales et, faucibus ac quam. Nunc condimentum et enim ut feugiat. Fusce ac feugiat lorem. Ut at
                arcu volutpat, hendrerit dui eu, dignissim magna.."""

        Users.insert {
            it[id] = name
            it[password] = pass
        }

        for (i in 0..5) {
            Comments.insert {
                it[user] = name
                it[comment] = text
            }
        }

        val commentId = Comments.insert {
            it[user] = name
            it[comment] = text
        } get Comments.id
    }
}