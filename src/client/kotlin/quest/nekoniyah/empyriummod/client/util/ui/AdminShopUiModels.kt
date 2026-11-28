package quest.nekoniyah.empyriummod.client.util.ui

/** Colours shared by both admin shop views (ARGB). */
object ShopTheme {
    const val WINDOW = 0xF0141419.toInt()
    const val PANE = 0xFF1E1F27.toInt()
    const val BORDER = 0xFF3A3C4A.toInt()
    const val SLOT = 0xFF2A2C37.toInt()
    const val HOVER = 0xFF3D4050.toInt()
    const val ACCENT = 0xFFE0B34C.toInt()

    const val NEUTRAL = 0xFF3F4354.toInt()
    const val BUY = 0xFF2F7D4A.toInt()
    const val SELL = 0xFF8A5A20.toInt()
    const val DANGER = 0xFF8C3038.toInt()

    // Text colours (RGB)
    const val TEXT = 0xFFFFFF
    const val MUTED = 0x9A9DAC
    const val GOLD = 0xE0B34C
    const val GOOD = 0x6EE08A
    const val BAD = 0xFF6B6B
}

/** Window measurements, based on the scaled screen size. */
data class ShopLayout(
    val width: Int, val height: Int,
    val sidebar: Int, val center: Int, val side: Int, val body: Int
) {
    /** Item slots per row: pane padding and scrollbar take 14 pixels. */
    val columns: Int get() = maxOf(1, (center - 14) / SLOT_STEP)

    companion object {
        const val SLOT_STEP = 22

        fun from(screenWidth: Int, screenHeight: Int): ShopLayout {
            val width = minOf(screenWidth - 8, 430).coerceAtLeast(312)
            val height = minOf(screenHeight - 8, 260).coerceAtLeast(220)
            val inner = width - 12
            val wide = inner >= 360
            val sidebar = if (wide) 90 else 76
            val side = if (wide) 148 else 126
            // 12 padding + 18 header + 12 status + 8 gaps
            return ShopLayout(width, height, sidebar, inner - sidebar - side - 8, side, height - 50)
        }
    }
}

/** What the admin is typing in the item editor. */
data class ShopItemForm(
    val itemId: Int = -1, // -1 = new item
    var slot: Int = -1,
    var itemName: String = "",
    var maxAmount: Int = 64,
    var amount: String = "1",
    var currency: String = "",
    var buyPrice: String = "",
    var sellPrice: String = ""
) {
    val isNew: Boolean get() = itemId == -1

    /** Blank means disabled (-1). Invalid input becomes 0, which is rejected. */
    fun parsePrice(value: String): Int = if (value.isBlank()) -1 else value.toIntOrNull() ?: 0

    /** For user feedback only. The server checks again. */
    fun error(): String? {
        val count = amount.toIntOrNull()
        val buy = parsePrice(buyPrice)
        val sell = parsePrice(sellPrice)

        return when {
            isNew && slot < 0 -> "Pick an item first."
            count == null || count !in 1..maxAmount -> "Amount must be 1-$maxAmount."
            buy == 0 || sell == 0 -> "Prices must be above 0."
            buy == -1 && sell == -1 -> "Set at least one price."
            buy != -1 && sell > buy -> "Sell price can't exceed buy price."
            currency.isEmpty() -> "No currency available."
            else -> null
        }
    }
}