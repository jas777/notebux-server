package xyz.notebux

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.notebux.entities.NoteShares
import xyz.notebux.entities.Notes
import xyz.notebux.entities.Users
import xyz.notebux.entities.VerificationCodes
import xyz.notebux.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureRouting()

    val config = HikariConfig()

    config.dataSourceClassName = "com.impossibl.postgres.jdbc.PGDataSource"

    config.addDataSourceProperty("databaseName", environment.config.property("database.db").getString())
    config.addDataSourceProperty("serverName", environment.config.property("database.ip").getString())
    config.addDataSourceProperty("portNumber", environment.config.property("database.port").getString().toInt())

    config.username = environment.config.property("database.username").getString()
    config.password = environment.config.property("database.password").getString()

    val hikariDataSource = HikariDataSource(config)

    Database.connect(hikariDataSource)

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Notes, Users, NoteShares, VerificationCodes)
    }
}
