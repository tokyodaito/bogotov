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

    private var _currentKeyword = ""

    private var updateList: List<FilmItem> = listOf()

    var showBack = false

    val currentKeyword: String
        get() = _currentKeyword

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
                    if (updateList.isEmpty())
                        currentState.data + filmItems
                    else {
                        updateList + filmItems
                    }
                } else {
                    filmItems
                }
                _films.postValue(DataState.Success(newItems))
                updateList = listOf()
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
            .andThen(Single.fromCallable { !filmItem.favorite })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ isNowFavorite ->
                if (_films.value is DataState.Success<*>) {
                    val currentList = (_films.value as DataState.Success<List<FilmItem>>).data
                    val updatedList = currentList.map { item ->
                        if (item.film.filmId == filmItem.film.filmId) item.copy(favorite = isNowFavorite) else item
                    }
                    updateList = updatedList
                }
            }, { error ->
                Log.e("PopularViewModel", "Error updating favorite status: ", error)
            })

        compositeDisposable.add(disposable)
    }

    fun searchFilmsByKeyword(keyword: String, isNextPage: Boolean = false) {
        if (keyword.isBlank() && !isNextPage) {
            return
        }

        if (!isNextPage) {
            currentPage = 1
            _currentKeyword = keyword
            _films.postValue(DataState.Loading)
        } else {
            if (currentPage >= totalPages) return
            currentPage++
        }

        val disposable = filmRepository.searchFilmsByKeyword(currentKeyword, currentPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                totalPages = response.pagesCount
                val filmItems = response.films.map {
                    FilmItem(
                        it,
                        false
                    )
                }
                val currentState = _films.value
                val newItems = if (isNextPage && currentState is DataState.Success) {
                    currentState.data + filmItems
                } else {
                    filmItems
                }
                if (newItems.isNotEmpty())
                    _films.postValue(DataState.Success(newItems))
                else
                    _films.postValue(DataState.NotFound)
            }, { error ->
                if (!isNextPage) {
                    _films.postValue(DataState.Error(error))
                }
            })

        compositeDisposable.add(disposable)
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}
