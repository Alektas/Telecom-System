package alektas.telecomapp.ui.datasource

import alektas.telecomapp.domain.entities.ChannelData

interface ChannelController {
    fun showChannelDetails(channel: ChannelData)
    fun removeChannel(channel: ChannelData)
}