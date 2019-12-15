package alektas.telecomapp.utils

import android.content.Context
import android.hardware.usb.UsbManager

class UsbDriver(context: Context) {
    private val mng: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    init {
        mng.deviceList.forEach { (n, d) ->
            println("name=$n device=$d")
        }
    }
}