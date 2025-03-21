package application.appsku.barcodesharing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import application.appsku.barcodesharing.databinding.ItemDbBinding

class ItemDbAdapter : RecyclerView.Adapter<ItemDbAdapter.ItemDbViewHolder>() {

    private val list = mutableListOf<String>()
    fun addData(data: List<String>) {
        list.clear()
        list.addAll(data.distinct())
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemDbViewHolder {
       return ItemDbViewHolder(ItemDbBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ItemDbViewHolder, position: Int) {
        holder.bind(list[position])
    }

    inner class ItemDbViewHolder (private val binding : ItemDbBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: String) {
            binding.run {
                txtItem.text = data
            }
        }
    }
}