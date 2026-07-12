package com.simplestsoft.twostrokecalc.ui.portarea

import androidx.lifecycle.ViewModel
import com.simplestsoft.twostrokecalc.data.session.CalculatorSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PortAreaCalculatorViewModel @Inject constructor(
    val session: CalculatorSession,
) : ViewModel()
