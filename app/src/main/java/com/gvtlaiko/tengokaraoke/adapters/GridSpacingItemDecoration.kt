package com.gvtlaiko.tengokaraoke.adapters

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // Posición del ítem
        val column = position % spanCount // Columna del ítem

        if (includeEdge) {
            // Con espaciado en los bordes
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) { // Fila superior
                outRect.top = spacing
            }
            outRect.bottom = spacing // Espaciado inferior para todos
        } else {
            // Sin espaciado en los bordes
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing // Espaciado superior para las filas que no son la primera
            }
        }
    }
}