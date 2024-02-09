package com.bogsnebes.tinkofffintech.ui.popular.recycler

import androidx.recyclerview.widget.DiffUtil
import com.bogsnebes.tinkofffintech.ui.favourites.recycler.FilmItem

object FilmDiffCallback : DiffUtil.ItemCallback<FilmItem>() {
    override fun areItemsTheSame(oldItem: FilmItem, newItem: FilmItem): Boolean {
        return oldItem.film.filmId == newItem.film.filmId
    }

    override fun areContentsTheSame(oldItem: FilmItem, newItem: FilmItem): Boolean {
        return oldItem == newItem
    }
}