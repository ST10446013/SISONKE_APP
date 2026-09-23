package com.example.sisonke_app.nav

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.example.sisonke_app.dashboard.DashboardActivity
import com.example.sisonke_app.emergency.EmergencyActivity
import com.example.sisonke_app.map.CrimeMapActivity
import com.example.sisonke_app.municipal.MunicipalActivity
import com.example.sisonke_app.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Wires up the shared bottom navigation bar for any activity whose layout
 * includes a BottomNavigationView with id @id/bottomNav and the
 * @menu/bottom_nav_menu menu.
 *
 * Each destination activity is declared launchMode="singleTop" in the
 * manifest, and we navigate with FLAG_ACTIVITY_CLEAR_TOP so switching tabs
 * doesn't pile up duplicate activities on the back stack.
 */
object BottomNavHelper {

    fun setup(activity: AppCompatActivity, selectedItemId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
            ?: return

        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->

            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val targetClass = when (item.itemId) {
                R.id.nav_home -> DashboardActivity::class.java
                R.id.nav_map -> CrimeMapActivity::class.java
                R.id.nav_emergency -> EmergencyActivity::class.java
                R.id.nav_municipal -> MunicipalActivity::class.java
                R.id.nav_settings -> SettingsActivity::class.java
                else -> null
            }

            if (targetClass != null) {
                val intent = Intent(activity, targetClass)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }

            true
        }
    }
}