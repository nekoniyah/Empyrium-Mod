package quest.nekoniyah.empyriummod.client.util.ui

import quest.nekoniyah.empyriummod.client.screens.EmpyriumScreen

data class LayoutPosition(val x: Int, val y: Int)

open class EmpyriumScreenLayout(val rows: Int, val columns: Int, title: String, override val identifier: String) :
    EmpyriumScreen(title) {
    open val boxWidth = 7 // pixels
    open val boxHeight = 7

    val grid: HashMap<LayoutPosition, ScreenLayoutElement?> = hashMapOf()

    init {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                grid[LayoutPosition(row, column)] = null
            }
        }
    }

    fun setElement(row: Int, col: Int, element: ScreenLayoutElement): Boolean {
        val fullBoxes: ArrayList<LayoutPosition> = arrayListOf()

        for (x in 0 until element.xSize) {
            val el = grid[LayoutPosition(x, col)]
            if (el != null && el.layer == element.layer) {
                fullBoxes.add(LayoutPosition(x, col))
            }
        }

        for (y in 0 until element.ySize) {
            val el = grid[LayoutPosition(row, y)]
            if (el != null && el.layer == element.layer) {
                fullBoxes.add(LayoutPosition(row, y))
            }
        }

        if (fullBoxes.isNotEmpty()) return false
        grid[LayoutPosition(row, col)] = element
        return true;
    }

    override fun init() {
        super.init()

        for ((key, element) in grid) {
            if (element != null) {
                if (element.widget) this.addRenderableWidget(element.build(key.x, key.y))
                else this.addRenderableOnly(element.build(key.x, key.y))
            }
        }
    }
}