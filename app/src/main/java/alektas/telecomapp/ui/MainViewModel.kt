package alektas.telecomapp.ui

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.processes.ProcessState
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
    val processState = MutableLiveData<ProcessState>()
    val disposable = CompositeDisposable()

    init {
        App.component.inject(this)

        disposable.addAll(
            processor.characteristicsProcessState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    processState.value = it
                },

            storage.observeTransmittingState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    processState.value = it
                }
            )
    }

    fun cancelCurrentProcess() {
        processor.cancelCurrentProcess()
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
