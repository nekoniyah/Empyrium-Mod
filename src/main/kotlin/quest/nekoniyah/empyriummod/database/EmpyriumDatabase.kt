package quest.nekoniyah.empyriummod.database

import com.mojang.serialization.JsonOps
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import quest.nekoniyah.empyriummod.EmpyriumMod
import quest.nekoniyah.empyriummod.economy.currencies.Currency
import java.sql.Connection


object EmpyriumDatabase {
    var db: Database? = null
    fun init() {
        db = Database.connect("jdbc:sqlite:./config/${EmpyriumMod.MOD_ID}/data.db", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction(db) {
            SchemaUtils.create(PlayerShopTable)
        }

    }

    fun sellItem(player: Player, itemstack: ItemStack, cur: Currency, curPrice: Int) {

        val jsonElement = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, itemstack).resultOrPartial().orElseThrow()

        val itemString = jsonElement.asJsonObject.toString()

        transaction(db) {
            PlayerShopTable.insert {
                it[sellerUuid] = player.stringUUID
                it[itemData] = itemString
                it[currency] = cur.identifier
                it[price] = curPrice
                it[itemCount] = itemstack.count
            }
        }


    }
}