package com.bluebird.zbar.sample.history

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluebird.zbar.sample.R
import com.bluebird.zbar.sample.databinding.FragmentHistoryBinding
import com.bluebird.zbar.sample.history.BarcodeAdapter
import com.bluebird.zbar.sample.roomdb.BarcodeDatabase
import com.bluebird.zbar.sample.roomdb.BarcodeRepository
import com.bluebird.zbar.sample.roomdb.BarcodeViewModel
import com.bluebird.zbar.sample.roomdb.ViewModelFactory


class HistoryFragment : Fragment() {
    private lateinit var mBinding: FragmentHistoryBinding
    private lateinit var viewModel: BarcodeViewModel
    private lateinit var mContext: Context
    private lateinit var adapter: BarcodeAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentHistoryBinding.inflate(inflater,container,false)
        mContext = inflater.context

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val database = BarcodeDatabase.getDatabase(mContext)
        val repository = BarcodeRepository(database.barcodeDao())
        adapter = BarcodeAdapter()
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(repository)
        )[BarcodeViewModel::class.java]
        val recyclerView: RecyclerView = mBinding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(mContext)
        recyclerView.adapter = adapter

    }

    override fun onStart() {
        super.onStart()
        viewModel.barcodes.observe(viewLifecycleOwner) { barcodes ->
            Log.d("TAG", "onStart: Changed")
            adapter.setBarcodes(barcodes.reversed())
        }
        mBinding.buttonClear.setOnClickListener{
            viewModel.deleteAllBarcodes()
            viewModel.fetchBarcodes()
        }
        // Fetch barcodes on fragment load
        viewModel.fetchBarcodes()
    }

}