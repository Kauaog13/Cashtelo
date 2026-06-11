package com.cashtelo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.cashtelo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment)

        val appBarConfig = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.transactionsFragment, R.id.reportsFragment, R.id.categoriesFragment, R.id.profileFragment)
        )
        setupActionBarWithNavController(navController, appBarConfig)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                R.id.transactionsFragment -> {
                    navController.navigate(R.id.transactionsFragment)
                    true
                }
                R.id.reportsFragment -> {
                    navController.navigate(R.id.reportsFragment)
                    true
                }
                R.id.categoriesFragment -> {
                    navController.navigate(R.id.categoriesFragment)
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in authDestinations) {
                binding.bottomNav.visibility = View.GONE
                supportActionBar?.hide()
            } else {
                binding.bottomNav.visibility = View.VISIBLE
                supportActionBar?.show()
            }
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
