package alektas.telecomapp.ui

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class MainViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    val processProgress = MutableLiveData<Int>()
    val disposable = CompositeDisposable()

    init {
        App.component.inject(this)

        disposable.addAll(
            processor.characteristicsProcess
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    processProgress.value = it
                },

            storage.observeTransmitProcess()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    processProgress.value = it
                }
            )
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
