package alektas.telecomapp

import alektas.telecomapp.ui.chart.ChartFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import alektas.telecomapp.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ChartFragment.newInstance())
                .commitNow()
        }
    }

}
