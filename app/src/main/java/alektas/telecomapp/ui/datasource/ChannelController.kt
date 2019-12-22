package alektas.telecomapp.ui.datasource

import alektas.telecomapp.domain.entities.Channel

interface ChannelController {
    fun showChannelDetails(channel: Channel)
    fun removeChannel(channel: Channel)
}