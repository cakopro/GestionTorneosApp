package com.cakocorps.administraciondetorneroselectronicos

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TorneoPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DetalleTorneoFragment()
            1 -> ParticipantesFragment()
            2 -> CrucesFragment()
            3 -> Vistapodioo()
            else -> DetalleTorneoFragment()
        }
    }
}