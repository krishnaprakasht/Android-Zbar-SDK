package com.bluebird.zbar.sample.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bluebird.zbar.sample.databinding.ItemBarcodeBinding
import com.bluebird.zbar.sample.roomdb.Barcode

class BarcodeAdapter : RecyclerView.Adapter<BarcodeAdapter.BarcodeViewHolder>() {

    private var barcodeList = listOf<Barcode>()
    private var isCont = false
    private var timeStampTemp = ""

    fun setBarcodes(barcodes: List<Barcode>) {
        this.barcodeList = barcodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBarcodeBinding.inflate(inflater, parent, false)
        return BarcodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        val barcode = barcodeList[position]
        holder.bind(barcode,position)
    }

    override fun getItemCount() = barcodeList.size

    inner class BarcodeViewHolder(private val binding: ItemBarcodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barcode: Barcode, position: Int) {
            binding.textViewMode.text =
                if (barcode.mode == 0) "(Single)" else "(Continuous)"
            binding.textViewBarcode.text = "${barcode.barcode}"
            binding.textViewSymbol.text = "${barcode.symbol}"
            binding.textViewDataTime.text = "${barcode.timeStamp}"

            isCont = if (position > 0) {
                val previousItem = barcodeList[position - 1]
                previousItem.timeStamp == barcode.timeStamp
            } else {
                false
            }

            timeStampTemp = barcode.timeStamp
            if (barcode.mode == 0) {
                // Single Mode
                binding.textViewCount.visibility = View.GONE
                binding.textViewCountLabel.visibility = View.GONE
                binding.topBorder2.visibility = View.GONE
                binding.topBorder1.visibility = View.VISIBLE
            } else {
                // Continuous Mode
                if(isCont) {
                    binding.topBorder2.visibility = View.VISIBLE
                    binding.topBorder1.visibility = View.GONE
                }
                else{
                    binding.topBorder2.visibility = View.GONE
                    binding.topBorder1.visibility = View.VISIBLE
                }
                binding.textViewCount.visibility = View.VISIBLE
                binding.textViewCountLabel.visibility = View.VISIBLE
                binding.textViewCount.text = "${barcode.count}"

            }
        }
    }
}
