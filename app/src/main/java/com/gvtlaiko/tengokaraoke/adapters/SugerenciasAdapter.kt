package com.gvtlaiko.tengokaraoke.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.gvtlaiko.tengokaraoke.R

class SugerenciasAdapter(
    context: Context,
    sugerencias: List<String>
) : ArrayAdapter<String>(context, 0, sugerencias) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_sugerencia,
            parent,
            false
        )

        val item = getItem(position)

        val tvTexto = view.findViewById<TextView>(R.id.tvSugerencia)

        tvTexto.text = item

        return view
    }
}