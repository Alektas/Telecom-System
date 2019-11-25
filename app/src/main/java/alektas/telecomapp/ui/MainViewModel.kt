package alektas.telecomapp.ui

import alektas.telecomapp.App
import alektas.telecomapp.domain.entities.SystemProcessor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class MainViewModel : ViewModel() {
    @Inject
    lateinit var processor: SystemProcessor
    val berProgress = MutableLiveData<Int>()
    val disposable = CompositeDisposable()

    init {
        App.component.inject(this)

        disposable.add(
            processor.berProcess
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                berProgress.value = it
            }
        )
    }
}
