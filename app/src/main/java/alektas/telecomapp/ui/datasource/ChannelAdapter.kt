package alektas.telecomapp.ui.datasource

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.ChannelData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_data_source_channel.view.*

class ChannelAdapter(val viewModel: DataSourceViewModel) :
    RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {
    var channels = listOf<ChannelData>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_source_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.bind(channel)
        holder.itemView.findViewById<ImageButton>(R.id.channel_del_btn).setOnClickListener {
            viewModel.removeChannel(channel)
        }
    }

    inner class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var channelName: TextView = view.channel_name
        var channelData: TextView = view.channel_data
        var channelCode: TextView = view.channel_code

        fun bind(channel: ChannelData) {
            channelName.text = channel.name
            channelData.text = channel.getDataString()
            channelCode.text = channel.getCodeString()
        }
    }
}