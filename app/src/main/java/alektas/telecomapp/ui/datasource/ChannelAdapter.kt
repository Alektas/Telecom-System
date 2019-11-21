package alektas.telecomapp.ui.datasource

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.ChannelData
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_channel.view.*

class ChannelAdapter(private val controller: ChannelController) :
    RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {
    var channels = listOf<ChannelData>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.bind(channel)
        holder.itemView.findViewById<ImageButton>(R.id.channel_del_btn).setOnClickListener {
            controller.removeChannel(channel)
        }
    }

    inner class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var channelName: TextView = view.channel_name
        var channelData: TextView = view.channel_data
        var channelCode: TextView = view.channel_code

        fun bind(channel: ChannelData) {
            channelName.text = channel.name
            channelCode.text = channel.getCodeString()

            channelData.text = if (channel.errors?.isEmpty() == true) {
                channel.getDataString()
            } else {
                withHighlightedErrors(channel)
            }
        }

        private fun withHighlightedErrors(channel: ChannelData): Spannable {
            val spanBuilder = SpannableStringBuilder()

            channel.data.forEachIndexed { i, b ->
                if (i != 0) spanBuilder.append(" ")
                val isError = channel.errors?.contains(i) ?: false
                if (isError) {
                    spanBuilder.append(
                        if (b) "1" else "0",
                        BackgroundColorSpan(Color.argb(80, 255, 0, 0)),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    spanBuilder.append(if (b) "1" else "0")
                }
            }

            return spanBuilder
        }
    }
}