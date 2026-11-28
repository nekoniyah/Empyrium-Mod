package quest.nekoniyah.empyriummod.database

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.world.item.ItemStack
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

object EmpyriumDatabase {
    var db: Database? = null
    fun init(dbDir: Path) {

        // Créer le répertoire dans data/
        Files.createDirectories(dbDir)

        // Récupère le path de la db
        val dbFile = dbDir.resolve("data.db")

        // Connexion à la db
        db = Database.connect("jdbc:sqlite:${dbFile.toAbsolutePath()}", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction(db) {
            SchemaUtils.create(
                PlayerShopTable, PlayerCacheTable, PlayerCraftsTable, CurrencyBalanceTable, FactionsTable
            )
        }
    }

    fun close() {
        db?.let { TransactionManager.closeAndUnregister(it) }
        db = null
    }

    fun decodeStringToItemStack(string: String): ItemStack? {
        return runCatching {
            ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(string)).result().orElse(null)
        }.getOrNull()
    }
}