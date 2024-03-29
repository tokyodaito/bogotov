package com.bogsnebes.tinkofffintech.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bogsnebes.tinkofffintech.R
import com.bogsnebes.tinkofffintech.databinding.ActivityMainBinding
import com.bogsnebes.tinkofffintech.ui.favourites.FavouritesFragment
import com.bogsnebes.tinkofffintech.ui.information.InformationFragment
import com.bogsnebes.tinkofffintech.ui.popular.PopularFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            openPopularFragment()
        }

        setupLandscapeListener()
        setupButtonOpenPopular()
        setupButtonOpenFavourites()
    }

    private fun openPopularFragment() {
        showProgressBar(true)
        if (isLandscape())
            if (existInFragmentContainerViewTag2())
                supportFragmentManager.beginTransaction()
                    .remove(supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag2) as InformationFragment)
                    .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view_tag, PopularFragment.newInstance())
            .commitAllowingStateLoss()
    }

    private fun openFavouritesFragment() {
        showProgressBar(true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view_tag, FavouritesFragment.newInstance())
            .commitAllowingStateLoss()
    }

    private fun setupButtonOpenPopular() {
        binding.appCompatButton2.setOnClickListener {
            if (supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag) !is PopularFragment) {
                if (isLandscape())
                    if (existInFragmentContainerViewTag2()) {
                        supportFragmentManager.beginTransaction()
                            .remove(supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag2) as InformationFragment)
                            .commitNow()
                        setupLandscapeListener()
                    }
                openPopularFragment()
            }
        }
    }

    private fun setupButtonOpenFavourites() {
        binding.appCompatButton.setOnClickListener {
            if (supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag) !is FavouritesFragment) {
                if (isLandscape())
                    if (existInFragmentContainerViewTag2()) {
                        supportFragmentManager.beginTransaction()
                            .remove(supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag2) as InformationFragment)
                            .commitNow()
                        setupLandscapeListener()
                    }
                openFavouritesFragment()
            }
        }
    }

    fun showProgressBar(show: Boolean) {
        binding.fragmentContainerViewTag.visibility = if (show) View.GONE else View.VISIBLE
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun showBottomNavigation(show: Boolean) {
        binding.bottomNavigation.visibility = if (show) View.VISIBLE else View.GONE
        binding.guideline.setGuidelinePercent(if (show) 0.92f else 1f)
    }

    fun setupLandscapeListener() {
        if (isLandscape()) {
            val fragmentInContainer1 =
                supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag)
            if (fragmentInContainer1 is InformationFragment) {
                openPopularFragment()
            }
            if (existInFragmentContainerViewTag2()) {
                binding.fragmentContainerViewTag2?.visibility = View.VISIBLE
            } else {
                binding.fragmentContainerViewTag2?.visibility = View.GONE
            }
        }
    }

    private fun existInFragmentContainerViewTag2(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag2) != null
    }

    private fun isLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}