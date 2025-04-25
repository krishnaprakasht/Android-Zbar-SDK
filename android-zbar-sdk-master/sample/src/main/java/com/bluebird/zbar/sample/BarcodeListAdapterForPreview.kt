package com.bluebird.zbar.sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bluebird.zbar.sample.roomdb.Barcode

class BarcodeListAdapterForPreview(private val barcodeList: MutableList<Barcode>
) : RecyclerView.Adapter<BarcodeListAdapterForPreview.BarcodeViewHolder>() {

    inner class BarcodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val barcodeId = itemView.findViewById<TextView>(R.id.barcodeId)
        private val count = itemView.findViewById<TextView>(R.id.count)
        private val symbology = itemView.findViewById<TextView>(R.id.symbology)

        fun bind(barcode: Barcode) {
            barcodeId.text = barcode.barcode
            count.text = barcode.count.toString()
            symbology.text = "${barcode.symbol} (${barcode.timeStamp})"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.list_item_preview, parent, false
        )
        return BarcodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        holder.bind(barcodeList[position])
    }

    override fun getItemCount(): Int {
        return barcodeList.size
    }

    fun updateData(newList: List<Barcode>) {
        barcodeList.clear()
        barcodeList.addAll(newList)
        notifyDataSetChanged()
    }
}