package com.bogsnebes.tinkofffintech.ui.popular

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bogsnebes.tinkofffintech.model.imlp.FilmRepository
import com.bogsnebes.tinkofffintech.ui.popular.recycler.FilmItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@HiltViewModel
class PopularViewModel @Inject constructor(
    private val filmRepository: FilmRepository
) : ViewModel() {
    private val _films = MutableLiveData<DataState<List<FilmItem>>>()
    val films: LiveData<DataState<List<FilmItem>>> = _films

    private var currentPage = 1
    private var totalPages = Int.MAX_VALUE

    private val compositeDisposable = CompositeDisposable()

    init {
        loadTopFilms()
    }

    fun loadTopFilms(isNextPage: Boolean = false) {
        if (isNextPage) {
            if (currentPage >= totalPages) return
            currentPage++
        } else {
            currentPage = 1
            _films.postValue(DataState.Loading)
        }

        val disposable = filmRepository.getTopFilms(currentPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { response ->
                totalPages = response.pagesCount
                Observable.fromIterable(response.films)
                    .concatMapSingle { film ->
                        filmRepository.isFilmFavorite(film.filmId)
                            .map { isFavorite -> FilmItem(film, isFavorite) }
                    }.toList()
            }
            .subscribe({ filmItems ->
                val currentState = _films.value
                val newItems = if (isNextPage && currentState is DataState.Success) {
                    currentState.data + filmItems
                } else {
                    filmItems
                }
                _films.postValue(DataState.Success(newItems))
            }, { error ->
                if (!isNextPage) {
                    _films.postValue(DataState.Error(error))
                }
                Log.e("PopularViewModel", "Error loading films: ", error)
            })

        compositeDisposable.add(disposable)
    }

    fun toggleFavoriteStatus(filmItem: FilmItem) {
        val action = if (filmItem.favorite) {
            filmRepository.removeFilmFromFavorites(filmItem.film.filmId)
        } else {
            filmRepository.saveFilmAsFavorite(filmItem.film)
        }

        val disposable = action
            .andThen(Single.fromCallable { !filmItem.favorite }) // Просто инвертируем статус, не делая повторного запроса в БД
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ isNowFavorite ->
                if (_films.value is DataState.Success<*>) {
                    val currentList = (_films.value as DataState.Success<List<FilmItem>>).data
                    val updatedList = currentList.map { item ->
                        if (item.film.filmId == filmItem.film.filmId) item.copy(favorite = isNowFavorite) else item
                    }
                    _films.postValue(DataState.Success(updatedList))
                }
            }, { error ->
                Log.e("PopularViewModel", "Error updating favorite status: ", error)
            })

        compositeDisposable.add(disposable)
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}
