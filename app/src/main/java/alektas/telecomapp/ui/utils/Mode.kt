package alektas.telecomapp.ui.utils

sealed class Mode
class DataCoding(val isEnabled: Boolean) : Mode()
class ChannelsAutoDetection(val isEnabled: Boolean) : Mode()